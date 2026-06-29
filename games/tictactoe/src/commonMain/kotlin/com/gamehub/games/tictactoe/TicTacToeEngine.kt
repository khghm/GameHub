package com.gamehub.games.tictactoe

import com.gamehub.shared.core.*
import com.gamehub.shared.engines.board.*

class TicTacToeEngine : BoardEngine<TicTacToeState, BoardAction, GameResult>() {
    override val rows: Int = 3
    override val cols: Int = 3

    override val metadata = GameMetadata(
        id = "tictactoe",
        name = "دوز (Tic Tac Toe)",
        minPlayers = 2,
        maxPlayers = 2,
        description = "بازی کلاسیک ۳×۳، سه تا پشت سر هم"
    )

    override fun createGrid(): List<List<PlayerId?>> = List(3) { List(3) { null } }

    override fun createState(
        grid: List<List<PlayerId?>>,
        currentPlayer: PlayerId?,
        players: List<PlayerId>
    ): TicTacToeState = TicTacToeState(grid, currentPlayer, players)

    override fun checkResult(state: TicTacToeState): GameResult? {
        val board = state.grid
        for (p in state.players) {
            for (i in 0..2) {
                if (board[i][0] == p && board[i][1] == p && board[i][2] == p) return GameResult.Win(p)
                if (board[0][i] == p && board[1][i] == p && board[2][i] == p) return GameResult.Win(p)
            }
            if (board[0][0] == p && board[1][1] == p && board[2][2] == p) return GameResult.Win(p)
            if (board[0][2] == p && board[1][1] == p && board[2][0] == p) return GameResult.Win(p)
        }
        if (board.all { row -> row.all { it != null } }) return GameResult.Draw
        return null
    }
}