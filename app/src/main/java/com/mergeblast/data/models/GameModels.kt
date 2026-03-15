package com.mergeblast.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

// ─── Block Values ────────────────────────────────────────────────────────────
enum class BlockTier(val label: String, val multiplier: Long) {
    B("B", 1L),
    KB("KB", 1_000L),
    MB("MB", 1_000_000L),
    GB("GB", 1_000_000_000L),
    T("T", 1_000_000_000_000L),
    Q("Q", 1_000_000_000_000_000L)
}

data class Block(
    val id: Int = 0,
    val value: Long = 1L,
    val row: Int = 0,
    val col: Int = 0,
    val colorIndex: Int = 0,
    val isNew: Boolean = false,
    val isMerging: Boolean = false
) {
    fun displayValue(): String {
        return when {
            value >= 1_000_000_000_000_000L -> "${value / 1_000_000_000_000_000L}Q"
            value >= 1_000_000_000_000L     -> "${value / 1_000_000_000_000L}T"
            value >= 1_000_000_000L         -> "${value / 1_000_000_000L}B"
            value >= 1_000_000L             -> "${value / 1_000_000L}M"
            value >= 1_000L                 -> "${value / 1_000L}K"
            else                            -> "$value"
        }
    }

    fun canMergeWith(other: Block): Boolean = value == other.value
    fun mergedValue(): Long = value * 2
}

// ─── Game State ───────────────────────────────────────────────────────────────
// ─── Game State ───────────────────────────────────────────────────────────────
data class GameState(
    val grid: Array<Array<Block?>> = Array(ROWS) { Array(COLS) { null } },
    val score: Long = 0L,
    val highestBlock: Long = 0L,
    val nextBlock: Block = Block(value = 1L),
    val queuedBlock: Block? = null,
    val isGameOver: Boolean = false,
    val comboCount: Int = 0
) {
    companion object {
        const val ROWS = 8
        const val COLS = 5
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GameState) return false
        return score == other.score &&
                highestBlock == other.highestBlock &&
                isGameOver == other.isGameOver &&
                comboCount == other.comboCount &&
                grid.contentDeepEquals(other.grid)
    }

    override fun hashCode(): Int {
        var result = grid.contentDeepHashCode()
        result = 31 * result + score.hashCode()
        result = 31 * result + highestBlock.hashCode()
        result = 31 * result + isGameOver.hashCode()
        result = 31 * result + comboCount
        return result
    }
}

// ─── Player Data ─────────────────────────────────────────────────────────────
@Entity(tableName = "player_data")
data class PlayerData(
    @PrimaryKey val id: Int = 1,
    val coins: Int = 0,
    val totalScore: Long = 0L,
    val highestBlock: Long = 0L,
    val gamesPlayed: Int = 0,
    val level: Int = 1,
    val exp: Int = 0,
    val trophies: Int = 0,
    val lastDailyReward: Long = 0L,
    val dailyRewardStreak: Int = 0,
    val totalMerges: Int = 0,
    val powerupsUsed: Int = 0,
    val adsWatched: Int = 0,
    val hammersOwned: Int = 0,
    val shufflesOwned: Int = 0
)

// ─── Leaderboard Entry ────────────────────────────────────────────────────────
@Entity(tableName = "leaderboard")
data class LeaderboardEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val playerName: String = "Player",
    val score: Long = 0L,
    val highestBlock: Long = 0L,
    val rank: String = "Bronze",
    val timestamp: Long = System.currentTimeMillis()
)

// ─── Shop Items ───────────────────────────────────────────────────────────────
data class ShopItem(
    val id: String,
    val name: String,
    val description: String,
    val coinCost: Int,
    val quantity: Int = 1,
    val iconRes: Int = 0,
    val type: ShopItemType
)

enum class ShopItemType {
    HAMMER, SWAP, COIN_PACK, EXP_BOOST, DAILY_REWARD_BOOST
}

// ─── Quest ────────────────────────────────────────────────────────────────────
data class Quest(
    val id: String,
    val title: String,
    val description: String,
    val target: Int,
    var progress: Int = 0,
    val reward: Int,
    val type: QuestType,
    val isCompleted: Boolean = false
) {
    val progressPercent: Float get() = (progress.toFloat() / target).coerceIn(0f, 1f)
}

enum class QuestType {
    MERGE_BLOCKS, CREATE_BLOCK, USE_POWERUPS, WATCH_ADS, COMPLETE_ALL
}

// ─── Power Up ─────────────────────────────────────────────────────────────────
data class PowerUp(
    val type: PowerUpType,
    val count: Int
)

enum class PowerUpType(val label: String, val cost: Int) {
    HAMMER("Hammer", 150),
    SWAP("Swap", 420)
}

// ─── Rank ─────────────────────────────────────────────────────────────────────
enum class Rank(val label: String, val minTrophies: Int) {
    BRONZE("Bronze", 0),
    SILVER("Silver", 10),
    GOLD("Gold", 30),
    PLATINUM("Platinum", 60),
    DIAMOND("Diamond", 100),
    MASTER("Master", 200)
}