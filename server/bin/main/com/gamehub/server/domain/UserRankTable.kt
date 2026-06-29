// server/src/main/kotlin/com/gamehub/server/domain/UserRankTable.kt
package com.gamehub.server.domain

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

object UserRankTable : Table("user_ranks") {
    val userId = text("user_id")
    val gameId = varchar("game_id", 50)
    val rating = integer("rating").default(1200)
    val ratingMean = double("rating_mean").default(1200.0)
    val ratingStdDev = double("rating_std_dev").default(350.0)
    val ratingVolatility = double("rating_volatility").default(0.06)
    val gamesPlayed = integer("games_played").default(0)
    val wins = integer("wins").default(0)
    val losses = integer("losses").default(0)
    val draws = integer("draws").default(0)
    val tier = varchar("tier", 20).default("Bronze")
    val division = integer("division").default(4)
    val seasonId = integer("season_id").default(1)
    val updatedAt = timestamp("updated_at")
    override val primaryKey = PrimaryKey(userId, gameId)
}