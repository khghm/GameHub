package com.gamehub.games.uno

import com.gamehub.shared.core.*
import com.gamehub.shared.engines.card.*
import com.gamehub.games.uno.model.*
import com.gamehub.shared.engine.GameUpdateResult

class UnoEngine : CardEngine<UnoState, CardAction, GameResult>() {

    override val metadata = GameMetadata(
        id = "uno",
        name = "اونو (Uno)",
        minPlayers = 2,
        maxPlayers = 10,
        description = "بازی کارتی پرطرفدار، زودتر از همه کارت‌هاتو تموم کن!"
    )

    override fun createInitialDeck(): Deck {
        val cards = mutableListOf<Card>()
        for (color in listOf(CardColor.RED, CardColor.BLUE, CardColor.GREEN, CardColor.YELLOW)) {
            cards.add(Card(color, CardValue.Number(0))) // one 0
            for (i in 1..9) {
                cards.add(Card(color, CardValue.Number(i)))
                cards.add(Card(color, CardValue.Number(i)))
            }
            cards.add(Card(color, CardValue.Skip))
            cards.add(Card(color, CardValue.Skip))
            cards.add(Card(color, CardValue.Reverse))
            cards.add(Card(color, CardValue.Reverse))
            cards.add(Card(color, CardValue.DrawTwo))
            cards.add(Card(color, CardValue.DrawTwo))
        }
        for (i in 1..4) {
            cards.add(Card(CardColor.WILD, CardValue.Wild))
            cards.add(Card(CardColor.WILD, CardValue.WildDrawFour))
        }
        return Deck(cards)
    }

    override fun dealHands(deck: Deck, players: List<PlayerId>): Pair<Deck, Map<PlayerId, Hand>> {
        var d = deck
        val hands = mutableMapOf<PlayerId, Hand>()
        for (p in players) {
            var hand = Hand(emptyList())
            for (i in 1..7) {
                val (card, newDeck) = d.draw()
                hand = hand.add(card)
                d = newDeck
            }
            hands[p] = hand
        }
        return d to hands
    }

    override fun createState(
        deck: Deck,
        discardPile: List<Card>,
        hands: Map<PlayerId, Hand>,
        currentPlayer: PlayerId?,
        players: List<PlayerId>,
        direction: Int
    ): UnoState = UnoState(
        deck = deck,
        discardPile = discardPile,
        hands = hands,
        currentPlayer = currentPlayer,
        players = players,
        direction = direction,
        chosenColor = null,
        drawCount = 0
    )

    override fun isValidPlay(card: Card, topCard: Card): Boolean {
        if (card.color == CardColor.WILD) return true
        if (card.color == topCard.color) return true
        if (card.value is CardValue.Number && topCard.value is CardValue.Number && card.value == topCard.value) return true
        if (card.value is CardValue.Skip && topCard.value is CardValue.Skip) return true
        if (card.value is CardValue.Reverse && topCard.value is CardValue.Reverse) return true
        if (card.value is CardValue.DrawTwo && topCard.value is CardValue.DrawTwo) return true
        return false
    }

    public override fun handlePlayCard(state: UnoState, action: CardAction.PlayCard, player: PlayerId): GameUpdateResult<UnoState, GameResult> {
        // first, if a wild is played and chosenColor is null, we need it; we'll assume action has chosenColor
        val card = action.card
        var chosenColor = action.chosenColor
        if (card.color == CardColor.WILD && chosenColor == null) throw IllegalArgumentException("Must choose a color for wild")
        val effectiveColor = if (card.color == CardColor.WILD) chosenColor!! else card.color

        var hand = state.hands[player]!!
        hand = hand.remove(card)
        val newHands = state.hands.toMutableMap()
        newHands[player] = hand

        var newDeck = state.deck
        var newDiscard = state.discardPile + card
        var newChosenColor: CardColor? = null
        var newDirection = state.direction
        var nextPlayer: PlayerId? = null
        var skip = false
        var drawCards = 0

        when (card.value) {
            CardValue.Skip -> skip = true
            CardValue.Reverse -> newDirection = -state.direction
            CardValue.DrawTwo -> drawCards = 2
            CardValue.Wild -> { /* just change color */ }
            CardValue.WildDrawFour -> {
                drawCards = 4
                chosenColor = action.chosenColor ?: CardColor.RED
                // choose color already stored
            }
            else -> {}
        }

        // apply draw cards to next player
        if (drawCards > 0) {
            val nextIdx = (state.players.indexOf(player) + newDirection + state.players.size) % state.players.size
            val target = state.players[nextIdx]
            var targetHand = newHands[target]!!
            repeat(drawCards) {
                if (newDeck.size == 0) {
                    newDeck = reshuffle(newDiscard)
                    newDiscard = listOf(newDiscard.last())
                }
                val (drawn, d2) = newDeck.draw()
                targetHand = targetHand.add(drawn)
                newDeck = d2
            }
            newHands[target] = targetHand
            // skip that player's turn as well
            nextPlayer = getNextPlayerAfterDraw(state, player, newDirection, 1) // skip
        } else {
            nextPlayer = if (skip) {
                getNextPlayerAfterDraw(state, player, newDirection, 1)
            } else {
                getNextPlayerSimple(state, player, newDirection)
            }
        }

        if (card.color == CardColor.WILD) {
            newChosenColor = chosenColor
        }

        val newState = UnoState(
            deck = newDeck,
            discardPile = newDiscard,
            hands = newHands,
            currentPlayer = nextPlayer,
            players = state.players,
            direction = newDirection,
            chosenColor = newChosenColor,
            drawCount = drawCards
        )

        if (hand.size == 0) {
            return GameUpdateResult(newState, GameResult.Win(player))
        }
        return GameUpdateResult(newState, null)
    }

    public override fun handleDrawCard(state: UnoState, player: PlayerId): GameUpdateResult<UnoState, GameResult> {

        var deck = state.deck
        if (deck.size == 0) {
            deck = reshuffle(state.discardPile)
        }
        val (card, newDeck) = deck.draw()
        var hand = state.hands[player]!!
        hand = hand.add(card)
        val newHands = state.hands.toMutableMap()
        newHands[player] = hand
        // if drawn card can be played, could give option; here skip turn
        val nextPlayer = getNextPlayerAfterDraw(state, player, state.direction, 0)
        val newState = UnoState(newDeck, state.discardPile, newHands, nextPlayer, state.players, state.direction)
        return GameUpdateResult(newState, null)
    }

    private fun getNextPlayerSimple(state: UnoState, current: PlayerId, direction: Int): PlayerId {
        val idx = state.players.indexOf(current)
        val nextIdx = (idx + direction + state.players.size) % state.players.size
        return state.players[nextIdx]
    }

    private fun getNextPlayerAfterDraw(state: UnoState, current: PlayerId, direction: Int, skip: Int): PlayerId {
        var idx = state.players.indexOf(current)
        idx = (idx + direction * (skip + 1) + state.players.size) % state.players.size
        return state.players[idx]
    }

    private fun reshuffle(discardPile: List<Card>): Deck {
        // آخرین کارت روی Discard باقی می‌ماند، بقیه را به عنوان دک جدید برمی‌گردانیم
        val others = discardPile.dropLast(1)
        return Deck(others).shuffle()
    }
}