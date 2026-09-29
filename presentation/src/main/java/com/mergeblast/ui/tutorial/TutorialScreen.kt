package com.mergeblast.ui.tutorial

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*

// ─── Tutorial Pages Data ──────────────────────────────────────────────────────
data class TutorialPage(
    val step: Int,
    val emoji: String,
    val title: String,
    val subtitle: String,
    val description: String,
    val accentColor: Color,
    val demoContent: @Composable () -> Unit
)

// ─── Tutorial Screen ──────────────────────────────────────────────────────────
@Composable
fun TutorialScreen(onFinish: () -> Unit) {

    var currentPage by remember { mutableStateOf(0) }
    val pageScrollState = rememberScrollState()
    LaunchedEffect(currentPage) { pageScrollState.scrollTo(0) }

    val pages = listOf(
        TutorialPage(
            step = 1,
            emoji = "🎯",
            title = "Drop & Match",
            subtitle = "Tap any column to drop a block",
            description = "Blocks fall to the bottom of the column.\nWhen same numbers touch each other they automatically merge!",
            accentColor = Color(0xFF4ECDC4),
            demoContent = { DropDemo() }
        ),
        TutorialPage(
            step = 2,
            emoji = "💥",
            title = "Merge & Blast",
            subtitle = "Same numbers combine into bigger ones",
            description = "2+2 = 4 · 4+4 = 8 · 8+8 = 16\n3 or more connected blocks merge at once!\nThe bigger the merge, the more coins you earn.",
            accentColor = Color(0xFFFF6B6B),
            demoContent = { MergeDemo() }
        ),
        TutorialPage(
            step = 3,
            emoji = "⚡",
            title = "Power Ups",
            subtitle = "Use tools to clear tough spots",
            description = "🔨 Hammer — remove any block from the grid\n🔁 Swap — tap 2 blocks to swap their positions\n\nEarn coins by merging to buy more!",
            accentColor = Color(0xFFFFD700),
            demoContent = { PowerUpDemo() }
        )
    )

    val page = pages[currentPage]

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0A0E1A), Color(0xFF0D1B3E), Color(0xFF0A1628))
                )
            )
    ) {
        // Animated background glow matching accent colour
        val infiniteTransition = rememberInfiniteTransition(label = "tut")
        val glowAlpha by infiniteTransition.animateFloat(
            initialValue = 0.08f, targetValue = 0.18f,
            animationSpec = infiniteRepeatable(tween(2000), RepeatMode.Reverse),
            label = "glow"
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.radialGradient(
                        colors = listOf(page.accentColor.copy(alpha = glowAlpha), Color.Transparent),
                        center = Offset.Unspecified,
                        radius = 600f
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(16.dp))

            // ── Step indicator ────────────────────────────────────────────────
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pages.forEachIndexed { i, _ ->
                    val isActive = i == currentPage
                    val width by animateDpAsState(
                        targetValue = if (isActive) 28.dp else 8.dp,
                        animationSpec = spring(stiffness = Spring.StiffnessMedium),
                        label = "dot"
                    )
                    Box(
                        modifier = Modifier
                            .height(8.dp)
                            .width(width)
                            .clip(CircleShape)
                            .background(
                                if (isActive) page.accentColor else Color.White.copy(0.25f)
                            )
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Animated page content ─────────────────────────────────────────
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(pageScrollState)
            ) {
            AnimatedContent(
                targetState = currentPage,
                modifier = Modifier.fillMaxWidth(),
                transitionSpec = {
                    (slideInHorizontally(tween(350)) { it } + fadeIn(tween(350)))
                        .togetherWith(slideOutHorizontally(tween(280)) { -it } + fadeOut(tween(280)))
                },
                label = "page"
            ) { pageIdx ->
                val p = pages[pageIdx]
                Column(horizontalAlignment = Alignment.CenterHorizontally) {

                    // ── Demo visual ───────────────────────────────────────────
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color(0xFF0D1B2A).copy(alpha = 0.8f))
                            .border(1.dp, p.accentColor.copy(0.3f), RoundedCornerShape(24.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        p.demoContent()
                    }

                    Spacer(Modifier.height(28.dp))

                    // ── Emoji + Title ─────────────────────────────────────────
                    Text(p.emoji, fontSize = 40.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        p.title,
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        p.subtitle,
                        color = p.accentColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        p.description,
                        color = Color.White.copy(alpha = 0.65f),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )
                }
            }
            }

            // ── Navigation buttons ────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Skip / Back button
                if (currentPage == 0) {
                    TextButton(onClick = onFinish) {
                        Text(
                            "Skip",
                            color = Color.White.copy(alpha = 0.45f),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                } else {
                    OutlinedButton(
                        onClick = { currentPage-- },
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color.White.copy(0.2f)),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        Text("← Back", color = Color.White.copy(0.7f), fontSize = 14.sp)
                    }
                }

                // Next / Play button
                Button(
                    onClick = {
                        if (currentPage < pages.lastIndex) currentPage++
                        else onFinish()
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.height(46.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .defaultMinSize(minWidth = 130.dp)
                            .height(46.dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(page.accentColor, page.accentColor.copy(
                                        red   = (page.accentColor.red   * 0.75f).coerceIn(0f,1f),
                                        green = (page.accentColor.green * 0.75f).coerceIn(0f,1f),
                                        blue  = (page.accentColor.blue  * 0.75f).coerceIn(0f,1f),
                                    ))
                                ),
                                RoundedCornerShape(14.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (currentPage < pages.lastIndex) "Next  →" else "▶  PLAY NOW",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp,
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
                    }
                }
            }
        }
    }
}

// ─── Demo Composables ─────────────────────────────────────────────────────────

@Composable
fun DropDemo() {
    val infiniteTransition = rememberInfiniteTransition(label = "drop")
    val dropY by infiniteTransition.animateFloat(
        initialValue = -60f, targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 2400
                -60f at 0 with FastOutSlowInEasing
                0f at 600
                0f at 1800
                -60f at 2400
            }
        ), label = "dropY"
    )
    val opacity by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 2400
                0f at 0; 1f at 200; 1f at 1800; 0f at 2400
            }
        ), label = "opacity"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Falling block
        Box(
            modifier = Modifier
                .offset(y = dropY.dp)
                .alpha(opacity)
                .size(52.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF4ECDC4), Color(0xFF00897B)))),
            contentAlignment = Alignment.Center
        ) {
            Text("4", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
        }
        Spacer(Modifier.height(4.dp))
        // Column of blocks at bottom
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("8" to Color(0xFFFF6B6B), "2" to Color(0xFF7B5EA7), "4" to Color(0xFF95C757), "16" to Color(0xFFFF9F1C)).forEach { (n, c) ->
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Brush.linearGradient(listOf(c, c.copy(red=(c.red*0.7f).coerceIn(0f,1f), green=(c.green*0.7f).coerceIn(0f,1f), blue=(c.blue*0.7f).coerceIn(0f,1f))))),
                    contentAlignment = Alignment.Center
                ) {
                    Text(n, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text("Tap any column to drop", color = Color.White.copy(0.5f), fontSize = 12.sp)
    }
}

@Composable
fun MergeDemo() {
    val infiniteTransition = rememberInfiniteTransition(label = "merge")
    val mergeScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 2000
                1f at 0; 1f at 700; 1.15f at 1000; 1f at 1300; 1f at 2000
            }
        ), label = "ms"
    )
    val showResult by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 2000
                0f at 0; 0f at 800; 1f at 1100; 1f at 2000
            }
        ), label = "sr"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 2000
                0f at 0; 0f at 700; 0.6f at 1000; 0f at 1400
            }
        ), label = "ga"
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showResult < 0.5f) {
                // Show 3 × 8 blocks
                listOf(Color(0xFFFF6B6B), Color(0xFFFF6B6B), Color(0xFFFF6B6B)).forEach { c ->
                    Box(
                        modifier = Modifier
                            .scale(mergeScale)
                            .size(56.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Brush.linearGradient(listOf(c, c.copy(red=(c.red*0.7f).coerceIn(0f,1f), green=(c.green*0.7f).coerceIn(0f,1f), blue=(c.blue*0.7f).coerceIn(0f,1f)))))
                            .then(if (glowAlpha > 0f) Modifier.border(2.dp, Color(0xFFFFD700).copy(glowAlpha), RoundedCornerShape(12.dp)) else Modifier),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("8", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            } else {
                // Show merged result
                Text("3×8 =", color = Color.White.copy(0.6f), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Box(
                    modifier = Modifier
                        .scale(mergeScale)
                        .size(68.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Brush.linearGradient(listOf(Color(0xFFFFD700), Color(0xFFFF8F00)))),
                    contentAlignment = Alignment.Center
                ) {
                    Text("32", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            if (showResult < 0.5f) "3 connected 8s detected..." else "Merged into 32! 🎉",
            color = Color.White.copy(0.6f),
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun PowerUpDemo() {
    val infiniteTransition = rememberInfiniteTransition(label = "power")
    val highlight by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
        label = "hl"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hammer demo
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF0D1B2A),
                border = BorderStroke(2.dp, Color(0xFF4ECDC4).copy(alpha = 0.5f + 0.5f * highlight))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("🔨", fontSize = 20.sp)
                    Text("Hammer", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            Text("→", color = Color.White.copy(0.5f), fontSize = 18.sp)
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFFF6B6B).copy(alpha = 0.3f))
                    .border(2.dp, Color(0xFFFF6B6B).copy(alpha = 0.7f + 0.3f * highlight), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("💥", fontSize = 20.sp)
            }
            Text("Removed!", color = Color(0xFFFF6B6B), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }

        // Swap demo
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF7B5EA7), Color(0xFF4A3070)))),
                contentAlignment = Alignment.Center
            ) {
                Text("2", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
            }
            Text("🔁", fontSize = 22.sp, modifier = Modifier.scale(0.8f + 0.4f * highlight))
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF95C757), Color(0xFF5A7A30)))),
                contentAlignment = Alignment.Center
            ) {
                Text("64", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
            }
            Text("Swapped!", color = Color(0xFFFFD700), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }

        Text(
            "💰 150 coins each use",
            color = Color(0xFFFFD700).copy(alpha = 0.7f),
            fontSize = 12.sp
        )
    }
}
