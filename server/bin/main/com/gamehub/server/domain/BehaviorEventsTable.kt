// server/src/main/kotlin/com/gamehub/server/domain/BehaviorEventsTable.kt
package com.gamehub.server.domain

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

object BehaviorEventsTable : Table("behavior_events") {
    val id = long("id").autoIncrement()
    val userId = text("user_id")
    val eventType = varchar("event_type", 50)
    val deltaScore = integer("delta_score")
    val matchId = text("match_id").nullable()
    val createdAt = timestamp("created_at")
    override val primaryKey = PrimaryKey(id)
}