// shared/src/commonMain/kotlin/com/gamehub/shared/engines/card/CardEngine.kt
package com.gamehub.shared.engines.card

import com.gamehub.shared.core.*
import com.gamehub.shared.engine.GameUpdateResult

abstract class CardEngine<
        State : CardGameState,
        Action : CardAction,
        Result : GameResult
        > : GameDefinition<State, Action, Result> {

    abstract override val metadata: GameMetadata
    protected abstract fun createInitialDeck(): Deck
    protected abstract fun dealHands(deck: Deck, players: List<PlayerId>): Pair<Deck, Map<PlayerId, Hand>>
    protected abstract fun createState(
        deck: Deck,
        discardPile: List<Card>,
        hands: Map<PlayerId, Hand>,
        currentPlayer: PlayerId?,
        players: List<PlayerId>,
        direction: Int
    ): State

    override fun createInitialState(players: List<PlayerId>): State {
        var deck = createInitialDeck().shuffle()
        val (newDeck, hands) = dealHands(deck, players)
        deck = newDeck
        var (firstCard, remainingDeck) = deck.draw()
        var discard = listOf(firstCard)
        return createState(
            deck = remainingDeck,
            discardPile = discard,
            hands = hands,
            currentPlayer = players[0],
            players = players,
            direction = 1
        )
    }

    override fun validateAction(state: State, action: Action, player: PlayerId): Boolean {
        if (state.currentPlayer != player) return false
        val hand = state.hands[player] ?: return false
        val topCard = state.discardPile.last()
        return when (action) {
            is CardAction.PlayCard -> {
                val card = action.card
                hand.contains(card) && isValidPlay(card, topCard)
            }
            is CardAction.DrawCard -> true
        }
    }

    protected open fun isValidPlay(card: Card, topCard: Card): Boolean {
        if (card.color == CardColor.WILD) return true
        if (card.color == topCard.color) return true
        if (card.value == topCard.value && card.value !is CardValue.Wild && card.value !is CardValue.WildDrawFour) return true
        return false
    }

    override fun applyAction(state: State, action: Action, player: PlayerId): GameUpdateResult<State, Result> {
        require(validateAction(state, action, player))
        return when (action) {
            is CardAction.PlayCard -> handlePlayCard(state, action, player)
            is CardAction.DrawCard -> handleDrawCard(state, player)
        }
    }

    protected open fun handlePlayCard(state: State, action: CardAction.PlayCard, player: PlayerId): GameUpdateResult<State, Result> {
        var hand = state.hands[player]!!
        hand = hand.remove(action.card)
        val newHands = state.hands.toMutableMap().apply { put(player, hand) }
        var newDeck = state.deck
        var newDiscard = state.discardPile + action.card
        if (hand.size == 0) {
            val finalState = createState(newDeck, newDiscard, newHands, null, state.players, state.direction)
            val result = GameResult.Win(player) as Result
            return GameUpdateResult(finalState, result)
        }
        val (nextPlayer, nextDirection) = getNextPlayerAndDirection(state, action.card)
        val nextState = createState(newDeck, newDiscard, newHands, nextPlayer, state.players, nextDirection)
        return GameUpdateResult(nextState, null)
    }

    protected open fun handleDrawCard(state: State, player: PlayerId): GameUpdateResult<State, Result> {
        var deck = state.deck
        if (deck.size == 0) {
            val toShuffle = state.discardPile.dropLast(1)
            val last = state.discardPile.last()
            deck = Deck(toShuffle).shuffle()
            deck = deck.copy(cards = listOf(last) + deck.cards)
        }
        val (drawn, newDeck) = deck.draw()
        var hand = state.hands[player]!!
        hand = hand.add(drawn)
        val newHands = state.hands.toMutableMap().apply { put(player, hand) }
        val nextPlayer = getNextPlayerAfterDraw(state)
        val nextState = createState(newDeck, state.discardPile, newHands, nextPlayer, state.players, state.direction)
        return GameUpdateResult(nextState, null)
    }

    protected open fun getNextPlayerAndDirection(state: State, card: Card): Pair<PlayerId, Int> {
        return getNextPlayerSimple(state) to state.direction
    }

    protected open fun getNextPlayerAfterDraw(state: State): PlayerId = getNextPlayerSimple(state)

    protected fun getNextPlayerSimple(state: State): PlayerId {
        val idx = state.players.indexOf(state.currentPlayer)
        val nextIdx = (idx + state.direction + state.players.size) % state.players.size
        return state.players[nextIdx]
    }

    override fun isTerminal(state: State): Boolean = getResult(state) != null

    override fun getResult(state: State): Result? {
        val winner = state.hands.entries.find { it.value.size == 0 }?.key
        return if (winner != null) GameResult.Win(winner) as Result? else null
    }
}