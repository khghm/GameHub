package com.gamehub.games.nard

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.gamehub.games.nard.ui.NardScreen
import com.gamehub.shared.core.GameDefinition
import com.gamehub.shared.core.GameMetadata
import com.gamehub.shared.core.GameModule
import com.gamehub.shared.core.GameResult

class NardModule : GameModule<NardState, NardAction, GameResult> {
    override val metadata: GameMetadata = GameMetadata(
        id = "nard",
        name = "تخته نرد شرقی (Nard)",
        minPlayers = 2,
        maxPlayers = 2,
        description = "بازی کلاسیک تخته نرد شرقی!"
    )

    override val definition: GameDefinition<NardState, NardAction, GameResult> = NardEngine()

    @Composable
    override fun GameScreen(
        gameState: NardState,
        onAction: (NardAction) -> Unit,
        modifier: Modifier
    ) {
        NardScreen(
            state = gameState,
            onAction = onAction,
            modifier = modifier
        )
    }
}
