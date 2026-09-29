package com.mergeblast.data.repository

import androidx.room.*
import com.mergeblast.data.models.LeaderboardEntry
import com.mergeblast.data.models.PlayerData
import com.mergeblast.data.models.PurchaseGrant
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

// ─── DAOs ─────────────────────────────────────────────────────────────────────
@Dao
interface PlayerDao {
    @Query("UPDATE player_data SET coins = coins + :coins, hammersOwned = hammersOwned + :hammers, shufflesOwned = shufflesOwned + :swaps WHERE id = 1")
    suspend fun grantItems(coins: Int, hammers: Int, swaps: Int)

    @Query("UPDATE player_data SET hammersOwned = hammersOwned - 1 WHERE id = 1 AND hammersOwned > 0")
    suspend fun useOwnedHammer(): Int

    @Query("UPDATE player_data SET shufflesOwned = shufflesOwned - 1 WHERE id = 1 AND shufflesOwned > 0")
    suspend fun useOwnedSwap(): Int
    @Query("SELECT * FROM player_data WHERE id = 1")
    fun getPlayerData(): Flow<PlayerData?>

    @Query("SELECT * FROM player_data WHERE id = 1")
    suspend fun getPlayerDataOnce(): PlayerData?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(playerData: PlayerData)

    @Query("UPDATE player_data SET coins = coins + :amount WHERE id = 1")
    suspend fun addCoins(amount: Int)

    @Query("UPDATE player_data SET coins = coins - :amount WHERE id = 1")
    suspend fun spendCoins(amount: Int)

    @Query("UPDATE player_data SET totalMerges = totalMerges + :count WHERE id = 1")
    suspend fun incrementMerges(count: Int)

    @Query("UPDATE player_data SET powerupsUsed = powerupsUsed + 1 WHERE id = 1")
    suspend fun incrementPowerupsUsed()

    @Query("UPDATE player_data SET adsWatched = adsWatched + 1 WHERE id = 1")
    suspend fun incrementAdsWatched()

    @Query("UPDATE player_data SET exp = exp + :amount WHERE id = 1")
    suspend fun addExp(amount: Int)

    @Query("UPDATE player_data SET trophies = trophies + :amount WHERE id = 1")
    suspend fun addTrophies(amount: Int)

    @Query("UPDATE player_data SET lastDailyReward = :timestamp, dailyRewardStreak = :streak WHERE id = 1")
    suspend fun updateDailyReward(timestamp: Long, streak: Int)

    @Query("UPDATE player_data SET level = :level WHERE id = 1")
    suspend fun setLevel(level: Int)
}

@Dao
interface LeaderboardDao {
    @Query("SELECT * FROM leaderboard ORDER BY score DESC LIMIT 100")
    fun getLeaderboard(): Flow<List<LeaderboardEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: LeaderboardEntry)

    @Query("SELECT COUNT(*) + 1 FROM leaderboard WHERE score > :score")
    suspend fun getRank(score: Long): Int
}

// ─── Database ─────────────────────────────────────────────────────────────────
@Database(
    entities = [PlayerData::class, LeaderboardEntry::class, PurchaseGrant::class],
    version = 2,
    exportSchema = false
)
abstract class MergeBlastDatabase : RoomDatabase() {
    abstract fun playerDao(): PlayerDao
    abstract fun leaderboardDao(): LeaderboardDao
    abstract fun purchaseDao(): PurchaseDao

    companion object {
        const val DATABASE_NAME = "merge_blast_db"
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS purchase_grants (token TEXT NOT NULL PRIMARY KEY, productId TEXT NOT NULL, purchasedAt INTEGER NOT NULL, active INTEGER NOT NULL)")
            }
        }
    }
}

@Dao
interface PurchaseDao {
    @Query("SELECT * FROM purchase_grants")
    fun observe(): Flow<List<PurchaseGrant>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(grant: PurchaseGrant): Long

    @Query("UPDATE purchase_grants SET active = :active WHERE token = :token")
    suspend fun setActive(token: String, active: Boolean)

    @Query("UPDATE purchase_grants SET active = 0 WHERE productId IN (:products)")
    suspend fun deactivate(products: List<String>)
}
