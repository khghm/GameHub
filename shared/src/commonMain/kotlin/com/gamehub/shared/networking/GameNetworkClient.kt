package com.gamehub.shared.networking

interface GameNetworkClient {
    suspend fun connect(url: String, gameId: String, playerId: String)
    suspend fun sendAction(action: Any)
    fun observeState(): kotlinx.coroutines.flow.Flow<Any>
    suspend fun disconnect()
}