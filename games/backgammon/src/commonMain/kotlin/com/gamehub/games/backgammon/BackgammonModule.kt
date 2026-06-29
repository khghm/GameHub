package com.gamehub.games.backgammon

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.gamehub.games.backgammon.ui.BackgammonScreen
import com.gamehub.shared.core.GameDefinition
import com.gamehub.shared.core.GameMetadata
import com.gamehub.shared.core.GameModule
import com.gamehub.shared.core.GameResult

class BackgammonModule : GameModule<BackgammonState, BackgammonAction, GameResult> {
    override val metadata: GameMetadata = GameMetadata(
        id = "backgammon",
        name = "تخته‌نرد (Backgammon)",
        minPlayers = 2,
        maxPlayers = 2,
        description = "بازی کلاسیک تخته‌نرد!"
    )

    override val definition: GameDefinition<BackgammonState, BackgammonAction, GameResult> = BackgammonEngine()

    @Composable
    override fun GameScreen(
        gameState: BackgammonState,
        onAction: (BackgammonAction) -> Unit,
        modifier: Modifier
    ) {
        BackgammonScreen(
            state = gameState,
            onAction = onAction,
            modifier = modifier
        )
    }
}
