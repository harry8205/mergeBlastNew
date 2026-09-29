package com.mergeblast.utils

import com.mergeblast.data.models.Block
import com.mergeblast.data.models.GameState
import com.mergeblast.data.models.GameState.Companion.COLS
import com.mergeblast.data.models.GameState.Companion.ROWS
import kotlin.random.Random

object GameEngine {

    // Block color palette indices cycle based on value power
    private val BLOCK_COLORS = listOf(
        0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11
    )

    fun colorIndexForValue(value: Long): Int {
        var v = value
        var power = 0
        while (v > 1L) { v /= 2; power++ }
        return power % BLOCK_COLORS.size
    }

    // All valid block values: powers of 2 starting from 2
    // 2, 4, 8, 16, 32, 64, 128, 256, 512, 1024(1KB), ...
    fun allBlockValues(): List<Long> {
        val list = mutableListOf<Long>()
        var v = 2L
        repeat(40) { list.add(v); v *= 2 }
        return list
    }

    // Generate a next block that scales with the current highest block on the board.
    //
    // Design rules (studied from Merge Block Puzzle / 2048-style games):
    //
    //  CEILING  = highest / 4   (never give a block > 1/4 of best, so merges always feel earned)
    //  FLOOR    = highest / 64  (scales up with board — never give blocks that are irrelevant)
    //
    // Distribution within [floor..ceiling]:
    //   ~50% weight on the bottom third  (most common — keeps game flowing)
    //   ~35% weight on the middle third  (occasional mid-tier)
    //   ~15% weight on the top third     (rare high-value drop — exciting)
    //
    // Early game (highest < 64):  floor = 2, ceiling = 16/32 — classic 2/4/8/16 start
    // Mid game   (highest = 1K):  floor = 16, ceiling = 256  — 16..256 range
    // Late game  (highest = 32K): floor = 512, ceiling = 8K  — 512..8192 range
    fun generateNextBlock(currentHighest: Long): Block {
        val allValues = allBlockValues()

        // Compute floor and ceiling, both snapped to nearest power of 2 in the list
        val rawCeil  = (currentHighest / 4L).coerceAtLeast(8L)
        val rawFloor = (currentHighest / 64L).coerceAtLeast(2L)

        val candidates = allValues.filter { it in rawFloor..rawCeil }
            .ifEmpty { listOf(allValues.first()) }  // safety fallback

        // Split candidates into thirds for weighted distribution
        val third = (candidates.size / 3).coerceAtLeast(1)
        val weighted = mutableListOf<Long>()

        candidates.forEachIndexed { i, v ->
            val weight = when {
                i < third                   -> 5   // bottom third — most common
                i < third * 2               -> 3   // middle third
                else                        -> 1   // top third — rare
            }
            repeat(weight) { weighted.add(v) }
        }

        val value = weighted.random()
        return Block(value = value, colorIndex = colorIndexForValue(value))
    }

    // Generate a batch of N blocks for the upcoming queue
    fun generateQueue(size: Int, currentHighest: Long): ArrayDeque<Block> {
        val queue = ArrayDeque<Block>()
        repeat(size) { queue.addLast(generateNextBlock(currentHighest)) }
        return queue
    }

    // Drop a block into a column, returning updated state + merged blocks count
    data class DropResult(
        val newState: GameState,
        val mergesCount: Int,
        val scoreGained: Long,
        val isGameOver: Boolean
    )

    // Animation frame types — used by ViewModel to apply correct delay per frame
    enum class FrameType { PLACE, PRE_MERGE, POST_MERGE, GRAVITY }

    data class AnimFrame(
        val grid: Array<Array<Block?>>,
        val score: Long,
        val highestBlock: Long,
        val type: FrameType = FrameType.PLACE
    )

    // Returns animation frames for a full drop+merge sequence:
    //
    //  PLACE      — block lands in column (isNew=true → bounce-in animation)
    //  PRE_MERGE  — all participating blocks highlighted (isMerging=true) BEFORE merging
    //               so user sees WHICH blocks will combine
    //  POST_MERGE — merged result block shown (isMerging=true on result → flash+pulse)
    //  GRAVITY    — board settles after merge
    //
    // Each cascade level repeats PRE_MERGE → POST_MERGE → GRAVITY
    fun dropBlockFrames(state: GameState, col: Int, block: Block): Pair<List<AnimFrame>, DropResult> {
        val frames = mutableListOf<AnimFrame>()
        val grid   = copyGrid(state.grid)

        // Find lowest empty row
        var landRow = -1
        for (r in ROWS - 1 downTo 0) {
            if (grid[r][col] == null) { landRow = r; break }
        }
        if (landRow == -1) {
            return Pair(emptyList(), DropResult(state, 0, 0L, checkGameOver(grid)))
        }

        // ── FRAME: PLACE ──────────────────────────────────────────────────────
        // Show block at landing position with isNew=true so UI triggers bounce-in
        grid[landRow][col] = block.copy(row = landRow, col = col, isNew = true, isMerging = false)
        frames.add(AnimFrame(copyGrid(grid), state.score, state.highestBlock, FrameType.PLACE))
        // Clear isNew — block has landed
        grid[landRow][col] = grid[landRow][col]?.copy(isNew = false)

        var totalMerges = 0
        var totalScore  = 0L
        var highest     = state.highestBlock

        var changed = true
        while (changed) {
            changed = false

            // Find all groups that CAN merge (size >= 2) without modifying grid yet
            val mergeableGroups = findAllMergeableGroups(grid)
            if (mergeableGroups.isEmpty()) break

            changed = true

            // ── FRAME: PRE_MERGE ─────────────────────────────────────────────
            // Highlight all cells in all mergeable groups BEFORE merging
            // so user sees "these blocks are about to combine"
            val preMergeGrid = copyGrid(grid)
            for (group in mergeableGroups) {
                for ((r, c) in group) {
                    preMergeGrid[r][c] = preMergeGrid[r][c]?.copy(isMerging = true)
                }
            }
            frames.add(AnimFrame(copyGrid(preMergeGrid), state.score + totalScore, highest, FrameType.PRE_MERGE))

            // ── Perform the actual merges on grid ─────────────────────────────
            val mergeResult = performMerges(grid)
            totalMerges += mergeResult.mergeCount
            totalScore  += mergeResult.scoreGained
            for (r in 0 until ROWS) for (c in 0 until COLS)
                grid[r][c]?.let { if (it.value > highest) highest = it.value }

            // ── FRAME: POST_MERGE ────────────────────────────────────────────
            // Show merged result blocks with isMerging=true (scale pulse + glow)
            frames.add(AnimFrame(copyGrid(grid), state.score + totalScore, highest, FrameType.POST_MERGE))

            // Clear isMerging before gravity
            for (r in 0 until ROWS) for (c in 0 until COLS)
                grid[r][c] = grid[r][c]?.copy(isMerging = false)

            applyGravity(grid)

            // ── FRAME: GRAVITY ───────────────────────────────────────────────
            frames.add(AnimFrame(copyGrid(grid), state.score + totalScore, highest, FrameType.GRAVITY))
        }

        val isGameOver = checkGameOver(grid)
        val newState   = state.copy(
            grid       = grid,
            score      = state.score + totalScore,
            highestBlock = highest,
            comboCount = if (totalMerges > 1) state.comboCount + 1 else 0
        )
        return Pair(frames, DropResult(newState, totalMerges, totalScore, isGameOver))
    }

    // Find all connected groups that have size >= 2 (mergeable) without modifying grid
    private fun findAllMergeableGroups(grid: Array<Array<Block?>>): List<List<Pair<Int,Int>>> {
        val visited = mutableSetOf<Pair<Int,Int>>()
        val groups  = mutableListOf<List<Pair<Int,Int>>>()
        for (r in 0 until ROWS) {
            for (c in 0 until COLS) {
                val cell = Pair(r, c)
                if (cell in visited || grid[r][c] == null) continue
                val group = findConnectedGroup(grid, r, c)
                visited.addAll(group)
                if (group.size >= 2) groups.add(group)
            }
        }
        return groups
    }

    // Keep simple version for internal use / testing
    fun dropBlock(state: GameState, col: Int, block: Block): DropResult {
        return dropBlockFrames(state, col, block).second
    }

    data class MergeResult(
        val hasMerges: Boolean,
        val mergeCount: Int,
        val scoreGained: Long
    )

    // 4-directional neighbours
    private val DIRS = arrayOf(intArrayOf(-1,0), intArrayOf(1,0), intArrayOf(0,-1), intArrayOf(0,1))

    // BFS flood-fill: find all connected cells with the same value as (startR, startC)
    private fun findConnectedGroup(
        grid: Array<Array<Block?>>,
        startR: Int, startC: Int
    ): List<Pair<Int,Int>> {
        val value = grid[startR][startC]?.value ?: return emptyList()
        val visited = mutableSetOf<Pair<Int,Int>>()
        val queue = ArrayDeque<Pair<Int,Int>>()
        val start = Pair(startR, startC)
        queue.add(start)
        visited.add(start)
        while (queue.isNotEmpty()) {
            val (r, c) = queue.removeFirst()
            for (d in DIRS) {
                val nr = r + d[0]; val nc = c + d[1]
                if (nr in 0 until ROWS && nc in 0 until COLS) {
                    val nb = Pair(nr, nc)
                    if (nb !in visited && grid[nr][nc]?.value == value) {
                        visited.add(nb)
                        queue.add(nb)
                    }
                }
            }
        }
        return visited.toList()
    }

    // Next power of 2 >= x
    private fun nextPow2(x: Long): Long {
        var p = 1L
        while (p < x) p *= 2
        return p
    }

    // Merge ALL connected groups of 2+ same-value blocks.
    // Rule: N connected blocks of value V → ALL collapse into ONE block.
    // Result value = nextPowerOf2(N * V)
    // Examples:
    //   2 × 16 = 32   (32 is already power of 2)
    //   3 × 16 = 48   → nextPow2 = 64
    //   4 × 16 = 64   (64 is already power of 2)
    //   3 ×  4 = 12   → nextPow2 = 16
    //   5 ×  8 = 40   → nextPow2 = 64
    private fun performMerges(grid: Array<Array<Block?>>): MergeResult {
        var mergeCount = 0
        var scoreGained = 0L
        var anyMerge = false

        val processed = mutableSetOf<Pair<Int,Int>>()

        for (r in 0 until ROWS) {
            for (c in 0 until COLS) {
                val cell = Pair(r, c)
                if (cell in processed) continue
                val block = grid[r][c] ?: continue

                // Find ALL connected cells with the same value
                val group = findConnectedGroup(grid, r, c)
                processed.addAll(group)

                if (group.size < 2) continue  // lone block — nothing to merge

                val n         = group.size
                val origVal   = block.value
                val totalVal  = origVal * n           // e.g. 3 × 16 = 48
                val mergedVal = nextPow2(totalVal)     // e.g. 48 → 64

                // Pick the bottom-most, right-most cell as the survivor
                val survivor = group.maxWithOrNull(
                    compareBy<Pair<Int,Int>> { it.first }.thenBy { it.second }
                )!!

                // Place merged block at survivor cell
                grid[survivor.first][survivor.second] = Block(
                    value      = mergedVal,
                    row        = survivor.first,
                    col        = survivor.second,
                    colorIndex = colorIndexForValue(mergedVal),
                    isMerging  = true
                )

                // Remove all other cells in the group
                for (cell2 in group) {
                    if (cell2 != survivor) grid[cell2.first][cell2.second] = null
                }

                scoreGained += mergedVal
                mergeCount  += n - 1   // n blocks → 1 block = (n-1) merges
                anyMerge     = true
            }
        }

        return MergeResult(anyMerge, mergeCount, scoreGained)
    }

    private fun applyGravity(grid: Array<Array<Block?>>) {
        for (c in 0 until COLS) {
            // Collect non-null blocks in column from bottom to top
            val blocks = mutableListOf<Block>()
            for (r in ROWS - 1 downTo 0) {
                grid[r][c]?.let { blocks.add(it) }
            }
            // Clear column
            for (r in 0 until ROWS) grid[r][c] = null
            // Re-place from bottom
            blocks.forEachIndexed { i, block ->
                val row = ROWS - 1 - i
                grid[row][c] = block.copy(row = row)
            }
        }
    }

    private fun checkGameOver(grid: Array<Array<Block?>>): Boolean {
        // Game over if all columns are full
        for (c in 0 until COLS) {
            if (grid[0][c] == null) return false
        }
        return true
    }

    fun useHammer(state: GameState, row: Int, col: Int): GameState {
        val grid = copyGrid(state.grid)
        grid[row][col] = null
        applyGravity(grid)
        return state.copy(grid = grid)
    }

    // Swap two cells — works for both filled and empty cells
    fun swapBlocks(state: GameState, row1: Int, col1: Int, row2: Int, col2: Int): GameState {
        val grid = copyGrid(state.grid)
        val tmp = grid[row1][col1]
        grid[row1][col1] = grid[row2][col2]?.copy(row = row1, col = col1)
        grid[row2][col2] = tmp?.copy(row = row2, col = col2)
        // Apply gravity so floating blocks fall after swap
        applyGravity(grid)
        return state.copy(grid = grid)
    }

    private fun copyGrid(grid: Array<Array<Block?>>): Array<Array<Block?>> {
        return Array(ROWS) { r -> Array(COLS) { c -> grid[r][c]?.copy() } }
    }

    fun initialState(): GameState {
        val state = GameState()
        val nextBlock = Block(value = 1L, colorIndex = colorIndexForValue(1L))
        return state.copy(nextBlock = nextBlock)
    }
}