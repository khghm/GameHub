package com.gamehub.server.rating

import com.gamehub.server.completion.MatchParticipantResult
import com.gamehub.server.repository.RatingChange
import com.gamehub.server.repository.RatingRepository
import com.gamehub.shared.matchmaking.SkillRating
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.pow
import java.util.concurrent.ConcurrentHashMap

class RatingService(private val ratingRepository: RatingRepository) {
    private val userLocks = ConcurrentHashMap<String, Mutex>()

    private fun getLock(userId: String): Mutex {
        return userLocks.computeIfAbsent(userId) { Mutex() }
    }

    suspend fun getRating(userId: String, gameId: String) = ratingRepository.getOrCreate(userId, gameId)

    suspend fun updateRatingsForMatch(matchId: String, gameId: String, results: List<MatchParticipantResult>) {
        // Step 1: Get all current ratings and lock users to prevent concurrent modifications
        val infos = results.map { p ->
            val lock = getLock(p.userId)
            lock.withLock {
                p to ratingRepository.getOrCreate(p.userId, gameId)
            }
        }

        // Step 2: Calculate new ratings
        val newInfos = mutableMapOf<String, Pair<Int, SkillRating>>()
        for ((p, info) in infos) {
            val newSkillRating = calculateNewSkillRating(info.skillRating, infos, p)
            val newRating = newSkillRating.mean.toInt().coerceIn(0, 10000)
            newInfos[p.userId] = newRating to newSkillRating
        }

        // Step 3: Update ratings in DB (with locks)
        for ((p, info) in infos) {
            val ratingUpdate = newInfos[p.userId] ?: continue
            val lock = getLock(p.userId)
            lock.withLock {
                val (newRating, newSkillRating) = ratingUpdate
                val newWins = info.wins + if (p.result == "win") 1 else 0
                val newLosses = info.losses + if (p.result == "loss") 1 else 0
                val newDraws = info.draws + if (p.result == "draw") 1 else 0
                val newGamesPlayed = info.gamesPlayed + 1

                ratingRepository.updateRating(
                    userId = p.userId,
                    gameId = gameId,
                    newRating = newRating,
                    newSkillRating = newSkillRating,
                    wins = newWins,
                    losses = newLosses,
                    draws = newDraws,
                    gamesPlayed = newGamesPlayed
                )
                ratingRepository.logChange(
                    RatingChange(
                        userId = p.userId,
                        gameId = gameId,
                        matchId = matchId,
                        oldRating = info.rating,
                        newRating = newRating,
                        change = newRating - info.rating,
                        reason = p.result.uppercase()
                    )
                )
            }
        }
    }

    suspend fun adjustRating(userId: String, gameId: String, delta: Int) {
        val lock = getLock(userId)
        lock.withLock {
            val current = ratingRepository.getOrCreate(userId, gameId)
            val newRating = (current.rating + delta).coerceIn(0, 10000)
            // Also adjust mean slightly
            val newSkillRating = current.skillRating.copy(mean = newRating.toDouble())
            ratingRepository.updateRating(
                userId = userId,
                gameId = gameId,
                newRating = newRating,
                newSkillRating = newSkillRating,
                wins = current.wins,
                losses = current.losses,
                draws = current.draws,
                gamesPlayed = current.gamesPlayed
            )
            ratingRepository.logChange(
                RatingChange(userId, gameId, "admin", current.rating, newRating, delta, "adjustment")
            )
        }
    }

    private fun calculateNewSkillRating(
        myRating: SkillRating,
        allParticipants: List<Pair<MatchParticipantResult, com.gamehub.shared.rating.RatingInfo>>,
        myResult: MatchParticipantResult
    ): SkillRating {
        // For simplicity, let's do a Glicko-like update, or just use Elo for now and keep SkillRating in sync
        var totalMeanChange = 0.0
        var count = 0
        for ((p, info) in allParticipants) {
            if (p.userId == myResult.userId) continue
            val expected = 1.0 / (1.0 + 10.0.pow((info.skillRating.mean - myRating.mean) / 400.0))
            val actual = when (myResult.result) {
                "win" -> 1.0
                "loss" -> 0.0
                else -> 0.5
            }
            val k = when (allParticipants.find { it.first.userId == myResult.userId }?.second?.gamesPlayed) {
                in 0..9 -> 60.0
                in 10..49 -> 32.0
                else -> 16.0
            }
            totalMeanChange += k * (actual - expected)
            count++
        }
        val avgChange = if (count > 0) totalMeanChange / count else 0.0
        // Update stdDev slightly (simplified)
        val newStdDev = (myRating.standardDeviation * 0.99).coerceAtLeast(30.0)
        return SkillRating(
            mean = myRating.mean + avgChange,
            standardDeviation = newStdDev,
            volatility = myRating.volatility
        )
    }

    @Deprecated("Use SkillRating-based calculation instead")
    private fun calculateEloChange(myRating: Int, oppRating: Int, result: Double, gamesPlayed: Int): Int {
        val expected = 1.0 / (1.0 + 10.0.pow((oppRating - myRating) / 400.0))
        val k = when {
            gamesPlayed < 10 -> 60
            gamesPlayed < 50 -> 32
            else -> 16
        }
        var change = (k * (result - expected)).toInt()
        // پاداش شگفتی (Upset Bonus) برای برد غیرمنتظره
        if (result == 1.0 && expected < 0.3) {
            change += (k * 0.25).toInt()
        }
        return change
    }
}