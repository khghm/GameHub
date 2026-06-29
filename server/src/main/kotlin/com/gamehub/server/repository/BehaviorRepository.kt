// server/src/main/kotlin/com/gamehub/server/repository/BehaviorRepository.kt
package com.gamehub.server.repository

import com.gamehub.server.domain.BehaviorEventsTable
import com.gamehub.server.domain.UserBehaviorTable
import com.gamehub.server.persistence.DatabaseFactory.dbQuery
import com.gamehub.shared.behavior.BehaviorInfo
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.selectAll
import java.time.Instant
import java.time.LocalDate

class BehaviorRepository {

    suspend fun getOrCreate(userId: String): BehaviorInfo = dbQuery {
        val row = UserBehaviorTable.select { UserBehaviorTable.userId eq userId }.singleOrNull()
        if (row != null) {
            BehaviorInfo(
                userId = userId,
                score = row[UserBehaviorTable.behaviorScore],
                band = row[UserBehaviorTable.behaviorBand],
                lastBandChange = row[UserBehaviorTable.lastBandChange]?.toEpochMilli()
            )
        } else {
            UserBehaviorTable.insert {
                it[UserBehaviorTable.userId] = userId
                it[UserBehaviorTable.behaviorScore] = 70
                it[UserBehaviorTable.behaviorBand] = "C"
                it[UserBehaviorTable.updatedAt] = Instant.now()
            }
            BehaviorInfo(userId, 70, "C")
        }
    }

    suspend fun updateScore(userId: String, delta: Int, eventType: String, matchId: String?): BehaviorInfo {
        // دریافت اطلاعات فعلی (این تابع suspend است و در اینجا قابل فراخوانی است)
        val current = getOrCreate(userId)
        val newScore = (current.score + delta).coerceIn(0, 100)
        val newBand = scoreToBand(newScore)
        val now = Instant.now()
        val lastBandChange = if (newBand != current.band) now else current.lastBandChange?.let { Instant.ofEpochMilli(it) } ?: now

        // عملیات دیتابیسی درون dbQuery (بدون توابع suspend دیگر)
        dbQuery {
            UserBehaviorTable.update({ UserBehaviorTable.userId eq userId }) {
                it[UserBehaviorTable.behaviorScore] = newScore
                it[UserBehaviorTable.behaviorBand] = newBand
                it[UserBehaviorTable.lastBandChange] = lastBandChange
                it[UserBehaviorTable.updatedAt] = now
            }

            BehaviorEventsTable.insert {
                it[BehaviorEventsTable.userId] = userId
                it[BehaviorEventsTable.eventType] = eventType
                it[BehaviorEventsTable.deltaScore] = delta
                it[BehaviorEventsTable.matchId] = matchId
                it[BehaviorEventsTable.createdAt] = now
            }
        }

        return BehaviorInfo(userId, newScore, newBand, lastBandChange.toEpochMilli())
    }

    suspend fun getLastEventTime(userId: String, eventType: String): Long? = dbQuery {
        BehaviorEventsTable
            .select { (BehaviorEventsTable.userId eq userId) and (BehaviorEventsTable.eventType eq eventType) }
            .orderBy(BehaviorEventsTable.createdAt to SortOrder.DESC)
            .limit(1)
            .singleOrNull()
            ?.let { it[BehaviorEventsTable.createdAt].toEpochMilli() }
    }

    suspend fun getTodayBonusSum(userId: String): Int = dbQuery {
        val startOfDay = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.DAYS)
        BehaviorEventsTable
            .selectAll()
            .where {
                (BehaviorEventsTable.userId eq userId) and
                        (BehaviorEventsTable.eventType eq "game_finished_clean") and
                        (BehaviorEventsTable.createdAt greaterEq startOfDay)
            }
            .sumOf { it[BehaviorEventsTable.deltaScore] }
    }

    // توابع موقت برای Grace Period (در فاز بعدی کامل می‌شوند)
    suspend fun saveBandChangeGrace(userId: String, graceUntilMillis: Long): Unit = dbQuery {
        // فعلاً خالی – بعداً کامل می‌شود
    }

    suspend fun getBandChangeGrace(userId: String): Long? = dbQuery { null }
    suspend fun getPreviousBand(userId: String): String? = dbQuery { null }

    private fun scoreToBand(score: Int): String = when (score) {
        in 80..100 -> "A"
        in 60..79 -> "B"
        in 40..59 -> "C"
        in 20..39 -> "D"
        else -> "E"
    }
}