// server/src/main/kotlin/com/gamehub/server/completion/MatchCompletionService.kt
package com.gamehub.server.completion

import com.gamehub.server.behavior.BehaviorService
import com.gamehub.server.rating.RatingService
import com.gamehub.server.repository.MatchHistoryRepository
import com.gamehub.shared.core.GameResult
import com.gamehub.shared.core.PlayerId

data class MatchParticipantResult(
    val userId: String,
    val result: String,
    val position: Int? = null
)

class MatchCompletionService(
    private val ratingService: RatingService,
    private val behaviorService: BehaviorService,
    private val matchHistoryRepository: MatchHistoryRepository
) {
    suspend fun onMatchEnd(matchId: String, gameId: String, players: List<PlayerId>, result: GameResult, gameSessionId: String? = null) {
        println("🏆 MatchCompletionService.onMatchEnd() CALLED! matchId=$matchId, gameId=$gameId, players=$players, result=$result, gameSessionId=$gameSessionId")
        val results = players.map { player ->
            val playerResult = when (result) {
                is GameResult.Win -> if (result.winner == player) "win" else "loss"
                is GameResult.Draw -> "draw"
                else -> "draw"
            }
            MatchParticipantResult(player.value, playerResult)
        }

        println("🏆 Saving to database (MatchHistoryRepository)...")
        // ذخیره در جدول match_history (database)
        matchHistoryRepository.saveMatch(matchId, gameId, players.map { it.value }, result, gameSessionId)
        
        println("🏆 Saving to MatchHistoryModule (in-memory)...")
        // ذخیره در MatchHistoryModule (in-memory for the API)
        val winnerId = when (result) {
            is GameResult.Win -> result.winner.value
            else -> null
        }
        val isDraw = result is GameResult.Draw
        com.gamehub.server.modules.MatchHistoryModule.addMatch(
            gameType = gameId,
            players = players.map { it.value },
            winner = winnerId,
            draw = isDraw,
            gameSessionId = gameSessionId
        )

        // به‌روزرسانی رتبه و رفتار
        ratingService.updateRatingsForMatch(matchId, gameId, results)
        for (player in players) {
            behaviorService.applyBonus(player.value, "game_finished_clean", matchId)
        }
        
        println("🏆 Match saved successfully in both places!")
    }

}