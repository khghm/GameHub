package com.gamehub.server.domain

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant

object CheatAttemptsTable : Table("cheat_attempts") {
    val id = long("id").autoIncrement()
    val userId = text("user_id")
    val gameId = varchar("game_id", 50)
    val matchId = text("match_id")
    val violationType = varchar("violation_type", 50)
    val confidenceScore = double("confidence_score")
    val details = text("details").nullable()
    val detectedAt = timestamp("detected_at")
    val penalized = bool("penalized")
    val appealed = bool("appealed")
    override val primaryKey = PrimaryKey(id)
}