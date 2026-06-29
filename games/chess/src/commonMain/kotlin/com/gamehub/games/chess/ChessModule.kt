package com.gamehub.games.chess

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.gamehub.shared.core.GameDefinition
import com.gamehub.shared.core.GameMetadata
import com.gamehub.shared.core.GameModule
import com.gamehub.shared.core.GameResult
import com.gamehub.games.chess.ui.ChessScreen

class ChessModule : GameModule<ChessState, ChessAction, GameResult> {

    override val metadata: GameMetadata = GameMetadata(
        id = "chess",
        name = "شطرنج",
        minPlayers = 2,
        maxPlayers = 2,
        description = "بازی کلاسیک شطرنج"
    )

    override val definition: GameDefinition<ChessState, ChessAction, GameResult> = ChessEngine()

    @Composable
    override fun GameScreen(
        gameState: ChessState,
        onAction: (ChessAction) -> Unit,
        modifier: Modifier
    ) {
        ChessScreen(
            state = gameState,
            onMove = { from, to, promotion ->
                onAction(ChessAction.Move(from, to, promotion))
            },
            modifier = modifier
        )
    }
}
