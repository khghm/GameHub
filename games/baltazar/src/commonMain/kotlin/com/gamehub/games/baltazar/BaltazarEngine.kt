package com.gamehub.games.baltazar

import com.gamehub.shared.core.GameDefinition
import com.gamehub.shared.core.GameResult
import com.gamehub.shared.core.PlayerId
import com.gamehub.shared.engine.GameUpdateResult
import kotlin.random.Random

class BaltazarEngine : GameDefinition<BaltazarState, BaltazarAction, GameResult> {
    override val metadata = com.gamehub.shared.core.GameMetadata(
        id = "baltazar",
        name = "بالتازار",
        minPlayers = 2,
        maxPlayers = 2,
        description = "بازی کلمه‌ای استراتژیک روی صفحه شش‌ضلعی"
    )

    override fun createInitialState(players: List<PlayerId>): BaltazarState {
        return BaltazarState.initial(players)
    }

    override fun validateAction(
        state: BaltazarState,
        action: BaltazarAction,
        playerId: PlayerId
    ): Boolean {
        if (state.currentPlayer != playerId || state.winner != null) return false

        return when (action) {
            is BaltazarAction.SelectCell -> {
                val cell = state.getCell(action.row, action.col) ?: return false
                if (cell.state != CellState.OpenNeutral) return false
                if (state.selectedCells.contains(action.row to action.col)) return false
                true
            }
            is BaltazarAction.DeselectLast -> state.selectedCells.isNotEmpty()
            is BaltazarAction.SubmitWord -> state.selectedCells.size >= 3
        }
    }

    override fun applyAction(
        state: BaltazarState,
        action: BaltazarAction,
        playerId: PlayerId
    ): GameUpdateResult<BaltazarState, GameResult> {
        if (!validateAction(state, action, playerId)) {
            return GameUpdateResult(state)
        }

        val newState = when (action) {
            is BaltazarAction.SelectCell -> {
                // Select cell - no turn change
                state.copy(
                    selectedCells = state.selectedCells + (action.row to action.col)
                )
            }
            is BaltazarAction.DeselectLast -> {
                // Deselect last cell - no turn change
                state.copy(
                    selectedCells = state.selectedCells.dropLast(1)
                )
            }
            is BaltazarAction.SubmitWord -> {
                // Submit word - process and change turn
                val word = state.selectedCells.mapNotNull { (r, c) -> state.getCell(r, c)?.letter }.joinToString("")
                if (BALTAR_VOCAB.contains(word)) {
                    processCapture(state, playerId)
                } else {
                    state
                }
            }
        }

        val winner = checkWinner(newState)
        val finalState = if (winner != null) {
            // Game ended with winner
            newState.copy(winner = winner, currentPlayer = null)
        } else if (action is BaltazarAction.SubmitWord) {
            // Only switch turn when word is submitted
            newState.copy(
                currentPlayer = if (newState.currentPlayer == newState.players[0]) newState.players[1] else newState.players[0],
                selectedCells = emptyList()
            )
        } else {
            // No turn change for select/deselect
            newState
        }

        return GameUpdateResult(finalState, if (winner != null) GameResult.Win(winner) else null)
    }

    private fun processCapture(state: BaltazarState, playerId: PlayerId): BaltazarState {
        val newBoard = state.board.toMutableList()
        
        // تابع کمکی برای گرفتن خانه از newBoard
        fun getNewCell(r: Int, c: Int): HexCell? {
            return newBoard.find { it.row == r && it.col == c }
        }
        
        // محاسبه قلمرو فعلی از روی newBoard (نه state)
        fun getCurrentTerritory(): MutableSet<Pair<Int, Int>> {
            val home = if (playerId == state.players[0]) (0 to 1) else (6 to 5)
            val territory = mutableListOf(home)
            val visited = mutableSetOf<Pair<Int, Int>>()
            visited.add(home)

            val queue = mutableListOf(home)
            while (queue.isNotEmpty()) {
                val (r, c) = queue.removeAt(0)
                val neighbors = state.getValidNeighbors(r, c)
                for ((nr, nc) in neighbors) {
                    val cell = getNewCell(nr, nc)
                    if (cell != null && cell.owner == playerId && !visited.contains(nr to nc)) {
                        visited.add(nr to nc)
                        territory.add(nr to nc)
                        queue.add(nr to nc)
                    }
                }
            }
            return territory.toMutableSet()
        }
        
        var territory = getCurrentTerritory()
        val opponentHome = if (playerId == state.players[0]) (6 to 5) else (0 to 1)
        var winner: PlayerId? = null

        // Process all selected cells in order (chain capture)
        for ((r, c) in state.selectedCells) {
            val cell = getNewCell(r, c) ?: continue
            if (cell.state != CellState.OpenNeutral) continue

            // Check if current cell is adjacent to our current territory
            val isAdjacent = state.getValidNeighbors(r, c).any { territory.contains(it) }
            
            if (isAdjacent) {
                val idx = newBoard.indexOfFirst { it.row == r && it.col == c }
                if (idx != -1) {
                    // Capture the cell
                    newBoard[idx] = newBoard[idx].copy(
                        state = CellState.Captured,
                        letter = null,
                        owner = playerId
                    )
                    // آپدیت قلمرو بعد از هر تصرف
                    territory = getCurrentTerritory()

                    // Check win condition: is this cell adjacent to opponent's home?
                    val neighborsOfCaptured = state.getValidNeighbors(r, c)
                    if (neighborsOfCaptured.contains(opponentHome)) {
                        winner = playerId
                    }

                    // Open neighboring cells
                    for ((nr, nc) in neighborsOfCaptured) {
                        val neighborIdx = newBoard.indexOfFirst { it.row == nr && it.col == nc }
                        if (neighborIdx != -1) {
                            val neighbor = newBoard[neighborIdx]
                            if (neighbor.state == CellState.Closed || (neighbor.owner != null && neighbor.owner != playerId)) {
                                newBoard[neighborIdx] = neighbor.copy(
                                    state = CellState.OpenNeutral,
                                    letter = getRandomPersianLetter(),
                                    owner = null
                                )
                            }
                        }
                    }
                }
            }
        }

        return state.copy(board = newBoard, winner = winner, selectedCells = emptyList())
    }

    private fun checkWinner(state: BaltazarState): PlayerId? {
        if (state.winner != null) return state.winner
        if (state.timeoutsA >= 3) return state.players[1]
        if (state.timeoutsB >= 3) return state.players[0]
        return null
    }

    override fun isTerminal(state: BaltazarState): Boolean {
        return state.winner != null || state.timeoutsA >= 3 || state.timeoutsB >= 3
    }

    override fun getResult(state: BaltazarState): GameResult? {
        val winner = checkWinner(state)
        return if (winner != null) GameResult.Win(winner) else null
    }
}
