package com.gamehub.games.matchmonster

import com.gamehub.shared.core.GameAction
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
sealed class MatchMonsterAction : GameAction() {
    @Serializable
    data class SelectPath(
        val path: List<Pair<Int, Int>> // list of (row, column)
    ) : MatchMonsterAction()

    @Serializable
    data class SwapTiles(
        val position1: Pair<Int, Int>,
        val position2: Pair<Int, Int>
    ) : MatchMonsterAction()

    @Serializable
    data class ActivateLightning(
        val position: Pair<Int, Int>,
        val isHorizontal: Boolean
    ) : MatchMonsterAction()

    @Serializable
    data class ActivateRainbow(
        val rainbowPosition: Pair<Int, Int>,
        val targetPosition: Pair<Int, Int>
    ) : MatchMonsterAction()
}
