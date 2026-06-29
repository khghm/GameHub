// server/src/main/kotlin/com/gamehub/server/domain/UserBehaviorTable.kt
package com.gamehub.server.domain

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.javatime.timestamp

object UserBehaviorTable : Table("user_behavior") {
    val userId = text("user_id")
    val behaviorScore = integer("behavior_score").default(70)
    val behaviorBand = varchar("behavior_band", 1).default("C")
    val lastBandChange = timestamp("last_band_change").nullable()
    val cleanDaysCount = integer("clean_days_count").default(0)
    val lastActivityDate = date("last_activity_date").nullable()
    val totalPositiveEvents = integer("total_positive_events").default(0)
    val totalNegativeEvents = integer("total_negative_events").default(0)
    val updatedAt = timestamp("updated_at")
    override val primaryKey = PrimaryKey(userId)
}