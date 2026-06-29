package com.gamehub.games.spadesbaloot

import com.gamehub.shared.core.GameAction
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
sealed class SpadesBalootAction : GameAction() {
    // Declaration actions
    @Serializable
    @SerialName("SpadesBalootAction.DeclareBaloot")
    data class DeclareBaloot(val declarations: List<Declaration>) : SpadesBalootAction()
    
    // Bidding actions
    @Serializable
    @SerialName("SpadesBalootAction.Bid")
    data class Bid(val tricks: Int, val isNil: Boolean = false) : SpadesBalootAction()
    
    // Play actions
    @Serializable
    @SerialName("SpadesBalootAction.PlayCard")
    data class PlayCard(val card: Card) : SpadesBalootAction()
}
