package com.gamehub.server.wal

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import java.time.Instant
import java.util.UUID

@Serializable
data class GameEvent(
    @Contextual val eventId: UUID,
    @Contextual val gameSessionId: UUID,
    val gameType: String,
    val eventType: String,
    val playerId: String?,
    @Contextual val timestamp: Instant,
    val sequenceNumber: Long,
    val payload: JsonObject,
    val isApplied: Boolean = false,
    @Contextual val appliedAt: Instant? = null
) {
    fun computeChecksum(): String {
        val data = "$eventId|$gameSessionId|$gameType|$eventType|$playerId|$timestamp|$sequenceNumber|$payload"
        return data.hashCode().toString(16).padStart(64, '0')
    }
}
