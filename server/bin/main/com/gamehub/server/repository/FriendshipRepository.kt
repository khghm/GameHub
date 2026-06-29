package com.gamehub.server.repository

import com.gamehub.server.domain.Friendship
import com.gamehub.server.domain.FriendshipsTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.OffsetDateTime
import java.util.*
class DuplicateFriendshipException(message: String) : RuntimeException(message)
class FriendshipRepository {
    fun findById(id: UUID): Friendship? = transaction {
        FriendshipsTable.selectAll().where { FriendshipsTable.id eq id }.singleOrNull()?.toFriendship()
    }

    fun findBetween(userA: UUID, userB: UUID): Friendship? = transaction {
        FriendshipsTable.selectAll()
            .where {
                ((FriendshipsTable.userId eq userA) and (FriendshipsTable.friendId eq userB)) or
                        ((FriendshipsTable.userId eq userB) and (FriendshipsTable.friendId eq userA))
            }.singleOrNull()?.toFriendship()
    }

    fun create(from: UUID, to: UUID, status: String): Friendship = transaction {
        try {
            FriendshipsTable.insert {
                it[userId] = from
                it[friendId] = to
                it[FriendshipsTable.status] = status
                it[createdAt] = OffsetDateTime.now()
            }.resultedValues!!.single().toFriendship()
        } catch (e: Exception) {
            // اگر رکورد تکراری بود، خطای مشخص بده
            if (e.message?.contains("Unique index or primary key violation") == true ||
                e.message?.contains("unique constraint") == true) {
                throw DuplicateFriendshipException("Friendship already exists between $from and $to")
            }
            throw e
        }
    }

    fun acceptRequest(requestId: UUID) = transaction {
        FriendshipsTable.update({ FriendshipsTable.id eq requestId }) {
            it[status] = "accepted"
            it[updatedAt] = OffsetDateTime.now()
        }
    }

    fun removeBetween(userA: UUID, userB: UUID) = transaction {
        FriendshipsTable.deleteWhere {
            ((FriendshipsTable.userId eq userA) and (FriendshipsTable.friendId eq userB)) or
                    ((FriendshipsTable.userId eq userB) and (FriendshipsTable.friendId eq userA))
        }
    }

    fun getAcceptedFriends(userId: UUID): List<Friendship> = transaction {
        FriendshipsTable.selectAll()
            .where { (FriendshipsTable.userId eq userId) or (FriendshipsTable.friendId eq userId) }
            .andWhere { FriendshipsTable.status eq "accepted" }
            .mapNotNull { it.toFriendship() }
    }
    fun getPendingRequestsFor(userId: UUID): List<Friendship> = transaction {
        FriendshipsTable.selectAll()
            .where { (FriendshipsTable.friendId eq userId) and (FriendshipsTable.status eq "pending") }
            .map { it.toFriendship() }
    }

    fun deleteRequest(requestId: UUID) = transaction {
        FriendshipsTable.deleteWhere { FriendshipsTable.id eq requestId }
    }

    private fun ResultRow.toFriendship() = Friendship(
        id = this[FriendshipsTable.id],
        userId = this[FriendshipsTable.userId],
        friendId = this[FriendshipsTable.friendId],
        status = this[FriendshipsTable.status],
        createdAt = this[FriendshipsTable.createdAt],
        updatedAt = this[FriendshipsTable.updatedAt]
    )
}