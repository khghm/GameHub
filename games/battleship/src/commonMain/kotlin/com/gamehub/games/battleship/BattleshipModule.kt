package com.gamehub.games.battleship

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.gamehub.shared.core.GameDefinition
import com.gamehub.shared.core.GameMetadata
import com.gamehub.shared.core.GameModule
import com.gamehub.shared.core.GameResult
import com.gamehub.games.battleship.ui.BattleshipScreen

class BattleshipModule : GameModule<BattleshipState, BattleshipAction, GameResult> {

    override val metadata: GameMetadata = GameMetadata(
        id = "battleship",
        name = "نبردناو (Battleship)",
        minPlayers = 2,
        maxPlayers = 2,
        description = "بازی استراتژیک تخته‌ای ۱۰×۱۰"
    )

    override val definition: GameDefinition<BattleshipState, BattleshipAction, GameResult> = BattleshipEngine()

    @Composable
    override fun GameScreen(
        gameState: BattleshipState,
        onAction: (BattleshipAction) -> Unit,
        modifier: Modifier
    ) {
        BattleshipScreen(
            state = gameState,
            onAction = onAction,
            modifier = modifier
        )
    }
}
