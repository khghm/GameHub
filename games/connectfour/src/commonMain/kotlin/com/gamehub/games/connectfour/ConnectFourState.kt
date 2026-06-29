package com.gamehub.games.connectfour

import com.gamehub.shared.core.PlayerId
import com.gamehub.shared.engines.board.BoardState
import kotlinx.serialization.Serializable

@Serializable
class ConnectFourState : BoardState {

    constructor(
        grid: List<List<PlayerId?>>,
        currentPlayer: PlayerId?,
        players: List<PlayerId>
    ) : super(grid, currentPlayer, players)
}