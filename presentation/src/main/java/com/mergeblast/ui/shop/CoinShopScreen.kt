package com.mergeblast.ui.shop

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.mergeblast.data.models.StoreProduct
import com.mergeblast.ui.components.ScreenHeader
import com.mergeblast.ui.game.CoinIcon
import com.mergeblast.viewmodel.GameViewModel

@Composable fun CoinShopScreen(viewModel: GameViewModel, prices: Map<String, String>, buy: (String)->Unit, message: String?, clear: ()->Unit, onBack: ()->Unit, restore: ()->Unit, manageSubscription: ()->Unit, restoring: Boolean) {
    val player by viewModel.playerData.collectAsState(initial = null)
    val purchases by viewModel.purchases.collectAsState(initial = emptyList())
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF1A1035), Color(0xFF0A2030))))) {
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            ScreenHeader("STORE", onBack)
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item { Row(Modifier.fillMaxWidth(), Arrangement.Center, Alignment.CenterVertically) { CoinIcon(24.dp); Spacer(Modifier.width(8.dp)); Text("${player?.coins ?: 0}", color=Color.White, fontSize=28.sp, fontWeight = FontWeight.Bold) } }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Purchases are linked to your Google Play account. No app sign-in needed.", color = Color(0xFFA7B7C8), fontSize = 13.sp)
                        OutlinedButton(onClick = restore, enabled = !restoring, modifier = Modifier.fillMaxWidth()) {
                            Text(if (restoring) "Checking purchases..." else "Restore purchases")
                        }
                        TextButton(onClick = manageSubscription, modifier = Modifier.fillMaxWidth()) { Text("Manage subscription in Google Play") }
                    }
                }
                items(StoreProduct.catalog) { product ->
                    val price = prices[product.id]
                    val owned = purchases.any { it.active && it.productId == product.id } && product.id in setOf("vip_monthly", "remove_ads", "starter_pack")
                    Surface(color=Color(0xFF0D1B2A), shape=RoundedCornerShape(8.dp), border=BorderStroke(1.dp, Color(0xFF2A4A6A))) {
                        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment=Alignment.CenterVertically) {
                            Text(if(product.id=="vip_monthly") "★" else if(product.id=="remove_ads") "×" else "●", color=Color(0xFFFFD166), fontSize=26.sp, modifier=Modifier.width(44.dp))
                            Column(Modifier.weight(1f)) { Text(product.title, color=Color.White, fontSize=16.sp, fontWeight=FontWeight.Bold); Text(product.description, color=Color(0xFFA7B7C8), fontSize=11.sp) }
                            Button(onClick={buy(product.id)}, enabled=price!=null && !owned && !restoring, shape=RoundedCornerShape(8.dp)) { Text(if (owned) { if (product.id == "vip_monthly") "Active" else "Owned" } else price ?: "Unavailable") }
                        }
                    }
                }
                item { Text("VIP renews monthly at the price shown by Google Play until canceled. Manage or cancel in Google Play. Restoring does not recover spent coins or game progress after reinstalling.", color = Color(0xFFA7B7C8), fontSize = 12.sp) }
            }
        }
        if(message!=null) AlertDialog(onDismissRequest=clear, confirmButton={TextButton(onClick=clear){Text("OK")}}, text={Text(message)})
    }
}
