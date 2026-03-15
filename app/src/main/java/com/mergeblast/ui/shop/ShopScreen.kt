package com.mergeblast.ui.shop

import androidx.compose.animation.*
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
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.*
import com.mergeblast.data.models.*
import com.mergeblast.ui.game.CoinIcon
import com.mergeblast.viewmodel.GameEvent
import com.mergeblast.viewmodel.GameViewModel
import kotlinx.coroutines.flow.collectLatest

@Composable
fun ShopScreen(
    viewModel: GameViewModel,
    onBack: () -> Unit
) {
    val playerData by viewModel.playerData.collectAsState(initial = null)
    val shopItems = remember { viewModel.getShopItems() }
    var snackMessage by remember { mutableStateOf("") }
    var showSnack by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is GameEvent.ItemPurchased -> {
                    snackMessage = "${event.item.name} purchased!"
                    showSnack = true
                }
                is GameEvent.NotEnoughCoins -> {
                    snackMessage = "Not enough coins!"
                    showSnack = true
                }
                else -> {}
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(WindowInsets.systemBars.asPaddingValues())
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF1A1035), Color(0xFF0D1B3E))
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
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Text("←", color = Color.White, fontSize = 24.sp)
                }
                Text(
                    "SHOP",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.align(Alignment.Center),
                    letterSpacing = 3.sp
                )
                // Coins display
                Row(
                    modifier = Modifier.align(Alignment.CenterEnd),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CoinIcon(size = 18.dp)
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = (playerData?.coins ?: 0).toString(),
                        color = Color(0xFFFFD700),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        "POWER UPS",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                items(shopItems.filter { it.type == ShopItemType.HAMMER || it.type == ShopItemType.SWAP }) { item ->
                    ShopItemCard(
                        item = item,
                        coins = playerData?.coins ?: 0,
                        onPurchase = { viewModel.purchaseItem(item) }
                    )
                }

                item {
                    Text(
                        "COINS",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                items(shopItems.filter { it.type == ShopItemType.COIN_PACK || it.type == ShopItemType.EXP_BOOST }) { item ->
                    ShopItemCard(
                        item = item,
                        coins = playerData?.coins ?: 0,
                        onPurchase = { viewModel.purchaseItem(item) }
                    )
                }

                item { Spacer(Modifier.height(24.dp)) }
            }
        }

        // Snackbar
        AnimatedVisibility(
            visible = showSnack,
            enter = slideInVertically { it },
            exit = slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
        ) {
            LaunchedEffect(showSnack) {
                if (showSnack) {
                    kotlinx.coroutines.delay(2000)
                    showSnack = false
                }
            }
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF1A3A5A)
            ) {
                Text(
                    snackMessage,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun ShopItemCard(item: ShopItem, coins: Int, onPurchase: () -> Unit) {
    val canAfford = item.coinCost == 0 || coins >= item.coinCost

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF0D1B2A),
        border = BorderStroke(1.dp, Color(0xFF2A4A6A)),
        modifier = Modifier.fillMaxWidth()
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
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Icon
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1A3A5A),
                    modifier = Modifier.size(52.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Text(
                            text = when (item.type) {
                                ShopItemType.HAMMER -> "🔨"
                                ShopItemType.SWAP -> "🔀"
                                ShopItemType.COIN_PACK -> "💰"
                                ShopItemType.EXP_BOOST -> "⚡"
                                ShopItemType.DAILY_REWARD_BOOST -> "🎁"
                            },
                            fontSize = 26.sp
                        )
                    }
                }

                Column {
                    Text(item.name, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text(item.description, color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                }
            }

            // Price button
            Button(
                onClick = onPurchase,
                enabled = canAfford,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (item.coinCost == 0) Color(0xFF4CAF50) else Color(0xFF2196F3),
                    disabledContainerColor = Color(0xFF2A4A6A)
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                if (item.coinCost == 0) {
                    Text("FREE", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CoinIcon(size = 14.dp)
                        Spacer(Modifier.width(4.dp))
                        Text(item.coinCost.toString(), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}