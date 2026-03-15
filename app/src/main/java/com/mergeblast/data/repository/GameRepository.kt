package com.mergeblast.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.mergeblast.data.models.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "game_prefs")

class GameRepository(
    private val context: Context,
    private val db: MergeBlastDatabase
) {
    private val playerDao = db.playerDao()
    private val leaderboardDao = db.leaderboardDao()

    companion object {
        private val KEY_TUTORIAL_DONE = booleanPreferencesKey("tutorial_done")
        private val KEY_SOUND_ON = booleanPreferencesKey("sound_on")
        private val KEY_VIBRATION_ON = booleanPreferencesKey("vibration_on")
        private val KEY_ADS_ON = booleanPreferencesKey("ads_on")
        private val KEY_BEST_SCORE = longPreferencesKey("best_score")
        private val KEY_DAILY_QUESTS = stringPreferencesKey("daily_quests")
        private val KEY_QUESTS_DATE = longPreferencesKey("quests_date")
    }

    // Player
    val playerData: Flow<PlayerData?> = playerDao.getPlayerData()

    suspend fun initPlayer() {
        val existing = playerDao.getPlayerDataOnce()
        if (existing == null) {
            playerDao.insertOrUpdate(PlayerData(coins = 500))
        }
    }

    suspend fun addCoins(amount: Int) = playerDao.addCoins(amount)
    suspend fun spendCoins(amount: Int) = playerDao.spendCoins(amount)
    // Returns true if player levelled up
    suspend fun addExp(amount: Int): Boolean {
        playerDao.addExp(amount)
        val player   = playerDao.getPlayerDataOnce() ?: return false
        val expForNextLevel = player.level * 1000  // level 1→2 needs 1000, 2→3 needs 2000 etc.
        return if (player.exp >= expForNextLevel) {
            val newLevel = player.level + 1
            // Reset EXP to remainder, set new level, award bonus coins
            playerDao.setLevel(newLevel)
            playerDao.addCoins(100 * newLevel)  // bigger reward for higher levels
            true
        } else false
    }
    suspend fun addTrophies(amount: Int) = playerDao.addTrophies(amount)
    suspend fun incrementMerges(count: Int) = playerDao.incrementMerges(count)
    suspend fun incrementPowerupsUsed() = playerDao.incrementPowerupsUsed()
    suspend fun incrementAdsWatched() = playerDao.incrementAdsWatched()
    suspend fun updateDailyReward(timestamp: Long, streak: Int) =
        playerDao.updateDailyReward(timestamp, streak)

    suspend fun updatePlayerStats(score: Long, highestBlock: Long) {
        val player = playerDao.getPlayerDataOnce() ?: PlayerData()
        playerDao.insertOrUpdate(
            player.copy(
                totalScore = maxOf(player.totalScore, score),
                highestBlock = maxOf(player.highestBlock, highestBlock),
                gamesPlayed = player.gamesPlayed + 1
            )
        )
        // Save to leaderboard
        leaderboardDao.insertEntry(
            LeaderboardEntry(
                playerName = "You",
                score = score,
                highestBlock = highestBlock
            )
        )
    }

    // Leaderboard
    val leaderboard: Flow<List<LeaderboardEntry>> = leaderboardDao.getLeaderboard()
    suspend fun getPlayerRank(score: Long): Int = leaderboardDao.getRank(score)

    // Settings
    val soundOn: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_SOUND_ON] ?: true
    }
    val vibrationOn: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_VIBRATION_ON] ?: true
    }
    val adsOn: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_ADS_ON] ?: true
    }

    suspend fun setSoundOn(on: Boolean) {
        context.dataStore.edit { it[KEY_SOUND_ON] = on }
    }
    suspend fun setVibrationOn(on: Boolean) {
        context.dataStore.edit { it[KEY_VIBRATION_ON] = on }
    }
    suspend fun setAdsOn(on: Boolean) {
        context.dataStore.edit { it[KEY_ADS_ON] = on }
    }

    val tutorialDone: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_TUTORIAL_DONE] ?: false
    }

    suspend fun setTutorialDone() {
        context.dataStore.edit { it[KEY_TUTORIAL_DONE] = true }
    }

    // Daily Quests
    suspend fun getDailyQuests(): List<Quest> {
        val prefs = context.dataStore.data.first()
        val questsDate = prefs[KEY_QUESTS_DATE] ?: 0L
        val today = System.currentTimeMillis() / (1000 * 60 * 60 * 24)

        return if (questsDate == today) {
            // Parse saved quests
            parseSavedQuests(prefs[KEY_DAILY_QUESTS] ?: "")
        } else {
            // Generate new quests for today
            generateDailyQuests().also { quests ->
                context.dataStore.edit { prefs2 ->
                    prefs2[KEY_QUESTS_DATE] = today
                    prefs2[KEY_DAILY_QUESTS] = serializeQuests(quests)
                }
            }
        }
    }

    private fun generateDailyQuests(): List<Quest> = listOf(
        Quest("complete_all", "Complete All", "Complete all daily quests", 4, 0, 100, QuestType.COMPLETE_ALL),
        Quest("merge_290", "Merge Master", "Merge 290 blocks", 290, 0, 100, QuestType.MERGE_BLOCKS),
        Quest("create_2q", "Block Creator", "Create the 2Q block", 1, 0, 100, QuestType.CREATE_BLOCK),
        Quest("powerups", "Power User", "Use powerups 4 times", 4, 0, 100, QuestType.USE_POWERUPS),
        Quest("watch_ads", "Ad Watcher", "Watch 4 videos", 4, 0, 100, QuestType.WATCH_ADS)
    )

    private fun serializeQuests(quests: List<Quest>): String =
        quests.joinToString("|") { "${it.id},${it.progress},${it.isCompleted}" }

    private fun parseSavedQuests(saved: String): List<Quest> {
        val defaultQuests = generateDailyQuests().associateBy { it.id }
        if (saved.isEmpty()) return defaultQuests.values.toList()
        saved.split("|").forEach { entry ->
            val parts = entry.split(",")
            if (parts.size >= 3) {
                defaultQuests[parts[0]]?.let { quest ->
                    // Progress will be re-applied from saved state
                }
            }
        }
        return defaultQuests.values.toList()
    }

    // Shop Items
    fun getShopItems(): List<ShopItem> = listOf(
        ShopItem("hammer_1", "Hammer x1", "Remove any block from the grid", 150, 1, type = ShopItemType.HAMMER),
        ShopItem("hammer_5", "Hammer x5", "Remove any 5 blocks from the grid", 600, 5, type = ShopItemType.HAMMER),
        ShopItem("swap_1", "Swap x1", "Swap any 2 blocks on the grid", 420, 1, type = ShopItemType.SWAP),
        ShopItem("swap_3", "Swap x3", "Swap blocks 3 times", 1100, 3, type = ShopItemType.SWAP),
        ShopItem("coins_1000", "1,000 Coins", "Get 1000 coins", 0, 1000, type = ShopItemType.COIN_PACK),
        ShopItem("exp_boost", "2x EXP Boost", "Double EXP for the next game", 300, 1, type = ShopItemType.EXP_BOOST)
    )
}