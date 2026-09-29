package com.mergeblast.ui.menu

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.systemBars
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import com.mergeblast.data.models.Block
import com.mergeblast.ui.game.BlockTile
import com.mergeblast.ui.game.CoinIcon
import com.mergeblast.ui.game.getRankLabel
import com.mergeblast.viewmodel.GameEvent
import com.mergeblast.viewmodel.GameViewModel

@Composable
fun MenuScreen(
    adLoading: Boolean,
    onWatchAd: (() -> Unit) -> Unit,
    showAdPrivacy: Boolean,
    onAdPrivacy: () -> Unit,
    adsRemoved: Boolean,
    onRemoveAds: () -> Unit,
    viewModel: GameViewModel,
    onPlay: () -> Unit,
    onShop: () -> Unit,
    onCoinShop: () -> Unit,
    onQuest: () -> Unit,
    onLeaderboard: () -> Unit
) {
    val playerData by viewModel.playerData.collectAsState(initial = null)
    var showDailyReward by remember { mutableStateOf(false) }
    var showNoAdMessage by remember { mutableStateOf(false) }

    // Badge shown only when reward has NOT been claimed today
    val oneDayMs = 24 * 60 * 60 * 1000L
    val lastClaimed = playerData?.lastDailyReward ?: 0L
    val rewardAvailable = (System.currentTimeMillis() - lastClaimed) >= oneDayMs || lastClaimed == 0L

    val infiniteTransition = rememberInfiniteTransition(label = "menu")
    val glowAnim by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2000), RepeatMode.Reverse),
        label = "glow"
    )
    val floatAnim by infiniteTransition.animateFloat(
        initialValue = -8f, targetValue = 8f,
        animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "float"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(WindowInsets.systemBars.asPaddingValues())
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF1A1035), Color(0xFF0D1B3E), Color(0xFF0A2545))
                )
            )
    ) {
        // Mountain silhouette background
        MountainBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(20.dp))

            // Top bar: coins & trophy
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CoinsDisplay(coins = playerData?.coins ?: 0, onClick = onCoinShop)
                TrophyDisplay(trophies = playerData?.trophies ?: 0)
            }

            Spacer(Modifier.height(16.dp))

            // Top action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MenuIconButton(label = "Daily\nQuest", emoji = "📋", onClick = onQuest)
                Spacer(Modifier.weight(1f))
                MenuIconButton(label = "Daily\nReward", emoji = "📅", onClick = { showDailyReward = true }, hasBadge = rewardAvailable)
            }

            Spacer(Modifier.weight(0.3f))

            // AD Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                AdButton(loading = adLoading, onWatch = { onWatchAd { showNoAdMessage = true } })
            }

            Spacer(Modifier.weight(0.2f))

            // ── Highest Block — cinematic showcase ──────────────────────
            HighestBlockShowcase(
                highestBlock = playerData?.highestBlock?.takeIf { it > 0L } ?: 0L,
                glowAnim = glowAnim,
                floatAnim = floatAnim
            )

            Spacer(Modifier.weight(0.4f))

            // PLAY Button
            PlayButton(onClick = onPlay, glowAnim = glowAnim)

            Spacer(Modifier.weight(0.3f))

            // Bottom actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shop
                MenuIconButton(label = "Shop", emoji = "💰", onClick = onShop)

                // Leaderboard
                MenuIconButton(label = "Leaders", emoji = "🏅", onClick = onLeaderboard)

                // Ads toggle
                AdsToggle(adsRemoved, onRemoveAds)
            }

            if (showAdPrivacy) {
                TextButton(onClick = onAdPrivacy, enabled = !adLoading) {
                    Text("Ad privacy choices", color = Color(0xFF76E1D6), fontSize = 11.sp)
                }
            }
            Spacer(Modifier.height(24.dp))
        }

        // No Ad snackbar
        if (showNoAdMessage) {
            LaunchedEffect(showNoAdMessage) {
                kotlinx.coroutines.delay(2500)
                showNoAdMessage = false
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 32.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF1A2A3A),
                    border = BorderStroke(1.dp, Color(0xFF2A4A6A)),
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .shadow(8.dp, RoundedCornerShape(16.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("📺", fontSize = 22.sp)
                        Column {
                            Text(
                                "No Ad Available",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Come back later to watch & earn coins",
                                color = Color.White.copy(alpha = 0.55f),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        // Daily reward dialog
        if (showDailyReward) {
            DailyRewardDialog(
                lastClaimedMs = playerData?.lastDailyReward ?: 0L,
                currentStreak = playerData?.dailyRewardStreak ?: 0,
                onClaim = {
                    viewModel.claimDailyReward()
                },
                onDismiss = { showDailyReward = false }
            )
        }
    }
}

@Composable
fun MountainBackground() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val path = Path().apply {
            moveTo(0f, size.height * 0.7f)
            lineTo(size.width * 0.2f, size.height * 0.45f)
            lineTo(size.width * 0.4f, size.height * 0.6f)
            lineTo(size.width * 0.6f, size.height * 0.35f)
            lineTo(size.width * 0.8f, size.height * 0.55f)
            lineTo(size.width, size.height * 0.4f)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(path, Color(0xFF0D1B2A).copy(alpha = 0.6f))
    }
}

@Composable
fun CoinsDisplay(coins: Int, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color.Black.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, Color(0xFFFFD700).copy(alpha = 0.4f)),
        modifier = Modifier.clickable(onClickLabel = "Open coin shop", onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CoinIcon(size = 22.dp)
            Text(
                text = coins.toString(),
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Text("+", color = Color(0xFF4CAF50), fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun TrophyDisplay(trophies: Int) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color.Black.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, Color(0xFFFFD700).copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("🏆", fontSize = 16.sp)
            Text(
                text = getRankLabel(trophies),
                color = Color(0xFFFFD700),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun MenuIconButton(
    label: String,
    emoji: String,
    onClick: () -> Unit,
    hasBadge: Boolean = false
) {
    Box(contentAlignment = Alignment.TopEnd) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFF0D1B2A).copy(alpha = 0.85f),
            border = BorderStroke(1.dp, Color(0xFF2A4A6A)),
            modifier = Modifier
                .size(68.dp)
                .clickable(onClick = onClick)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    emoji,
                    fontSize = 24.sp,
                    modifier = Modifier.padding(bottom = 3.dp)
                )
                Text(
                    label,
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    lineHeight = 11.sp,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Clip,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        // Red dot badge — only shown when hasBadge = true
        if (hasBadge) {
            Box(
                modifier = Modifier
                    .offset(x = 4.dp, y = (-4).dp)
                    .size(13.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE53935))
                    .border(1.5.dp, Color.White, CircleShape)
            )
        }
    }
}

@Composable
fun AdButton(loading: Boolean, onWatch: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF1A3A5A),
        border = BorderStroke(1.dp, Color(0xFF2A6A9A)),
        modifier = Modifier.width(116.dp).clickable(enabled = !loading, onClick = onWatch)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF1565C0)
            ) {
                Text(
                    if (loading) "Loading..." else "Watch Ads",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                CoinIcon(size = 12.dp)
                Spacer(Modifier.width(3.dp))
                Text("+ 100", color = Color(0xFFFFD700), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun AdsToggle(adsRemoved: Boolean, onRemoveAds: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(if (adsRemoved) "AD-FREE" else "REMOVE ADS", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Switch(
            checked = adsRemoved,
            onCheckedChange = { if (!adsRemoved) onRemoveAds() },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF4CAF50)
            )
        )
    }
}

// ─── Highest Block Showcase ──────────────────────────────────────────────────
@Composable
fun HighestBlockShowcase(highestBlock: Long, glowAnim: Float, floatAnim: Float) {
    val infiniteTransition = rememberInfiniteTransition(label = "showcase")

    // Slow rotation for orbital ring
    val ringRotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(6000, easing = LinearEasing)),
        label = "ring"
    )
    // Counter-rotate inner ring
    val ringRotation2 by infiniteTransition.animateFloat(
        initialValue = 360f, targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(9000, easing = LinearEasing)),
        label = "ring2"
    )
    // Pulse for outer glow
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.92f, targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse"
    )

    val blockValue = if (highestBlock > 0L) highestBlock else 2L
    fun colorIdxFor(v: Long): Int { var x = v; var p = 0; while (x > 1L) { x /= 2; p++ }; return p % 12 }
    val block = com.mergeblast.data.models.Block(value = blockValue, colorIndex = colorIdxFor(blockValue))
    val blockColor = com.mergeblast.ui.game.BLOCK_COLORS[block.colorIndex % com.mergeblast.ui.game.BLOCK_COLORS.size]

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .offset(y = floatAnim.dp)
                .size(180.dp),
            contentAlignment = Alignment.Center
        ) {
            // ── Outermost glow halo ───────────────────────────────────────────
            Canvas(modifier = Modifier
                .size(180.dp)
                .scale(pulseScale)
            ) {
                for (i in 4 downTo 1) {
                    drawCircle(
                        color = blockColor.copy(alpha = 0.04f * i),
                        radius = size.minDimension / 2f * (0.5f + i * 0.13f)
                    )
                }
            }

            // ── Outer orbital ring (rotating dashes) ──────────────────────────
            Canvas(modifier = Modifier
                .size(168.dp)
                .rotate(ringRotation)
            ) {
                val r = size.minDimension / 2f
                val dotCount = 12
                for (i in 0 until dotCount) {
                    val angle = Math.toRadians((i * 360.0 / dotCount))
                    val x = (center.x + r * kotlin.math.cos(angle)).toFloat()
                    val y = (center.y + r * kotlin.math.sin(angle)).toFloat()
                    drawCircle(
                        color = blockColor.copy(alpha = if (i % 3 == 0) 0.9f else 0.35f),
                        radius = if (i % 3 == 0) 5f else 3f,
                        center = androidx.compose.ui.geometry.Offset(x, y)
                    )
                }
            }

            // ── Inner orbital ring (counter-rotating) ─────────────────────────
            Canvas(modifier = Modifier
                .size(148.dp)
                .rotate(ringRotation2)
            ) {
                val r = size.minDimension / 2f
                val dotCount = 8
                for (i in 0 until dotCount) {
                    val angle = Math.toRadians((i * 360.0 / dotCount))
                    val x = (center.x + r * kotlin.math.cos(angle)).toFloat()
                    val y = (center.y + r * kotlin.math.sin(angle)).toFloat()
                    drawCircle(
                        color = Color.White.copy(alpha = if (i % 2 == 0) 0.6f else 0.2f),
                        radius = if (i % 2 == 0) 4f else 2.5f,
                        center = androidx.compose.ui.geometry.Offset(x, y)
                    )
                }
            }

            // ── Block tile with glowing border ────────────────────────────────
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .shadow(
                        elevation = (16 + glowAnim * 24).dp,
                        shape = RoundedCornerShape(22.dp),
                        ambientColor = blockColor.copy(alpha = 0.8f),
                        spotColor = blockColor
                    )
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                blockColor,
                                blockColor.copy(
                                    red   = (blockColor.red   * 0.65f).coerceIn(0f,1f),
                                    green = (blockColor.green * 0.65f).coerceIn(0f,1f),
                                    blue  = (blockColor.blue  * 0.65f).coerceIn(0f,1f)
                                )
                            )
                        )
                    )
                    .border(
                        (2 + glowAnim * 1.5f).dp,
                        Brush.linearGradient(listOf(Color.White.copy(0.6f), blockColor.copy(0.3f))),
                        RoundedCornerShape(22.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Gloss
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.45f)
                        .align(Alignment.TopCenter)
                        .background(
                            Brush.verticalGradient(listOf(Color.White.copy(0.28f), Color.Transparent)),
                            RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)
                        )
                )
                Text(
                    text = block.displayValue(),
                    color = Color.White,
                    fontSize = when {
                        block.displayValue().length <= 2 -> 32.sp
                        block.displayValue().length == 3 -> 26.sp
                        else -> 20.sp
                    },
                    fontWeight = FontWeight.ExtraBold,
                    style = androidx.compose.ui.text.TextStyle(
                        shadow = androidx.compose.ui.graphics.Shadow(
                            color = Color.Black.copy(0.5f),
                            offset = androidx.compose.ui.geometry.Offset(2f, 2f),
                            blurRadius = 4f
                        )
                    )
                )
            }

            // ── Crown on top ──────────────────────────────────────────────────
            if (highestBlock > 0L) {
                Text(
                    "👑",
                    fontSize = 26.sp,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = 4.dp)
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        // ── Label + rank badge ────────────────────────────────────────────────
        Text(
            "YOUR HIGHEST BLOCK",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 3.sp
        )

        if (highestBlock > 0L) {
            Spacer(Modifier.height(4.dp))
            val tier = when {
                highestBlock >= 1_000_000_000_000L -> "🔥 LEGENDARY"
                highestBlock >= 1_000_000_000L     -> "💎 EPIC"
                highestBlock >= 1_000_000L          -> "⭐ GREAT"
                highestBlock >= 1_000L              -> "✨ GOOD"
                else                               -> "🌱 STARTER"
            }
            val tierColor = when {
                highestBlock >= 1_000_000_000_000L -> Color(0xFFFF6B35)
                highestBlock >= 1_000_000_000L     -> Color(0xFF00E5FF)
                highestBlock >= 1_000_000L          -> Color(0xFFFFD700)
                highestBlock >= 1_000L              -> Color(0xFF4CAF50)
                else                               -> Color(0xFF90A4AE)
            }
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = tierColor.copy(alpha = 0.15f),
                border = BorderStroke(1.dp, tierColor.copy(alpha = 0.5f))
            ) {
                Text(
                    tier,
                    color = tierColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                )
            }
        }
    }
}

@Composable
fun PlayButton(onClick: () -> Unit, glowAnim: Float) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth(0.8f)
            .height(60.dp)
            .shadow(
                elevation = (12 + glowAnim * 12).dp,
                shape = RoundedCornerShape(30.dp),
                ambientColor = Color(0xFFFF9800)
            ),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        contentPadding = PaddingValues(0.dp),
        shape = RoundedCornerShape(30.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xFFFFA000), Color(0xFFFF6F00))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("▶", color = Color.White, fontSize = 20.sp)
                Text(
                    "PLAY",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 3.sp
                )
            }
        }
    }
}

@Composable
fun DailyRewardDialog(
    lastClaimedMs: Long,
    currentStreak: Int,
    onClaim: () -> Unit,
    onDismiss: () -> Unit
) {
    val rewards = listOf(100, 200, 350, 450, 550)
    val oneDayMs = 24 * 60 * 60 * 1000L
    val now = System.currentTimeMillis()

    // Has the player claimed today already?
    val msSinceClaim = now - lastClaimedMs
    val claimedToday = msSinceClaim < oneDayMs && lastClaimedMs > 0L

    // Countdown to next claim (ms remaining until 24h since last claim)
    var msRemaining by remember { mutableStateOf(if (claimedToday) (oneDayMs - msSinceClaim) else 0L) }

    // Live countdown ticker
    LaunchedEffect(claimedToday) {
        if (claimedToday) {
            while (msRemaining > 0L) {
                kotlinx.coroutines.delay(1000L)
                msRemaining = (oneDayMs - (System.currentTimeMillis() - lastClaimedMs)).coerceAtLeast(0L)
            }
        }
    }

    fun formatCountdown(ms: Long): String {
        val totalSecs = ms / 1000L
        val h = totalSecs / 3600
        val m = (totalSecs % 3600) / 60
        val s = totalSecs % 60
        return "%02d:%02d:%02d".format(h, m, s)
    }

    // Which day slot is active (0-indexed). Streak is 1-based after first claim.
    // If streak == 0 or streak reset → day 0 is next
    val activeDayIndex = if (claimedToday) (currentStreak - 1) % 5
    else currentStreak % 5

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.78f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = Color(0xFF0A1628),
            border = BorderStroke(2.dp,
                Brush.linearGradient(listOf(Color(0xFF2196F3), Color(0xFF00BCD4)))
            ),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clickable { }
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // ── Header with close button ─────────────────────────────
                Box(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "DAILY REWARD",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 3.sp,
                        modifier = Modifier.align(Alignment.Center)
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE53935))
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("✕", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Text(
                    "Missing a day will reset the reward progress",
                    color = Color.White.copy(alpha = 0.45f),
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )

                // ── Day 1-3 row ───────────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    for (i in 0..2) {
                        DayRewardItem(
                            day      = i + 1,
                            reward   = rewards[i],
                            state    = when {
                                i < activeDayIndex && currentStreak > 0 -> DayState.CLAIMED
                                i == activeDayIndex && !claimedToday    -> DayState.TODAY
                                i == activeDayIndex && claimedToday     -> DayState.CLAIMED
                                else                                     -> DayState.LOCKED
                            }
                        )
                    }
                }

                // ── Day 4-5 row ───────────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    for (i in 3..4) {
                        DayRewardItem(
                            day      = i + 1,
                            reward   = rewards[i],
                            state    = when {
                                i < activeDayIndex && currentStreak > 0 -> DayState.CLAIMED
                                i == activeDayIndex && !claimedToday    -> DayState.TODAY
                                i == activeDayIndex && claimedToday     -> DayState.CLAIMED
                                else                                     -> DayState.LOCKED
                            }
                        )
                    }
                    // Empty spacer to keep layout balanced
                    Spacer(Modifier.width(68.dp))
                }

                // ── Claim / Timer button ───────────────────────────────────────
                if (claimedToday) {
                    // Claimed — show countdown
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF1A2A3A),
                        border = BorderStroke(1.dp, Color(0xFF2A4A6A)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "✅  TODAY'S REWARD CLAIMED",
                                color = Color(0xFF4CAF50),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Next reward in",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 11.sp
                            )
                            Text(
                                formatCountdown(msRemaining),
                                color = Color(0xFF64B5F6),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 2.sp
                            )
                        }
                    }
                } else {
                    // Not claimed — show CLAIM button
                    Button(
                        onClick = {
                            onClaim()
                            onDismiss()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(Color(0xFFFFA000), Color(0xFFFF6F00))
                                    ),
                                    RoundedCornerShape(14.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "CLAIM  +${rewards[activeDayIndex]} coins",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

enum class DayState { CLAIMED, TODAY, LOCKED }

@Composable
fun DayRewardItem(day: Int, reward: Int, state: DayState) {
    val bgColor = when (state) {
        DayState.CLAIMED -> Color(0xFF1B3A1B)
        DayState.TODAY   -> Color(0xFF1A3A6A)
        DayState.LOCKED  -> Color(0xFF0D1822)
    }
    val borderColor = when (state) {
        DayState.CLAIMED -> Color(0xFF4CAF50)
        DayState.TODAY   -> Color(0xFF2196F3)
        DayState.LOCKED  -> Color(0xFF1E3050)
    }
    val borderWidth = if (state == DayState.TODAY) 2.dp else 1.dp
    val dayLabelColor = when (state) {
        DayState.CLAIMED -> Color(0xFF4CAF50)
        DayState.TODAY   -> Color(0xFF64B5F6)
        DayState.LOCKED  -> Color.White.copy(alpha = 0.30f)
    }
    val coinAlpha = if (state == DayState.LOCKED) 0.35f else 1f

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = bgColor,
        border = BorderStroke(borderWidth, borderColor),
        modifier = Modifier.width(68.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Day label
            Text(
                "DAY $day",
                color = dayLabelColor,
                fontSize = 9.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp
            )
            Spacer(Modifier.height(7.dp))

            // Icon — checkmark if claimed, coin otherwise
            if (state == DayState.CLAIMED) {
                Text("✅", fontSize = 24.sp)
            } else {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                if (state == DayState.TODAY)
                                    listOf(Color(0xFFFFF176), Color(0xFFFFB300))
                                else
                                    listOf(Color(0xFFFFD54F).copy(alpha = 0.5f), Color(0xFFA07000).copy(alpha = 0.5f))
                            )
                        )
                        .alpha(coinAlpha)
                )
            }

            Spacer(Modifier.height(5.dp))

            // Coin amount — always show real number, never "?"
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // Small coin icon
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    Color(0xFFFFF176).copy(alpha = coinAlpha),
                                    Color(0xFFFFB300).copy(alpha = coinAlpha)
                                )
                            )
                        )
                )
                Spacer(Modifier.width(3.dp))
                Text(
                    reward.toString(),
                    color = Color.White.copy(alpha = if (state == DayState.LOCKED) 0.45f else 1f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}
