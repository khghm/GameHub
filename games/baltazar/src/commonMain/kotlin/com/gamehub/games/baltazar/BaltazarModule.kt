package com.gamehub.games.baltazar

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.gamehub.games.baltazar.ui.BaltazarScreen
import com.gamehub.shared.core.GameDefinition
import com.gamehub.shared.core.GameMetadata
import com.gamehub.shared.core.GameModule
import com.gamehub.shared.core.GameResult

class BaltazarModule : GameModule<BaltazarState, BaltazarAction, GameResult> {
    override val metadata: GameMetadata = GameMetadata(
        id = "baltazar",
        name = "بالتازار",
        minPlayers = 2,
        maxPlayers = 2,
        description = "بازی کلمه‌ای استراتژیک روی صفحه شش‌ضلعی"
    )

    override val definition: GameDefinition<BaltazarState, BaltazarAction, GameResult> = BaltazarEngine()

    @Composable
    override fun GameScreen(
        gameState: BaltazarState,
        onAction: (BaltazarAction) -> Unit,
        modifier: Modifier
    ) {
        BaltazarScreen(state = gameState, onAction = onAction, modifier = modifier)
    }
}
