package com.gamehub.server.domain

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestampWithTimeZone

object ReportsTable : Table("reports") {
    val id = long("id").autoIncrement()
    val reporterId = varchar("reporter_id", 255)
    val reportedUserId = varchar("reported_user_id", 255)
    val type = varchar("type", 20).default("user")
    val reason = text("reason")
    val details = text("details").nullable()
    val evidenceUrl = text("evidence_url").nullable()
    val reporterScoreSnapshot = integer("reporter_score_snapshot")
    val status = varchar("status", 20).default("pending")
    val violationType = varchar("violation_type", 30).default("other")
    val decision = varchar("decision", 20).nullable()
    val moderatorId = varchar("moderator_id", 255).nullable()
    val createdAt = timestampWithTimeZone("created_at")
    val reviewedAt = timestampWithTimeZone("reviewed_at").nullable()
    override val primaryKey = PrimaryKey(id)
}