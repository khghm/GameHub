package com.gamehub.games.abalone

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.gamehub.games.abalone.ui.AbaloneScreen
import com.gamehub.shared.core.GameDefinition
import com.gamehub.shared.core.GameMetadata
import com.gamehub.shared.core.GameModule
import com.gamehub.shared.core.GameResult

class AbaloneModule : GameModule<AbaloneState, AbaloneAction, GameResult> {

    override val metadata: GameMetadata = GameMetadata(
        id = "abalone",
        name = "ابلون",
        minPlayers = 2,
        maxPlayers = 2,
        description = "بازی استراتژیک تخته‌ای"
    )

    override val definition: GameDefinition<AbaloneState, AbaloneAction, GameResult> = AbaloneEngine()

    @Composable
    override fun GameScreen(
        gameState: AbaloneState,
        onAction: (AbaloneAction) -> Unit,
        modifier: Modifier
    ) {
        AbaloneScreen(
            state = gameState,
            onAction = onAction,
            modifier = modifier
        )
    }
}
