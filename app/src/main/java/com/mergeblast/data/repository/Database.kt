package com.mergeblast.data.repository

import androidx.room.*
import com.mergeblast.data.models.LeaderboardEntry
import com.mergeblast.data.models.PlayerData
import kotlinx.coroutines.flow.Flow

// ─── DAOs ─────────────────────────────────────────────────────────────────────
@Dao
interface PlayerDao {
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
    entities = [PlayerData::class, LeaderboardEntry::class],
    version = 1,
    exportSchema = false
)
abstract class MergeBlastDatabase : RoomDatabase() {
    abstract fun playerDao(): PlayerDao
    abstract fun leaderboardDao(): LeaderboardDao

    companion object {
        const val DATABASE_NAME = "merge_blast_db"
    }
}
