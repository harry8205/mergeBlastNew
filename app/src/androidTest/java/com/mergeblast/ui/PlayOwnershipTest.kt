package com.mergeblast.ui

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mergeblast.data.models.PurchaseGrant
import com.mergeblast.data.repository.GameRepository
import com.mergeblast.data.repository.MergeBlastDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlayOwnershipTest {
    private lateinit var db: MergeBlastDatabase
    private lateinit var repository: GameRepository
    private val vip = PurchaseGrant("play-tokenvip_monthly", "vip_monthly", 123L)

    @Before fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, MergeBlastDatabase::class.java).build()
        repository = GameRepository(context, db)
    }

    @After fun teardown() { db.close() }

    @Test fun restoresVipOnFreshInstallAndReactivatesSameToken() = runBlocking {
        repository.syncOwnership(listOf("vip_monthly"), listOf(vip))
        assertTrue(repository.purchases.first().single().active)
        repository.syncOwnership(listOf("vip_monthly"), emptyList())
        assertFalse(repository.purchases.first().single().active)
        repository.syncOwnership(listOf("vip_monthly"), listOf(vip))
        assertTrue(repository.purchases.first().single().active)
    }

    @Test fun expiredVipDoesNotRevokePermanentAdRemoval() = runBlocking {
        repository.grantPurchase("ad-tokenremove_ads", "remove_ads", 1L, 1)
        repository.syncOwnership(listOf("vip_monthly"), listOf(vip))
        repository.syncOwnership(listOf("vip_monthly"), emptyList())
        val grants = repository.purchases.first()
        assertFalse(grants.single { it.productId == "vip_monthly" }.active)
        assertTrue(grants.single { it.productId == "remove_ads" }.active)
    }

    @Test fun repeatedRestoreDoesNotRepeatStarterRewards() = runBlocking {
        val starter = PurchaseGrant("starter-tokenstarter_pack", "starter_pack", 1L)
        repeat(3) { repository.syncOwnership(listOf("remove_ads", "starter_pack"), listOf(starter)) }
        assertEquals(3000, repository.playerData.first()!!.coins)
        assertEquals(1, repository.purchases.first().size)
    }

    @Test fun replayedConsumableKeepsOriginalQuantityWithoutDuplicateCoins() = runBlocking {
        repeat(3) { repository.grantPurchase("coin-tokencoins_1000", "coins_1000", 1L, 2) }
        assertEquals(2500, repository.playerData.first()!!.coins)
    }
}
