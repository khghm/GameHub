package com.gamehub.games.battleship

import com.gamehub.shared.bot.BotStrategy
import com.gamehub.shared.core.PlayerId
import kotlinx.coroutines.delay
import kotlin.random.Random

class BattleshipBotStrategy : BotStrategy<BattleshipState, BattleshipAction> {
    override val gameId: String = "battleship"
    override val supportedDifficultyLevels: IntRange = 1..10

    override suspend fun getNextMove(
        state: BattleshipState,
        botPlayerId: PlayerId,
        difficultyLevel: Int
    ): BattleshipAction? {
        // Add a little delay to make it feel natural
        delay(300)

        val botData = state.playerData[botPlayerId] ?: return null

        if (state.phase == GamePhase.PLACEMENT) {
            // Placement phase
            val shipToPlace = botData.ships.entries.firstOrNull { it.value == null }?.key
            if (shipToPlace != null) {
                // Try to place this ship
                repeat(50) { // Try up to 50 times to find a valid position
                    val startRow = Random.nextInt(0, 10)
                    val startCol = Random.nextInt(0, 10)
                    val direction = if (Random.nextBoolean()) Direction.HORIZONTAL else Direction.VERTICAL
                    val placement = ShipPlacement(shipToPlace, startRow, startCol, direction)
                    if (validatePlacementForBot(botData, placement)) {
                        return BattleshipAction.PlaceShip(shipToPlace, startRow, startCol, direction)
                    }
                }
            } else if (!botData.isReady) {
                // All ships are placed, mark ready
                return BattleshipAction.MarkReady
            }
        } else {
            // Battle phase
            if (state.currentPlayer == botPlayerId) {
                // Find a random empty cell to shoot at
                val emptyCells = mutableListOf<Pair<Int, Int>>()
                for (row in 0 until 10) {
                    for (col in 0 until 10) {
                        if (botData.targetGrid[row][col] == CellState.EMPTY) {
                            emptyCells.add(row to col)
                        }
                    }
                }

                if (emptyCells.isNotEmpty()) {
                    val target = if (difficultyLevel >= 5) {
                        // For difficulty 5+, try to be smarter by hunting near hits
                        findSmartTarget(botData) ?: emptyCells.random()
                    } else {
                        emptyCells.random()
                    }
                    return BattleshipAction.Shoot(target.first, target.second)
                }
            }
        }

        return null
    }

    private fun findSmartTarget(botData: PlayerGameData): Pair<Int, Int>? {
        // First, find all hit cells that aren't part of a sunk ship
        val hits = mutableListOf<Pair<Int, Int>>()
        for (row in 0 until 10) {
            for (col in 0 until 10) {
                if (botData.targetGrid[row][col] == CellState.HIT) {
                    hits.add(row to col)
                }
            }
        }

        if (hits.isNotEmpty()) {
            // Check adjacent cells of the last hit for empty cells to shoot
            val lastHit = hits.last()
            val neighbors = listOf(
                lastHit.first - 1 to lastHit.second, // Up
                lastHit.first + 1 to lastHit.second, // Down
                lastHit.first to lastHit.second - 1, // Left
                lastHit.first to lastHit.second + 1  // Right
            )

            return neighbors.firstOrNull { (r, c) ->
                r in 0..9 && c in 0..9 && botData.targetGrid[r][c] == CellState.EMPTY
            }
        }

        return null
    }

    private fun validatePlacementForBot(data: PlayerGameData, placement: ShipPlacement): Boolean {
        val cells = placement.getOccupiedCells()

        // Check all cells are within bounds
        if (cells.any { it.first !in 0..9 || it.second !in 0..9 }) return false

        // Check no overlap and no adjacent ships
        for ((row, col) in cells) {
            if (data.shipGrid[row][col] != CellState.EMPTY) return false
            for (dr in -1..1) {
                for (dc in -1..1) {
                    val nr = row + dr
                    val nc = col + dc
                    if (nr in 0..9 && nc in 0..9) {
                        if (data.shipGrid[nr][nc] == CellState.SHIP && !cells.contains(nr to nc)) {
                            return false
                        }
                    }
                }
            }
        }
        return true
    }
}
