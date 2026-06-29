package com.gamehub.games.backgammon

import com.gamehub.shared.bot.BotStrategy
import com.gamehub.shared.core.PlayerId
import kotlinx.coroutines.delay
import kotlin.random.Random

class BackgammonBotStrategy : BotStrategy<BackgammonState, BackgammonAction> {
    override val gameId: String = "backgammon"
    override val supportedDifficultyLevels: IntRange = 1..10
    private val engine = BackgammonEngine()

    override suspend fun getNextMove(
        state: BackgammonState,
        botPlayerId: PlayerId,
        difficultyLevel: Int
    ): BackgammonAction? {
        val delayMs = when {
            difficultyLevel <= 3 -> 50L  // Very fast
            difficultyLevel <= 6 -> 100L // Fast
            else -> 150L // Still fast but slight delay
        }
        delay(delayMs)

        val botColor = BackgammonRules.getPlayerColor(state, botPlayerId)

        if (!state.diceRolled) {
            return BackgammonAction.RollDice
        }

        val validMoves = getValidMovesWithDice(state, botColor)
        if (validMoves.isEmpty()) {
            return BackgammonAction.EndTurn
        }

        // Choose move based on difficulty
        val selectedMove = if (difficultyLevel <= 2) {
            validMoves.random()
        } else {
            // Simple heuristic: prefer bearing off, then hitting blots, then moving forward
            validMoves.sortedWith(
                compareByDescending<BackgammonAction.Move> { it.to == 25 } // Prefer bearing off
                    .thenByDescending { move -> isHit(state, botColor, move.to) } // Then prefer hitting
                    .thenByDescending { move -> distanceForward(botColor, move.from, move.to) } // Then prefer moving forward more
            ).firstOrNull() ?: validMoves.random()
        }

        return selectedMove
    }

    private fun getValidMovesWithDice(
        state: BackgammonState,
        color: BackgammonColor
    ): List<BackgammonAction.Move> {
        val moves = mutableListOf<BackgammonAction.Move>()
        for (die in state.dice.distinct()) {
            val rulesMoves = BackgammonRules.getValidMoves(state, color, listOf(die))
            for ((from, to) in rulesMoves) {
                moves.add(BackgammonAction.Move(from = from, to = to, die = die))
            }
        }
        return moves
    }

    private fun isHit(state: BackgammonState, color: BackgammonColor, to: Int): Boolean {
        if (to !in 1..24) return false
        val target = state.points[to]
        return target.isBlot && target.owner == color.opponent()
    }

    private fun distanceForward(color: BackgammonColor, from: Int, to: Int): Int {
        return if (color == BackgammonColor.WHITE) to - from else from - to
    }
}
