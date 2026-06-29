package com.gamehub.games.hex

import com.gamehub.shared.core.PlayerId
import com.gamehub.shared.engines.board.BoardState
import kotlinx.serialization.Serializable

@Serializable
class HexState : BoardState {

    constructor(
        grid: List<List<PlayerId?>>,
        currentPlayer: PlayerId?,
        players: List<PlayerId>
    ) : super(grid, currentPlayer, players)
}
