package com.mergeblast.ui.quest

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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.*
import com.mergeblast.data.models.*
import com.mergeblast.ui.game.CoinIcon
import com.mergeblast.ui.components.ScreenHeader
import com.mergeblast.viewmodel.GameViewModel

@Composable
fun QuestScreen(
    viewModel: GameViewModel,
    onBack: () -> Unit
) {
    val playerData by viewModel.playerData.collectAsState(initial = null)
    var quests by remember { mutableStateOf<List<Quest>>(emptyList()) }

    LaunchedEffect(Unit) {
        quests = viewModel.getDailyQuests()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(WindowInsets.systemBars.asPaddingValues())
            .background(Color.Black)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            ScreenHeader(title = "DAILY QUEST", onBack = onBack)

            // Coins display
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xFF0D1B2A),
                    border = BorderStroke(1.dp, Color(0xFFFFD700).copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CoinIcon(size = 22.dp)
                        Text(
                            text = (playerData?.coins ?: 0).toString(),
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text("+", color = Color(0xFF4CAF50), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(quests) { quest ->
                    QuestCard(quest = quest)
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
fun QuestCard(quest: Quest) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF0F2036),
        border = BorderStroke(1.dp, Color(0xFF1A4A7A))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Quest icon
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1A3A5A),
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Text(
                            text = when (quest.type) {
                                QuestType.MERGE_BLOCKS -> "🟧"
                                QuestType.CREATE_BLOCK -> "2Q"
                                QuestType.USE_POWERUPS -> "🔨"
                                QuestType.WATCH_ADS -> "📺"
                                QuestType.COMPLETE_ALL -> "✅"
                            },
                            fontSize = if (quest.type == QuestType.CREATE_BLOCK) 18.sp else 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (quest.type == QuestType.CREATE_BLOCK) Color(0xFF4ECDC4) else Color.Unspecified
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        quest.title.uppercase(),
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(Modifier.height(4.dp))

                    // Progress bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color(0xFF1A3A5A))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(quest.progressPercent)
                                .fillMaxHeight()
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(Color(0xFF2196F3), Color(0xFF00BCD4))
                                    )
                                )
                        )
                    }

                    Spacer(Modifier.height(2.dp))
                    Text(
                        "${quest.progress} / ${quest.target}",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            // Reward
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (quest.isCompleted) Color(0xFF1B5E20) else Color(0xFF0D1B2A),
                border = BorderStroke(1.dp, if (quest.isCompleted) Color(0xFF4CAF50) else Color(0xFF2A4A6A))
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CoinIcon(size = 20.dp)
                    Text(
                        quest.reward.toString(),
                        color = if (quest.isCompleted) Color(0xFF4CAF50) else Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
