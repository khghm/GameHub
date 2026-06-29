package com.gamehub.games.battleship

import com.gamehub.shared.core.GameState
import com.gamehub.shared.core.PlayerId
import kotlinx.serialization.Serializable

// Ship types and lengths
enum class ShipType(val displayName: String, val length: Int) {
    CARRIER("ناو هواپیمابر", 5),
    BATTLESHIP("رزم‌ناو", 4),
    CRUISER("ناوشکن", 3),
    SUBMARINE("زیردریایی", 3),
    DESTROYER("ناوچه", 2),
    PATROL_BOAT("قایق گشت", 2)
}

// Direction for ship placement
enum class Direction {
    HORIZONTAL,
    VERTICAL
}

// A single cell's state (for tracking hits/misses)
enum class CellState {
    EMPTY,
    SHIP,
    HIT,
    MISS
}

// Player-specific data
@Serializable
data class PlayerGameData(
    val shipGrid: List<List<CellState>> = List(10) { List(10) { CellState.EMPTY } },
    val targetGrid: List<List<CellState>> = List(10) { List(10) { CellState.EMPTY } },
    val ships: Map<ShipType, ShipPlacement?> = emptyMap(), // Ship type to its placement
    val shipsSunk: Set<ShipType> = emptySet(),
    val isReady: Boolean = false
)

@Serializable
data class ShipPlacement(
    val type: ShipType,
    val startRow: Int,
    val startCol: Int,
    val direction: Direction
) {
    // Get all cells occupied by this ship
    fun getOccupiedCells(): List<Pair<Int, Int>> {
        val cells = mutableListOf<Pair<Int, Int>>()
        for (i in 0 until type.length) {
            if (direction == Direction.HORIZONTAL) {
                cells.add(startRow to startCol + i)
            } else {
                cells.add(startRow + i to startCol)
            }
        }
        return cells
    }
}

enum class GamePhase {
    PLACEMENT, // Players placing ships
    BATTLE     // Shooting phase
}

@Serializable
data class BattleshipState(
    val playerData: Map<PlayerId, PlayerGameData>,
    val currentPlayer: PlayerId?,
    val players: List<PlayerId>,
    val phase: GamePhase = GamePhase.PLACEMENT
) : GameState()
