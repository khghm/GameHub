package com.gamehub.games.tictactoe

import com.gamehub.shared.core.GameResult
import com.gamehub.shared.core.PlayerId
import com.gamehub.shared.engines.board.BoardAction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TicTacToeEngineTest {
    private val engine = TicTacToeEngine()
    private val player1 = PlayerId("player1")
    private val player2 = PlayerId("player2")

    @Test
    fun `بازی باید با دو بازیکن شروع شود`() {
        val state = engine.createInitialState(listOf(player1, player2))
        assertEquals(player1, state.currentPlayer)
        assertTrue(state.grid.all { row -> row.all { it == null } })
    }

    @Test
    fun `حرکت معتبر باید پذیرفته شود`() {
        val state = engine.createInitialState(listOf(player1, player2))
        assertTrue(engine.validateAction(state, BoardAction(0, 0), player1))
    }

    @Test
    fun `حرکت تکراری باید رد شود`() {
        val state = engine.createInitialState(listOf(player1, player2))
        val result1 = engine.applyAction(state, BoardAction(0, 0), player1)
        assertFalse(engine.validateAction(result1.newState, BoardAction(0, 0), player2))
    }

    @Test
    fun `برد در یک ردیف افقی باید تشخیص داده شود`() {
        var state = engine.createInitialState(listOf(player1, player2))
        state = engine.applyAction(state, BoardAction(0, 0), player1).newState // X
        state = engine.applyAction(state, BoardAction(1, 0), player2).newState // O
        state = engine.applyAction(state, BoardAction(0, 1), player1).newState // X
        state = engine.applyAction(state, BoardAction(1, 1), player2).newState // O
        val result = engine.applyAction(state, BoardAction(0, 2), player1) // X - برد!
        assertTrue(result.result is GameResult.Win)
        assertEquals(player1, (result.result as GameResult.Win).winner)
    }
}