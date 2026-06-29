package com.gamehub.server.repository

import com.gamehub.server.domain.UserTutorialsTable
import com.gamehub.server.persistence.DatabaseFactory.dbQuery
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.time.Instant

class UserTutorialRepository {
    suspend fun isCompleted(userId: String, gameId: String): Boolean = dbQuery {
        val count = UserTutorialsTable.selectAll()
            .where { (UserTutorialsTable.userId eq userId) and (UserTutorialsTable.gameId eq gameId) }
            .count()
        println("📚 UserTutorialRepository.isCompleted: userId=$userId, gameId=$gameId, count=$count")
        count > 0
    }

    suspend fun markCompleted(userId: String, gameId: String): Unit = dbQuery {
        println("📚 UserTutorialRepository.markCompleted: userId=$userId, gameId=$gameId")
        UserTutorialsTable.insertIgnore {
            it[this.userId] = userId
            it[this.gameId] = gameId
            it[completedAt] = Instant.now()
        }
    }
}
