// server/src/main/kotlin/com/gamehub/server/domain/RankChangeLogTable.kt
package com.gamehub.server.domain

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

object RankChangeLogTable : Table("rank_change_log") {
    val id = long("id").autoIncrement()
    val userId = text("user_id")
    val gameId = varchar("game_id", 50)
    val matchId = text("match_id")
    val oldRating = integer("old_rating")
    val newRating = integer("new_rating")
    val changeAmount = integer("change_amount")
    val reason = varchar("reason", 50)
    val operatorId = text("operator_id").nullable()
    val adminNote = text("admin_note").nullable()
    val createdAt = timestamp("created_at")
    override val primaryKey = PrimaryKey(id)
}