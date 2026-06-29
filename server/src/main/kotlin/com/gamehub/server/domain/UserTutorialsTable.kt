package com.gamehub.server.domain

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

object UserTutorialsTable : Table("user_tutorials") {
    val userId = varchar("user_id", 255)
    val gameId = varchar("game_id", 50)
    val completedAt = timestamp("completed_at").defaultExpression(org.jetbrains.exposed.sql.javatime.CurrentTimestamp)
    override val primaryKey = PrimaryKey(userId, gameId)
}
