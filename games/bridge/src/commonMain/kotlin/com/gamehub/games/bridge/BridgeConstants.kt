package com.gamehub.games.bridge

import kotlinx.serialization.Serializable

// Suit (خال)
@Serializable
enum class Suit(val symbol: String, val persianName: String, val order: Int) {
    CLUBS("♣", "گشنیز", 0),
    DIAMONDS("♦", "خشت", 1),
    HEARTS("♥", "دل", 2),
    SPADES("♠", "پیک", 3),
    NT("NT", "بی‌حکم", 4); // No Trump for bidding

    companion object {
        val bidSuits = listOf(CLUBS, DIAMONDS, HEARTS, SPADES, NT)
        val playSuits = listOf(CLUBS, DIAMONDS, HEARTS, SPADES)
    }
}

// Rank (رتبه)
@Serializable
enum class Rank(val symbol: String, val persianName: String, val order: Int) {
    TWO("2", "دو", 0),
    THREE("3", "سه", 1),
    FOUR("4", "چهار", 2),
    FIVE("5", "پنج", 3),
    SIX("6", "شش", 4),
    SEVEN("7", "هفت", 5),
    EIGHT("8", "هشت", 6),
    NINE("9", "نه", 7),
    TEN("10", "ده", 8),
    JACK("J", "جک", 9),
    QUEEN("Q", "ملکه", 10),
    KING("K", "شاه", 11),
    ACE("A", "آس", 12);
}

// Card (کارت)
@Serializable
data class Card(val suit: Suit, val rank: Rank) {
    override fun toString(): String = "${rank.symbol}${suit.symbol}"
}

// Seat (صندلی)
@Serializable
enum class Seat(val abbreviation: String, val persianName: String) {
    NORTH("N", "شمال"),
    EAST("E", "شرق"),
    SOUTH("S", "جنوب"),
    WEST("W", "غرب");

    fun next(): Seat = values()[(ordinal + 1) % 4]
    fun partner(): Seat = values()[(ordinal + 2) % 4]
    fun team(): Team = if (this == NORTH || this == SOUTH) Team.NS else Team.EW
}

// Team (تیم)
@Serializable
enum class Team(val persianName: String) {
    NS("شمال-جنوب"),
    EW("شرق-غرب");
}

// Vulnerability (آسیب‌پذیری)
@Serializable
enum class Vulnerability(val persianName: String) {
    NONE("هیچ‌کسی"),
    NS("شمال-جنوب"),
    EW("شرق-غرب"),
    ALL("همه");

    fun isVulnerable(team: Team): Boolean = when (this) {
        NONE -> false
        NS -> team == Team.NS
        EW -> team == Team.EW
        ALL -> true
    }
}

// Contract (قرارداد)
@Serializable
data class Contract(
    val level: Int, // 1-7
    val suit: Suit,
    val doubled: Boolean = false,
    val redoubled: Boolean = false,
    val declarer: Seat? = null // Declarer seat
) {
    val requiredTricks: Int = level + 6
}

// Bid (پیشنهاد مزایده)
@Serializable
sealed class Bid {
    @Serializable
    object Pass : Bid()
    @Serializable
    data class Call(val level: Int, val suit: Suit) : Bid()
    @Serializable
    object Double : Bid()
    @Serializable
    object Redouble : Bid()
}

// Trick (دست)
@Serializable
data class Trick(
    val cards: Map<Seat, Card> = emptyMap(),
    val leadSuit: Suit? = null,
    val winner: Seat? = null
)

// Board info for a single board (بورد)
@Serializable
data class BoardInfo(
    val boardNumber: Int,
    val dealer: Seat,
    val vulnerability: Vulnerability
)
