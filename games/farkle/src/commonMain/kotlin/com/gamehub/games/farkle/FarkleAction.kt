package com.gamehub.games.farkle

import com.gamehub.shared.core.GameAction
import kotlinx.serialization.Serializable

@Serializable
sealed class FarkleAction : GameAction() {
    @Serializable
    data object RollDice : FarkleAction()

    @Serializable
    data class SelectDice(val diceIds: List<Int>) : FarkleAction()

    @Serializable
    data object BankScore : FarkleAction()

    @Serializable
    data object ContinueHotDice : FarkleAction()
}
