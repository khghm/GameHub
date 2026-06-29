package com.gamehub.games.othello

import com.gamehub.shared.bot.BotStrategy
import com.gamehub.shared.core.PlayerId
import kotlinx.coroutines.delay
import kotlin.random.Random

class OthelloBotStrategy : BotStrategy<OthelloState, OthelloAction> {
    override val gameId: String = "othello"
    override val supportedDifficultyLevels: IntRange = 1..10

    override suspend fun getNextMove(
        state: OthelloState,
        botPlayerId: PlayerId,
        difficultyLevel: Int
    ): OthelloAction? {
        val validMoves = getValidMoves(state, botPlayerId)
        if (validMoves.isEmpty()) return null

        // Delay based on difficulty
        val delayMs = when {
            difficultyLevel <= 3 -> 500L
            difficultyLevel <= 6 -> 1000L
            else -> 1500L
        }
        delay(delayMs)

        return when {
            difficultyLevel >= 7 -> validMoves.maxByOrNull { evaluateMove(state, it, botPlayerId) }
            difficultyLevel >= 4 -> validMoves.random(Random)
            else -> validMoves.random(Random)
        }
    }

    private fun getValidMoves(state: OthelloState, playerId: PlayerId): List<OthelloAction.Move> {
        val moves = mutableListOf<OthelloAction.Move>()
        for (row in 0..7) {
            for (col in 0..7) {
                if (isValidMove(state, row, col, playerId)) {
                    moves.add(OthelloAction.Move(row, col))
                }
            }
        }
        return moves
    }

    private fun isValidMove(state: OthelloState, row: Int, col: Int, playerId: PlayerId): Boolean {
        if (row !in 0..7 || col !in 0..7) return false
        if (state.board[row][col] != OthelloPiece.EMPTY) return false
        val piece = state.getPlayerPiece(playerId)
        return getFlips(state, row, col, piece).isNotEmpty()
    }

    private fun getFlips(state: OthelloState, row: Int, col: Int, piece: OthelloPiece): List<Pair<Int, Int>> {
        val opponent = if (piece == OthelloPiece.BLACK) OthelloPiece.WHITE else OthelloPiece.BLACK
        val flips = mutableListOf<Pair<Int, Int>>()
        val directions = listOf(-1 to -1, -1 to 0, -1 to 1, 0 to -1, 0 to 1, 1 to -1, 1 to 0, 1 to 1)

        for ((dr, dc) in directions) {
            val currentFlips = mutableListOf<Pair<Int, Int>>()
            var r = row + dr
            var c = col + dc
            while (r in 0..7 && c in 0..7) {
                val currentPiece = state.board[r][c]
                when {
                    currentPiece == opponent -> currentFlips.add(r to c)
                    currentPiece == piece && currentFlips.isNotEmpty() -> {
                        flips.addAll(currentFlips)
                        break
                    }
                    else -> break
                }
                r += dr
                c += dc
            }
        }
        return flips
    }

    private fun evaluateMove(state: OthelloState, move: OthelloAction.Move, playerId: PlayerId): Int {
        val row = move.row
        val col = move.col
        val piece = state.getPlayerPiece(playerId)
        val flips = getFlips(state, row, col, piece)
        var score = flips.size

        // Corner bonus
        if ((row == 0 || row == 7) && (col == 0 || col == 7)) {
            score += 100
        }
        // Edge bonus
        else if (row == 0 || row == 7 || col == 0 || col == 7) {
            score += 10
        }
        // X-square penalty (next to corners)
        else if ((row in 1..6 && col in 1..6) &&
            ((row == 1 && col == 1) ||
                (row == 1 && col == 6) ||
                (row == 6 && col == 1) ||
                (row == 6 && col == 6))
        ) {
            score -= 20
        }
        return score
    }
}
