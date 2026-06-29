package com.gamehub.games.esmofamil

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.gamehub.games.esmofamil.ui.EsmoFamilScreen
import com.gamehub.shared.core.GameDefinition
import com.gamehub.shared.core.GameMetadata
import com.gamehub.shared.core.GameModule
import com.gamehub.shared.core.GameResult

class EsmoFamilModule : GameModule<EsmoFamilState, EsmoFamilAction, GameResult> {
    override val metadata: GameMetadata = GameMetadata(
        id = EsmoFamilState.GAME_ID,
        name = "اسم و فامیل",
        minPlayers = 2,
        maxPlayers = 8,
        description = "بازی اسم و فامیل با ۸ دسته‌بندی!"
    )

    override val definition: GameDefinition<EsmoFamilState, EsmoFamilAction, GameResult> =
        EsmoFamilEngine()

    @Composable
    override fun GameScreen(
        gameState: EsmoFamilState,
        onAction: (EsmoFamilAction) -> Unit,
        modifier: Modifier
    ) {
        EsmoFamilScreen(
            state = gameState,
            currentPlayerId = gameState.currentPlayer?.value ?: "",
            onAction = onAction,
            modifier = modifier
        )
    }
}
