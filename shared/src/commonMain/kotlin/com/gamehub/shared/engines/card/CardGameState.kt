package com.gamehub.shared.engines.card

import com.gamehub.shared.core.GameState
import com.gamehub.shared.core.PlayerId
import kotlinx.serialization.Serializable

@Serializable
open class CardGameState(
    open val deck: Deck,
    open val discardPile: List<Card>,
    open val hands: Map<PlayerId, Hand>,
    open val currentPlayer: PlayerId?,
    open val players: List<PlayerId>,
    open val direction: Int = 1  // 1 or -1
) : GameState()