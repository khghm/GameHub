package com.gamehub.games.monopoly

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.gamehub.shared.core.GameDefinition
import com.gamehub.shared.core.GameMetadata
import com.gamehub.shared.core.GameModule
import com.gamehub.shared.core.GameResult
import com.gamehub.games.monopoly.ui.MonopolyScreen

class MonopolyModule : GameModule<MonopolyState, MonopolyAction, GameResult> {
    override val metadata = GameMetadata(
        id = "monopoly", name = "بانک‌رول", minPlayers = 2, maxPlayers = 6,
        description = "بازی اقتصادی و استراتژیک"
    )
    override val definition: GameDefinition<MonopolyState, MonopolyAction, GameResult> = MonopolyEngine()

    @Composable
    override fun GameScreen(gameState: MonopolyState, onAction: (MonopolyAction) -> Unit, modifier: Modifier) {
        MonopolyScreen(state = gameState, onAction = onAction, modifier = modifier)
    }
}