package com.gamehub.games.tictactoe

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.gamehub.shared.core.GameDefinition
import com.gamehub.shared.core.GameMetadata
import com.gamehub.shared.core.GameModule
import com.gamehub.shared.core.GameResult
import com.gamehub.shared.engines.board.BoardAction
import com.gamehub.games.tictactoe.ui.TicTacToeScreen

class TicTacToeModule : GameModule<TicTacToeState, BoardAction, GameResult> {

    override val metadata: GameMetadata = GameMetadata(
        id = "tictactoe",
        name = "Tic Tac Toe",
        minPlayers = 2,
        maxPlayers = 2,
        description = "Classic 3x3 grid game"
    )

    override val definition: GameDefinition<TicTacToeState, BoardAction, GameResult> = TicTacToeEngine()

    @Composable
    override fun GameScreen(
        gameState: TicTacToeState,
        onAction: (BoardAction) -> Unit,
        modifier: Modifier
    ) {
        TicTacToeScreen(
            state = gameState,
            onCellClick = { row, col -> onAction(BoardAction(row, col)) },
            modifier = modifier
        )
    }
}