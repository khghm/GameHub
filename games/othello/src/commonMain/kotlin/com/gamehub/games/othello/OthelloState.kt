package com.gamehub.games.othello

import com.gamehub.shared.core.GameState
import com.gamehub.shared.core.PlayerId
import kotlinx.serialization.Serializable

@Serializable
enum class OthelloPiece {
    EMPTY,
    BLACK,
    WHITE
}

@Serializable
data class OthelloState(
    val players: List<PlayerId>,
    val board: List<List<OthelloPiece>>,
    val currentPlayer: PlayerId?,
    val consecutivePasses: Int = 0,
    val blackCount: Int = 2,
    val whiteCount: Int = 2,
    val gameOver: Boolean = false
) : GameState() {

    companion object {
        fun initial(players: List<PlayerId>): OthelloState {
            require(players.size == 2) { "Othello requires exactly 2 players" }
            val board = List(8) { MutableList(8) { OthelloPiece.EMPTY } }
            board[3][3] = OthelloPiece.WHITE
            board[3][4] = OthelloPiece.BLACK
            board[4][3] = OthelloPiece.BLACK
            board[4][4] = OthelloPiece.WHITE
            return OthelloState(
                players = players,
                board = board.map { it.toList() },
                currentPlayer = players[0],
                blackCount = 2,
                whiteCount = 2,
                consecutivePasses = 0,
                gameOver = false
            )
        }
    }

    fun getPlayerPiece(playerId: PlayerId): OthelloPiece {
        return if (playerId == players[0]) OthelloPiece.BLACK else OthelloPiece.WHITE
    }

    fun getOpponentPiece(piece: OthelloPiece): OthelloPiece {
        return when (piece) {
            OthelloPiece.BLACK -> OthelloPiece.WHITE
            OthelloPiece.WHITE -> OthelloPiece.BLACK
            else -> OthelloPiece.EMPTY
        }
    }

    fun hasLegalMovesForPlayer(piece: OthelloPiece): Boolean {
        for (row in 0..7) {
            for (col in 0..7) {
                if (board[row][col] == OthelloPiece.EMPTY) {
                    if (isLegalMove(row, col, piece)) {
                        return true
                    }
                }
            }
        }
        return false
    }

    private fun isLegalMove(row: Int, col: Int, piece: OthelloPiece): Boolean {
        val directions = listOf(-1 to -1, -1 to 0, -1 to 1, 0 to -1, 0 to 1, 1 to -1, 1 to 0, 1 to 1)
        val opponent = getOpponentPiece(piece)

        directions.forEach { (dr, dc) ->
            val currentFlips = mutableListOf<Pair<Int, Int>>()
            var r = row + dr
            var c = col + dc
            while (r in 0..7 && c in 0..7) {
                val currentPiece = board[r][c]
                when {
                    currentPiece == opponent -> currentFlips.add(r to c)
                    currentPiece == piece && currentFlips.isNotEmpty() -> return true
                    else -> break
                }
                r += dr
                c += dc
            }
        }
        return false
    }
}
