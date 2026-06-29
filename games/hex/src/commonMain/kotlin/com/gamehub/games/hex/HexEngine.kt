package com.gamehub.games.hex

import com.gamehub.shared.core.*
import com.gamehub.shared.engines.board.*

class HexEngine : BoardEngine<HexState, BoardAction, GameResult>() {
    override val rows: Int = 11
    override val cols: Int = 11

    override val metadata = GameMetadata(
        id = "hex",
        name = "هگس (Hex)",
        minPlayers = 2,
        maxPlayers = 2,
        description = "بازی استراتژیک شش‌ضلعی، اتصال دو لبه"
    )

    override fun createGrid(): List<List<PlayerId?>> = List(11) { List(11) { null } }

    override fun createState(
        grid: List<List<PlayerId?>>,
        currentPlayer: PlayerId?,
        players: List<PlayerId>
    ): HexState = HexState(grid, currentPlayer, players)

    private val directions = listOf(
        -1 to 0, // up-left
        -1 to 1, // up-right
        0 to -1, // left
        0 to 1,  // right
        1 to -1, // down-left
        1 to 0   // down-right
    )

    private fun hasPath(state: HexState, player: PlayerId, startSide: (Int, Int) -> Boolean, endSide: (Int, Int) -> Boolean): Boolean {
        val visited = mutableSetOf<Pair<Int, Int>>()
        val queue = mutableListOf<Pair<Int, Int>>()

        for (row in 0 until rows) {
            for (col in 0 until cols) {
                if (state.grid[row][col] == player && startSide(row, col)) {
                    queue.add(row to col)
                    visited.add(row to col)
                }
            }
        }

        while (queue.isNotEmpty()) {
            val (row, col) = queue.removeFirst()
            if (endSide(row, col)) return true

            for ((dr, dc) in directions) {
                val nr = row + dr
                val nc = col + dc
                if (nr in 0 until rows && nc in 0 until cols &&
                    state.grid[nr][nc] == player && !visited.contains(nr to nc)) {
                    visited.add(nr to nc)
                    queue.add(nr to nc)
                }
            }
        }
        return false
    }

    override fun checkResult(state: HexState): GameResult? {
        val redPlayer = state.players[0]
        val bluePlayer = state.players[1]

        // Red connects left (col 0) to right (col 10)
        if (hasPath(state, redPlayer, { _, c -> c == 0 }, { _, c -> c == 10 })) {
            return GameResult.Win(redPlayer)
        }

        // Blue connects top (row 0) to bottom (row 10)
        if (hasPath(state, bluePlayer, { r, _ -> r == 0 }, { r, _ -> r == 10 })) {
            return GameResult.Win(bluePlayer)
        }

        return null
    }
}
