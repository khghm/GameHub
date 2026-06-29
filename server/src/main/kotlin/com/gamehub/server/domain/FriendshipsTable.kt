package com.gamehub.server.domain

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestampWithTimeZone

object FriendshipsTable : Table("friendships") {
    val id = uuid("id").autoGenerate()
    val userId = uuid("user_id")
    val friendId = uuid("friend_id")
    val status = varchar("status", 10).default("pending")
    val createdAt = timestampWithTimeZone("created_at")
    val updatedAt = timestampWithTimeZone("updated_at").nullable()
    override val primaryKey = PrimaryKey(id)

    init {
        uniqueIndex("idx_user_friend", userId, friendId)
    }
}