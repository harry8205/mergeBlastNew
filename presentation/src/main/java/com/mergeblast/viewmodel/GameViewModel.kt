package com.mergeblast.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mergeblast.data.models.*
import com.mergeblast.data.repository.GameRepository
import com.mergeblast.utils.GameEngine
import com.mergeblast.utils.SoundManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class GameViewModel(val repository: GameRepository, private val soundManager: SoundManager) : ViewModel() {

    // ── Game State ────────────────────────────────────────────────────────────
    private val _gameState = MutableStateFlow(GameEngine.initialState())
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    private val _events = MutableSharedFlow<GameEvent>()
    val events: SharedFlow<GameEvent> = _events.asSharedFlow()

    // Queue of 5 upcoming blocks: index 0 = next to drop, index 1..4 = upcoming
    private val _blockQueue = MutableStateFlow<List<Block>>(
        GameEngine.generateQueue(5, 2L).toList()
    )
    val blockQueue: StateFlow<List<Block>> = _blockQueue.asStateFlow()

    // Convenience accessors derived from blockQueue (auto-update when queue changes)
    val nextBlock: Flow<Block> = _blockQueue.map { it.first() }
    //val queuedBlock: Flow<Block> = _blockQueue.map { it.getOrElse(1) { it.first() } }

    private val _selectedColumn = MutableStateFlow(-1)
    val selectedColumn: StateFlow<Int> = _selectedColumn.asStateFlow()

    private val _hammerMode = MutableStateFlow(false)
    val hammerMode: StateFlow<Boolean> = _hammerMode.asStateFlow()

    private val _hammerTarget = MutableStateFlow<Pair<Int, Int>?>(null)
    val hammerTarget = _hammerTarget.asStateFlow()
    private var hammerJob: kotlinx.coroutines.Job? = null

    private val _swapMode = MutableStateFlow(false)
    val swapMode: StateFlow<Boolean> = _swapMode.asStateFlow()

    // First cell selected for swap — Pair(row, col), null if none selected yet
    private val _swapFirst = MutableStateFlow<Pair<Int,Int>?>(null)
    val swapFirst: StateFlow<Pair<Int,Int>?> = _swapFirst.asStateFlow()

    // ── Player & Persistent Data ──────────────────────────────────────────────
    val playerData: Flow<PlayerData?> = repository.playerData
    val purchases = repository.purchases

    fun grantPurchase(token: String, productId: String, time: Long, quantity: Int, done: () -> Unit) {
        viewModelScope.launch { repository.grantPurchase(token, productId, time, quantity); done() }
    }
    fun syncPurchases(products: List<String>, owned: List<PurchaseGrant>, done: () -> Unit) {
        viewModelScope.launch { repository.syncOwnership(products, owned); done() }
    }
    val leaderboard: Flow<List<LeaderboardEntry>> = repository.leaderboard

    init {
        viewModelScope.launch { repository.initPlayer() }
        prepareNextBlock()
    }

    // ── Animation gate ────────────────────────────────────────────────────────
    private val _isAnimating = MutableStateFlow(false)
    val isAnimating: StateFlow<Boolean> = _isAnimating.asStateFlow()

    // Frame delays tuned for satisfying visual feedback
    private val FRAME_PLACE_MS      = 220L  // block lands + bounce completes
    private val FRAME_PRE_MERGE_MS  = 200L  // all participating blocks glow — user sees what will merge
    private val FRAME_POST_MERGE_MS = 250L  // merged result flashes + pulses
    private val FRAME_GRAVITY_MS    = 150L  // board settles

    // ── Game Actions ──────────────────────────────────────────────────────────
    fun selectColumn(col: Int) {
        if (!_isAnimating.value) _selectedColumn.value = col
    }

    fun dropBlock(col: Int) {
        if (_isAnimating.value) return
        val state = _gameState.value
        val block = _blockQueue.value.first()
        val (frames, result) = GameEngine.dropBlockFrames(state, col, block)

        viewModelScope.launch {
            _isAnimating.value = true
            var mergeLevel = 0  // tracks cascade depth for escalating sounds

            for (frame in frames) {
                _gameState.value = _gameState.value.copy(
                    grid         = frame.grid,
                    score        = frame.score,
                    highestBlock = frame.highestBlock
                )
                // ── Play sound for every frame ────────────────────────────────
                when (frame.type) {

                    GameEngine.FrameType.PLACE -> {
                        // Check if this drop will trigger any merges
                        // If next frame is PRE_MERGE we play blast, else plain drop
                        val nextFrame = frames.getOrNull(frames.indexOf(frame) + 1)
                        if (nextFrame?.type == GameEngine.FrameType.PRE_MERGE) {
                            soundManager.playBlast()   // lands on matching neighbour!
                        } else {
                            soundManager.playDrop()    // plain placement, no match
                        }
                    }

                    GameEngine.FrameType.PRE_MERGE -> {
                        // All matching blocks are highlighted — play a short
                        // anticipation "tick" so user hears something is about to happen
                        soundManager.playButton()
                    }

                    GameEngine.FrameType.POST_MERGE -> {
                        // Escalating merge sound per cascade level
                        mergeLevel++
                        when (mergeLevel) {
                            1    -> soundManager.playMerge()        // 1st merge — ding
                            2    -> soundManager.playDoubleMerge()  // 2nd cascade — chime
                            else -> soundManager.playTripleMerge()  // 3rd+ — chord
                        }
                    }

                    GameEngine.FrameType.GRAVITY -> {
                        // Silent settle — no sound needed
                    }
                }
                val delay = when (frame.type) {
                    GameEngine.FrameType.PLACE      -> FRAME_PLACE_MS
                    GameEngine.FrameType.PRE_MERGE  -> FRAME_PRE_MERGE_MS
                    GameEngine.FrameType.POST_MERGE -> FRAME_POST_MERGE_MS
                    GameEngine.FrameType.GRAVITY    -> FRAME_GRAVITY_MS
                }
                kotlinx.coroutines.delay(delay)
            }

            _gameState.value = result.newState
            _isAnimating.value = false

            if (result.mergesCount > 0) {
                repository.incrementMerges(result.mergesCount)
                _events.emit(GameEvent.Merge(result.mergesCount))
                if (result.mergesCount >= 3) {
                    soundManager.playCombo()
                    _events.emit(GameEvent.Combo(result.mergesCount))
                }
                // Award EXP for merges: scoreGained / 100, minimum 1 per merge
                val expGained = (result.scoreGained / 100L).toInt().coerceAtLeast(result.mergesCount)
                val leveledUp = repository.addExp(expGained)
                if (leveledUp) {
                    val player = repository.playerData.first()
                    val level  = player?.level ?: 1
                    val bonus  = 100 * level
                    _events.emit(GameEvent.LevelUp(level, bonus))
                }
            }
            if (result.isGameOver) {
                soundManager.playGameOver()
                endGame()
            } else {
                prepareNextBlock()
                _selectedColumn.value = -1
                _events.emit(GameEvent.MoveCompleted)
            }
        }
    }

    fun toggleHammerMode() {
        if (_isAnimating.value) return
        soundManager.playButton()
        _hammerMode.value = !_hammerMode.value
        if (_hammerMode.value) { _swapMode.value = false; _swapFirst.value = null }
    }

    fun useHammer(row: Int, col: Int) {
        if (_isAnimating.value || !_hammerMode.value ||
            _gameState.value.grid.getOrNull(row)?.getOrNull(col) == null) return
        _isAnimating.value = true
        hammerJob = viewModelScope.launch {
            try {
            if (repository.payForPowerUp(PowerUpType.HAMMER)) {
                repository.incrementPowerupsUsed()
                _hammerMode.value = false
                _hammerTarget.value = row to col
                kotlinx.coroutines.delay(260)
                soundManager.playHammerBreak()
                _gameState.value = GameEngine.useHammer(_gameState.value, row, col)
                kotlinx.coroutines.delay(320)
                _events.emit(GameEvent.PowerUpUsed(PowerUpType.HAMMER))
            } else _events.emit(GameEvent.NotEnoughCoins)
            } finally {
                _hammerTarget.value = null
                _isAnimating.value = false
            }
        }
    }

    fun toggleSwapMode() {
        if (_isAnimating.value) return
        soundManager.playButton()
        _swapMode.value = !_swapMode.value
        _swapFirst.value = null
        if (_swapMode.value) _hammerMode.value = false
    }

    // Called when user taps a cell while swap mode is active
    fun selectSwapCell(row: Int, col: Int) {
        if (_isAnimating.value) return
        val first = _swapFirst.value
        if (first == null) {
            // First cell selected — highlight it
            _swapFirst.value = Pair(row, col)
        } else if (first.first == row && first.second == col) {
            // Tapped same cell again — deselect
            _swapFirst.value = null
        } else {
            // Second cell selected — execute swap
            _isAnimating.value = true
            viewModelScope.launch {
                try {
                if (repository.payForPowerUp(PowerUpType.SWAP)) {
                    repository.incrementPowerupsUsed()
                    _gameState.value = GameEngine.swapBlocks(
                        _gameState.value,
                        first.first, first.second,
                        row, col
                    )
                    _events.emit(GameEvent.PowerUpUsed(PowerUpType.SWAP))
                } else {
                    _events.emit(GameEvent.NotEnoughCoins)
                }
                _swapMode.value = false
                _swapFirst.value = null
                } finally { _isAnimating.value = false }
            }
        }
    }

    fun restartGame() {
        hammerJob?.cancel()
        _hammerTarget.value = null
        _isAnimating.value = false
        _gameState.value = GameEngine.initialState()
        _hammerMode.value = false
        _swapMode.value = false
        _swapFirst.value = null
        _blockQueue.value = GameEngine.generateQueue(5, 2L).toList()
    }

    // ── Shop ──────────────────────────────────────────────────────────────────
    fun getShopItems(): List<ShopItem> = repository.getShopItems()

    fun purchaseItem(item: ShopItem) {
        viewModelScope.launch {
            val player = repository.playerData.first() ?: return@launch
            if (player.coins >= item.coinCost) {
                repository.spendCoins(item.coinCost)
                _events.emit(GameEvent.ItemPurchased(item))
            } else _events.emit(GameEvent.NotEnoughCoins)
        }
    }

    // ── Quests ────────────────────────────────────────────────────────────────
    suspend fun getDailyQuests(): List<Quest> = repository.getDailyQuests()

    // ── Ads / Rewards ─────────────────────────────────────────────────────────
    fun addCoinsFromAd(amount: Int) {
        viewModelScope.launch {
            repository.addCoins(amount)
            repository.incrementAdsWatched()
            _events.emit(GameEvent.CoinsAdded(amount))
        }
    }

    fun setSoundEnabled(enabled: Boolean) { soundManager.soundEnabled = enabled }
    fun setVibrationEnabled(enabled: Boolean) { soundManager.vibrationEnabled = enabled }

    val tutorialDone: Flow<Boolean> = repository.tutorialDone

    fun completeTutorial() {
        viewModelScope.launch { repository.setTutorialDone() }
    }

    fun claimDailyReward() {
        viewModelScope.launch {
            val player   = repository.playerData.first() ?: return@launch
            val now      = System.currentTimeMillis()
            val daysSince = (now - player.lastDailyReward) / (24 * 60 * 60 * 1000L)
            val streak   = if (daysSince <= 1) player.dailyRewardStreak + 1 else 1
            val reward   = when (streak % 5) { 1->100; 2->200; 3->350; 4->450; 0->550; else->100 }
            val vip = repository.purchases.first().any { it.active && it.productId == "vip_monthly" }
            repository.addCoins(if (vip) reward * 2 + 100 else reward)
            if (vip) repository.grantItems(0, 1, 1)
            repository.updateDailyReward(now, streak)
            _events.emit(GameEvent.DailyRewardClaimed(reward, streak))
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private fun prepareNextBlock() {
        val highest = _gameState.value.highestBlock.coerceAtLeast(2L)
        // Shift queue: drop used block, append a freshly-scaled one
        val newQueue = _blockQueue.value.drop(1).toMutableList()
        newQueue.add(GameEngine.generateNextBlock(highest))

        // If the board has grown significantly, regenerate any queue items
        // that are now below the new floor (they'd be useless to drop)
        val rawFloor = (highest / 64L).coerceAtLeast(2L)
        val refreshed = newQueue.map { block ->
            if (block.value < rawFloor) GameEngine.generateNextBlock(highest) else block
        }
        _blockQueue.value = refreshed
    }

    private suspend fun endGame() {
        val state = _gameState.value
        repository.updatePlayerStats(state.score, state.highestBlock)
        // Trophies based on highest block reached
        val trophies = when {
            state.highestBlock >= 1_000_000_000_000L -> 50
            state.highestBlock >= 1_000_000_000L     -> 20
            state.highestBlock >= 1_000_000L         -> 10
            state.highestBlock >= 1_000L             -> 5
            else                                     -> 1
        }
        repository.addTrophies(trophies)
        // EXP already awarded per-merge during the game — no double award here
        _events.emit(GameEvent.GameOver(state.score, state.highestBlock))
    }
}

sealed class GameEvent {
    object MoveCompleted : GameEvent()
    data class Merge(val count: Int)                               : GameEvent()
    data class Combo(val count: Int)                               : GameEvent()
    data class GameOver(val score: Long, val highest: Long)        : GameEvent()
    data class PowerUpUsed(val type: PowerUpType)                  : GameEvent()
    data class ItemPurchased(val item: ShopItem)                   : GameEvent()
    data class CoinsAdded(val amount: Int)                         : GameEvent()
    data class DailyRewardClaimed(val amount: Int, val streak: Int): GameEvent()
    data class LevelUp(val newLevel: Int, val coinsRewarded: Int)  : GameEvent()
    object NotEnoughCoins                                          : GameEvent()
}

class GameViewModelFactory(private val repo: GameRepository, private val soundManager: SoundManager) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GameViewModel::class.java)) return GameViewModel(repo, soundManager) as T
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
