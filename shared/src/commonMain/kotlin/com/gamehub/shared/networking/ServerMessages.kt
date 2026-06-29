package com.gamehub.shared.networking

import kotlinx.serialization.Serializable

@Serializable
sealed class ClientMessage {
    @Serializable
    data class CreateGame(val gameType: String, val playerName: String) : ClientMessage()
    @Serializable
    data class JoinGame(val sessionId: String, val playerName: String) : ClientMessage()
    @Serializable
    data class GameAction(val sessionId: String, val actionJson: String, val actionType: String) : ClientMessage()
}

@Serializable
sealed class ServerMessage {
    @Serializable
    data class GameStarted(val sessionId: String, val initialStateJson: String, val yourPlayerId: String) : ServerMessage()
    @Serializable
    data class StateUpdate(val stateJson: String) : ServerMessage()
    @Serializable
    data class GameEnded(val resultJson: String) : ServerMessage()
    @Serializable
    data class Error(val message: String) : ServerMessage()
}