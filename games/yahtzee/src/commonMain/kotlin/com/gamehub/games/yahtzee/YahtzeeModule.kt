package com.gamehub.games.yahtzee

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.gamehub.shared.core.GameDefinition
import com.gamehub.shared.core.GameMetadata
import com.gamehub.shared.core.GameModule
import com.gamehub.shared.core.GameResult
import com.gamehub.games.yahtzee.ui.YahtzeeScreen

class YahtzeeModule : GameModule<YahtzeeState, YahtzeeAction, GameResult> {
    override val metadata: GameMetadata = GameMetadata(
        id = "yahtzee",
        name = "Yahtzee (یاتزی)",
        minPlayers = 2,
        maxPlayers = 4,
        description = "بازی استراتژی و شانس با ۵ تاس"
    )

    override val definition: GameDefinition<YahtzeeState, YahtzeeAction, GameResult> = YahtzeeEngine()

    @Composable
    override fun GameScreen(
        gameState: YahtzeeState,
        onAction: (YahtzeeAction) -> Unit,
        modifier: Modifier
    ) {
        YahtzeeScreen(
            state = gameState,
            onAction = onAction,
            modifier = modifier
        )
    }
}
