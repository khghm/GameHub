package com.gamehub.games.matchmonster

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.gamehub.shared.core.GameDefinition
import com.gamehub.shared.core.GameMetadata
import com.gamehub.shared.core.GameModule
import com.gamehub.shared.core.GameResult
import com.gamehub.shared.core.PlayerId
import com.gamehub.games.matchmonster.ui.MatchMonsterScreen

class MatchMonsterModule : GameModule<MatchMonsterState, MatchMonsterAction, GameResult> {

    override val metadata: GameMetadata = GameMetadata(
        id = "match-monster",
        name = "مچ مانستر (Match Monster)",
        minPlayers = 2,
        maxPlayers = 2,
        description = "بازی پازل هم‌زمان تطبیق کاشی‌های هیولا"
    )

    override val definition: GameDefinition<MatchMonsterState, MatchMonsterAction, GameResult> = MatchMonsterEngine()

    @Composable
    override fun GameScreen(
        gameState: MatchMonsterState,
        onAction: (MatchMonsterAction) -> Unit,
        modifier: Modifier
    ) {
        MatchMonsterScreen(
            state = gameState,
            currentPlayerId = gameState.players.firstOrNull() ?: PlayerId(""),
            onAction = onAction,
            modifier = modifier
        )
    }

    @Composable
    fun GameScreen(
        gameState: MatchMonsterState,
        currentPlayerId: PlayerId,
        onAction: (MatchMonsterAction) -> Unit,
        modifier: Modifier
    ) {
        MatchMonsterScreen(
            state = gameState,
            currentPlayerId = currentPlayerId,
            onAction = onAction,
            modifier = modifier
        )
    }
}
