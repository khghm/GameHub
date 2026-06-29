package com.gamehub.games.uno

import com.gamehub.shared.core.PlayerId
import com.gamehub.shared.engines.card.*
import kotlinx.serialization.Serializable

@Serializable
class UnoState : CardGameState {

    val chosenColor: CardColor?
    val drawCount: Int
    val winner: PlayerId?
    val gameOver: Boolean

    constructor(
        deck: Deck,
        discardPile: List<Card>,
        hands: Map<PlayerId, Hand>,
        currentPlayer: PlayerId?,
        players: List<PlayerId>,
        direction: Int = 1,
        chosenColor: CardColor? = null,
        drawCount: Int = 0,
        winner: PlayerId? = null,
        gameOver: Boolean = false
    ) : super(deck, discardPile, hands, currentPlayer, players, direction) {
        this.chosenColor = chosenColor
        this.drawCount = drawCount
        this.winner = winner
        this.gameOver = gameOver
    }
}