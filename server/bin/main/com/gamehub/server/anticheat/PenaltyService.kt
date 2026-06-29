package com.gamehub.server.anticheat

import com.gamehub.server.behavior.BehaviorService
import com.gamehub.server.domain.CheatAttemptsTable
import com.gamehub.server.domain.CheatAttemptsTable.penalized
import com.gamehub.server.economy.EconomyService
import com.gamehub.server.rating.RatingService
import com.gamehub.shared.anticheat.CheatAttempt
import com.gamehub.shared.anticheat.ViolationType
import com.gamehub.shared.cache.CacheProvider
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.time.Instant

data class PenaltyLevel(
    val trustDelta: Int,
    val eloDelta: Int,
    val coinDelta: Long,
    val suspensionHours: Int,
    val shadowPool: Boolean
)

class PenaltyService(
    private val cache: CacheProvider,
    private val behaviorService: BehaviorService,
    private val ratingService: RatingService,
    private val economyService: EconomyService
) {
    suspend fun applyPenalty(cheat: CheatAttempt) {
        val userId = cheat.userId
        val violationCount = getViolationCount(userId, cheat.violationType, 30)
        val penalty = when (violationCount) {
            1 -> PenaltyLevel(-10, -50, -100L, 0, false)
            2 -> PenaltyLevel(-20, -100, -200L, 24, true)
            3 -> PenaltyLevel(-40, -200, -500L, 168, true)
            else -> PenaltyLevel(-100, -500, -1000L, 720, true) // ریست به 0
        }

        // اعمال جریمه
        behaviorService.applyPenalty(userId, "cheat_confirmed", cheat.matchId)
        ratingService.adjustRating(userId, cheat.gameId, penalty.eloDelta)
        economyService.deductCoins(userId, penalty.coinDelta, "cheat_penalty", null)

        if (penalty.suspensionHours > 0) {
            cache.set("suspension:$userId", "1", penalty.suspensionHours * 3600L)
        }
        if (penalty.shadowPool) {
            ShadowPoolManager(cache, behaviorService).addToShadowPool(userId, cheat.violationType.name)
        }

        // ذخیره در جدول cheat_attempts (به‌روزرسانی penalized)
        transaction {
            // روش صحیح: استفاده از where با دو شرط
            CheatAttemptsTable.update(
                where = {
                    (CheatAttemptsTable.userId eq userId) and (CheatAttemptsTable.matchId eq cheat.matchId)
                }
            ) {
                it[penalized] = true
            }
        }

        println("⚠️ Penalty applied to $userId: trust=${penalty.trustDelta}, elo=${penalty.eloDelta}, coins=${penalty.coinDelta}, suspension=${penalty.suspensionHours}")
    }

    private suspend fun getViolationCount(userId: String, type: ViolationType, days: Int): Int {
        val cutoff = Instant.now().minusSeconds(days * 86400L)
        return transaction {
            CheatAttemptsTable
                .selectAll()
                .where {
                    (CheatAttemptsTable.userId eq userId) and
                            (CheatAttemptsTable.violationType eq type.name) and
                            (CheatAttemptsTable.detectedAt greater cutoff)
                }
                .count()
                .toInt()
        }
    }
}