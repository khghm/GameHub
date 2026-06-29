package com.gamehub.games.ludo

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.gamehub.shared.core.*
import com.gamehub.games.ludo.ui.LudoScreen

class LudoModule : GameModule<LudoState, LudoAction, GameResult> {
    override val metadata: GameMetadata = LudoEngine().metadata
    override val definition: GameDefinition<LudoState, LudoAction, GameResult> = LudoEngine()

    @Composable
    override fun GameScreen(gameState: LudoState, onAction: (LudoAction) -> Unit, modifier: Modifier) {
        // LudoScreen نیاز به localPlayerId دارد، ولی از GameModule نمی‌آید.
        // بنابراین اینجا نمی‌توانیم ازش استفاده کنیم. در عوض GameScreenV2 باید مستقیماً LudoScreen را صدا بزند.
        // فقط برای رفع خطا، یک مقدار پیش‌فرض می‌گذاریم.
        LudoScreen(
            state = gameState,
            localPlayerId = PlayerId("player1"), // مقدار موقت، بعداً از GameScreenV2 پاس داده می‌شود
            onAction = onAction,
            modifier = modifier
        )
    }
}