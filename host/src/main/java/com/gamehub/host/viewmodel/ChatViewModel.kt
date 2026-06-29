package com.gamehub.host.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gamehub.host.network.ChatClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ChatDisplayMessage(
    val from: String,
    val message: String,
    val isMine: Boolean,
    val isSystem: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

class ChatViewModel : ViewModel() {
    private val chatClient = ChatClient()

    private val _messages = MutableStateFlow<List<ChatDisplayMessage>>(emptyList())
    val messages: StateFlow<List<ChatDisplayMessage>> = _messages

    private val _onlineUsers = MutableStateFlow<List<String>>(emptyList())
    val onlineUsers: StateFlow<List<String>> = _onlineUsers

    private var localPlayerName = ""

    fun connect(serverUrl: String, playerName: String) {
        localPlayerName = playerName
        println("ChatViewModel: Connecting as $playerName")
        viewModelScope.launch {
            chatClient.connect(serverUrl, playerName)

            launch {
                chatClient.messages.collect { msg ->
                    val displayMsg = ChatDisplayMessage(
                        from = msg.from,
                        message = msg.message,
                        isMine = msg.from == localPlayerName,
                        isSystem = msg.type == "system",
                        timestamp = msg.timestamp
                    )
                    _messages.value = _messages.value + displayMsg
                    println("ChatViewModel: Received message from ${msg.from}: ${msg.message}")
                }
            }

            launch {
                chatClient.onlineUsers.collect { users ->
                    _onlineUsers.value = users
                    println("ChatViewModel: Online users updated: $users")
                }
            }
        }
    }

    fun sendMessage(message: String) {
        viewModelScope.launch {
            println("ChatViewModel: Sending message: $message")
            chatClient.sendMessage(message, "all")
        }
    }

    override fun onCleared() {
        viewModelScope.launch {
            chatClient.disconnect()
        }
        super.onCleared()
    }
}