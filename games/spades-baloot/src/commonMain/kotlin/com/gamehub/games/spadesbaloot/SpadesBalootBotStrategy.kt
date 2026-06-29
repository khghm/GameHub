package com.gamehub.games.spadesbaloot

import com.gamehub.shared.bot.BotStrategy
import com.gamehub.shared.core.PlayerId
import kotlinx.coroutines.delay
import kotlin.math.min
import kotlin.random.Random

class SpadesBalootBotStrategy : BotStrategy<SpadesBalootState, SpadesBalootAction> {
    override val gameId = "spades-baloot"
    override val supportedDifficultyLevels = 1..10

    override suspend fun getNextMove(
        state: SpadesBalootState,
        botPlayerId: PlayerId,
        difficultyLevel: Int
    ): SpadesBalootAction? {
        val hand = state.hands[botPlayerId] ?: return null

        // Delay based on difficulty
        val delayMs = when {
            difficultyLevel <= 3 -> 20L
            difficultyLevel <= 6 -> 40L
            else -> 60L
        }
        delay(delayMs)

        return when (state.gamePhase) {
            GamePhase.BIDDING -> {
                calculateBid(state, hand, botPlayerId, difficultyLevel)
            }

            GamePhase.PLAYING -> {
                selectBestCard(state, hand, botPlayerId, difficultyLevel)
            }

            else -> null
        }
    }

    // --- Bidding Strategy ---
    private fun calculateBid(
        state: SpadesBalootState,
        hand: List<Card>,
        botPlayerId: PlayerId,
        difficultyLevel: Int
    ): SpadesBalootAction.Bid {
        val spadeCount = hand.count { it.suit == Suit.SPADES }
        val strongSpades = hand.count { it.suit == Suit.SPADES && it.rank.value >= 11 } // J, Q, K, A
        val aces = hand.count { it.rank == Rank.ACE }
        val kings = hand.count { it.rank == Rank.KING }
        val voids = countVoids(hand) // Suits with 0 cards
        val singletons = countSingletons(hand) // Suits with 1 card

        // Check if nil bid is possible
        val canNil = canBidNil(hand, difficultyLevel)

        if (canNil && difficultyLevel >= 4) {
            return SpadesBalootAction.Bid(0, true)
        }

        // Base bid calculation
        var bid = 0

        // 1. Spade strength
        bid += strongSpades
        if (spadeCount >= 5) bid += 1
        if (spadeCount >= 7) bid += 1

        // 2. Aces and Kings
        bid += aces
        bid += (kings / 2)

        // 3. Voids and singletons (good for ruffing)
        bid += voids
        bid += (singletons / 2)

        // Adjust for difficulty
        bid = when {
            difficultyLevel <= 3 -> bid - 1 // Conservative for low difficulty
            difficultyLevel <= 6 -> bid // Balanced
            else -> bid + 1 // Aggressive for high difficulty
        }

        // Ensure bid is within valid range
        val finalBid = bid.coerceIn(1, 13)

        return SpadesBalootAction.Bid(finalBid, false)
    }

    private fun canBidNil(hand: List<Card>, difficultyLevel: Int): Boolean {
        if (difficultyLevel < 4) return false // No nil for low difficulty

        // Don't bid nil if we have strong spades
        val highSpades = hand.count { it.suit == Suit.SPADES && it.rank.value >= 10 }
        if (highSpades >= 2) return false

        // Don't bid nil with Ace or King of spades
        if (hand.any { it.suit == Suit.SPADES && (it.rank == Rank.ACE || it.rank == Rank.KING) }) {
            return false
        }

        // Count of "safe" cards (low cards)
        val safeCards = hand.count { it.rank.value <= 7 }
        if (safeCards < 7) return false

        return true
    }

    private fun countVoids(hand: List<Card>): Int {
        val suitsPresent = hand.map { it.suit }.toSet()
        return 4 - suitsPresent.size
    }

    private fun countSingletons(hand: List<Card>): Int {
        return hand.groupBy { it.suit }.count { it.value.size == 1 }
    }

    // --- Card Playing Strategy ---
    private fun selectBestCard(
        state: SpadesBalootState,
        hand: List<Card>,
        botPlayerId: PlayerId,
        difficultyLevel: Int
    ): SpadesBalootAction.PlayCard? {
        val validCards = hand.filter { isValidPlay(state, botPlayerId, it) }
        if (validCards.isEmpty()) return null

        val teammate = state.getTeammate(botPlayerId)
        val myTricks = state.tricksWon[botPlayerId] ?: 0
        val teammateTricks = state.tricksWon[teammate] ?: 0
        val myBid = state.bids[botPlayerId] ?: 0
        val teammateBid = state.bids[teammate] ?: 0
        val teamBid = myBid + teammateBid
        val teamTricks = myTricks + teammateTricks
        val needMoreTricks = teamTricks < teamBid
        val tryingForBag = (teamTricks - teamBid) < 5 && teamBid <= teamTricks

        return when {
            state.leadSuit == null -> selectLeadCard(validCards, state, needMoreTricks, tryingForBag, difficultyLevel)
            else -> selectFollowCard(validCards, state, botPlayerId, needMoreTricks, tryingForBag, difficultyLevel)
        }
    }

    private fun selectLeadCard(
        validCards: List<Card>,
        state: SpadesBalootState,
        needMoreTricks: Boolean,
        tryingForBag: Boolean,
        difficultyLevel: Int
    ): SpadesBalootAction.PlayCard {
        // High difficulty: strategic leading
        if (difficultyLevel >= 7) {
            // 1. Lead ace of non-spade if we have it
            val aceNonSpade = validCards.find { it.rank == Rank.ACE && it.suit != Suit.SPADES }
            if (aceNonSpade != null && needMoreTricks) {
                return SpadesBalootAction.PlayCard(aceNonSpade)
            }

            // 2. Lead lowest card if we're trying to avoid bags
            if (!needMoreTricks || tryingForBag) {
                val lowestCard = validCards.minByOrNull { it.rank.value }
                if (lowestCard != null) {
                    return SpadesBalootAction.PlayCard(lowestCard)
                }
            }

            // 3. Lead a singleton to try to ruff later
            val singletons = validCards
                .groupBy { it.suit }
                .filter { it.value.size == 1 }
                .values
                .flatten()
            if (singletons.isNotEmpty()) {
                return SpadesBalootAction.PlayCard(singletons.first())
            }
        }

        // Medium difficulty: balanced play
        if (difficultyLevel >= 4) {
            // Lead middle card
            val sorted = validCards.sortedBy { it.rank.value }
            val midIndex = sorted.size / 2
            return SpadesBalootAction.PlayCard(sorted[midIndex])
        }

        // Low difficulty: random valid card
        return SpadesBalootAction.PlayCard(validCards.random())
    }

    private fun selectFollowCard(
        validCards: List<Card>,
        state: SpadesBalootState,
        botPlayerId: PlayerId,
        needMoreTricks: Boolean,
        tryingForBag: Boolean,
        difficultyLevel: Int
    ): SpadesBalootAction.PlayCard {
        val leadSuit = state.leadSuit!!
        val cardsInLeadSuit = validCards.filter { it.suit == leadSuit }
        val currentTrick = state.currentTrick.values.toList()
        val canWinTrick = canWinCurrentTrick(validCards, currentTrick, leadSuit)
        val teammate = state.getTeammate(botPlayerId)
        val teammateWining = isTeammateWinning(state, teammate)

        // High difficulty: advanced play
        if (difficultyLevel >= 7) {
            // 1. If teammate is winning and we don't need tricks, play low
            if (teammateWining && !needMoreTricks) {
                val lowestCard = validCards.minByOrNull { it.rank.value }
                if (lowestCard != null) {
                    return SpadesBalootAction.PlayCard(lowestCard)
                }
            }

            // 2. If we need tricks and can win, play the lowest winning card
            if (needMoreTricks && canWinTrick) {
                val winningCards = getWinningCards(validCards, currentTrick, leadSuit)
                val lowestWinner = winningCards.minByOrNull { it.rank.value }
                if (lowestWinner != null) {
                    return SpadesBalootAction.PlayCard(lowestWinner)
                }
            }

            // 3. If we have lead suit but can't win, play lowest
            if (cardsInLeadSuit.isNotEmpty()) {
                val lowestInSuit = cardsInLeadSuit.minByOrNull { it.rank.value }
                if (lowestInSuit != null) {
                    return SpadesBalootAction.PlayCard(lowestInSuit)
                }
            }

            // 4. If we don't have lead suit, try to trump with spade
            val spades = validCards.filter { it.suit == Suit.SPADES }
            if (spades.isNotEmpty() && needMoreTricks) {
                val lowestSpade = spades.minByOrNull { it.rank.value }
                if (lowestSpade != null) {
                    return SpadesBalootAction.PlayCard(lowestSpade)
                }
            }
        }

        // Medium difficulty: basic strategy
        if (difficultyLevel >= 4) {
            if (needMoreTricks && canWinTrick) {
                return SpadesBalootAction.PlayCard(getWinningCards(validCards, currentTrick, leadSuit).first())
            }
            if (cardsInLeadSuit.isNotEmpty()) {
                return SpadesBalootAction.PlayCard(cardsInLeadSuit.minByOrNull { it.rank.value }!!)
            }
        }

        // Fallback: random valid card
        return SpadesBalootAction.PlayCard(validCards.random())
    }

    private fun canWinCurrentTrick(validCards: List<Card>, currentTrick: List<Card>, leadSuit: Suit): Boolean {
        if (currentTrick.isEmpty()) return true
        val currentWinner = getCurrentTrickWinner(currentTrick, leadSuit) ?: return true
        return validCards.any { card ->
            when {
                card.suit == Suit.SPADES && currentWinner.suit != Suit.SPADES -> true
                card.suit == Suit.SPADES && currentWinner.suit == Suit.SPADES -> card.rank.value > currentWinner.rank.value
                card.suit == leadSuit && card.rank.value > currentWinner.rank.value -> true
                else -> false
            }
        }
    }

    private fun getWinningCards(validCards: List<Card>, currentTrick: List<Card>, leadSuit: Suit): List<Card> {
        if (currentTrick.isEmpty()) return validCards
        val currentWinner = getCurrentTrickWinner(currentTrick, leadSuit) ?: return validCards
        return validCards.filter { card ->
            when {
                card.suit == Suit.SPADES && currentWinner.suit != Suit.SPADES -> true
                card.suit == Suit.SPADES && currentWinner.suit == Suit.SPADES -> card.rank.value > currentWinner.rank.value
                card.suit == leadSuit && card.rank.value > currentWinner.rank.value -> true
                else -> false
            }
        }
    }

    private fun getCurrentTrickWinner(currentTrick: List<Card>, leadSuit: Suit): Card? {
        if (currentTrick.isEmpty()) return null
        val spades = currentTrick.filter { it.suit == Suit.SPADES }
        if (spades.isNotEmpty()) {
            return spades.maxByOrNull { it.rank.value }
        }
        val leadCards = currentTrick.filter { it.suit == leadSuit }
        return leadCards.maxByOrNull { it.rank.value }
    }

    private fun isTeammateWinning(state: SpadesBalootState, teammate: PlayerId): Boolean {
        val teammateCard = state.currentTrick[teammate] ?: return false
        val leadSuit = state.leadSuit ?: return false
        val currentWinner = getCurrentTrickWinner(state.currentTrick.values.toList(), leadSuit)
        return currentWinner == teammateCard
    }

    private fun isValidPlay(state: SpadesBalootState, playerId: PlayerId, card: Card): Boolean {
        val hand = state.hands[playerId] ?: return false
        if (!hand.contains(card)) return false

        if (state.leadSuit == null) {
            if (card.suit == Suit.SPADES && !state.spadesBroken) {
                val hasNonSpades = hand.any { it.suit != Suit.SPADES }
                if (hasNonSpades) return false
            }
            return true
        }

        val hasLeadSuit = hand.any { it.suit == state.leadSuit }
        if (hasLeadSuit && card.suit != state.leadSuit) return false

        return true
    }
}
