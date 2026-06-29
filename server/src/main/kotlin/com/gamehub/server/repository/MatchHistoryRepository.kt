// server/src/main/kotlin/com/gamehub/server/repository/MatchHistoryRepository.kt
package com.gamehub.server.repository

import com.gamehub.server.persistence.DatabaseFactory.dbQuery
import com.gamehub.shared.core.GameResult
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.selectAll
import java.time.Instant
import java.time.OffsetDateTime

object MatchHistoryTable : Table("match_history") {
    val id = varchar("id", 50)
    val gameType = varchar("game_type", 50)
    val players = text("players")
    val winner = text("winner").nullable()
    val draw = bool("draw").default(false)
    val gameSessionId = text("game_session_id").nullable()
    val createdAt = timestamp("created_at").default(Instant.now())
    override val primaryKey = PrimaryKey(id)
}

class MatchHistoryRepository {
    data class MatchRecord(val winner: String?)

    suspend fun getMatchesBetween(userA: String, userB: String, gameId: String, days: Int): List<MatchRecord> = dbQuery {
        val cutoff = Instant.now().minusSeconds(days * 86400L)
        MatchHistoryTable.selectAll().where {
            (MatchHistoryTable.gameType eq gameId) and
                    (MatchHistoryTable.createdAt  greaterEq cutoff) and
                    (MatchHistoryTable.players like "%$userA%") and
                    (MatchHistoryTable.players like "%$userB%")
        }.map {
            MatchRecord(it[MatchHistoryTable.winner])
        }
    }
    suspend fun countMatchesAfter(after: Instant): Int = dbQuery {
        MatchHistoryTable.select { MatchHistoryTable.createdAt  greaterEq after }.count().toInt()
    }

    suspend fun saveMatch(matchId: String, gameType: String, players: List<String>, result: GameResult, gameSessionId: String? = null) = dbQuery {
        // بررسی اینکه آیا قبلاً ذخیره شده است
        val exists = MatchHistoryTable.select { MatchHistoryTable.id eq matchId }.any()
        if (exists) {
            println("⚠️ Match $matchId already saved, skipping duplicate insert")
            return@dbQuery
        }

        val winner = when (result) {
            is GameResult.Win -> result.winner.value
            else -> null
        }
        val draw = result is GameResult.Draw
        MatchHistoryTable.insert {
            it[MatchHistoryTable.id] = matchId
            it[MatchHistoryTable.gameType] = gameType
            it[MatchHistoryTable.players] = players.joinToString(",")
            it[MatchHistoryTable.winner] = winner
            it[MatchHistoryTable.draw] = draw
            it[MatchHistoryTable.gameSessionId] = gameSessionId
            it[MatchHistoryTable.createdAt] = Instant.now()
        }
    }

}