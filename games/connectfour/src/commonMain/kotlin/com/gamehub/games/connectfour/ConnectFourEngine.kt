package com.gamehub.games.connectfour

import com.gamehub.shared.core.*
import com.gamehub.shared.engines.board.*
import com.gamehub.shared.engine.GameUpdateResult

class ConnectFourEngine : BoardEngine<ConnectFourState, BoardAction, GameResult>() {
    override val rows: Int = 6
    override val cols: Int = 7

    override val metadata = GameMetadata(
        id = "connectfour",
        name = "Connect Four",
        minPlayers = 2,
        maxPlayers = 2,
        description = "Classic 6x7 connect four game"
    )

    override fun createGrid(): List<List<PlayerId?>> = List(6) { List(7) { null } }

    override fun createState(
        grid: List<List<PlayerId?>>,
        currentPlayer: PlayerId?,
        players: List<PlayerId>
    ): ConnectFourState = ConnectFourState(grid, currentPlayer, players)

    override fun checkResult(state: ConnectFourState): GameResult? {
        val board = state.grid
        val rows = board.size
        val cols = board[0].size

        for (p in state.players) {
            // Horizontal
            for (r in 0 until rows) {
                for (c in 0..cols - 4) {
                    if (board[r][c] == p && board[r+0][c+1] == p && board[r+0][c+2] == p && board[r+0][c+3] == p)
                        return GameResult.Win(p)
                }
            }
            // Vertical
            for (c in 0 until cols) {
                for (r in 0..rows - 4) {
                    if (board[r][c] == p && board[r+1][c] == p && board[r+2][c] == p && board[r+3][c] == p)
                        return GameResult.Win(p)
                }
            }
            // Diagonal down-right
            for (r in 0..rows - 4) {
                for (c in 0..cols - 4) {
                    if (board[r][c] == p && board[r+1][c+1] == p && board[r+2][c+2] == p && board[r+3][c+3] == p)
                        return GameResult.Win(p)
                }
            }
            // Diagonal up-right
            for (r in 3 until rows) {
                for (c in 0..cols - 4) {
                    if (board[r][c] == p && board[r-1][c+1] == p && board[r-2][c+2] == p && board[r-3][c+3] == p)
                        return GameResult.Win(p)
                }
            }
        }

        if (board.all { row -> row.all { it != null } }) return GameResult.Draw
        return null
    }

    override fun applyAction(state: ConnectFourState, action: BoardAction, player: PlayerId): GameUpdateResult<ConnectFourState, GameResult> {
        require(validateAction(state, action, player))
        val col = action.col
        val newGrid = state.grid.map { it.toMutableList() }
        val row = (rows - 1 downTo 0).firstOrNull { newGrid[it][col] == null }
            ?: throw IllegalStateException("Column is full")
        newGrid[row][col] = player
        val nextPlayer = state.players.first { it != player }
        val newState = createState(grid = newGrid, currentPlayer = nextPlayer, players = state.players)
        val result = checkResult(newState)
        return GameUpdateResult(newState, result)
    }

    override fun validateAction(state: ConnectFourState, action: BoardAction, player: PlayerId): Boolean {
        if (state.currentPlayer != player) return false
        if (action.col !in 0 until cols) return false
        return (0 until rows).any { state.grid[it][action.col] == null }
    }
}