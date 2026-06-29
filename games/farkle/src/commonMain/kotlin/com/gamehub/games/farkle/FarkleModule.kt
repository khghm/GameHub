package com.gamehub.games.farkle

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.gamehub.games.farkle.ui.FarkleScreen
import com.gamehub.shared.core.GameDefinition
import com.gamehub.shared.core.GameMetadata
import com.gamehub.shared.core.GameModule
import com.gamehub.shared.core.GameResult

class FarkleModule : GameModule<FarkleState, FarkleAction, GameResult> {
    override val metadata: GameMetadata = GameMetadata(
        id = "farkle",
        name = "فارکل (Farkle)",
        minPlayers = 2,
        maxPlayers = 8,
        description = "بازی شانسی و استراتژیک فارکل!"
    )

    override val definition: GameDefinition<FarkleState, FarkleAction, GameResult> = FarkleEngine()

    @Composable
    override fun GameScreen(
        gameState: FarkleState,
        onAction: (FarkleAction) -> Unit,
        modifier: Modifier
    ) {
        FarkleScreen(
            state = gameState,
            onAction = onAction,
            modifier = modifier
        )
    }
}
