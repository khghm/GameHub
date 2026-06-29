package com.gamehub.shared.engines.card

import com.gamehub.shared.core.GameAction
import kotlinx.serialization.Serializable

@Serializable
sealed class CardAction : GameAction() {
    @Serializable
    data class PlayCard(val card: Card, val chosenColor: CardColor? = null) : CardAction()

    @Serializable
    data object DrawCard : CardAction()
}