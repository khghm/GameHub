package com.gamehub.server.repository

import com.gamehub.server.domain.GameEventLogTable
import com.gamehub.server.wal.GameEvent
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

class GameEventLogRepository {
    
    private val json = Json { ignoreUnknownKeys = true }

    private fun Instant.toOffsetDateTime(): OffsetDateTime = atOffset(ZoneOffset.UTC)
    private fun OffsetDateTime.toInstantCompat(): Instant = toInstant()

    fun insert(event: GameEvent): Long = transaction {
        GameEventLogTable.insert {
            it[eventId] = event.eventId
            it[gameSessionId] = event.gameSessionId
            it[gameType] = event.gameType
            it[eventType] = event.eventType
            it[playerId] = event.playerId
            it[eventTimestamp] = event.timestamp.toOffsetDateTime()
            it[sequenceNumber] = event.sequenceNumber
            it[payload] = event.payload.toString()
            it[isApplied] = event.isApplied
            it[appliedAt] = event.appliedAt?.toOffsetDateTime()
            it[checksum] = event.computeChecksum()
        } get GameEventLogTable.id
    }

    fun findByEventId(eventId: UUID): GameEvent? = transaction {
        GameEventLogTable
            .select { GameEventLogTable.eventId eq eventId }
            .singleOrNull()
            ?.let { rowToGameEvent(it) }
    }

    fun findByGameSession(gameSessionId: UUID, fromSequence: Long = 0): List<GameEvent> = transaction {
        GameEventLogTable
            .select { 
                (GameEventLogTable.gameSessionId eq gameSessionId) and 
                (GameEventLogTable.sequenceNumber greater fromSequence)
            }
            .orderBy(GameEventLogTable.sequenceNumber to SortOrder.ASC)
            .map { rowToGameEvent(it) }
    }

    fun findUnappliedEvents(gameSessionId: UUID): List<GameEvent> = transaction {
        GameEventLogTable
            .select { 
                (GameEventLogTable.gameSessionId eq gameSessionId) and 
                (GameEventLogTable.isApplied eq false)
            }
            .orderBy(GameEventLogTable.sequenceNumber to SortOrder.ASC)
            .map { rowToGameEvent(it) }
    }

    fun markAsApplied(eventId: UUID): Boolean = transaction {
        GameEventLogTable.update({ GameEventLogTable.eventId eq eventId }) {
            it[isApplied] = true
            it[appliedAt] = Instant.now().toOffsetDateTime()
        } > 0
    }

    fun markBatchAsApplied(eventIds: List<UUID>): Int = transaction {
        GameEventLogTable.update({ GameEventLogTable.eventId inList eventIds }) {
            it[isApplied] = true
            it[appliedAt] = Instant.now().toOffsetDateTime()
        }
    }

    fun getNextSequenceNumber(gameSessionId: UUID): Long = transaction {
        val maxSeq = GameEventLogTable
            .slice(GameEventLogTable.sequenceNumber.max())
            .select { GameEventLogTable.gameSessionId eq gameSessionId }
            .singleOrNull()
            ?.get(GameEventLogTable.sequenceNumber.max())
        (maxSeq ?: 0L) + 1
    }

    // TODO: Fix deleteOldEvents operator issue
    fun deleteOldEvents(before: Instant): Int = transaction {
        0 // Placeholder - temporarily disabled
    }

    private fun rowToGameEvent(row: ResultRow): GameEvent {
        return GameEvent(
            eventId = row[GameEventLogTable.eventId],
            gameSessionId = row[GameEventLogTable.gameSessionId],
            gameType = row[GameEventLogTable.gameType],
            eventType = row[GameEventLogTable.eventType],
            playerId = row[GameEventLogTable.playerId],
            timestamp = row[GameEventLogTable.eventTimestamp].toInstantCompat(),
            sequenceNumber = row[GameEventLogTable.sequenceNumber],
            payload = json.decodeFromString<JsonObject>(row[GameEventLogTable.payload]),
            isApplied = row[GameEventLogTable.isApplied],
            appliedAt = row[GameEventLogTable.appliedAt]?.toInstantCompat()
        )
    }
}
