package com.gamehub.games.soccerstriker

import com.gamehub.shared.core.GameAction
import kotlinx.serialization.Serializable

@Serializable
sealed class SoccerStrikerAction : GameAction() {
    @Serializable
    data class SelectDisc(val discId: String) : SoccerStrikerAction()

    @Serializable
    data class FlickDisc(val discId: String, val angle: Float, val power: Float) : SoccerStrikerAction()

    @Serializable
    data class AutoFlick(val discId: String, val angle: Float, val power: Float) : SoccerStrikerAction()

    @Serializable
    data object AnimationComplete : SoccerStrikerAction()

    @Serializable
    data object Reset : SoccerStrikerAction()

    @Serializable
    data object SkipTurn : SoccerStrikerAction()
}