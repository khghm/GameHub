package com.gamehub.games.othello

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.gamehub.games.othello.ui.OthelloScreen
import com.gamehub.shared.core.GameDefinition
import com.gamehub.shared.core.GameMetadata
import com.gamehub.shared.core.GameModule
import com.gamehub.shared.core.GameResult

class OthelloModule : GameModule<OthelloState, OthelloAction, GameResult> {

    override val metadata: GameMetadata = GameMetadata(
        id = "othello",
        name = "اتللو",
        minPlayers = 2,
        maxPlayers = 2,
        description = "بازی استراتژیک تخته‌ای"
    )

    override val definition: GameDefinition<OthelloState, OthelloAction, GameResult> = OthelloEngine()

    @Composable
    override fun GameScreen(
        gameState: OthelloState,
        onAction: (OthelloAction) -> Unit,
        modifier: Modifier
    ) {
        OthelloScreen(
            state = gameState,
            onAction = onAction,
            modifier = modifier
        )
    }
}
