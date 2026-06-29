package com.gamehub.server.anticheat

import com.gamehub.server.domain.CheatAttemptsTable
import com.gamehub.shared.anticheat.CheatAttempt
import com.gamehub.shared.anticheat.ViolationType
import com.gamehub.shared.cache.CacheProvider
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant

class AntiCheatService(
    private val cache: CacheProvider,
    private val timeAttestation: TimeAttestation,
    private val speedHackDetector: SpeedHackDetector,
    private val lagSwitchDetector: LagSwitchDetector,
    private val macroDetector: MacroDetector,
    private val collusionDetector: CollusionDetector,
    private val penaltyService: PenaltyService,
    private val behaviorDetector: BotBehaviorDetector
) {
    suspend fun checkMove(
        userId: String,
        sessionId: String,
        gameId: String,
        matchId: String,
        moveType: String,
        clientSendTime: Long,
        serverRecvTime: Long,
        signature: String,
        reactionMs: Long? = null
    ) {
        // 1. اسپید هک
        val isValidTime = timeAttestation.verifyMoveTimestamp(userId, sessionId, clientSendTime, serverRecvTime, signature, moveType)
        if (!isValidTime) {
            reportViolation(userId, gameId, matchId, ViolationType.SPEED_HACK, 0.9)
            return
        }
        val turnDuration = serverRecvTime - clientSendTime
        speedHackDetector.recordTurnDuration(gameId, moveType, turnDuration)
        if (speedHackDetector.isSuspiciouslyFast(gameId, moveType, turnDuration, userId)) {
            reportViolation(userId, gameId, matchId, ViolationType.SPEED_HACK, 0.7)
        }
        if (reactionMs != null) {
            behaviorDetector.recordMove(userId, gameId, reactionMs, moveType)
            if (behaviorDetector.isSuspiciousBot(userId, gameId)) {
                reportViolation(userId, gameId, matchId, ViolationType.MACRO, 0.85)
            }
        }

        // 2. تشخیص ماکرو (واکنش سریع ثابت)
        if (reactionMs != null) {
            macroDetector.recordReactionTime(userId, gameId, reactionMs)
            if (macroDetector.isSuspicious(userId, gameId)) {
                reportViolation(userId, gameId, matchId, ViolationType.MACRO, 0.8)
            }
        }
    }

    suspend fun checkLagSwitch(userId: String, matchId: String, rttMs: Long) {
        lagSwitchDetector.recordRTT(userId, rttMs)
        if (lagSwitchDetector.detectLagSwitch(userId)) {
            reportViolation(userId, "", matchId, ViolationType.LAG_SWITCH, 0.85)
        }
    }

    suspend fun checkCollusion(userA: String, userB: String, gameId: String, matchId: String) {
        if (collusionDetector.detectWinTrading(userA, userB, gameId)) {
            reportViolation(userA, gameId, matchId, ViolationType.COLLUSION, 0.9)
            reportViolation(userB, gameId, matchId, ViolationType.COLLUSION, 0.9)
        }
    }

    private suspend fun reportViolation(userId: String, gameId: String, matchId: String, type: ViolationType, confidence: Double) {
        // ذخیره در دیتابیس
        transaction {
            CheatAttemptsTable.insert {
                it[CheatAttemptsTable.userId] = userId
                it[CheatAttemptsTable.gameId] = gameId
                it[CheatAttemptsTable.matchId] = matchId
                it[CheatAttemptsTable.violationType] = type.name
                it[CheatAttemptsTable.confidenceScore] = confidence
                it[CheatAttemptsTable.detectedAt] = Instant.now()
                it[CheatAttemptsTable.penalized] = false
                it[CheatAttemptsTable.appealed] = false
            }
        }

        // اعمال جریمه در صورت اطمینان بالا
        if (confidence >= 0.7) {
            penaltyService.applyPenalty(
                CheatAttempt(
                    userId = userId,
                    gameId = gameId,
                    matchId = matchId,
                    violationType = type,
                    confidenceScore = confidence,
                    details = emptyMap()
                )
            )
        } else {
            println("⚠️ Suspicious activity (confidence $confidence) from $userId: $type")
        }
    }
}