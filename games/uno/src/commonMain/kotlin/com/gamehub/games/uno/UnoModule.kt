package com.gamehub.games.uno

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.gamehub.shared.core.*
import com.gamehub.games.uno.ui.UnoScreen
import com.gamehub.shared.engines.card.CardAction

class UnoModule : GameModule<UnoState, CardAction, GameResult> {
    override val metadata: GameMetadata get() = UnoEngine().metadata
    override val definition: GameDefinition<UnoState, CardAction, GameResult> = UnoEngine()

    @Composable
    override fun GameScreen(gameState: UnoState, onAction: (CardAction) -> Unit, modifier: Modifier) {
        // این متد توسط Plugin فراخوانی می‌شود و localPlayerId ندارد.
        // ما در GameScreen.kt مستقیماً از UnoScreen استفاده خواهیم کرد.
    }
}