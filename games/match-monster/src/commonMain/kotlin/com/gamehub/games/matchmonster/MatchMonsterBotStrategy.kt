package com.gamehub.games.matchmonster

import com.gamehub.shared.bot.BotStrategy
import com.gamehub.shared.core.PlayerId
import kotlinx.coroutines.delay
import kotlin.random.Random

class MatchMonsterBotStrategy : BotStrategy<MatchMonsterState, MatchMonsterAction> {
    override val gameId: String = "match-monster"
    override val supportedDifficultyLevels: IntRange = 1..10

    private val engine = MatchMonsterEngine()

    override suspend fun getNextMove(
        state: MatchMonsterState,
        botPlayerId: PlayerId,
        difficultyLevel: Int
    ): MatchMonsterAction? {
        val playerDataMap = state.getPlayerDataMap()
        val playerData = playerDataMap[botPlayerId] ?: return null
        val board = playerData.board

        // Step 1: Generate all valid swap candidates
        val candidates = generateAllValidCandidates(board)
        if (candidates.isEmpty()) return null

        // Step 2: Apply delay based on difficulty
        val delayMs = getDelayForDifficulty(difficultyLevel)
        delay(delayMs)

        // Step 3: Evaluate and select candidate
        val selectedCandidate = when {
            difficultyLevel <= 3 -> selectWithHighRandomness(candidates, board, difficultyLevel)
            difficultyLevel in 4..7 -> selectWithHeuristic(candidates, board, difficultyLevel)
            else -> selectOptimal(candidates, board)
        }

        return MatchMonsterAction.SwapTiles(selectedCandidate.first, selectedCandidate.second)
    }

    private fun generateAllValidCandidates(board: List<List<Tile?>>): List<Pair<Pair<Int, Int>, Pair<Int, Int>>> {
        val candidates = mutableListOf<Pair<Pair<Int, Int>, Pair<Int, Int>>>()

        for (row in 0 until 8) {
            for (col in 0 until 6) {
                // Check swap with right neighbor
                if (col < 5) {
                    val p1 = row to col
                    val p2 = row to (col + 1)
                    if (engine.validateSwap(board, p1, p2)) {
                        candidates.add(p1 to p2)
                    }
                }
                // Check swap with down neighbor
                if (row < 7) {
                    val p1 = row to col
                    val p2 = (row + 1) to col
                    if (engine.validateSwap(board, p1, p2)) {
                        candidates.add(p1 to p2)
                    }
                }
            }
        }

        return candidates
    }

    private fun getDelayForDifficulty(level: Int): Long {
        return when (level) {
            1 -> Random.nextLong(3000, 4000)
            2 -> Random.nextLong(2000, 3000)
            3 -> Random.nextLong(1500, 2000)
            4 -> Random.nextLong(1000, 1500)
            5 -> Random.nextLong(800, 1200)
            6 -> Random.nextLong(500, 800)
            7 -> Random.nextLong(400, 600)
            8 -> Random.nextLong(200, 400)
            9 -> Random.nextLong(100, 200)
            10 -> Random.nextLong(50, 150)
            else -> 500
        }
    }

    private fun getRandomnessChance(level: Int): Double {
        return when (level) {
            1 -> 0.9
            2 -> 0.7
            3 -> 0.5
            4 -> 0.3
            5 -> 0.15
            else -> 0.0
        }
    }

    private fun selectWithHighRandomness(
        candidates: List<Pair<Pair<Int, Int>, Pair<Int, Int>>>,
        board: List<List<Tile?>>,
        level: Int
    ): Pair<Pair<Int, Int>, Pair<Int, Int>> {
        val chance = getRandomnessChance(level)
        return if (Random.nextDouble() < chance) {
            candidates.random()
        } else {
            selectOptimal(candidates, board)
        }
    }

    private fun selectWithHeuristic(
        candidates: List<Pair<Pair<Int, Int>, Pair<Int, Int>>>,
        board: List<List<Tile?>>,
        level: Int
    ): Pair<Pair<Int, Int>, Pair<Int, Int>> {
        val chance = getRandomnessChance(level)
        return if (Random.nextDouble() < chance) {
            candidates.sortedByDescending { scoreCandidate(it, board) }[1] // Pick second best
        } else {
            selectOptimal(candidates, board)
        }
    }

    private fun selectOptimal(
        candidates: List<Pair<Pair<Int, Int>, Pair<Int, Int>>>,
        board: List<List<Tile?>>
    ): Pair<Pair<Int, Int>, Pair<Int, Int>> {
        return candidates.maxByOrNull { scoreCandidate(it, board) } ?: candidates.first()
    }

    private fun scoreCandidate(candidate: Pair<Pair<Int, Int>, Pair<Int, Int>>, board: List<List<Tile?>>): Double {
        val (p1, p2) = candidate
        val result = engine.processSwap(board, p1, p2)

        // Heuristic: Score = damage * 1.0 + garbage * 2.0
        return result.damage * 1.0 + result.garbageCount * 2.0
    }
}
