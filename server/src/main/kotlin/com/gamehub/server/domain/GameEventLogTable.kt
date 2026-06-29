package com.gamehub.server.domain

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestampWithTimeZone

object GameEventLogTable : Table("game_event_log") {
    val id = long("id").autoIncrement()
    val eventId = uuid("event_id").uniqueIndex()
    val gameSessionId = uuid("game_session_id").index()
    val gameType = varchar("game_type", 50)
    val eventType = varchar("event_type", 100)
    val playerId = varchar("player_id", 255).nullable().index()
    val eventTimestamp = timestampWithTimeZone("event_timestamp")
    val sequenceNumber = long("sequence_number")
    val payload = text("payload")
    val isApplied = bool("is_applied").default(false)
    val appliedAt = timestampWithTimeZone("applied_at").nullable()
    val checksum = varchar("checksum", 64)
    
    override val primaryKey = PrimaryKey(id)
}
