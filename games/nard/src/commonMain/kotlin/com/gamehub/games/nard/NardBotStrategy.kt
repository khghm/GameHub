package com.gamehub.games.nard

import com.gamehub.shared.bot.BotStrategy
import com.gamehub.shared.core.PlayerId
import kotlinx.coroutines.delay
import kotlin.random.Random

class NardBotStrategy : BotStrategy<NardState, NardAction> {
    override val gameId: String = "nard"
    override val supportedDifficultyLevels: IntRange = 1..10
    private val engine = NardEngine()

    override suspend fun getNextMove(
        state: NardState,
        botPlayerId: PlayerId,
        difficultyLevel: Int
    ): NardAction? {
        val delayMs = when {
            difficultyLevel <= 3 -> 50L
            difficultyLevel <= 6 -> 100L
            else -> 150L
        }
        delay(delayMs)

        val botColor = NardRules.getPlayerColor(state, botPlayerId)

        // Handle double offer response
        if (state.doubleOffered && state.doubleOfferedBy != botPlayerId) {
            return if (shouldAcceptDouble(state, botColor, difficultyLevel)) {
                NardAction.AcceptDouble
            } else {
                NardAction.DeclineDouble
            }
        }

        // Offer double if advantageous
        if (!state.diceRolled && state.canOfferDouble && !state.isCrawfordGame &&
            (state.doublingCube.owner == null || state.doublingCube.owner == botPlayerId)) {
            if (shouldOfferDouble(state, botColor, difficultyLevel)) {
                return NardAction.OfferDouble
            }
        }

        if (!state.diceRolled) {
            return NardAction.RollDice
        }

        // Get all valid move sequences and choose the best one
        val validSequences = NardRules.getAllValidMoveSequences(state, botColor, state.dice)
        if (validSequences.isEmpty()) {
            return NardAction.EndTurn
        }

        val bestSequence = chooseBestSequence(validSequences, state, botColor, difficultyLevel)
        if (bestSequence.isNotEmpty()) {
            return bestSequence.first()
        }

        return NardAction.EndTurn
    }

    private fun shouldAcceptDouble(
        state: NardState,
        color: NardColor,
        difficultyLevel: Int
    ): Boolean {
        // Simple heuristic: accept if winning or tied
        val ourScore = evaluatePosition(state, color)
        val theirScore = evaluatePosition(state, color.opponent())
        val acceptThreshold = when (difficultyLevel) {
            in 1..3 -> 0.3
            in 4..7 -> 0.5
            else -> 0.7
        }
        return ourScore >= theirScore * acceptThreshold
    }

    private fun shouldOfferDouble(
        state: NardState,
        color: NardColor,
        difficultyLevel: Int
    ): Boolean {
        if (difficultyLevel <= 3) return false // Don't offer on easy
        if (state.doublingCube.value >= 64) return false

        val ourScore = evaluatePosition(state, color)
        val theirScore = evaluatePosition(state, color.opponent())
        val offerThreshold = when (difficultyLevel) {
            in 4..7 -> 1.5
            else -> 1.3
        }
        return ourScore >= theirScore * offerThreshold
    }

    private fun chooseBestSequence(
        sequences: List<List<NardAction.Move>>,
        state: NardState,
        color: NardColor,
        difficultyLevel: Int
    ): List<NardAction.Move> {
        if (sequences.isEmpty()) return emptyList()
        if (difficultyLevel <= 2) return sequences.random()

        val scoredSequences = sequences.map { seq ->
            val finalState = applySequence(state, color, seq)
            val score = evaluatePosition(finalState, color)
            seq to score
        }

        return scoredSequences.maxByOrNull { it.second }?.first ?: sequences.first()
    }

    private fun applySequence(
        initialState: NardState,
        color: NardColor,
        moves: List<NardAction.Move>
    ): NardState {
        var state = initialState
        for (move in moves) {
            state = NardRules.applyMove(state, color, move.from, move.to, move.die)
        }
        return state
    }

    private fun evaluatePosition(state: NardState, color: NardColor): Double {
        var score = 0.0

        // 1. Count of checkers borne off (most important)
        val borneOff = if (color == NardColor.WHITE) state.borneOffWhite else state.borneOffBlack
        score += borneOff * 10.0

        // 2. Checkers in home board
        val homeRange = if (color == NardColor.WHITE) 19..24 else 1..6
        for (point in homeRange) {
            val p = state.points[point]
            if (p.owner == color) {
                score += p.checkers.size * 2.0
            }
        }

        // 3. Penalty for checkers on bar
        val barCount = if (color == NardColor.WHITE) state.barWhite else state.barBlack
        score -= barCount * 15.0

        // 4. Bonus for hitting opponent blots and blocking
        val opponent = color.opponent()
        for (point in 1..24) {
            val p = state.points[point]
            if (p.owner == opponent && p.isBlot) {
                // Check if we can hit this blot (simplified)
                score += 5.0
            }
            if (p.owner == color && p.checkers.size >= 2) {
                score += 3.0 // Bonus for blocks
            }
        }

        return score
    }
}
