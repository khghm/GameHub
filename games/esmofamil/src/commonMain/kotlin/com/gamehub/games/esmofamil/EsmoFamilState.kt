package com.gamehub.games.esmofamil

import com.gamehub.shared.core.GameState
import com.gamehub.shared.core.PlayerId
import kotlinx.serialization.Serializable

@Serializable
data class EsmoFamilPlayerAnswers(
    val answers: Map<Int, String?> = emptyMap()
)

@Serializable
data class EsmoFamilPlayerStats(
    val playerId: PlayerId,
    val totalScore: Int = 0
)

@Serializable
enum class EsmoFamilPhase {
    SELECTING_LETTER,
    ANSWERING,
    SHOWING_RESULTS
}

@Serializable
data class EsmoFamilState(
    val players: List<PlayerId>,
    val stats: Map<PlayerId, EsmoFamilPlayerStats>,
    val currentPlayer: PlayerId? = players.firstOrNull(),
    val phase: EsmoFamilPhase = EsmoFamilPhase.SELECTING_LETTER,
    val roundNumber: Int = 1,
    val maxRounds: Int = 5,
    val currentLetter: Char = 'ا',
    val timeRemaining: Int = 60,
    val playerAnswers: Map<PlayerId, EsmoFamilPlayerAnswers> = emptyMap(),
    val usedLetters: List<Char> = emptyList()
) : GameState() {

    companion object {
        const val GAME_ID = "esmofamil"
    }
}
