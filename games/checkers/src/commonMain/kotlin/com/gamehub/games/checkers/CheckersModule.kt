package com.gamehub.games.checkers

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.gamehub.shared.core.GameDefinition
import com.gamehub.shared.core.GameMetadata
import com.gamehub.shared.core.GameModule
import com.gamehub.shared.core.GameResult
import com.gamehub.games.checkers.ui.CheckersScreen

class CheckersModule : GameModule<CheckersState, CheckersAction, GameResult> {

    override val metadata: GameMetadata = GameMetadata(
        id = "checkers",
        name = "چکرز",
        minPlayers = 2,
        maxPlayers = 2,
        description = "بازی کلاسیک چکرز"
    )

    override val definition: GameDefinition<CheckersState, CheckersAction, GameResult> = CheckersEngine()

    @Composable
    override fun GameScreen(
        gameState: CheckersState,
        onAction: (CheckersAction) -> Unit,
        modifier: Modifier
    ) {
        CheckersScreen(
            state = gameState,
            onAction = onAction,
            modifier = modifier
        )
    }
}
