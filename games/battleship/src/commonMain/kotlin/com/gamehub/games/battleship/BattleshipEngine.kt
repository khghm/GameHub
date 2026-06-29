package com.gamehub.games.battleship

import com.gamehub.shared.core.*
import com.gamehub.shared.engine.GameUpdateResult
import kotlin.random.Random

class BattleshipEngine : GameDefinition<BattleshipState, BattleshipAction, GameResult> {
    override val metadata = GameMetadata(
        id = "battleship",
        name = "نبردناو (Battleship)",
        minPlayers = 2,
        maxPlayers = 2,
        description = "بازی استراتژیک تخته‌ای 10×10، غرق کردن کشتی‌های حریف"
    )

    override fun createInitialState(players: List<PlayerId>): BattleshipState {
        require(players.size == 2) { "Battleship requires exactly 2 players" }
        val initialPlayerData = players.associateWith {
            PlayerGameData(
                ships = ShipType.entries.associateWith { null }
            )
        }
        return BattleshipState(
            playerData = initialPlayerData,
            currentPlayer = null,
            players = players,
            phase = GamePhase.PLACEMENT
        )
    }

    private fun randomPlaceAllShips(initialData: PlayerGameData): PlayerGameData {
        var data = initialData.copy(
            shipGrid = List(10) { List(10) { CellState.EMPTY } },
            ships = ShipType.entries.associateWith { null }
        )

        for (shipType in ShipType.entries) {
            var placed = false
            repeat(1000) { // Try many times to place all ships
                if (placed) return@repeat
                val startRow = Random.nextInt(0, 10)
                val startCol = Random.nextInt(0, 10)
                val direction = if (Random.nextBoolean()) Direction.HORIZONTAL else Direction.VERTICAL
                val placement = ShipPlacement(shipType, startRow, startCol, direction)
                if (validateShipPlacement(data, placement)) {
                    // Place the ship
                    val newShipGrid = data.shipGrid.map { it.toMutableList() }
                    for ((row, col) in placement.getOccupiedCells()) {
                        newShipGrid[row][col] = CellState.SHIP
                    }
                    data = data.copy(
                        shipGrid = newShipGrid,
                        ships = data.ships.toMutableMap().apply {
                            put(shipType, placement)
                        }
                    )
                    placed = true
                }
            }
        }
        return data
    }

    private fun validateShipPlacement(
        playerData: PlayerGameData,
        placement: ShipPlacement
    ): Boolean {
        val occupiedCells = placement.getOccupiedCells()
        // Check all cells are within bounds
        if (occupiedCells.any { (r, c) -> r !in 0..9 || c !in 0..9 }) {
            return false
        }

        // Check that the ship doesn't overlap with existing ships and isn't adjacent
        for ((row, col) in occupiedCells) {
            // Check if cell is already occupied
            if (playerData.shipGrid[row][col] == CellState.SHIP) {
                return false
            }
            // Check adjacent cells (8 directions)
            for (dr in -1..1) {
                for (dc in -1..1) {
                    val nr = row + dr
                    val nc = col + dc
                    if (nr in 0..9 && nc in 0..9) {
                        if (playerData.shipGrid[nr][nc] == CellState.SHIP &&
                            !occupiedCells.contains(nr to nc)
                        ) {
                            return false
                        }
                    }
                }
            }
        }
        return true
    }

    override fun validateAction(
        state: BattleshipState,
        action: BattleshipAction,
        playerId: PlayerId
    ): Boolean {
        val playerData = state.playerData[playerId] ?: return false

        return when (action) {
            is BattleshipAction.PlaceShip -> {
                if (state.phase != GamePhase.PLACEMENT) return false
                if (playerData.isReady) return false
                if (playerData.ships[action.shipType] != null) return false
                validateShipPlacement(
                    playerData,
                    ShipPlacement(action.shipType, action.startRow, action.startCol, action.direction)
                )
            }
            is BattleshipAction.RandomPlaceAll -> {
                state.phase == GamePhase.PLACEMENT && !playerData.isReady
            }
            is BattleshipAction.MarkReady -> {
                if (state.phase != GamePhase.PLACEMENT) return false
                !playerData.isReady && playerData.ships.values.all { it != null }
            }
            is BattleshipAction.Shoot -> {
                if (state.phase != GamePhase.BATTLE) return false
                if (state.currentPlayer != playerId) return false
                if (action.row !in 0..9 || action.col !in 0..9) return false
                playerData.targetGrid[action.row][action.col] == CellState.EMPTY
            }
        }
    }

    override fun applyAction(
        state: BattleshipState,
        action: BattleshipAction,
        playerId: PlayerId
    ): GameUpdateResult<BattleshipState, GameResult> {
        require(validateAction(state, action, playerId))

        val newPlayerData = state.playerData.toMutableMap()
        val playerData = newPlayerData[playerId]!!

        when (action) {
            is BattleshipAction.PlaceShip -> {
                val placement = ShipPlacement(
                    action.shipType,
                    action.startRow,
                    action.startCol,
                    action.direction
                )
                val newShipGrid = playerData.shipGrid.map { it.toMutableList() }
                for ((row, col) in placement.getOccupiedCells()) {
                    newShipGrid[row][col] = CellState.SHIP
                }
                newPlayerData[playerId] = playerData.copy(
                    shipGrid = newShipGrid,
                    ships = playerData.ships.toMutableMap().apply {
                        put(action.shipType, placement)
                    }
                )
            }
            is BattleshipAction.RandomPlaceAll -> {
                newPlayerData[playerId] = randomPlaceAllShips(playerData)
            }
            is BattleshipAction.MarkReady -> {
                newPlayerData[playerId] = playerData.copy(isReady = true)
            }
            is BattleshipAction.Shoot -> {
                // Get opponent data
                val opponentId = state.players.first { it != playerId }
                val opponentData = newPlayerData[opponentId]!!

                val opponentShipGrid = opponentData.shipGrid.map { it.toMutableList() }
                val playerTargetGrid = playerData.targetGrid.map { it.toMutableList() }

                val hitOpponentShip = opponentShipGrid[action.row][action.col] == CellState.SHIP

                // Update grids
                if (hitOpponentShip) {
                    opponentShipGrid[action.row][action.col] = CellState.HIT
                    playerTargetGrid[action.row][action.col] = CellState.HIT
                } else {
                    playerTargetGrid[action.row][action.col] = CellState.MISS
                }

                // Check if any ship was sunk
                var newOpponentShipsSunk = opponentData.shipsSunk.toMutableSet()
                for ((shipType, placement) in opponentData.ships) {
                    if (placement != null && shipType !in newOpponentShipsSunk) {
                        val allHit = placement.getOccupiedCells().all { (r, c) ->
                            opponentShipGrid[r][c] == CellState.HIT
                        }
                        if (allHit) {
                            newOpponentShipsSunk.add(shipType)
                        }
                    }
                }

                // Update player data
                newPlayerData[playerId] = playerData.copy(targetGrid = playerTargetGrid)
                newPlayerData[opponentId] = opponentData.copy(
                    shipGrid = opponentShipGrid,
                    shipsSunk = newOpponentShipsSunk
                )
            }
        }

        // Determine new phase and current player
        var newPhase = state.phase
        var newCurrentPlayer = state.currentPlayer

        if (newPhase == GamePhase.PLACEMENT) {
            val allReady = newPlayerData.values.all { it.isReady }
            if (allReady) {
                newPhase = GamePhase.BATTLE
                // Randomly select first player
                newCurrentPlayer = state.players[Random.nextInt(state.players.size)]
            }
        } else if (newPhase == GamePhase.BATTLE && action is BattleshipAction.Shoot) {
            // Get opponent data to check if we hit
            val opponentId = state.players.first { it != playerId }
            val opponentData = newPlayerData[opponentId]!!
            val hit = opponentData.shipGrid[action.row][action.col] == CellState.HIT

            // Only switch turn if we missed
            if (!hit) {
                newCurrentPlayer = state.players.first { it != newCurrentPlayer }
            }
        }

        val newState = BattleshipState(
            playerData = newPlayerData,
            currentPlayer = newCurrentPlayer,
            players = state.players,
            phase = newPhase
        )

        val result = getResult(newState)

        return GameUpdateResult(newState, result)
    }

    override fun isTerminal(state: BattleshipState): Boolean = getResult(state) != null

    override fun getResult(state: BattleshipState): GameResult? {
        if (state.phase != GamePhase.BATTLE) return null

        for (playerId in state.players) {
            val playerData = state.playerData[playerId]!!
            // Check if all ships are sunk
            if (playerData.shipsSunk.size == ShipType.entries.size) {
                // The other player wins
                val winnerId = state.players.first { it != playerId }
                return GameResult.Win(winnerId)
            }
        }
        return null
    }

    override fun getPlayers(state: BattleshipState): List<PlayerId> = state.players

    override fun setCurrentPlayer(state: BattleshipState, playerId: PlayerId): BattleshipState {
        return state.copy(currentPlayer = playerId)
    }
}
