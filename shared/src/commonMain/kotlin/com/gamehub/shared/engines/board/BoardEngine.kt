// shared/src/commonMain/kotlin/com/gamehub/shared/engines/board/BoardEngine.kt
package com.gamehub.shared.engines.board

import com.gamehub.shared.core.*
import com.gamehub.shared.engine.GameUpdateResult

abstract class BoardEngine<
        State : BoardState,
        Action : BoardAction,
        Result : GameResult
        > : GameDefinition<State, Action, Result> {

    abstract val rows: Int
    abstract val cols: Int

    protected abstract fun createGrid(): List<List<PlayerId?>>
    protected abstract fun createState(
        grid: List<List<PlayerId?>>,
        currentPlayer: PlayerId?,
        players: List<PlayerId>
    ): State

    abstract override val metadata: GameMetadata

    override fun createInitialState(players: List<PlayerId>): State {
        require(players.size >= metadata.minPlayers)
        require(players.size <= metadata.maxPlayers)
        return createState(
            grid = createGrid(),
            currentPlayer = players[0],
            players = players
        )
    }

    override fun validateAction(state: State, action: Action, player: PlayerId): Boolean {
        if (state.currentPlayer != player) return false
        if (action.row !in 0 until rows || action.col !in 0 until cols) return false
        return state.grid[action.row][action.col] == null
    }

    override fun applyAction(state: State, action: Action, player: PlayerId): GameUpdateResult<State, Result> {
        require(validateAction(state, action, player))
        val newGrid = state.grid.map { it.toMutableList() }
        newGrid[action.row][action.col] = player
        val nextPlayer = state.players.first { it != player }
        val newState = createState(grid = newGrid, currentPlayer = nextPlayer, players = state.players)
        val result = checkResult(newState)
        return GameUpdateResult(newState, result)
    }

    override fun isTerminal(state: State): Boolean = checkResult(state) != null

    override fun getResult(state: State): Result? = checkResult(state)

    protected abstract fun checkResult(state: State): Result?
}