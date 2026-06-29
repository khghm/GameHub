package com.gamehub.games.blokus

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.gamehub.games.blokus.ui.BlokusScreen
import com.gamehub.shared.core.GameDefinition
import com.gamehub.shared.core.GameMetadata
import com.gamehub.shared.core.GameModule
import com.gamehub.shared.core.GameResult

class BlokusModule : GameModule<BlokusState, BlokusAction, GameResult> {

    override val metadata: GameMetadata = GameMetadata(
        id = "blokus",
        name = "بلوکِس",
        minPlayers = 2,
        maxPlayers = 4,
        description = "بازی استراتژیک پلیومینو"
    )

    override val definition: GameDefinition<BlokusState, BlokusAction, GameResult> = BlokusEngine()

    @Composable
    override fun GameScreen(
        gameState: BlokusState,
        onAction: (BlokusAction) -> Unit,
        modifier: Modifier
    ) {
        BlokusScreen(
            state = gameState,
            onAction = onAction,
            modifier = modifier
        )
    }
}
