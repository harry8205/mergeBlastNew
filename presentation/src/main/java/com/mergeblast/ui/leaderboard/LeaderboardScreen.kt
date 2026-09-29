package com.mergeblast.ui.leaderboard

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.mergeblast.data.models.*
import com.mergeblast.data.models.Block
import com.mergeblast.ui.game.BlockTile
import com.mergeblast.ui.components.ScreenHeader
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
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF1A1035), Color(0xFF0A2030))
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
            ScreenHeader(title = "LEADERBOARD", onBack = onBack)

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp)
            ) {
                if (allEntries.size >= 3) {
                    item {
                        PodiumSection(allEntries[0], allEntries[1], allEntries[2])
                    }
                }
                item {
                    Row(
                        Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("RANK / PLAYER", color = Color(0xFF8CA4B8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("SCORE", color = Color(0xFF8CA4B8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Divider(color = Color(0xFF304255))
                }
                itemsIndexed(if (allEntries.size >= 3) allEntries.drop(3) else allEntries) { index, entry ->
                    LeaderboardRow(
                        rank = index + if (allEntries.size >= 3) 4 else 1,
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
            .padding(top = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        PodiumItem(entry = second, rank = 2, height = 64.dp, modifier = Modifier.weight(1f))
        PodiumItem(entry = first, rank = 1, height = 92.dp, modifier = Modifier.weight(1f))
        PodiumItem(entry = third, rank = 3, height = 48.dp, modifier = Modifier.weight(1f))
    }
}

@Composable
fun PodiumItem(entry: LeaderboardEntry, rank: Int, height: Dp, modifier: Modifier = Modifier) {
    val (medalEmoji, borderColor) = when (rank) {
        1 -> "🥇" to Color(0xFFFFD700)
        2 -> "🥈" to Color(0xFFB0BEC5)
        else -> "🥉" to Color(0xFFBF8970)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(medalEmoji, fontSize = 22.sp)
        Text(
            entry.playerName,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            formatScore(entry.score),
            color = borderColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
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
            color = borderColor.copy(alpha = 0.1f),
            border = BorderStroke(1.dp, borderColor.copy(alpha = 0.5f))
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    "#$rank",
                    color = borderColor,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

@Composable
fun LeaderboardRow(rank: Int, entry: LeaderboardEntry, isYou: Boolean) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isYou) Color(0xFF163C40) else Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 76.dp)
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "#$rank",
                color = if (isYou) Color(0xFF76E1D6) else Color(0xFF8CA4B8),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(36.dp)
            )

            Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                Text(
                    entry.playerName + if (isYou) " (You)" else "",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "Best block  ${formatScore(entry.highestBlock)}",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Text(
                formatScore(entry.score),
                color = if (isYou) Color(0xFF76E1D6) else Color(0xFFFFD166),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
    Divider(color = Color(0xFF304255).copy(alpha = 0.5f))
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
