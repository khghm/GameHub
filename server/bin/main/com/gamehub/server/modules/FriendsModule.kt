package com.gamehub.server.modules

import com.gamehub.server.cache.PresenceCache
import com.gamehub.server.domain.Friendship
import com.gamehub.server.repository.DuplicateFriendshipException
import com.gamehub.server.repository.FriendshipRepository
import com.gamehub.server.repository.UserRepository
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.*

@Serializable
data class FriendInfo(
    val userId: String,
    val username: String,
    val displayName: String? = null,
    val avatarUrl: String? = null,
    val requestId: String? = null,
    val isOnline: Boolean = false
)

class FriendsModule(private val friendshipRepo: FriendshipRepository) {
    private val userRepo = UserRepository()

    fun sendRequest(fromUserId: UUID, toUsername: String): FriendshipResult {
        val toUser = userRepo.findByUsername(toUsername)
            ?: return FriendshipResult(false, "User not found")
        if (fromUserId == toUser.id) return FriendshipResult(false, "Cannot friend yourself")

        val existing = friendshipRepo.findBetween(fromUserId, toUser.id!!)
        if (existing != null) {
            when (existing.status) {
                "accepted" -> return FriendshipResult(false, "Already friends")
                "pending" -> {
                    // اگر درخواست از طرف مقابل (toUser) باشد، Accept کن
                    if (existing.userId == toUser.id && existing.friendId == fromUserId) {
                        try {
                            friendshipRepo.acceptRequest(existing.id)
                            // حالا یک رکورد معکوس بساز (fromUserId -> toUser) به شرطی که وجود نداشته باشد
                            val reverse = friendshipRepo.findBetween(fromUserId, toUser.id!!)
                            if (reverse == null || reverse.status != "accepted") {
                                try {
                                    friendshipRepo.create(fromUserId, toUser.id!!, "accepted")
                                } catch (dup: DuplicateFriendshipException) {
                                    // اگر قبلاً وجود داشت (پارالل)، مهم نیست
                                }
                            }
                            return FriendshipResult(true, "Friend added")
                        } catch (e: Exception) {
                            return FriendshipResult(false, "Server error while accepting: ${e.message}")
                        }
                    } else {
                        return FriendshipResult(false, "Friend request already sent")
                    }
                }
                "blocked" -> return FriendshipResult(false, "You are blocked by this user")
            }
        }

        try {
            friendshipRepo.create(fromUserId, toUser.id!!, "pending")
            return FriendshipResult(true, "Friend request sent")
        } catch (dup: DuplicateFriendshipException) {
            return FriendshipResult(false, "Friendship already exists")
        } catch (e: Exception) {
            return FriendshipResult(false, "Server error: ${e.message}")
        }
    }

    fun acceptRequest(userId: UUID, requestId: UUID): FriendshipResult {
        val friendship = friendshipRepo.findById(requestId)
            ?: return FriendshipResult(false, "Request not found")
        if (friendship.friendId != userId) return FriendshipResult(false, "Not for you")
        if (friendship.status != "pending") return FriendshipResult(false, "Already processed")
        friendshipRepo.acceptRequest(requestId)
        friendshipRepo.create(userId, friendship.userId, "accepted")
        return FriendshipResult(true, "Friend accepted")
    }

    fun blockUser(userId: UUID, friendId: UUID): FriendshipResult {
        friendshipRepo.removeBetween(userId, friendId)
        friendshipRepo.create(userId, friendId, "blocked")
        return FriendshipResult(true, "User blocked")
    }

    fun unfriend(userId: UUID, friendId: UUID): FriendshipResult {
        friendshipRepo.removeBetween(userId, friendId)
        return FriendshipResult(true, "Friend removed")
    }

    suspend fun getFriends(userId: UUID): List<FriendInfo> {
        val friendships = friendshipRepo.getAcceptedFriends(userId)
        val friends = mutableListOf<FriendInfo>()
        for (f in friendships) {
            val friendId = if (f.userId == userId) f.friendId else f.userId
            val user = userRepo.findById(friendId)
            if (user != null) {
                friends.add(
                    FriendInfo(
                        userId = user.id.toString(),
                        username = user.username,
                        displayName = user.displayName,
                        avatarUrl = user.avatarUrl,
                        isOnline = PresenceCache.isUserOnline(friendId.toString())
                    )
                )
            }
        }
        return friends
    }
    suspend fun getPendingRequests(userId: UUID): List<FriendInfo> {
        val requests = friendshipRepo.getPendingRequestsFor(userId)
        return requests.map { f ->
            val requester = userRepo.findById(f.userId)
            FriendInfo(
                userId = requester?.id.toString(),
                username = requester?.username ?: "",
                displayName = requester?.displayName,
                avatarUrl = requester?.avatarUrl,
                requestId = f.id.toString(),
                isOnline = PresenceCache.isUserOnline(f.userId.toString())
            )
        }
    }

    fun rejectRequest(userId: UUID, requestId: UUID): FriendshipResult {
        val friendship = friendshipRepo.findById(requestId)
            ?: return FriendshipResult(false, "Request not found")
        if (friendship.friendId != userId) return FriendshipResult(false, "Not for you")
        if (friendship.status != "pending") return FriendshipResult(false, "Already processed")
        friendshipRepo.deleteRequest(requestId)
        return FriendshipResult(true, "Request rejected")
    }

}

@Serializable
data class FriendshipResult(
    val success: Boolean,
    val message: String
)