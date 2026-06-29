// host/src/main/java/com/gamehub/host/statemanager/SocialStore.kt
package com.gamehub.host.statemanager

import com.gamehub.shared.networking.ChatMessageMsg
import com.gamehub.shared.state.Store
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class FriendInfo(
    val userId: String,
    val username: String,
    val isOnline: Boolean,
    val currentGame: String? = null
)

data class ChatMessage(
    val id: String,
    val senderId: String,
    val message: String,
    val timestamp: Long,
    val isMine: Boolean
)

class SocialStore(
    private val myUserId: String,
    private val onSendChat: (ChatMessageMsg) -> Unit
) : Store {

    private val _friends = MutableStateFlow<List<FriendInfo>>(emptyList())
    val friends: StateFlow<List<FriendInfo>> = _friends.asStateFlow()

    private val _chatMessages = MutableStateFlow<Map<String, List<ChatMessage>>>(emptyMap())
    val chatMessages: StateFlow<Map<String, List<ChatMessage>>> = _chatMessages.asStateFlow()

    override suspend fun start() {}

    override suspend fun stop() {}

    fun sendMessage(channelType: String, channelId: String, body: String) {
        val msg = ChatMessageMsg(
            channelType = channelType,
            channelId = channelId,
            senderId = myUserId,
            content = com.gamehub.shared.networking.ChatContent(
                type = "text",
                body = body,
                metadata = emptyMap()
            )
        )
        onSendChat(msg)
    }

    fun onReceiveMessage(msg: ChatMessageMsg) {
        val channelKey = "${msg.channelType}:${msg.channelId}"
        val newMsg = ChatMessage(
            id = System.currentTimeMillis().toString(),
            senderId = msg.senderId,
            message = msg.content.body,
            timestamp = System.currentTimeMillis(),
            isMine = msg.senderId == myUserId
        )
        val current = _chatMessages.value[channelKey] ?: emptyList()
        _chatMessages.value = _chatMessages.value.toMutableMap().apply {
            this[channelKey] = current + newMsg
        }
    }
}