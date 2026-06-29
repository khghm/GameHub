package com.gamehub.server.modules

import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap

@Serializable
data class ChatMessage(
    val type: String = "chat",
    val from: String,
    val to: String = "all",          // "all", "private:userId", "clan:clanId", "society:societyId"
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val gameId: String? = null
)

@Serializable
data class UserList(
    val type: String = "userList",
    val users: List<String>
)

// ذخیره اشتراک‌های کاربران در کانال‌های گروهی
data class ChannelSubscription(
    val userId: String,
    val session: WebSocketSession
)

object ChatServer {
    private val onlineUsers = ConcurrentHashMap<String, WebSocketSession>()
    private val groupSubscribers = ConcurrentHashMap<String, MutableSet<String>>() // channel -> set of userIds
    private val messageHistory = mutableListOf<ChatMessage>()
    private val broadcastChannel = Channel<ChatMessage>(Channel.UNLIMITED)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    init {
        startBroadcastLoop()
    }

    private fun startBroadcastLoop() {
        scope.launch {
            for (message in broadcastChannel) {
                val json = Json.encodeToString(message)
                when {
                    message.to == "all" -> {
                        messageHistory.add(message)
                        if (messageHistory.size > 100) {
                            messageHistory.removeAt(0)
                        }
                        // Broadcast to everyone
                        onlineUsers.values.forEach { session ->
                            launch { safeSend(session, json) }
                        }
                    }
                    message.to.startsWith("private:") -> {
                        val targetUserId = message.to.substringAfter("private:")
                        val targetSession = onlineUsers[targetUserId]
                        if (targetSession != null) {
                            launch { safeSend(targetSession, json) }
                        }
                        // Also send to sender
                        val senderSession = onlineUsers[message.from]
                        if (senderSession != null && targetUserId != message.from) {
                            launch { safeSend(senderSession, json) }
                        }
                    }
                    message.to.startsWith("clan:") || message.to.startsWith("society:") -> {
                        // گروهی: فقط به اعضای کانال ارسال شود
                        val channel = message.to
                        val members = groupSubscribers[channel] ?: emptySet()
                        members.forEach { userId ->
                            onlineUsers[userId]?.let { session ->
                                launch { safeSend(session, json) }
                            }
                        }
                        // ذخیره تاریخچه پیام‌های گروهی (اختیاری)
                    }
                }
            }
        }
    }

    private suspend fun safeSend(session: WebSocketSession, message: String) {
        try {
            session.send(Frame.Text(message))
        } catch (e: Exception) {
            // ignore
        }
    }

    // ثبت کاربر در کانال گروهی (کلن یا انجمن)
    fun subscribeToChannel(userId: String, channel: String) {
        groupSubscribers.getOrPut(channel) { mutableSetOf() }.add(userId)
        println("User $userId subscribed to channel $channel")
    }

    fun unsubscribeFromChannel(userId: String, channel: String) {
        groupSubscribers[channel]?.remove(userId)
        if (groupSubscribers[channel]?.isEmpty() == true) {
            groupSubscribers.remove(channel)
        }
    }

    fun registerUser(playerName: String, session: WebSocketSession) {
        println("Registering user: $playerName")
        onlineUsers[playerName] = session

        // Send welcome message
        val welcomeMsg = ChatMessage(
            type = "system",
            from = "Server",
            message = "Welcome to chat, $playerName!",
            to = "private:$playerName"
        )
        scope.launch { safeSend(session, Json.encodeToString(welcomeMsg)) }

        // Notify everyone
        val joinMsg = ChatMessage(
            type = "system",
            from = "Server",
            message = "$playerName joined the chat",
            to = "all"
        )
        broadcastChannel.trySend(joinMsg)

        broadcastUserList()
        println("Users online: ${onlineUsers.keys().toList()}")
    }

    fun unregisterUser(playerName: String) {
        println("Unregistering user: $playerName")
        onlineUsers.remove(playerName)

        // حذف از تمام گروه‌ها
        groupSubscribers.entries.forEach { (channel, members) ->
            members.remove(playerName)
        }

        val leaveMsg = ChatMessage(
            type = "system",
            from = "Server",
            message = "$playerName left the chat",
            to = "all"
        )
        broadcastChannel.trySend(leaveMsg)
        broadcastUserList()
    }

    fun sendMessage(message: ChatMessage) {
        broadcastChannel.trySend(message)
    }

    private fun broadcastUserList() {
        val users = onlineUsers.keys().toList()
        val userList = UserList(users = users)
        val json = Json.encodeToString(userList)
        onlineUsers.values.forEach { session ->
            scope.launch { safeSend(session, json) }
        }
    }

    // دریافت لیست کاربران آنلاین یک کانال گروهی
    fun getOnlineUsersInChannel(channel: String): List<String> {
        return groupSubscribers[channel]?.filter { onlineUsers.containsKey(it) } ?: emptyList()
    }
}