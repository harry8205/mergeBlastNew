package com.mergeblast.ui.game

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import com.mergeblast.data.models.*
import com.mergeblast.data.models.GameState.Companion.COLS
import com.mergeblast.data.models.GameState.Companion.ROWS
import com.mergeblast.viewmodel.GameEvent
import com.mergeblast.viewmodel.GameViewModel
import kotlinx.coroutines.flow.collectLatest

// ─── Block Color Palette ──────────────────────────────────────────────────────
val BLOCK_COLORS = listOf(
    Color(0xFF7B5EA7), // 1B  - Purple
    Color(0xFF4ECDC4), // 2B  - Teal
    Color(0xFF95C757), // 4B  - Green
    Color(0xFFFF6B6B), // 8B  - Red
    Color(0xFFFF9F1C), // 16B - Orange
    Color(0xFF2196F3), // 32B - Blue
    Color(0xFFE91E8C), // 64B - Pink
    Color(0xFF00BCD4), // 128B- Cyan
    Color(0xFFFFC107), // 256B- Amber
    Color(0xFF9C27B0), // 512B- Deep Purple
    Color(0xFF4CAF50), // 1KB - Lime
    Color(0xFFF44336), // 2KB - Deep Red
)

// ─── Game Screen ──────────────────────────────────────────────────────────────
@Composable
fun GameScreen(
    viewModel: GameViewModel,
    onBack: () -> Unit
) {
    val gameState by viewModel.gameState.collectAsState()
    val blockQueue by viewModel.blockQueue.collectAsState()
    val playerData by viewModel.playerData.collectAsState(initial = null)
    val selectedCol by viewModel.selectedColumn.collectAsState()
    val hammerMode by viewModel.hammerMode.collectAsState()
    val swapMode by viewModel.swapMode.collectAsState()
    val swapFirst by viewModel.swapFirst.collectAsState()
    val isAnimating by viewModel.isAnimating.collectAsState()
    val haptic = LocalHapticFeedback.current

    var showGameOver by remember { mutableStateOf(false) }
    var gameOverScore by remember { mutableStateOf(0L) }
    var gameOverHighest by remember { mutableStateOf(0L) }
    var showComboText by remember { mutableStateOf("") }
    var showPauseMenu by remember { mutableStateOf(false) }
    var musicOn by remember { mutableStateOf(true) }
    var vibrateOn by remember { mutableStateOf(true) }
    var levelUpText by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is GameEvent.GameOver -> {
                    gameOverScore = event.score
                    gameOverHighest = event.highest
                    showGameOver = true
                }
                is GameEvent.Combo -> {
                    showComboText = "COMBO x${event.count}!"
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
                is GameEvent.Merge -> {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
                is GameEvent.LevelUp -> {
                    levelUpText = "LEVEL ${event.newLevel}! +${event.coinsRewarded} coins"
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
                else -> {}
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1A1035),
                        Color(0xFF0D1B3E),
                        Color(0xFF0A2545)
                    )
                )
            )
    ) {
        // Background stars
        StarfieldBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .displayCutoutPadding()
                .padding(top = 8.dp)
        ) {
            // Top HUD
            GameHUD(
                score = gameState.score,
                coins = playerData?.coins ?: 0,
                level = playerData?.level ?: 1,
                exp = playerData?.exp ?: 0,
                expNeeded = (playerData?.level ?: 1) * 1000,
                trophies = playerData?.trophies ?: 0,
                onBack = onBack
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Game Grid — weight(1f) so it only takes leftover space
            // after HUD, preview and controls are measured
            Box(modifier = Modifier.weight(1f)) {
                GameGrid(
                    gameState = gameState,
                    selectedCol = selectedCol,
                    hammerMode = hammerMode,
                    swapMode = swapMode,
                    swapFirst = swapFirst,
                    onColumnSelected = { col ->
                        if (!hammerMode && !swapMode) viewModel.selectColumn(col)
                    },
                    onColumnTapped = { col ->
                        if (!hammerMode && !swapMode && !isAnimating) viewModel.dropBlock(col)
                    },
                    onCellTapped = { row, col ->
                        when {
                            hammerMode -> viewModel.useHammer(row, col)
                            swapMode   -> viewModel.selectSwapCell(row, col)
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Next blocks preview — BELOW the grid
            NextBlocksPreview(
                blockQueue = blockQueue,
                onPause    = { showPauseMenu = true }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Bottom Controls — always visible, never cut off
            GameControls(
                coins = playerData?.coins ?: 0,
                hammerMode = hammerMode,
                swapMode = swapMode,
                onHammerTap = { viewModel.toggleHammerMode() },
                onSwapTap   = { viewModel.toggleSwapMode() }
            )

            Spacer(modifier = Modifier.height(4.dp))
        }

        // Combo text
        if (showComboText.isNotEmpty()) {
            ComboPopup(text = showComboText) {
                showComboText = ""
            }
        }

        // Level-up popup
        if (levelUpText.isNotEmpty()) {
            LevelUpPopup(text = levelUpText) {
                levelUpText = ""
            }
        }

        // Pause / Settings overlay
        if (showPauseMenu) {
            PauseMenuDialog(
                musicOn    = musicOn,
                vibrateOn  = vibrateOn,
                onMusicToggle   = {
                    musicOn = !musicOn
                    viewModel.setSoundEnabled(musicOn)
                },
                onVibrateToggle = {
                    vibrateOn = !vibrateOn
                    viewModel.setVibrationEnabled(vibrateOn)
                },
                onRestart  = {
                    showPauseMenu = false
                    viewModel.restartGame()
                },
                onContinue = { showPauseMenu = false },
                onHome     = { showPauseMenu = false; onBack() }
            )
        }

        // Game Over overlay
        if (showGameOver) {
            GameOverScreen(
                score = gameOverScore,
                highestBlock = Block(value = gameOverHighest),
                onRestart = {
                    showGameOver = false
                    viewModel.restartGame()
                },
                onMenu = onBack
            )
        }
    }
}

// ─── Starfield Background ─────────────────────────────────────────────────────
@Composable
fun StarfieldBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "stars")
    val offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing)
        ),
        label = "starOffset"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val stars = listOf(
            Offset(50f, 80f), Offset(200f, 150f), Offset(350f, 60f),
            Offset(120f, 300f), Offset(280f, 420f), Offset(420f, 200f),
            Offset(80f, 500f), Offset(320f, 700f), Offset(180f, 900f),
            Offset(450f, 600f), Offset(60f, 750f), Offset(380f, 350f)
        )
        stars.forEach { star ->
            val alpha = ((offset + star.x / size.width) % 1f)
            drawCircle(
                color = Color.White.copy(alpha = 0.3f + 0.5f * alpha),
                radius = 1.5f + 1f * alpha,
                center = star
            )
        }
    }
}

// ─── HUD ─────────────────────────────────────────────────────────────────────
@Composable
fun GameHUD(
    score: Long,
    coins: Int,
    level: Int,
    exp: Int,
    expNeeded: Int,
    trophies: Int,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Coins
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.Black.copy(alpha = 0.4f),
                modifier = Modifier.border(1.dp, Color(0xFFFFD700).copy(alpha = 0.5f), RoundedCornerShape(20.dp))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CoinIcon(size = 20.dp)
                    Text(
                        text = coins.toString(),
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Score
            Text(
                text = formatScore(score),
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp
            )

            // Trophy / Rank — shows count + rank name + progress bar to next rank
            TrophyDisplay(trophies = trophies)
        }

        Spacer(modifier = Modifier.height(6.dp))

        // EXP Bar — animates smoothly as exp fills
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "LEVEL $level",
                    color = Color(0xFF64B5F6),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "$exp / $expNeeded XP",
                    color = Color.White.copy(alpha = 0.55f),
                    fontSize = 10.sp
                )
            }
            val animatedExp by animateFloatAsState(
                targetValue  = (exp.toFloat() / expNeeded.toFloat()).coerceIn(0f, 1f),
                animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
                label = "expBar"
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.White.copy(alpha = 0.08f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedExp)
                        .fillMaxHeight()
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFF2196F3), Color(0xFF00E5FF))
                            )
                        )
                )
                // Shine overlay
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedExp)
                        .fillMaxHeight(0.5f)
                        .align(Alignment.TopStart)
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color.White.copy(alpha = 0.20f), Color.Transparent)
                            )
                        )
                )
            }
        }
    }
}

// ─── Next Blocks Preview ─────────────────────────────────────────────────────
@Composable
fun NextBlocksPreview(blockQueue: List<Block>, onPause: () -> Unit = {}) {
    if (blockQueue.isEmpty()) return

    val current = blockQueue.getOrNull(0) ?: return
    val next    = blockQueue.getOrNull(1) ?: return

    val color1 = BLOCK_COLORS[current.colorIndex % BLOCK_COLORS.size]
    val color2 = BLOCK_COLORS[next.colorIndex % BLOCK_COLORS.size]

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        // ── Block 1 — NEXT (large, glowing, attractive) ───────────────────────
        Box(
            modifier = Modifier
                .size(54.dp)
                .shadow(
                    elevation = 12.dp,
                    shape = RoundedCornerShape(14.dp),
                    ambientColor = color1.copy(alpha = 0.6f),
                    spotColor = color1.copy(alpha = 0.8f)
                )
                .clip(RoundedCornerShape(14.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            color1,
                            color1.copy(
                                red   = (color1.red   * 0.7f).coerceIn(0f,1f),
                                green = (color1.green * 0.7f).coerceIn(0f,1f),
                                blue  = (color1.blue  * 0.7f).coerceIn(0f,1f)
                            )
                        )
                    )
                )
                .border(2.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            // Gloss top-half
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.45f)
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.White.copy(alpha = 0.28f), Color.Transparent)
                        ),
                        RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)
                    )
            )
            Text(
                text = current.displayValue(),
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                style = LocalTextStyle.current.copy(
                    shadow = Shadow(Color.Black.copy(alpha = 0.4f), Offset(1f,1f), 2f)
                )
            )
        }

        Spacer(Modifier.width(10.dp))

        // ── Block 2 — upcoming (smaller, dimmed, centred value) ───────────────
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            color2.copy(alpha = 0.55f),
                            color2.copy(
                                red   = (color2.red   * 0.7f).coerceIn(0f,1f),
                                green = (color2.green * 0.7f).coerceIn(0f,1f),
                                blue  = (color2.blue  * 0.7f).coerceIn(0f,1f),
                                alpha = 0.45f
                            )
                        )
                    )
                )
                .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = next.displayValue(),
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }

        Spacer(Modifier.weight(1f))

        // ── Pause button ──────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .size(40.dp)
                .shadow(6.dp, CircleShape, ambientColor = Color(0xFF4CAF50).copy(alpha = 0.5f))
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(Color(0xFF66BB6A), Color(0xFF2E7D32))
                    )
                )
                .border(2.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                .clickable { onPause() },
            contentAlignment = Alignment.Center
        ) {
            Text("⏸", fontSize = 17.sp)
        }
    }
}

// ─── Game Grid ────────────────────────────────────────────────────────────────
@Composable
fun GameGrid(
    gameState: GameState,
    selectedCol: Int,
    hammerMode: Boolean,
    swapMode: Boolean = false,
    swapFirst: Pair<Int,Int>? = null,
    onColumnSelected: (Int) -> Unit,
    onColumnTapped: (Int) -> Unit,
    onCellTapped: (Int, Int) -> Unit
) {
    val gap = 3.dp

    // Use BoxWithConstraints to compute cell size from available width
    // so grid is never taller than it needs to be
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .padding(horizontal = 8.dp)
    ) {
        // Cell size = min of (width-based size, height-based size) to always fit screen
        val totalHPadding = 16.dp + gap * (COLS - 1)
        val totalVPadding = 16.dp + gap * (ROWS - 1)
        val cellByWidth  = (maxWidth  - totalHPadding) / COLS
        val cellByHeight = (maxHeight - totalVPadding) / ROWS
        val cellSize = minOf(cellByWidth, cellByHeight)

        // Grid background
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF0D1B2A).copy(alpha = 0.8f),
            border = BorderStroke(1.dp, Color(0xFF2A4A6A))
        ) {
            Row(
                modifier = Modifier.padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(gap)
            ) {
                for (c in 0 until COLS) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                onColumnSelected(c)
                                onColumnTapped(c)
                            },
                        verticalArrangement = Arrangement.spacedBy(gap)
                    ) {
                        // Rows rendered top→bottom on screen = row 0 (top/empty) first,
                        // row ROWS-1 (bottom/filled) last — so blocks visually stack upward.
                        for (r in ROWS - 1 downTo 0) {
                            val block = gameState.grid[r][c]
                            val isSwapSelected = swapFirst?.first == r && swapFirst?.second == c
                            Box(
                                modifier = Modifier
                                    .size(cellSize)
                                    .clip(RoundedCornerShape(8.dp))
                                    .then(
                                        if (isSwapSelected)
                                            Modifier.border(2.dp, Color(0xFFFFD700), RoundedCornerShape(8.dp))
                                        else Modifier
                                    )
                                    .then(
                                        if ((block != null && hammerMode) || swapMode) {
                                            Modifier.clickable { onCellTapped(r, c) }
                                        } else Modifier
                                    )
                            ) {
                                if (block != null) {
                                    AnimatedBlockTile(
                                        block = block,
                                        hammerMode = hammerMode
                                    )
                                } else {
                                    // Empty cell
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Color(0xFF1A3050).copy(alpha = 0.4f),
                                                RoundedCornerShape(8.dp)
                                            )
                                    )
                                }
                            }
                        }

                        // Drop indicator at BOTTOM — shows which column is selected
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(
                                    if (selectedCol == c && !hammerMode)
                                        Color(0xFF00BCD4)
                                    else Color.Transparent
                                )
                        )
                    }
                }
            }
        }
    }
}

// ─── Block Tile ───────────────────────────────────────────────────────────────
@Composable
fun AnimatedBlockTile(block: Block, hammerMode: Boolean) {

    // ── Drop-in animation ─────────────────────────────────────────────────────
    // When isNew=true the block just landed. We animate from 0→1 scale so it
    // "pops" into existence with a satisfying bounce. Use `key(block.isNew)` so
    // Compose restarts the animation every time isNew flips to true.
    var dropScaleTarget by remember { mutableStateOf(if (block.isNew) 0f else 1f) }
    LaunchedEffect(block.isNew) {
        if (block.isNew) {
            dropScaleTarget = 0f          // start at 0
            // tiny yield so Compose paints the 0-scale frame first
            kotlinx.coroutines.delay(16)
            dropScaleTarget = 1f          // then animate to full size
        } else {
            dropScaleTarget = 1f
        }
    }
    val dropScale by animateFloatAsState(
        targetValue  = dropScaleTarget,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness    = Spring.StiffnessMediumLow
        ),
        label = "dropScale"
    )

    // ── Pre-merge highlight: all merging blocks glow gold ─────────────────────
    // isMerging is set on ALL blocks in the group during PRE_MERGE frame,
    // then only on the result block in POST_MERGE frame.
    val mergeScale by animateFloatAsState(
        targetValue  = if (block.isMerging) 1.18f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness    = Spring.StiffnessMedium
        ),
        label = "mergeScale"
    )

    // Gold overlay for PRE_MERGE (participating blocks), white for POST_MERGE (result)
    val mergeGlowAlpha by animateFloatAsState(
        targetValue  = if (block.isMerging) 0.45f else 0f,
        animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
        label = "mergeGlow"
    )

    val dimAlpha by animateFloatAsState(
        targetValue = if (hammerMode) 0.55f else 1f,
        label = "dimAlpha"
    )

    val finalScale = if (block.isNew) dropScale else mergeScale

    Box(
        modifier = Modifier
            .fillMaxSize()
            .scale(finalScale)
            .alpha(dimAlpha)
    ) {
        BlockTile(block = block, size = 0.dp, isSelected = false, fillContainer = true)

        // Glow overlay — gold tint on pre-merge, white flash on post-merge result
        if (mergeGlowAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        // Gold for pre-merge highlight, white for post-merge flash
                        if (block.isMerging)
                            Color(0xFFFFD700).copy(alpha = mergeGlowAlpha)
                        else
                            Color.White.copy(alpha = mergeGlowAlpha)
                    )
            )
        }
    }
}

@Composable
fun BlockTile(
    block: Block,
    size: Dp,
    isSelected: Boolean,
    fillContainer: Boolean = false,
    dimmed: Boolean = false        // true for "after next" preview
) {
    val color = BLOCK_COLORS[block.colorIndex % BLOCK_COLORS.size]
        .let { if (dimmed) it.copy(alpha = 0.55f) else it }
    val darkerColor = color.copy(
        red = (color.red * 0.7f).coerceIn(0f, 1f),
        green = (color.green * 0.7f).coerceIn(0f, 1f),
        blue = (color.blue * 0.7f).coerceIn(0f, 1f)
    )

    val modifier = if (fillContainer) {
        Modifier.fillMaxSize()
    } else {
        Modifier.size(size)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(color, darkerColor),
                    start = Offset(0f, 0f),
                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                )
            )
            .then(
                if (isSelected) Modifier.border(2.dp, Color(0xFF00E5FF), RoundedCornerShape(8.dp))
                else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        // Gloss overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.5f)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.25f),
                            Color.Transparent
                        )
                    ),
                    RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
                )
        )

        // Auto-size font: shorter labels get bigger text, longer ones shrink to fit
        val displayText = block.displayValue()
        val fontSize = when {
            !fillContainer          -> 15.sp
            displayText.length <= 2 -> 20.sp   // "2", "4K" etc  — biggest
            displayText.length == 3 -> 17.sp   // "16", "64K"
            displayText.length == 4 -> 14.sp   // "128", "1024"
            else                    -> 12.sp   // "1024K" or longer
        }
        Text(
            text = displayText,
            color = Color.White,
            fontSize = fontSize,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            style = LocalTextStyle.current.copy(
                shadow = Shadow(
                    color = Color.Black.copy(alpha = 0.5f),
                    offset = Offset(1f, 1f),
                    blurRadius = 3f
                )
            )
        )
    }
}

// ─── Game Controls ────────────────────────────────────────────────────────────
@Composable
fun GameControls(
    coins: Int,
    hammerMode: Boolean,
    swapMode: Boolean = false,
    onHammerTap: () -> Unit,
    onSwapTap: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Hammer — remove a block
        PowerUpButton(
            label = "Hammer",
            cost = PowerUpType.HAMMER.cost,
            emoji = "🔨",
            isActive = hammerMode,
            onClick = onHammerTap
        )

        // Swap — select 2 blocks to swap positions
        PowerUpButton(
            label = "Swap",
            cost = PowerUpType.SWAP.cost,
            emoji = "🔁",
            isActive = swapMode,
            onClick = onSwapTap
        )
    }
}

@Composable
fun PowerUpButton(
    label: String,
    cost: Int,
    emoji: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = if (isActive) Color(0xFF00FF88) else Color(0xFF2A4A6A),
        label = "borderColor"
    )

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF0D1B2A).copy(alpha = 0.9f),
        modifier = Modifier
            .width(90.dp)
            .height(56.dp)
            .border(2.dp, borderColor, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(emoji, fontSize = 20.sp)
            Text(
                label,
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                CoinIcon(size = 10.dp)
                Spacer(Modifier.width(2.dp))
                Text(
                    cost.toString(),
                    color = Color(0xFFFFD700),
                    fontSize = 10.sp
                )
            }
        }
    }
}

// ─── Combo Popup ──────────────────────────────────────────────────────────────
@Composable
fun ComboPopup(text: String, onDismiss: () -> Unit) {
    LaunchedEffect(text) {
        kotlinx.coroutines.delay(1500)
        onDismiss()
    }
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = true,
            enter = scaleIn(spring(Spring.DampingRatioLowBouncy)) + fadeIn(),
            exit = fadeOut()
        ) {
            Text(
                text = text,
                fontSize = 36.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFFFFD700),
                style = LocalTextStyle.current.copy(
                    shadow = Shadow(Color.Black, Offset(2f, 2f), 4f)
                )
            )
        }
    }
}

// ─── Game Over ────────────────────────────────────────────────────────────────
@Composable
fun GameOverScreen(
    score: Long,
    highestBlock: Block,
    onRestart: () -> Unit,
    onMenu: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF0D1B2A),
            border = BorderStroke(2.dp, Color(0xFF2196F3)),
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("GAME OVER", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                Spacer(Modifier.height(16.dp))
                Text("Score", color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
                Text(formatScore(score), fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFFFD700))
                Spacer(Modifier.height(8.dp))
                Text("Highest Block", color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
                BlockTile(block = highestBlock, size = 72.dp, isSelected = false)
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = onRestart,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("PLAY AGAIN", fontWeight = FontWeight.Bold, modifier = Modifier.padding(8.dp))
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onMenu,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF2196F3))
                ) {
                    Text("MAIN MENU", color = Color(0xFF2196F3), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ─── Helpers ─────────────────────────────────────────────────────────────────
@Composable
fun CoinIcon(size: Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    listOf(Color(0xFFFFF176), Color(0xFFFFB300))
                )
            )
    )
}

fun formatScore(score: Long): String {
    return when {
        score >= 1_000_000_000_000L -> "${score / 1_000_000_000_000L}T"
        score >= 1_000_000_000L -> "${score / 1_000_000_000L}B"
        score >= 1_000_000L -> "${score / 1_000_000L}M"
        score >= 1_000L -> "${score / 1_000L}K"
        else -> score.toString()
    }
}

// ─── Level Up Popup ──────────────────────────────────────────────────────────
@Composable
fun LevelUpPopup(text: String, onDismiss: () -> Unit) {
    LaunchedEffect(text) {
        kotlinx.coroutines.delay(2200)
        onDismiss()
    }
    val infiniteTransition = rememberInfiniteTransition(label = "levelup")
    val glow by infiniteTransition.animateFloat(
        initialValue = 0.6f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse),
        label = "glow"
    )
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF1A2A4A),
            border = BorderStroke((2 * glow).dp, Color(0xFFFFD700)),
            modifier = Modifier.padding(32.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("🏆", fontSize = 32.sp)
                Spacer(Modifier.height(4.dp))
                Text(
                    "LEVEL UP!",
                    color = Color(0xFFFFD700),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 3.sp
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text,
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// ─── Pause / Settings Dialog ─────────────────────────────────────────────────
@Composable
fun PauseMenuDialog(
    musicOn: Boolean,
    vibrateOn: Boolean,
    onMusicToggle: () -> Unit,
    onVibrateToggle: () -> Unit,
    onRestart: () -> Unit,
    onContinue: () -> Unit,
    onHome: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF1A2A4A),
            border = BorderStroke(2.dp, Color(0xFF4A9EFF)),
            modifier = Modifier
                .fillMaxWidth(0.82f)
                .wrapContentHeight()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Title banner
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1565C0),
                    border = BorderStroke(2.dp, Color(0xFF4A9EFF))
                ) {
                    Text(
                        "SETTINGS",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 3.sp,
                        modifier = Modifier.padding(horizontal = 32.dp, vertical = 10.dp)
                    )
                }

                // Toggle buttons row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf(
                        Triple(if (musicOn) "MUSIC ON" else "MUSIC OFF",  if (musicOn) "🔊" else "🔇",  Triple(musicOn,  onMusicToggle,   Color(0xFF1565C0))),
                        Triple(if (vibrateOn) "VIBRATE ON" else "VIBRATE OFF", if (vibrateOn) "📳" else "📴", Triple(vibrateOn, onVibrateToggle, Color(0xFF1565C0))),
                        Triple("RESTART", "🔄", Triple(false, onRestart, Color(0xFFFF6B35)))
                    ).forEach { (label, emoji, extra) ->
                        val (active, action, color) = extra
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            SettingsToggleButton(
                                label       = label,
                                emoji       = emoji,
                                isActive    = active,
                                onClick     = action,
                                accentColor = color
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                label,
                                color     = Color.White.copy(alpha = 0.75f),
                                fontSize  = 9.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                maxLines  = 1
                            )
                        }
                    }
                }

                // Continue / Home buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Continue
                    Button(
                        onClick = onContinue,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFA000))
                    ) {
                        Text(
                            "CONTINUE",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                            letterSpacing = 1.sp
                        )
                    }
                    // Home
                    Button(
                        onClick = onHome,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                    ) {
                        Text(
                            "HOME",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsToggleButton(
    label: String,
    emoji: String,
    isActive: Boolean,
    onClick: () -> Unit,
    accentColor: Color = Color(0xFF1565C0)
) {
    val bgColor = if (isActive) accentColor else Color(0xFF0D2040)
    val borderColor = if (isActive) accentColor.copy(alpha = 0.8f) else Color(0xFF2A4A6A)

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = bgColor,
        border = BorderStroke(2.dp, borderColor),
        modifier = Modifier
            .size(72.dp)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(emoji, fontSize = 26.sp)
        }
    }
    // Label below button — rendered as separate item outside Surface via Column wrapper
}

// ─── Trophy / Rank Display ───────────────────────────────────────────────────
fun getRankColor(trophies: Int): Color = when {
    trophies >= 200 -> Color(0xFFE040FB)  // Master  — purple
    trophies >= 100 -> Color(0xFF00E5FF)  // Diamond — cyan
    trophies >= 60  -> Color(0xFFB0BEC5)  // Platinum — silver-blue
    trophies >= 30  -> Color(0xFFFFD700)  // Gold
    trophies >= 10  -> Color(0xFFB0BEC5)  // Silver
    else            -> Color(0xFFFF8A65)  // Bronze  — copper
}

fun getNextRankTrophies(trophies: Int): Int = when {
    trophies >= 200 -> Int.MAX_VALUE  // Max rank
    trophies >= 100 -> 200
    trophies >= 60  -> 100
    trophies >= 30  -> 60
    trophies >= 10  -> 30
    else            -> 10
}

fun getPrevRankTrophies(trophies: Int): Int = when {
    trophies >= 200 -> 200
    trophies >= 100 -> 100
    trophies >= 60  -> 60
    trophies >= 30  -> 30
    trophies >= 10  -> 10
    else            -> 0
}

@Composable
fun TrophyDisplay(trophies: Int) {
    val rankLabel  = getRankLabel(trophies)
    val rankColor  = getRankColor(trophies)
    val nextRank   = getNextRankTrophies(trophies)
    val prevRank   = getPrevRankTrophies(trophies)
    val isMaxRank  = nextRank == Int.MAX_VALUE

    val progress   = if (isMaxRank) 1f else
        ((trophies - prevRank).toFloat() / (nextRank - prevRank)).coerceIn(0f, 1f)

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.Black.copy(alpha = 0.45f),
        modifier = Modifier.border(1.dp, rankColor.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("🏆", fontSize = 13.sp)
                Text(
                    text = "$trophies",
                    color = rankColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = rankLabel,
                    color = rankColor.copy(alpha = 0.85f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            if (!isMaxRank) {
                Spacer(Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .width(72.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White.copy(alpha = 0.1f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .fillMaxHeight()
                            .background(rankColor)
                    )
                }
            }
        }
    }
}

fun getRankLabel(trophies: Int): String {
    return when {
        trophies >= 200 -> "Master"
        trophies >= 100 -> "Diamond"
        trophies >= 60 -> "Platinum"
        trophies >= 30 -> "Gold"
        trophies >= 10 -> "Silver"
        else -> "Bronze"
    }
}