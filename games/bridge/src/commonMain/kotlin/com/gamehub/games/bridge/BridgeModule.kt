package com.gamehub.games.bridge

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.gamehub.games.bridge.ui.BridgeScreen
import com.gamehub.shared.core.GameDefinition
import com.gamehub.shared.core.GameMetadata
import com.gamehub.shared.core.GameModule
import com.gamehub.shared.core.GameResult

class BridgeModule : GameModule<BridgeState, BridgeAction, GameResult> {
    override val metadata: GameMetadata = GameMetadata(
        id = "bridge",
        name = "بریج (Bridge)",
        minPlayers = 2,
        maxPlayers = 4,
        description = "بازی استراتژیک کارتی ۴ نفره"
    )

    override val definition: GameDefinition<BridgeState, BridgeAction, GameResult> = BridgeEngine()

    @Composable
    override fun GameScreen(
        gameState: BridgeState,
        onAction: (BridgeAction) -> Unit,
        modifier: Modifier
    ) {
        BridgeScreen(gameState, onAction, modifier)
    }
}
