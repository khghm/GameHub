package com.gamehub.games.hex

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.gamehub.games.hex.ui.HexScreen
import com.gamehub.shared.core.GameDefinition
import com.gamehub.shared.core.GameMetadata
import com.gamehub.shared.core.GameModule
import com.gamehub.shared.core.GameResult
import com.gamehub.shared.engines.board.BoardAction

class HexModule : GameModule<HexState, BoardAction, GameResult> {

    override val metadata: GameMetadata = GameMetadata(
        id = "hex",
        name = "هگس (Hex)",
        minPlayers = 2,
        maxPlayers = 2,
        description = "بازی استراتژیک شش‌ضلعی"
    )

    override val definition: GameDefinition<HexState, BoardAction, GameResult> = HexEngine()

    @Composable
    override fun GameScreen(
        gameState: HexState,
        onAction: (BoardAction) -> Unit,
        modifier: Modifier
    ) {
        HexScreen(
            state = gameState,
            onCellClick = { row, col -> onAction(BoardAction(row, col)) },
            modifier = modifier
        )
    }
}
