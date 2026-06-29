package com.gamehub.shared.core

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

interface GameModule<State : GameState, Action : GameAction, Result : GameResult> {
    val metadata: GameMetadata
    val definition: GameDefinition<State, Action, Result>

    @Composable
    fun GameScreen(
        gameState: State,
        onAction: (Action) -> Unit,
        modifier: Modifier
    )
}