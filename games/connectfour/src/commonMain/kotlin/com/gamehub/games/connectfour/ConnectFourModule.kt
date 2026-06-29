package com.gamehub.games.connectfour

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.gamehub.shared.core.*
import com.gamehub.shared.engines.board.BoardAction
import com.gamehub.games.connectfour.ui.ConnectFourScreen

class ConnectFourModule : GameModule<ConnectFourState, BoardAction, GameResult> {

    override val metadata: GameMetadata = GameMetadata(
        id = "connectfour",
        name = "چهارخطی (Connect Four)",
        minPlayers = 2,
        maxPlayers = 2,
        description = "۴ مهره ردیف کن، عمودی یا افقی یا مورب"
    )

    override val definition: GameDefinition<ConnectFourState, BoardAction, GameResult> = ConnectFourEngine()

    @Composable
    override fun GameScreen(
        gameState: ConnectFourState,
        onAction: (BoardAction) -> Unit,
        modifier: Modifier
    ) {
        ConnectFourScreen(
            state = gameState,
            onColumnClick = { col -> onAction(BoardAction(0, col)) },
            modifier = modifier
        )
    }
}