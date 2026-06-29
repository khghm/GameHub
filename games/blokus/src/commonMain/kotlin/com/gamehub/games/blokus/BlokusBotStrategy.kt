package com.gamehub.games.blokus

import com.gamehub.shared.bot.BotStrategy
import com.gamehub.shared.core.PlayerId
import kotlinx.coroutines.delay
import kotlin.random.Random

class BlokusBotStrategy : BotStrategy<BlokusState, BlokusAction> {
    override val gameId: String = "blokus"
    override val supportedDifficultyLevels: IntRange = 1..10

    private val engine = BlokusEngine()

    private fun getValidMoves(state: BlokusState, playerId: PlayerId): List<BlokusAction.Place> {
        val moves = mutableListOf<BlokusAction.Place>()
        val playerData = state.playerData[playerId] ?: return moves
        println("?? [Blokus] Bot checking for moves for $playerId, remaining pieces: ${playerData.remainingPieces}")

        // Try larger pieces first
        for (pieceId in playerData.remainingPieces.sortedDescending()) {
            val piece = BlokusPieces.firstOrNull { it.id == pieceId } ?: continue
            for (rotation in piece.shapes.indices) {
                for (r in 0..19) {
                    for (c in 0..19) {
                        if (engine.isValidMove(state, playerId, pieceId, rotation, r, c)) {
                            moves.add(BlokusAction.Place(pieceId, rotation, r, c))
                        }
                    }
                }
            }
        }

        println("?? [Blokus] Bot found ${moves.size} valid moves total")
        return moves
    }

    override suspend fun getNextMove(
        state: BlokusState,
        botPlayerId: PlayerId,
        difficultyLevel: Int
    ): BlokusAction? {
        println("?? [Blokus] Bot getNextMove called for player: $botPlayerId")
        val validMoves = getValidMoves(state, botPlayerId)
        if (validMoves.isEmpty()) {
            println("?? [Blokus] Bot no valid moves found, returning Pass")
            return BlokusAction.Pass
        }

        val delayMs = when {
            difficultyLevel <= 3 -> 50L
            difficultyLevel <= 6 -> 100L
            else -> 150L
        }
        delay(delayMs)

        val selectedMove = when {
            difficultyLevel >= 7 -> validMoves.maxByOrNull { evaluateMove(state, it, botPlayerId) }
            else -> validMoves.random(Random)
        }

        println("?? [Blokus] Bot selected move: $selectedMove")
        return selectedMove
    }

    private fun evaluateMove(
        state: BlokusState,
        move: BlokusAction.Place,
        playerId: PlayerId
    ): Int {
        // Simple heuristic: prefer larger pieces first!
        val piece = BlokusPieces.first { it.id == move.pieceId }
        return piece.size * 100
    }
}
