// games/tictactoe/src/commonMain/kotlin/com/gamehub/games/tictactoe/TicTacToeBotStrategy.kt
package com.gamehub.games.tictactoe

import com.gamehub.shared.bot.BotStrategy
import com.gamehub.shared.core.PlayerId
import com.gamehub.shared.engines.board.BoardAction
import kotlin.random.Random

class TicTacToeBotStrategy : BotStrategy<TicTacToeState, BoardAction> {
    override val gameId: String = "tictactoe"
    override val supportedDifficultyLevels: IntRange = 1..10

    override suspend fun getNextMove(state: TicTacToeState, botPlayerId: PlayerId, difficultyLevel: Int): BoardAction? {
        val emptyCells = getEmptyCells(state)
        if (emptyCells.isEmpty()) return null

        val opponent = state.players.first { it != botPlayerId }

        // سطح 1-2: حرکت کاملاً تصادفی
        if (difficultyLevel <= 2) {
            return emptyCells.random()
        }

        // سطح 3-4: اولویت مرکز و گوشه‌ها
        if (difficultyLevel <= 4) {
            // اولویت: مرکز (1,1)
            if (emptyCells.any { it.row == 1 && it.col == 1 }) {
                return BoardAction(1, 1)
            }
            // گوشه‌ها
            val corners = listOf(0 to 0, 0 to 2, 2 to 0, 2 to 2)
            for ((r, c) in corners) {
                if (emptyCells.any { it.row == r && it.col == c }) {
                    return BoardAction(r, c)
                }
            }
            return emptyCells.random()
        }

        // سطح 5-7: مسدود کردن برد حریف
        for (move in emptyCells) {
            val simState = simulateMove(state, move, opponent)
            if (checkWin(simState, opponent)) {
                return move
            }
        }

        // سطح 8-10: Minimax با عمق کامل
        if (difficultyLevel >= 8) {
            val bestMove = findBestMoveMinimax(state, botPlayerId)
            if (bestMove != null) return bestMove
        }

        // در غیر این صورت حرکت تصادفی
        return emptyCells.random()
    }

    private fun getEmptyCells(state: TicTacToeState): List<BoardAction> {
        val result = mutableListOf<BoardAction>()
        for (r in 0 until 3) {
            for (c in 0 until 3) {
                if (state.grid[r][c] == null) result.add(BoardAction(r, c))
            }
        }
        return result
    }

    private fun simulateMove(state: TicTacToeState, move: BoardAction, player: PlayerId): TicTacToeState {
        val newGrid = state.grid.map { it.toMutableList() }
        newGrid[move.row][move.col] = player
        return TicTacToeState(newGrid, state.currentPlayer, state.players)
    }

    private fun checkWin(state: TicTacToeState, player: PlayerId): Boolean {
        val b = state.grid
        for (i in 0..2) {
            if (b[i][0] == player && b[i][1] == player && b[i][2] == player) return true
            if (b[0][i] == player && b[1][i] == player && b[2][i] == player) return true
        }
        if (b[0][0] == player && b[1][1] == player && b[2][2] == player) return true
        if (b[0][2] == player && b[1][1] == player && b[2][0] == player) return true
        return false
    }

    private fun findBestMoveMinimax(state: TicTacToeState, bot: PlayerId): BoardAction? {
        val opponent = state.players.first { it != bot }
        var bestScore = Int.MIN_VALUE
        var bestMove: BoardAction? = null

        for (move in getEmptyCells(state)) {
            val newState = simulateMove(state, move, bot)
            val score = minimax(newState, 0, false, bot, opponent)
            if (score > bestScore) {
                bestScore = score
                bestMove = move
            }
        }
        return bestMove
    }

    private fun minimax(state: TicTacToeState, depth: Int, isMaximizing: Boolean, bot: PlayerId, opponent: PlayerId): Int {
        if (checkWin(state, bot)) return 10 - depth
        if (checkWin(state, opponent)) return depth - 10
        if (getEmptyCells(state).isEmpty()) return 0

        if (isMaximizing) {
            var best = Int.MIN_VALUE
            for (move in getEmptyCells(state)) {
                val newState = simulateMove(state, move, bot)
                best = maxOf(best, minimax(newState, depth + 1, false, bot, opponent))
            }
            return best
        } else {
            var best = Int.MAX_VALUE
            for (move in getEmptyCells(state)) {
                val newState = simulateMove(state, move, opponent)
                best = minOf(best, minimax(newState, depth + 1, true, bot, opponent))
            }
            return best
        }
    }
}