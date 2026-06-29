package com.gamehub.games.spadesbaloot

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.gamehub.shared.core.GameDefinition
import com.gamehub.shared.core.GameModule
import com.gamehub.shared.core.GameResult
import com.gamehub.games.spadesbaloot.ui.SpadesBalootScreen

class SpadesBalootModule : GameModule<SpadesBalootState, SpadesBalootAction, GameResult> {
    override val metadata = SpadesBalootEngine().metadata
    override val definition: GameDefinition<SpadesBalootState, SpadesBalootAction, GameResult> = SpadesBalootEngine()

    @Composable
    override fun GameScreen(
        gameState: SpadesBalootState,
        onAction: (SpadesBalootAction) -> Unit,
        modifier: Modifier
    ) {
        SpadesBalootScreen(gameState, onAction, modifier)
    }
}
