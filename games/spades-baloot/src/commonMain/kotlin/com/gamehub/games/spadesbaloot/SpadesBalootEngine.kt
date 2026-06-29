package com.gamehub.games.spadesbaloot

import com.gamehub.shared.core.GameDefinition
import com.gamehub.shared.core.GameResult
import com.gamehub.shared.core.PlayerId
import com.gamehub.shared.engine.GameUpdateResult

class SpadesBalootEngine : GameDefinition<SpadesBalootState, SpadesBalootAction, GameResult> {

    override val metadata = com.gamehub.shared.core.GameMetadata(
        id = "spades-baloot",
        name = "اسپیدز بلوت",
        minPlayers = 2,
        maxPlayers = 4,
        description = "بازی استراتژیک کارتی"
    )

    override fun createInitialState(players: List<PlayerId>): SpadesBalootState {
        // Fill with bots if needed
        val fullPlayers = if (players.size < 4) {
            players + List(4 - players.size) { i -> PlayerId("BOT_${i + 1}") }
        } else {
            players
        }
        return SpadesBalootState.initial(fullPlayers)
    }

    override fun validateAction(state: SpadesBalootState, action: SpadesBalootAction, playerId: PlayerId): Boolean {
        if (state.currentPlayer != playerId) return false
        
        return when (state.gamePhase) {
            GamePhase.BIDDING -> action is SpadesBalootAction.Bid && action.tricks in 0..13
            GamePhase.PLAYING -> action is SpadesBalootAction.PlayCard && isValidPlay(state, playerId, action.card)
            GamePhase.DECLARATION -> true // TODO: Check valid declarations
            else -> false
        }
    }

    private fun isValidPlay(state: SpadesBalootState, playerId: PlayerId, card: Card): Boolean {
        val hand = state.hands[playerId] ?: return false
        if (!hand.contains(card)) return false

        // First card of the trick
        if (state.leadSuit == null) {
            // Can't lead spades unless only spades left or already broken
            if (card.suit == Suit.SPADES && !state.spadesBroken) {
                val hasNonSpades = hand.any { it.suit != Suit.SPADES }
                if (hasNonSpades) return false
            }
            return true
        }

        // Must follow lead suit if possible
        val hasLeadSuit = hand.any { it.suit == state.leadSuit }
        if (hasLeadSuit && card.suit != state.leadSuit) return false

        return true
    }

    override fun applyAction(state: SpadesBalootState, action: SpadesBalootAction, playerId: PlayerId): GameUpdateResult<SpadesBalootState, GameResult> {
        if (!validateAction(state, action, playerId)) {
            return GameUpdateResult(state)
        }

        val newState = when (action) {
            is SpadesBalootAction.Bid -> applyBid(state, playerId, action.tricks, action.isNil)
            is SpadesBalootAction.PlayCard -> applyPlayCard(state, playerId, action.card)
            is SpadesBalootAction.DeclareBaloot -> applyDeclaration(state, playerId, action.declarations)
        }

        return GameUpdateResult(newState, getResult(newState))
    }

    private fun applyDeclaration(state: SpadesBalootState, playerId: PlayerId, declarations: List<Declaration>): SpadesBalootState {
        // TODO: Validate declarations first!
        val newDeclarations = state.declarations + declarations
        val nextPlayer = nextPlayer(state)
        return state.copy(
            declarations = newDeclarations,
            currentPlayer = nextPlayer,
            gamePhase = if (nextPlayer == state.players[0]) GamePhase.BIDDING else state.gamePhase
        )
    }

    private fun applyBid(state: SpadesBalootState, playerId: PlayerId, tricks: Int, isNil: Boolean): SpadesBalootState {
        val newBids = state.bids.toMutableMap()
        val newNilBids = state.nilBids.toMutableSet()
        
        newBids[playerId] = tricks
        if (isNil) newNilBids.add(playerId)

        val nextPlayer = nextPlayer(state)
        val allBidsReceived = newBids.size == 4

        return state.copy(
            bids = newBids,
            nilBids = newNilBids,
            currentPlayer = nextPlayer,
            gamePhase = if (allBidsReceived) GamePhase.PLAYING else state.gamePhase
        )
    }

    private fun applyPlayCard(state: SpadesBalootState, playerId: PlayerId, card: Card): SpadesBalootState {
        // Remove card from player's hand
        val newHands = state.hands.toMutableMap()
        newHands[playerId] = (state.hands[playerId] ?: emptyList()).filter { it != card }

        val newTrick = state.currentTrick.toMutableMap()
        newTrick[playerId] = card

        val newSpadesBroken = state.spadesBroken || (card.suit == Suit.SPADES)
        val newLeadSuit = state.leadSuit ?: card.suit

        // Check if trick is complete (4 cards)
        if (newTrick.size == 4) {
            val winner = determineTrickWinner(newTrick, newLeadSuit)
            val newTricksWon = state.tricksWon.toMutableMap()
            newTricksWon[winner] = (newTricksWon[winner] ?: 0) + 1

            val newTricksPlayed = state.tricksPlayed + 1

            // Check if all 13 tricks are played
            if (newTricksPlayed == 13) {
                // Calculate final scores
                val (finalState, result) = calculateFinalScores(state.copy(
                    hands = newHands,
                    currentTrick = emptyMap(),
                    leadSuit = null,
                    spadesBroken = newSpadesBroken,
                    tricksPlayed = newTricksPlayed,
                    tricksWon = newTricksWon,
                    currentPlayer = winner
                ))
                return finalState
            }

            return state.copy(
                hands = newHands,
                currentTrick = emptyMap(),
                leadSuit = null,
                spadesBroken = newSpadesBroken,
                tricksPlayed = newTricksPlayed,
                tricksWon = newTricksWon,
                currentPlayer = winner
            )
        }

        return state.copy(
            hands = newHands,
            currentTrick = newTrick,
            leadSuit = newLeadSuit,
            spadesBroken = newSpadesBroken,
            currentPlayer = nextPlayer(state)
        )
    }

    private fun determineTrickWinner(trick: Map<PlayerId, Card>, leadSuit: Suit): PlayerId {
        val trumps = trick.filter { it.value.suit == Suit.SPADES }
        if (trumps.isNotEmpty()) {
            return trumps.maxByOrNull { it.value.rank.value }?.key ?: trick.keys.first()
        }
        val leadCards = trick.filter { it.value.suit == leadSuit }
        return leadCards.maxByOrNull { it.value.rank.value }?.key ?: trick.keys.first()
    }

    private fun calculateFinalScores(state: SpadesBalootState): Pair<SpadesBalootState, GameResult?> {
        // Calculate team scores
        val team1Tricks = state.tricksWon.getOrDefault(state.players[0], 0) + state.tricksWon.getOrDefault(state.players[2], 0)
        val team2Tricks = state.tricksWon.getOrDefault(state.players[1], 0) + state.tricksWon.getOrDefault(state.players[3], 0)
        val team1Bid = state.bids.getOrDefault(state.players[0], 0) + state.bids.getOrDefault(state.players[2], 0)
        val team2Bid = state.bids.getOrDefault(state.players[1], 0) + state.bids.getOrDefault(state.players[3], 0)

        var newTeamScore1 = state.teamScore1
        var newTeamBags1 = state.teamBags1
        var newTeamScore2 = state.teamScore2
        var newTeamBags2 = state.teamBags2

        // Team 1 scoring
        if (team1Tricks >= team1Bid) {
            newTeamScore1 += team1Bid * 10
            val bags = team1Tricks - team1Bid
            newTeamBags1 += bags
        } else {
            newTeamScore1 -= team1Bid * 10
        }

        // Team 2 scoring
        if (team2Tricks >= team2Bid) {
            newTeamScore2 += team2Bid * 10
            val bags = team2Tricks - team2Bid
            newTeamBags2 += bags
        } else {
            newTeamScore2 -= team2Bid * 10
        }

        // Handle bag penalty (10 bags = -100 points)
        while (newTeamBags1 >= 10) {
            newTeamScore1 -= 100
            newTeamBags1 -= 10
        }
        while (newTeamBags2 >= 10) {
            newTeamScore2 -= 100
            newTeamBags2 -= 10
        }

        // Check for nil bonus/penalty
        state.nilBids.forEach { playerId ->
            val tricksWon = state.tricksWon.getOrDefault(playerId, 0)
            val isTeam1 = state.isTeam1(playerId)
            if (tricksWon == 0) {
                if (isTeam1) newTeamScore1 += 100 else newTeamScore2 += 100
            } else {
                if (isTeam1) newTeamScore1 -= 100 else newTeamScore2 -= 100
            }
        }

        // Check for end condition (500 points)
        val targetScore = 500
        val team1Wins = newTeamScore1 >= targetScore && newTeamScore1 > newTeamScore2
        val team2Wins = newTeamScore2 >= targetScore && newTeamScore2 > newTeamScore1

        val winner = when {
            team1Wins -> state.players[0] // Or any player from team 1
            team2Wins -> state.players[1] // Or any player from team 2
            else -> null
        }

        val newState = state.copy(
            teamScore1 = newTeamScore1,
            teamScore2 = newTeamScore2,
            teamBags1 = newTeamBags1,
            teamBags2 = newTeamBags2,
            winner = winner,
            gamePhase = if (team1Wins || team2Wins) GamePhase.FINISHED else GamePhase.SCORING
        )

        val result = if (team1Wins) {
            GameResult.Win(state.players[0]) // Or player 2
        } else if (team2Wins) {
            GameResult.Win(state.players[1]) // Or player 3
        } else null

        return Pair(newState, result)
    }

    private fun nextPlayer(state: SpadesBalootState): PlayerId {
        val currentIdx = state.players.indexOf(state.currentPlayer)
        return state.players[(currentIdx + 1) % 4]
    }

    override fun isTerminal(state: SpadesBalootState): Boolean {
        return state.gamePhase == GamePhase.FINISHED
    }

    override fun getResult(state: SpadesBalootState): GameResult? {
        if (!isTerminal(state)) return null
        return if (state.teamScore1 > state.teamScore2) GameResult.Win(state.players[0]) else GameResult.Win(state.players[1])
    }
}
