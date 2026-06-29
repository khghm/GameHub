package com.gamehub.games.battleship

import com.gamehub.shared.core.GameAction
import kotlinx.serialization.Serializable

@Serializable
sealed class BattleshipAction : GameAction() {
    // Place a ship
    @Serializable
    data class PlaceShip(
        val shipType: ShipType,
        val startRow: Int,
        val startCol: Int,
        val direction: Direction
    ) : BattleshipAction()

    // Random placement of all ships
    @Serializable
    data object RandomPlaceAll : BattleshipAction()

    // Mark player as ready
    @Serializable
    data object MarkReady : BattleshipAction()

    // Shoot at a coordinate
    @Serializable
    data class Shoot(
        val row: Int,
        val col: Int
    ) : BattleshipAction()
}
