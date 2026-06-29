package com.gamehub.games.bridge

import com.gamehub.shared.core.*
import com.gamehub.shared.engine.GameUpdateResult
import kotlin.random.Random

class BridgeEngine : GameDefinition<BridgeState, BridgeAction, GameResult> {
    override val metadata = GameMetadata(
        id = "bridge",
        name = "بریج (Bridge)",
        minPlayers = 2,
        maxPlayers = 4,
        description = "بازی استراتژیک کارتی چهار نفره"
    )

    private val vulnerabilitySchedule = listOf(
        Vulnerability.NONE, Vulnerability.NS, Vulnerability.EW, Vulnerability.ALL,
        Vulnerability.NS, Vulnerability.EW, Vulnerability.ALL, Vulnerability.NONE,
        Vulnerability.EW, Vulnerability.ALL, Vulnerability.NONE, Vulnerability.NS,
        Vulnerability.ALL, Vulnerability.NONE, Vulnerability.NS, Vulnerability.EW
    )

    override fun createInitialState(players: List<PlayerId>): BridgeState {
        val fullSeats = Seat.values().toList()
        val seatPlayers = fullSeats.mapIndexed { index, seat ->
            seat to (players.getOrNull(index) ?: PlayerId("BOT_${seat.abbreviation}"))
        }.toMap()
        val allPlayers = seatPlayers.values.toList()

        val firstBoard = 1
        val dealer = getDealerForBoard(firstBoard)
        val vulnerability = getVulnerabilityForBoard(firstBoard)
        val hands = dealHands()
        val currentPlayer = seatPlayers[dealer]

        return BridgeState(
            seatPlayers = seatPlayers,
            players = allPlayers,
            currentPlayer = currentPlayer,
            isFinished = false,
            currentBoard = firstBoard,
            boardInfo = BoardInfo(firstBoard, dealer, vulnerability),
            hands = hands,
            currentBidder = dealer,
            biddingPhase = true
        )
    }

    private fun dealHands(seed: Long? = null): Map<Seat, List<Card>> {
        val random = if (seed != null) Random(seed) else Random
        val deck = Suit.playSuits.flatMap { suit ->
            Rank.values().map { rank -> Card(suit, rank) }
        }.shuffled(random)

        return mapOf(
            Seat.NORTH to deck.subList(0, 13).sortedWith(compareBy({ it.suit.ordinal }, { it.rank.ordinal })),
            Seat.EAST to deck.subList(13, 26).sortedWith(compareBy({ it.suit.ordinal }, { it.rank.ordinal })),
            Seat.SOUTH to deck.subList(26, 39).sortedWith(compareBy({ it.suit.ordinal }, { it.rank.ordinal })),
            Seat.WEST to deck.subList(39, 52).sortedWith(compareBy({ it.suit.ordinal }, { it.rank.ordinal }))
        )
    }

    private fun getDealerForBoard(boardNumber: Int): Seat =
        Seat.values()[(boardNumber - 1) % 4]

    private fun getVulnerabilityForBoard(boardNumber: Int): Vulnerability =
        vulnerabilitySchedule[(boardNumber - 1) % 16]

    override fun validateAction(state: BridgeState, action: BridgeAction, playerId: PlayerId): Boolean {
        val actingSeat = state.seatPlayers.entries.find { it.value == playerId }?.key ?: return false

        if (state.biddingPhase) {
            return validateBidding(state, action, actingSeat)
        } else {
            return validatePlay(state, action, actingSeat)
        }
    }

    private fun validateBidding(state: BridgeState, action: BridgeAction, actingSeat: Seat): Boolean {
        if (action !is BridgeAction.MakeBid) return false
        if (actingSeat != state.currentBidder) return false
        return isValidBid(state, action.bid, actingSeat)
    }

    private fun isValidBid(state: BridgeState, bid: Bid, seat: Seat): Boolean {
        val lastCall = state.biddingHistory.lastOrNull { it.second is Bid.Call }?.second as Bid.Call?
        val lastAction = state.biddingHistory.lastOrNull()?.second

        when (bid) {
            is Bid.Pass -> return true
            is Bid.Call -> {
                if (lastCall == null) {
                    return bid.level in 1..7
                }
                if (bid.level > lastCall.level) return true
                if (bid.level == lastCall.level && bid.suit.ordinal > lastCall.suit.ordinal) return true
                return false
            }
            is Bid.Double -> {
                if (lastCall == null) return false
                val lastCallSeat = state.biddingHistory.lastOrNull { it.second is Bid.Call }?.first
                if (lastCallSeat?.team() == seat.team()) return false
                if (lastAction is Bid.Double || lastAction is Bid.Redouble) return false
                return true
            }
            is Bid.Redouble -> {
                if (lastAction != Bid.Double) return false
                val lastDoubleSeat = state.biddingHistory.lastOrNull { it.second == Bid.Double }?.first
                val lastCallSeat = state.biddingHistory.lastOrNull { it.second is Bid.Call }?.first
                if (lastDoubleSeat?.team() == seat.team()) return false
                if (lastCallSeat?.team() != seat.team()) return false
                return true
            }
        }
    }

    private fun validatePlay(state: BridgeState, action: BridgeAction, actingSeat: Seat): Boolean {
        if (action !is BridgeAction.PlayCard) return false

        val currentSeat = state.seatPlayers.entries.find { it.value == state.currentPlayer }?.key ?: return false
        if (actingSeat != currentSeat) return false

        val hand = state.hands[actingSeat] ?: return false
        if (!hand.contains(action.card)) return false

        val leadSuit = state.currentTrick.leadSuit
        if (leadSuit != null) {
            val hasLeadSuit = hand.any { it.suit == leadSuit }
            if (hasLeadSuit && action.card.suit != leadSuit) return false
        }

        return true
    }

    override fun applyAction(state: BridgeState, action: BridgeAction, playerId: PlayerId): GameUpdateResult<BridgeState, GameResult> {
        if (!validateAction(state, action, playerId)) {
            return GameUpdateResult(state)
        }

        val actingSeat = state.seatPlayers.entries.find { it.value == playerId }?.key ?: return GameUpdateResult(state)

        val newState = if (state.biddingPhase) {
            applyBidding(state, action as BridgeAction.MakeBid, actingSeat)
        } else {
            applyPlay(state, action as BridgeAction.PlayCard, actingSeat)
        }

        return GameUpdateResult(newState, getResult(newState))
    }

    private fun applyBidding(state: BridgeState, action: BridgeAction.MakeBid, actingSeat: Seat): BridgeState {
        val bid = action.bid
        val newHistory = state.biddingHistory + (actingSeat to bid)

        val lastCall = newHistory.lastOrNull { it.second is Bid.Call }?.second as Bid.Call?
        val biddingClosed = when {
            newHistory.size == 4 && newHistory.all { it.second == Bid.Pass } -> true
            lastCall != null && newHistory.takeLast(3).all { it.second == Bid.Pass } -> true
            else -> false
        }

        if (biddingClosed) {
            val contract = if (lastCall != null) {
                val declaringTeam = newHistory.firstOrNull {
                    it.second is Bid.Call && (it.second as Bid.Call).suit == lastCall.suit
                }?.first?.team()

                val firstBidder = newHistory.firstOrNull {
                    it.second is Bid.Call &&
                    (it.second as Bid.Call).suit == lastCall.suit &&
                    it.first.team() == declaringTeam
                }?.first ?: actingSeat

                var doubled = false
                var redoubled = false
                val lastCallIndex = newHistory.indexOfLast { it.second is Bid.Call }
                for (i in lastCallIndex + 1 until newHistory.size) {
                    if (newHistory[i].second == Bid.Double) doubled = true
                    if (newHistory[i].second == Bid.Redouble) {
                        redoubled = true
                        doubled = false
                    }
                }

                Contract(
                    level = lastCall.level,
                    suit = lastCall.suit,
                    doubled = doubled,
                    redoubled = redoubled,
                    declarer = firstBidder
                )
            } else null

            val openingLeader = contract?.declarer?.next()?.next()
            val currentPlayer = openingLeader?.let { state.seatPlayers[it] }

            return state.copy(
                biddingHistory = newHistory,
                biddingPhase = false,
                contract = contract,
                currentBidder = null,
                currentPlayer = currentPlayer
            )
        }

        val nextBidder = actingSeat.next()
        val nextPlayer = state.seatPlayers[nextBidder]
        return state.copy(
            biddingHistory = newHistory,
            currentBidder = nextBidder,
            currentPlayer = nextPlayer
        )
    }

    private fun applyPlay(state: BridgeState, action: BridgeAction.PlayCard, actingSeat: Seat): BridgeState {
        val newHands = state.hands.toMutableMap()
        val hand = newHands[actingSeat]?.toMutableList() ?: return state
        hand.remove(action.card)
        newHands[actingSeat] = hand

        val newCards = state.currentTrick.cards + (actingSeat to action.card)
        val newLeadSuit = state.currentTrick.leadSuit ?: action.card.suit

        val dummyRevealed = state.dummyRevealed || newCards.size == 1

        if (newCards.size == 4) {
            val winner = determineTrickWinner(newCards, newLeadSuit, state.contract?.suit)
            val newTricksWon = state.tricksWon.toMutableMap()
            newTricksWon[winner.team()] = (newTricksWon[winner.team()] ?: 0) + 1

            val newTrick = Trick(newCards, newLeadSuit, winner)
            val newTricksList = state.tricks + newTrick

            if (newTricksList.size == 13) {
                val scoredState = scoreBoard(state.copy(
                    hands = newHands,
                    currentTrick = Trick(),
                    tricks = newTricksList,
                    tricksWon = newTricksWon,
                    dummyRevealed = dummyRevealed
                ))

                if (scoredState.currentBoard >= scoredState.totalBoards) {
                    return scoredState.copy(
                        isFinished = true,
                        currentPlayer = null
                    )
                }

                return startNextBoard(scoredState)
            }

            return state.copy(
                hands = newHands,
                currentTrick = Trick(),
                tricks = newTricksList,
                tricksWon = newTricksWon,
                dummyRevealed = dummyRevealed,
                currentBidder = null,
                currentPlayer = state.seatPlayers[winner]
            )
        }

        val nextSeat = newCards.keys.last().next()
        return state.copy(
            hands = newHands,
            currentTrick = Trick(newCards, newLeadSuit, null),
            dummyRevealed = dummyRevealed,
            currentPlayer = state.seatPlayers[nextSeat]
        )
    }

    private fun determineTrickWinner(cards: Map<Seat, Card>, leadSuit: Suit, trumpSuit: Suit?): Seat {
        val effectiveTrump = if (trumpSuit == Suit.NT) null else trumpSuit
        val trumps = if (effectiveTrump != null) {
            cards.filter { it.value.suit == effectiveTrump }
        } else emptyMap()

        val relevantCards = if (trumps.isNotEmpty()) {
            trumps
        } else {
            cards.filter { it.value.suit == leadSuit }
        }

        return relevantCards.maxByOrNull { it.value.rank.ordinal }?.key ?: cards.keys.first()
    }

    private fun scoreBoard(state: BridgeState): BridgeState {
        val contract = state.contract ?: return state
        val declaringTeam = contract.declarer?.team() ?: return state
        val tricksMade = state.tricksWon[declaringTeam] ?: 0
        val required = contract.requiredTricks

        val newScores = state.scores.toMutableMap()

        if (tricksMade >= required) {
            val baseScore = when (contract.suit) {
                Suit.CLUBS, Suit.DIAMONDS -> contract.level * 20
                Suit.HEARTS, Suit.SPADES -> contract.level * 30
                Suit.NT -> 40 + (contract.level - 1) * 30
            }
            newScores[declaringTeam] = (newScores[declaringTeam] ?: 0) + baseScore + 50
        } else {
            val undertricks = required - tricksMade
            val defendingTeam = if (declaringTeam == Team.NS) Team.EW else Team.NS
            newScores[defendingTeam] = (newScores[defendingTeam] ?: 0) + undertricks * 50
        }

        return state.copy(scores = newScores)
    }

    private fun startNextBoard(state: BridgeState): BridgeState {
        val nextBoard = state.currentBoard + 1
        val dealer = getDealerForBoard(nextBoard)
        val vulnerability = getVulnerabilityForBoard(nextBoard)
        val hands = dealHands()
        val currentPlayer = state.seatPlayers[dealer]

        return state.copy(
            currentBoard = nextBoard,
            boardInfo = BoardInfo(nextBoard, dealer, vulnerability),
            hands = hands,
            biddingPhase = true,
            biddingHistory = emptyList(),
            currentBidder = dealer,
            contract = null,
            currentTrick = Trick(),
            tricks = emptyList(),
            tricksWon = mapOf(Team.NS to 0, Team.EW to 0),
            dummyRevealed = false,
            currentPlayer = currentPlayer
        )
    }

    override fun isTerminal(state: BridgeState): Boolean {
        return state.isFinished
    }

    override fun getResult(state: BridgeState): GameResult? {
        if (!isTerminal(state)) return null
        val nsScore = state.impScores[Team.NS] ?: 0
        val ewScore = state.impScores[Team.EW] ?: 0
        return when {
            nsScore > ewScore -> GameResult.Win(state.seatPlayers[Seat.NORTH] ?: state.players.first())
            ewScore > nsScore -> GameResult.Win(state.seatPlayers[Seat.EAST] ?: state.players.first())
            else -> GameResult.Draw
        }
    }
}