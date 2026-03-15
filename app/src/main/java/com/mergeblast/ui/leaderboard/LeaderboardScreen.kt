package com.mergeblast.ui.leaderboard

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.*
import com.mergeblast.data.models.*
import com.mergeblast.data.models.Block
import com.mergeblast.ui.game.BlockTile
import com.mergeblast.ui.game.formatScore
import com.mergeblast.viewmodel.GameViewModel

@Composable
fun LeaderboardScreen(
    viewModel: GameViewModel,
    onBack: () -> Unit
) {
    val entries by viewModel.leaderboard.collectAsState(initial = emptyList())

    // Generate mock AI opponents for display
    val mockEntries = remember { generateMockLeaderboard() }
    val allEntries = remember(entries) { (mockEntries + entries).sortedByDescending { it.score }.take(50) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(WindowInsets.systemBars.asPaddingValues())
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF1A1035), Color(0xFF0D1B3E))
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                    Text("←", color = Color.White, fontSize = 24.sp)
                }
                Text(
                    "LEADERBOARD",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.align(Alignment.Center),
                    letterSpacing = 2.sp
                )
            }

            // Top 3 podium
            if (allEntries.size >= 3) {
                PodiumSection(
                    first = allEntries[0],
                    second = allEntries[1],
                    third = allEntries[2]
                )
            }

            Spacer(Modifier.height(8.dp))

            // Full list
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                itemsIndexed(allEntries.drop(3)) { index, entry ->
                    LeaderboardRow(
                        rank = index + 4,
                        entry = entry,
                        isYou = entry.playerName == "You"
                    )
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
fun PodiumSection(
    first: LeaderboardEntry,
    second: LeaderboardEntry,
    third: LeaderboardEntry
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        PodiumItem(entry = second, rank = 2, height = 80.dp)
        PodiumItem(entry = first, rank = 1, height = 110.dp)
        PodiumItem(entry = third, rank = 3, height = 60.dp)
    }
}

@Composable
fun PodiumItem(entry: LeaderboardEntry, rank: Int, height: Dp) {
    val (medalEmoji, borderColor) = when (rank) {
        1 -> "🥇" to Color(0xFFFFD700)
        2 -> "🥈" to Color(0xFFB0BEC5)
        else -> "🥉" to Color(0xFFBF8970)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(100.dp)
    ) {
        Text(medalEmoji, fontSize = 22.sp)
        Text(
            entry.playerName,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
        Text(
            formatScore(entry.score),
            color = Color(0xFFFFD700),
            fontSize = 11.sp
        )

        BlockTile(
            block = Block(value = entry.highestBlock.coerceAtLeast(1L), colorIndex = rank),
            size = 42.dp,
            isSelected = false
        )

        // Podium base
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(height),
            shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
            color = Color(0xFF0D1B2A),
            border = BorderStroke(1.dp, borderColor.copy(alpha = 0.5f))
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    "#$rank",
                    color = borderColor,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

@Composable
fun LeaderboardRow(rank: Int, entry: LeaderboardEntry, isYou: Boolean) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isYou) Color(0xFF1A3A5A) else Color(0xFF0D1B2A),
        border = BorderStroke(
            1.dp,
            if (isYou) Color(0xFF2196F3) else Color(0xFF1A3A5A)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "#$rank",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(36.dp)
            )

            Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                Text(
                    entry.playerName + if (isYou) " (You)" else "",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "Best: ${formatScore(entry.highestBlock)}",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 11.sp
                )
            }

            Text(
                formatScore(entry.score),
                color = if (isYou) Color(0xFF64B5F6) else Color(0xFFFFD700),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun generateMockLeaderboard(): List<LeaderboardEntry> {
    val names = listOf("MergeKing", "BlockMaster", "TileGuru", "NumbCrush", "MegaMerge",
        "PixelPro", "GridWizard", "FusionAce", "MathBlast", "ComboKing",
        "ZenMerger", "TurboBlock", "UltraFuse", "MegaTile", "QuantumMerge")
    val baseScores = listOf(
        500_000_000_000_000L, 450_000_000_000_000L, 380_000_000_000_000L,
        320_000_000_000_000L, 280_000_000_000_000L, 240_000_000_000_000L,
        200_000_000_000_000L, 170_000_000_000_000L, 140_000_000_000_000L,
        120_000_000_000_000L, 100_000_000_000_000L, 80_000_000_000_000L,
        60_000_000_000_000L, 40_000_000_000_000L, 20_000_000_000_000L
    )
    return names.mapIndexed { i, name ->
        LeaderboardEntry(
            id = -(i + 1),
            playerName = name,
            score = baseScores[i],
            highestBlock = baseScores[i] / 1000,
            rank = when {
                i * 10 >= 200 -> "Master"
                i * 10 >= 100 -> "Diamond"
                i * 10 >= 60  -> "Platinum"
                i * 10 >= 30  -> "Gold"
                i * 10 >= 10  -> "Silver"
                else          -> "Bronze"
            }
        )
    }
}