package com.gamehub.games.esmofamil

import com.gamehub.shared.core.GameDefinition
import com.gamehub.shared.core.GameResult
import com.gamehub.shared.core.PlayerId
import com.gamehub.shared.engine.GameUpdateResult
import kotlinx.serialization.Serializable
import kotlin.random.Random

val PERSIAN_LETTERS = listOf(
    'ا', 'ب', 'پ', 'ت', 'ث', 'ج', 'چ', 'ح', 'خ', 'د', 'ذ', 'ر', 'ز', 'ژ',
    'س', 'ش', 'ص', 'ض', 'ط', 'ظ', 'ع', 'غ', 'ف', 'ق', 'ک', 'گ', 'ل', 'م',
    'ن', 'و', 'ه', 'ی'
)

val ESF_CATEGORIES = listOf(
    "اسم", "فامیل", "شهر", "کشور", "میوه", "حیوان", "شغل", "ورزش"
)

class EsmoFamilEngine : GameDefinition<EsmoFamilState, EsmoFamilAction, GameResult> {

    override val metadata = com.gamehub.shared.core.GameMetadata(
        id = EsmoFamilState.GAME_ID,
        name = "اسم و فامیل",
        minPlayers = 2,
        maxPlayers = 8,
        description = "بازی اسم و فامیل با ۸ دسته‌بندی!"
    )

    fun startNewRound(state: EsmoFamilState): EsmoFamilState {
        val availableLetters = PERSIAN_LETTERS.filterNot { it in state.usedLetters }
        val letter = if (availableLetters.isEmpty()) {
            PERSIAN_LETTERS.random()
        } else {
            availableLetters.random()
        }

        val emptyAnswers = state.players.associateWith {
            EsmoFamilPlayerAnswers(emptyMap())
        }

        return state.copy(
            phase = EsmoFamilPhase.ANSWERING,
            currentLetter = letter,
            timeRemaining = 60,
            playerAnswers = emptyAnswers
        )
    }

    fun calculateRoundScore(state: EsmoFamilState): Map<PlayerId, Int> {
        val scores = mutableMapOf<PlayerId, Int>()

        // Calculate unique/duplicate answers
        for (categoryIdx in ESF_CATEGORIES.indices) {
            val answersInCategory = state.playerAnswers.mapNotNull { (playerId, pa) ->
                val answer = pa.answers[categoryIdx]
                if (!answer.isNullOrBlank()) {
                    playerId to answer.trim().lowercase()
                } else {
                    null
                }
            }

            val answerCounts = answersInCategory.groupBy { it.second }.mapValues { it.value.size }

            answersInCategory.forEach { (playerId, answer) ->
                val score = if (answerCounts[answer] == 1) 10 else 5
                scores[playerId] = (scores[playerId] ?: 0) + score
            }
        }

        return scores
    }

    fun applyRoundResults(state: EsmoFamilState): GameUpdateResult<EsmoFamilState, GameResult>? {
        val roundScores = calculateRoundScore(state)
        val newStats = state.stats.mapValues { (playerId, stats) ->
            stats.copy(
                totalScore = stats.totalScore + (roundScores[playerId] ?: 0)
            )
        }

        val newRoundNumber = state.roundNumber + 1
        val isGameOver = newRoundNumber > state.maxRounds

        val result = if (isGameOver) {
            val maxScore = newStats.values.maxOfOrNull { it.totalScore } ?: 0
            val winners = newStats.filterValues { it.totalScore == maxScore }.keys.toList()
            if (winners.size == 1) {
                GameResult.Win(winners.first())
            } else {
                GameResult.Draw
            }
        } else null

        var newState = state.copy(
            roundNumber = newRoundNumber,
            stats = newStats,
            usedLetters = state.usedLetters + state.currentLetter
        )

        if (isGameOver) {
            newState = newState.copy(phase = EsmoFamilPhase.SHOWING_RESULTS)
        } else {
            newState = startNewRound(newState)
        }

        return GameUpdateResult(newState, result)
    }

    override fun applyAction(
        state: EsmoFamilState,
        action: EsmoFamilAction,
        playerId: PlayerId
    ): GameUpdateResult<EsmoFamilState, GameResult> {
        return when (action) {
            is EsmoFamilAction.SubmitAnswers -> {
                val newAnswers = state.playerAnswers.toMutableMap()
                newAnswers[playerId] = EsmoFamilPlayerAnswers(action.answers)

                val newState = state.copy(playerAnswers = newAnswers)

                // Check if all players have submitted
                val allSubmitted = newAnswers.keys.containsAll(state.players) &&
                        newAnswers.values.all { it.answers.isNotEmpty() }

                if (allSubmitted) {
                    applyRoundResults(newState) ?: GameUpdateResult(newState)
                } else {
                    GameUpdateResult(newState)
                }
            }
        }
    }

    override fun validateAction(
        state: EsmoFamilState,
        action: EsmoFamilAction,
        playerId: PlayerId
    ): Boolean {
        return state.phase == EsmoFamilPhase.ANSWERING
    }

    override fun isTerminal(state: EsmoFamilState): Boolean =
        state.roundNumber > state.maxRounds || state.phase == EsmoFamilPhase.SHOWING_RESULTS

    override fun getResult(state: EsmoFamilState): GameResult? {
        if (!isTerminal(state)) return null
        val maxScore = state.stats.values.maxOfOrNull { it.totalScore } ?: 0
        val winners = state.stats.filterValues { it.totalScore == maxScore }.keys.toList()
        return if (winners.size == 1) GameResult.Win(winners.first()) else GameResult.Draw
    }

    override fun getPlayers(state: EsmoFamilState): List<PlayerId> = state.players

    override fun setCurrentPlayer(
        state: EsmoFamilState,
        playerId: PlayerId
    ): EsmoFamilState = state.copy(currentPlayer = playerId)

    override fun createInitialState(players: List<PlayerId>): EsmoFamilState {
        val initialStats = players.associateWith { playerId ->
            EsmoFamilPlayerStats(playerId = playerId)
        }

        return startNewRound(
            EsmoFamilState(
                players = players,
                stats = initialStats
            )
        )
    }
}
