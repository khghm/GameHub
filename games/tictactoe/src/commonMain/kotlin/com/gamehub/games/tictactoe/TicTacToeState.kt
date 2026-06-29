package com.gamehub.games.tictactoe

import com.gamehub.shared.core.PlayerId
import com.gamehub.shared.engines.board.BoardState
import kotlinx.serialization.Serializable

@Serializable
class TicTacToeState : BoardState {

    // این constructor برای استفاده در BoardEngine لازم است
    constructor(
        grid: List<List<PlayerId?>>,
        currentPlayer: PlayerId?,
        players: List<PlayerId>
    ) : super(grid, currentPlayer, players)
}