package com.gamehub.games.soccerstriker

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.gamehub.shared.core.GameDefinition
import com.gamehub.shared.core.GameMetadata
import com.gamehub.shared.core.GameModule
import com.gamehub.shared.core.GameResult
import com.gamehub.games.soccerstriker.ui.SoccerStrikerScreen

class SoccerStrikerModule : GameModule<SoccerStrikerState, SoccerStrikerAction, GameResult> {
    override val metadata: GameMetadata = GameMetadata(
        id = "soccer-striker",
        name = "Soccer Striker (فوتبال انگشتی)",
        minPlayers = 2,
        maxPlayers = 2,
        description = "بازی فوتبال با دیسک‌ها با استفاده از فیزیک JBox2D"
    )

    override val definition: GameDefinition<SoccerStrikerState, SoccerStrikerAction, GameResult> = SoccerStrikerEngine()

    @Composable
    override fun GameScreen(
        gameState: SoccerStrikerState,
        onAction: (SoccerStrikerAction) -> Unit,
        modifier: Modifier
    ) {
        SoccerStrikerScreen(
            state = gameState,
            onAction = onAction,
            modifier = modifier
        )
    }
}