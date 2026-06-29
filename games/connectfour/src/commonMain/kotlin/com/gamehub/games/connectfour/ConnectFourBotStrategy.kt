// games/connectfour/src/commonMain/kotlin/com/gamehub/games/connectfour/ConnectFourBotStrategy.kt
package com.gamehub.games.connectfour

import com.gamehub.shared.bot.BotStrategy
import com.gamehub.shared.core.PlayerId
import com.gamehub.shared.engines.board.BoardAction
import kotlin.random.Random

class ConnectFourBotStrategy : BotStrategy<ConnectFourState, BoardAction> {
    override val gameId: String = "connectfour"
    override val supportedDifficultyLevels: IntRange = 1..10

    override suspend fun getNextMove(state: ConnectFourState, botPlayerId: PlayerId, difficultyLevel: Int): BoardAction? {
        val validCols = getValidColumns(state)
        if (validCols.isEmpty()) return null

        val opponent = state.players.first { it != botPlayerId }

        // سطح 1-2: حرکت تصادفی
        if (difficultyLevel <= 2) {
            return BoardAction(0, validCols.random())
        }

        // سطح 3-4: اولویت مرکز
        if (difficultyLevel <= 4) {
            val centerCols = listOf(3, 2, 4, 1, 5, 0, 6)
            for (col in centerCols) {
                if (col in validCols) return BoardAction(0, col)
            }
        }

        // سطح 5-7: مسدود کردن برد حریف
        for (col in validCols) {
            val row = getEmptyRow(state, col)
            val simState = simulateMove(state, row, col, opponent)
            if (checkWin(simState, opponent)) {
                return BoardAction(0, col)
            }
        }

        // سطح 8-10: انتخاب بهترین ستون با امتیازدهی
        if (difficultyLevel >= 8) {
            var bestCol = validCols[0]
            var bestScore = -1
            for (col in validCols) {
                val row = getEmptyRow(state, col)
                val simState = simulateMove(state, row, col, botPlayerId)
                val score = evaluatePosition(simState, botPlayerId, col)
                if (score > bestScore) {
                    bestScore = score
                    bestCol = col
                }
            }
            return BoardAction(0, bestCol)
        }

        return BoardAction(0, validCols.random())
    }

    private fun getValidColumns(state: ConnectFourState): List<Int> {
        val cols = mutableListOf<Int>()
        for (c in 0 until 7) {
            for (r in 0 until 6) {
                if (state.grid[r][c] == null) {
                    cols.add(c)
                    break
                }
            }
        }
        return cols
    }

    private fun getEmptyRow(state: ConnectFourState, col: Int): Int {
        for (r in 5 downTo 0) {
            if (state.grid[r][col] == null) return r
        }
        return -1
    }

    private fun simulateMove(state: ConnectFourState, row: Int, col: Int, player: PlayerId): ConnectFourState {
        val newGrid = state.grid.map { it.toMutableList() }
        newGrid[row][col] = player
        return ConnectFourState(newGrid, state.currentPlayer, state.players)
    }

    private fun checkWin(state: ConnectFourState, player: PlayerId): Boolean {
        val grid = state.grid
        for (r in 0 until 6) {
            for (c in 0 until 7) {
                if (grid[r][c] != player) continue
                // افقی
                if (c + 3 < 7 && grid[r][c+1] == player && grid[r][c+2] == player && grid[r][c+3] == player) return true
                // عمودی
                if (r + 3 < 6 && grid[r+1][c] == player && grid[r+2][c] == player && grid[r+3][c] == player) return true
                // مورب \
                if (r + 3 < 6 && c + 3 < 7 && grid[r+1][c+1] == player && grid[r+2][c+2] == player && grid[r+3][c+3] == player) return true
                // مورب /
                if (r - 3 >= 0 && c + 3 < 7 && grid[r-1][c+1] == player && grid[r-2][c+2] == player && grid[r-3][c+3] == player) return true
            }
        }
        return false
    }

    private fun evaluatePosition(state: ConnectFourState, player: PlayerId, col: Int): Int {
        // امتیازدهی ساده: تعداد مهره‌های متوالی در هر جهت
        val row = getEmptyRow(state, col)
        if (row == -1) return 0
        var score = 0
        // مرکز بیشترین امتیاز را دارد
        score += when (col) {
            3 -> 4
            2, 4 -> 3
            1, 5 -> 2
            else -> 1
        }
        // امتیاز ارتفاع (هرچه پایین‌تر بهتر)
        score += (6 - row) * 2
        return score
    }
}