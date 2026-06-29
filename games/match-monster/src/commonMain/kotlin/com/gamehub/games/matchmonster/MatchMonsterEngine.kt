package com.gamehub.games.matchmonster

import com.gamehub.shared.core.*
import com.gamehub.shared.engine.GameUpdateResult
import kotlin.random.Random

class MatchMonsterEngine : GameDefinition<MatchMonsterState, MatchMonsterAction, GameResult> {
    override val metadata = GameMetadata(
        id = "match-monster",
        name = "مچ مانستر (Match Monster)",
        minPlayers = 2,
        maxPlayers = 2,
        description = "بازی پازل هم‌زمان تطبیق کاشی‌های هیولا"
    )

    override fun createInitialState(players: List<PlayerId>): MatchMonsterState {
        require(players.size == 2) { "Match Monster requires exactly 2 players" }
        val playerData = players.map {
            PlayerDataEntry(
                playerId = it,
                data = PlayerGameData(
                    board = generateInitialBoard(),
                    hp = 100,
                    garbageQueue = 0,
                    garbageRows = emptyList()
                )
            )
        }
        return MatchMonsterState(
            playerData = playerData,
            currentPlayer = null,
            players = players,
            isGameOver = false
        )
    }

    private fun generateInitialBoard(): List<List<Tile?>> {
        val board = MutableList(8) { MutableList<Tile?>(6) { null } }

        for (row in 0 until 8) {
            for (col in 0 until 6) {
                board[row][col] = Tile(
                    type = MonsterType.entries.random(Random),
                    state = TileState.NORMAL,
                    special = SpecialTileType.NONE
                )
            }
        }

        var hasMatches = true
        while (hasMatches) {
            hasMatches = false
            for (row in 0 until 8) {
                for (col in 0 until 6) {
                    if (hasMatchAt(board, row, col)) {
                        board[row][col] = Tile(
                            type = MonsterType.entries.random(Random),
                            state = TileState.NORMAL,
                            special = SpecialTileType.NONE
                        )
                        hasMatches = true
                    }
                }
            }
        }

        return board.map { it.toList() }
    }

    private fun hasMatchAt(board: List<List<Tile?>>, row: Int, col: Int): Boolean {
        val tile = board[row][col] ?: return false

        var horizontalCount = 1
        var c = col - 1
        while (c >= 0 && board[row][c]?.type == tile.type) {
            horizontalCount++
            c--
        }
        c = col + 1
        while (c < 6 && board[row][c]?.type == tile.type) {
            horizontalCount++
            c++
        }
        if (horizontalCount >= 3) return true

        var verticalCount = 1
        var r = row - 1
        while (r >= 0 && board[r][col]?.type == tile.type) {
            verticalCount++
            r--
        }
        r = row + 1
        while (r < 8 && board[r][col]?.type == tile.type) {
            verticalCount++
            r++
        }
        return verticalCount >= 3
    }

    fun isAdjacent(p1: Pair<Int, Int>, p2: Pair<Int, Int>): Boolean {
        val rowDiff = kotlin.math.abs(p1.first - p2.first)
        val colDiff = kotlin.math.abs(p1.second - p2.second)
        return (rowDiff == 1 && colDiff == 0) || (rowDiff == 0 && colDiff == 1)
    }

    fun findAllMatches(board: List<List<Tile?>>): Set<Pair<Int, Int>> {
        val matches = mutableSetOf<Pair<Int, Int>>()

        for (row in 0 until 8) {
            var col = 0
            while (col < 6) {
                val tile = board[row][col] ?: break
                var count = 1
                var nextCol = col + 1
                while (nextCol < 6 && board[row][nextCol]?.type == tile.type) {
                    count++
                    nextCol++
                }
                if (count >= 3) {
                    for (c in col until nextCol) {
                        matches.add(row to c)
                    }
                }
                col = nextCol
            }
        }

        for (col in 0 until 6) {
            var row = 0
            while (row < 8) {
                val tile = board[row][col] ?: break
                var count = 1
                var nextRow = row + 1
                while (nextRow < 8 && board[nextRow][col]?.type == tile.type) {
                    count++
                    nextRow++
                }
                if (count >= 3) {
                    for (r in row until nextRow) {
                        matches.add(r to col)
                    }
                }
                row = nextRow
            }
        }

        return matches
    }

    override fun validateAction(
        state: MatchMonsterState,
        action: MatchMonsterAction,
        playerId: PlayerId
    ): Boolean {
        val playerDataMap = state.getPlayerDataMap()
        val playerData = playerDataMap[playerId] ?: return false
        return when (action) {
            is MatchMonsterAction.SelectPath -> validatePath(playerData.board, action.path)
            is MatchMonsterAction.SwapTiles -> validateSwap(playerData.board, action.position1, action.position2)
            is MatchMonsterAction.ActivateLightning -> true // TODO: Validate properly
            is MatchMonsterAction.ActivateRainbow -> true // TODO: Validate properly
        }
    }

    fun validateSwap(board: List<List<Tile?>>, p1: Pair<Int, Int>, p2: Pair<Int, Int>): Boolean {
        if (!isAdjacent(p1, p2)) return false
        val tile1 = board[p1.first][p1.second] ?: return false
        val tile2 = board[p2.first][p2.second] ?: return false
        if (tile1.state != TileState.NORMAL || tile2.state != TileState.NORMAL) return false

        val newBoard = board.map { it.toMutableList() }
        newBoard[p1.first][p1.second] = tile2
        newBoard[p2.first][p2.second] = tile1
        return findAllMatches(newBoard).isNotEmpty()
    }

    private fun validatePath(board: List<List<Tile?>>, path: List<Pair<Int, Int>>): Boolean {
        if (path.size < 3) return false

        val firstTile = board[path[0].first][path[0].second] ?: return false
        for (i in path.indices) {
            val (r, c) = path[i]
            val tile = board[r][c] ?: return false
            if (tile.type != firstTile.type) return false
            if (tile.state != TileState.NORMAL) return false

            if (i > 0) {
                val (pr, pc) = path[i - 1]
                val rowDiff = kotlin.math.abs(r - pr)
                val colDiff = kotlin.math.abs(c - pc)
                if (!((rowDiff == 1 && colDiff == 0) || (rowDiff == 0 && colDiff == 1))) {
                    return false
                }
            }
        }
        return true
    }

    data class SwapResult(
        val isValid: Boolean,
        val board: List<List<Tile?>>,
        val damage: Int,
        val garbageCount: Int
    )

    fun processSwap(board: List<List<Tile?>>, p1: Pair<Int, Int>, p2: Pair<Int, Int>): SwapResult {
        val newBoardMutable = board.map { it.toMutableList() }
        val tile1 = newBoardMutable[p1.first][p1.second] ?: return SwapResult(false, board, 0, 0)
        val tile2 = newBoardMutable[p2.first][p2.second] ?: return SwapResult(false, board, 0, 0)
        newBoardMutable[p1.first][p1.second] = tile2
        newBoardMutable[p2.first][p2.second] = tile1
        var currentBoard = newBoardMutable.map { it.toList() }

        var totalDamage = 0
        var totalGarbage = 0
        var hasMatches = true

        while (hasMatches) {
            val matches = findAllMatches(currentBoard)
            if (matches.isEmpty()) {
                hasMatches = false
            } else {
                totalDamage += matches.size
                totalGarbage += (matches.size - 3).coerceAtLeast(0)

                val boardAfterRemove = currentBoard.map { it.toMutableList() }
                for ((r, c) in matches) {
                    boardAfterRemove[r][c] = null
                }

                currentBoard = applyGravity(boardAfterRemove)
            }
        }

        return SwapResult(true, currentBoard, totalDamage, totalGarbage)
    }

    override fun applyAction(
        state: MatchMonsterState,
        action: MatchMonsterAction,
        playerId: PlayerId
    ): GameUpdateResult<MatchMonsterState, GameResult> {
        require(validateAction(state, action, playerId))

        val playerDataMap = state.getPlayerDataMap().toMutableMap()

        when (action) {
            is MatchMonsterAction.SelectPath -> {
                val currentPlayerData = playerDataMap[playerId] ?: return GameUpdateResult(state, null)
                val pathResult = processPath(currentPlayerData.board, action.path)
                if (!pathResult.isValid) {
                    return GameUpdateResult(state, null)
                }
                val updatedBoard = applyGravity(pathResult.board)
                playerDataMap[playerId] = currentPlayerData.copy(board = updatedBoard)

                val opponentId = state.players.first { it != playerId }
                val opponentData = playerDataMap[opponentId] ?: return GameUpdateResult(state, null)
                val newOpponentHp = (opponentData.hp - pathResult.damage).coerceAtLeast(0)
                val newGarbageQueue = opponentData.garbageQueue + pathResult.garbageCount

                // TODO: Apply garbage rows properly
                playerDataMap[opponentId] = opponentData.copy(
                    hp = newOpponentHp,
                    garbageQueue = newGarbageQueue
                )
            }
            is MatchMonsterAction.SwapTiles -> {
                val currentPlayerData = playerDataMap[playerId] ?: return GameUpdateResult(state, null)
                val swapResult = processSwap(currentPlayerData.board, action.position1, action.position2)
                if (!swapResult.isValid) {
                    return GameUpdateResult(state, null)
                }
                playerDataMap[playerId] = currentPlayerData.copy(board = swapResult.board)

                val opponentId = state.players.first { it != playerId }
                val opponentData = playerDataMap[opponentId] ?: return GameUpdateResult(state, null)
                val newOpponentHp = (opponentData.hp - swapResult.damage).coerceAtLeast(0)
                val newGarbageQueue = opponentData.garbageQueue + swapResult.garbageCount

                // TODO: Apply garbage rows properly
                playerDataMap[opponentId] = opponentData.copy(
                    hp = newOpponentHp,
                    garbageQueue = newGarbageQueue
                )
            }
            is MatchMonsterAction.ActivateLightning -> {} // TODO
            is MatchMonsterAction.ActivateRainbow -> {} // TODO
        }

        val newPlayerDataList = playerDataMap.map { (id, data) -> PlayerDataEntry(id, data) }
        val newIsGameOver = newPlayerDataList.any { it.data.hp <= 0 }
        val newState = state.copy(
            playerData = newPlayerDataList,
            isGameOver = newIsGameOver
        )
        val result = getResult(newState)

        return GameUpdateResult(newState, result)
    }

    private data class PathResult(
        val isValid: Boolean,
        val board: List<List<Tile?>>,
        val damage: Int,
        val garbageCount: Int
    )

    private fun processPath(board: List<List<Tile?>>, path: List<Pair<Int, Int>>): PathResult {
        if (path.size < 3) return PathResult(false, board, 0, 0)

        val newBoard = board.map { it.toMutableList() }

        for ((r, c) in path) {
            newBoard[r][c] = null
        }

        val damage = path.size
        val garbageCount = (path.size - 3).coerceAtLeast(0)

        return PathResult(true, newBoard, damage, garbageCount)
    }

    private fun applyGravity(board: List<List<Tile?>>): List<List<Tile?>> {
        val newBoard = MutableList(8) { MutableList<Tile?>(6) { null } }

        for (col in 0 until 6) {
            var writeRow = 7
            for (row in 7 downTo 0) {
                if (board[row][col] != null) {
                    newBoard[writeRow][col] = board[row][col]
                    writeRow--
                }
            }
            while (writeRow >= 0) {
                newBoard[writeRow][col] = Tile(
                    type = MonsterType.entries.random(Random),
                    state = TileState.NORMAL,
                    special = SpecialTileType.NONE
                )
                writeRow--
            }
        }

        return newBoard
    }

    override fun isTerminal(state: MatchMonsterState): Boolean = state.isGameOver

    override fun getResult(state: MatchMonsterState): GameResult? {
        if (!state.isGameOver) return null

        val alivePlayers = state.playerData.filter { it.data.hp > 0 }.map { it.playerId }
        return if (alivePlayers.size == 1) {
            GameResult.Win(alivePlayers.first())
        } else {
            GameResult.Draw
        }
    }

    override fun getPlayers(state: MatchMonsterState): List<PlayerId> = state.players

    override fun setCurrentPlayer(state: MatchMonsterState, playerId: PlayerId): MatchMonsterState {
        return state.copy(currentPlayer = playerId)
    }
}
