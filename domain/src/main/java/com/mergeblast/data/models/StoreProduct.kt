package com.mergeblast.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

data class StoreProduct(val id: String, val title: String, val description: String, val coins: Int = 0) {
    companion object {
        val catalog = listOf(
            StoreProduct("remove_ads", "Remove Ads Forever", "No forced ads. Rewarded ads remain optional."),
            StoreProduct("starter_pack", "Starter Pack", "2,500 coins + 5 Hammers + 5 Swaps + 3 days without forced ads", 2500),
            StoreProduct("coins_1000", "1,000 Coins", "1,000 coins", 1000),
            StoreProduct("coins_2500", "2,500 Coins", "2,500 coins", 2500),
            StoreProduct("coins_5000", "5,000 Coins", "5,000 coins", 5000),
            StoreProduct("coins_10000", "10,000 Coins", "10,000 coins", 10000),
            StoreProduct("coins_25000", "25,000 Coins", "25,000 coins", 25000),
            StoreProduct("vip_monthly", "MergeBlast VIP", "Ad-free + daily coins, 1 Hammer, 1 Swap, and 2x Daily Rewards")
        )
    }
}

@Entity(tableName = "purchase_grants")
data class PurchaseGrant(
    @PrimaryKey val token: String,
    val productId: String,
    val purchasedAt: Long,
    val active: Boolean = true
)
