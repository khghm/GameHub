package com.gamehub.games.bridge

import com.gamehub.shared.core.GameAction
import kotlinx.serialization.Serializable

@Serializable
sealed class BridgeAction : GameAction() {
    // Bidding actions
    @Serializable
    data class MakeBid(val bid: Bid) : BridgeAction()
    // Play actions
    @Serializable
    data class PlayCard(val card: Card) : BridgeAction()
}
