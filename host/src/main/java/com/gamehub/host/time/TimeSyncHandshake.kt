// host/src/main/java/com/gamehub/host/time/TimeSyncHandshake.kt
package com.gamehub.host.time

import com.gamehub.shared.networking.TimeSyncRequest
import com.gamehub.shared.networking.TimeSyncResponse
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlin.random.Random

class TimeSyncHandshake(
    private val timeSyncClient: TimeSyncClient,
    private val onSendRequest: (TimeSyncRequest) -> Unit
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val responseChannel = Channel<TimeSyncResponse>(10)
    private var pendingNonce: Long = 0L
    private var pendingSendTime: Long = 0L

    init {
        scope.launch { processResponses() }
    }

    suspend fun startHandshake() {
        val nonce = Random.nextLong()
        pendingNonce = nonce
        pendingSendTime = System.currentTimeMillis()
        onSendRequest(TimeSyncRequest(
            clientSendTime = pendingSendTime,
            nonce = nonce
        ))
    }

    suspend fun onResponse(response: TimeSyncResponse) {
        if (response.nonce == pendingNonce) {
            responseChannel.send(response)
        }
    }

    private suspend fun processResponses() {
        for (resp in responseChannel) {
            val clientRecvTime = System.currentTimeMillis()
            timeSyncClient.calculateOffset(
                clientSendTime = pendingSendTime,
                serverRecvTime = resp.serverRecvTime,
                serverSendTime = resp.serverSendTime,
                clientRecvTime = clientRecvTime
            )
        }
    }
}