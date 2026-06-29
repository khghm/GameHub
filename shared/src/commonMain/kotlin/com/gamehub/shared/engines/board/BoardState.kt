package com.gamehub.shared.engines.board

import com.gamehub.shared.core.GameState
import com.gamehub.shared.core.PlayerId
import kotlinx.serialization.Serializable

@Serializable
open class BoardState(
    open val grid: List<List<PlayerId?>>,
    open val currentPlayer: PlayerId?,
    open val players: List<PlayerId>
) : GameState()