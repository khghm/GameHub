package com.gamehub.games.spadesbaloot

import com.gamehub.shared.core.GameState
import com.gamehub.shared.core.PlayerId
import kotlinx.serialization.Serializable

// --- Core Game Types ---
@Serializable
enum class Suit(val symbol: String, val isTrump: Boolean) {
    SPADES("♠", true),
    HEARTS("♥", false),
    DIAMONDS("♦", false),
    CLUBS("♣", false)
}

@Serializable
enum class Rank(val value: Int) {
    TWO(2),
    THREE(3),
    FOUR(4),
    FIVE(5),
    SIX(6),
    SEVEN(7),
    EIGHT(8),
    NINE(9),
    TEN(10),
    JACK(11),
    QUEEN(12),
    KING(13),
    ACE(14)
}

@Serializable
data class Card(val suit: Suit, val rank: Rank) {
    fun getPointValue(): Int = when (rank) {
        Rank.ACE -> 4
        Rank.KING -> 3
        Rank.QUEEN -> 2
        Rank.JACK -> 1
        Rank.TEN -> 10
        else -> 0
    }
}

// --- Declarations ---
@Serializable
sealed class Declaration {
    @Serializable
    data class Baloot(val playerId: PlayerId) : Declaration()
    
    @Serializable
    data class Sequence(val playerId: PlayerId, val cards: List<Card>) : Declaration()
}

// --- Game Phase ---
@Serializable
enum class GamePhase {
    DEALING, DECLARATION, BIDDING, PLAYING, SCORING, FINISHED
}

// --- The Main State ---
@Serializable
data class SpadesBalootState(
    val players: List<PlayerId>, // 4 players, fixed teams: 0 & 2, 1 & 3
    val hands: Map<PlayerId, List<Card>>,
    val currentPlayer: PlayerId?,
    val gamePhase: GamePhase,
    val tricksPlayed: Int = 0,
    val currentTrick: Map<PlayerId, Card> = emptyMap(),
    val leadSuit: Suit? = null,
    val spadesBroken: Boolean = false,
    val bids: Map<PlayerId, Int> = emptyMap(),
    val nilBids: Set<PlayerId> = emptySet(),
    val tricksWon: Map<PlayerId, Int> = emptyMap(),
    val teamScore1: Int = 0, // Players 0 & 2
    val teamScore2: Int = 0, // Players 1 & 3
    val teamBags1: Int = 0,
    val teamBags2: Int = 0,
    val declarations: List<Declaration> = emptyList(),
    val winner: PlayerId? = null
) : GameState() {
    companion object {
        fun initial(players: List<PlayerId>): SpadesBalootState {
            require(players.size == 4) { "SpadesBaloot requires exactly 4 players" }
            
            // Create and shuffle deck
            val deck = mutableListOf<Card>()
            Suit.entries.forEach { suit ->
                Rank.entries.forEach { rank ->
                    deck.add(Card(suit, rank))
                }
            }
            deck.shuffle()
            
            // Deal 13 cards to each player
            val hands = mutableMapOf<PlayerId, List<Card>>()
            hands[players[0]] = deck.slice(0..12).sortedWith(compareBy({ it.suit }, { it.rank }))
            hands[players[1]] = deck.slice(13..25).sortedWith(compareBy({ it.suit }, { it.rank }))
            hands[players[2]] = deck.slice(26..38).sortedWith(compareBy({ it.suit }, { it.rank }))
            hands[players[3]] = deck.slice(39..51).sortedWith(compareBy({ it.suit }, { it.rank }))
            
            return SpadesBalootState(
                players = players,
                hands = hands,
                currentPlayer = players[0],
                gamePhase = GamePhase.BIDDING
            )
        }
    }
    
    fun getTeamScore(playerId: PlayerId): Int = if (isTeam1(playerId)) teamScore1 else teamScore2
    fun getTeamBags(playerId: PlayerId): Int = if (isTeam1(playerId)) teamBags1 else teamBags2
    fun isTeam1(playerId: PlayerId): Boolean = players.indexOf(playerId) % 2 == 0
    fun getTeammate(playerId: PlayerId): PlayerId {
        val idx = players.indexOf(playerId)
        return players[(idx + 2) % 4]
    }
}
