package com.gamehub.server.domain

import java.time.OffsetDateTime
import java.util.UUID

data class Friendship(
    val id: UUID,
    val userId: UUID,
    val friendId: UUID,
    val status: String,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime?
)

fun Friendship.toFriendInfo():  com.gamehub.server.modules.FriendInfo? = null // placeholder, we'll add conversion later