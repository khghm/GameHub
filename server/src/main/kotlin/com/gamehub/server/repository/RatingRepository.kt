// server/src/main/kotlin/com/gamehub/server/repository/RatingRepository.kt
package com.gamehub.server.repository

import com.gamehub.server.domain.RankChangeLogTable
import com.gamehub.server.domain.UserRankTable
import com.gamehub.server.persistence.DatabaseFactory.dbQuery
import com.gamehub.shared.matchmaking.SkillRating
import com.gamehub.shared.rating.RatingInfo
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.selectAll
import java.time.Instant

data class RatingChange(
    val userId: String,
    val gameId: String,
    val matchId: String,
    val oldRating: Int,
    val newRating: Int,
    val change: Int,
    val reason: String
)

class RatingRepository {

    suspend fun getOrCreate(userId: String, gameId: String): RatingInfo = dbQuery {
        val row = UserRankTable.selectAll()
            .where { (UserRankTable.userId eq userId) and (UserRankTable.gameId eq gameId) }
            .singleOrNull()

        if (row != null) {
            RatingInfo(
                rating = row[UserRankTable.rating],
                skillRating = SkillRating(
                    mean = row[UserRankTable.ratingMean],
                    standardDeviation = row[UserRankTable.ratingStdDev],
                    volatility = row[UserRankTable.ratingVolatility]
                ),
                gamesPlayed = row[UserRankTable.gamesPlayed],
                wins = row[UserRankTable.wins],
                losses = row[UserRankTable.losses],
                draws = row[UserRankTable.draws],
                tier = row[UserRankTable.tier],
                division = row[UserRankTable.division]
            )
        } else {
            UserRankTable.insert {
                it[UserRankTable.userId] = userId
                it[UserRankTable.gameId] = gameId
                it[UserRankTable.rating] = 1200
                it[UserRankTable.ratingMean] = 1200.0
                it[UserRankTable.ratingStdDev] = 350.0
                it[UserRankTable.ratingVolatility] = 0.06
                it[UserRankTable.gamesPlayed] = 0
                it[UserRankTable.wins] = 0
                it[UserRankTable.losses] = 0
                it[UserRankTable.draws] = 0
                it[UserRankTable.tier] = "Bronze"
                it[UserRankTable.division] = 4
                it[UserRankTable.seasonId] = 1
                it[UserRankTable.updatedAt] = Instant.now()
            }
            RatingInfo(
                rating = 1200,
                skillRating = SkillRating(1200.0, 350.0),
                gamesPlayed = 0,
                wins = 0,
                losses = 0,
                draws = 0,
                tier = "Bronze",
                division = 4
            )
        }
    }

    suspend fun updateRating(
        userId: String,
        gameId: String,
        newRating: Int,
        newSkillRating: SkillRating,
        wins: Int,
        losses: Int,
        draws: Int,
        gamesPlayed: Int
    ): Unit = dbQuery {
        val (tier, division) = getTierAndDivision(newRating)
        UserRankTable.update({ (UserRankTable.userId eq userId) and (UserRankTable.gameId eq gameId) }) {
            it[UserRankTable.rating] = newRating
            it[UserRankTable.ratingMean] = newSkillRating.mean
            it[UserRankTable.ratingStdDev] = newSkillRating.standardDeviation
            it[UserRankTable.ratingVolatility] = newSkillRating.volatility
            it[UserRankTable.wins] = wins
            it[UserRankTable.losses] = losses
            it[UserRankTable.draws] = draws
            it[UserRankTable.gamesPlayed] = gamesPlayed
            it[UserRankTable.tier] = tier
            it[UserRankTable.division] = division
            it[UserRankTable.updatedAt] = Instant.now()
        }
    }

    suspend fun logChange(change: RatingChange): Unit = dbQuery {
        RankChangeLogTable.insert {
            it[RankChangeLogTable.userId] = change.userId
            it[RankChangeLogTable.gameId] = change.gameId
            it[RankChangeLogTable.matchId] = change.matchId
            it[RankChangeLogTable.oldRating] = change.oldRating
            it[RankChangeLogTable.newRating] = change.newRating
            it[RankChangeLogTable.changeAmount] = change.change
            it[RankChangeLogTable.reason] = change.reason
            it[RankChangeLogTable.createdAt] = Instant.now()
        }
    }

    private fun getTierAndDivision(rating: Int): Pair<String, Int> {
        return when {
            rating < 1500 -> "Bronze" to (4 - ((rating - 0) / 375)).coerceIn(1, 4)
            rating < 3000 -> "Silver" to (4 - ((rating - 1500) / 375)).coerceIn(1, 4)
            rating < 4500 -> "Gold" to (4 - ((rating - 3000) / 375)).coerceIn(1, 4)
            rating < 6000 -> "Platinum" to (4 - ((rating - 4500) / 375)).coerceIn(1, 4)
            rating < 7500 -> "Diamond" to (4 - ((rating - 6000) / 375)).coerceIn(1, 4)
            rating < 9000 -> "Master" to (4 - ((rating - 7500) / 375)).coerceIn(1, 4)
            else -> "Grandmaster" to (4 - ((rating - 9000) / 375)).coerceIn(1, 4)
        }
    }
}