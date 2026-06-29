package com.gamehub.games.bridge

import com.gamehub.shared.core.GameState
import com.gamehub.shared.core.PlayerId
import kotlinx.serialization.Serializable

@Serializable
data class BridgeState(
    val seatPlayers: Map<Seat, PlayerId> = emptyMap(),
    val players: List<PlayerId> = emptyList(),
    val currentPlayer: PlayerId? = null,
    val currentBoard: Int = 1,
    val totalBoards: Int = 8,
    val boardInfo: BoardInfo = BoardInfo(1, Seat.NORTH, Vulnerability.NONE),
    val hands: Map<Seat, List<Card>> = emptyMap(),
    val biddingPhase: Boolean = true,
    val biddingHistory: List<Pair<Seat, Bid>> = emptyList(),
    val currentBidder: Seat? = null,
    val contract: Contract? = null,
    val currentTrick: Trick = Trick(),
    val tricks: List<Trick> = emptyList(),
    val tricksWon: Map<Team, Int> = mapOf(Team.NS to 0, Team.EW to 0),
    val dummyRevealed: Boolean = false,
    val scores: Map<Team, Int> = mapOf(Team.NS to 0, Team.EW to 0),
    val impScores: Map<Team, Int> = mapOf(Team.NS to 0, Team.EW to 0),
    val isFinished: Boolean = false,
) : GameState()