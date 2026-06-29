// host/src/main/java/com/gamehub/host/time/TimeSyncClient.kt
package com.gamehub.host.time

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.random.Random

class TimeSyncClient {
    private val mutex = Mutex()
    private var offsetMs: Long = 0L
    private var rttMs: Long = 0L
    private val history = mutableListOf<Long>()
    private val _isSynchronized = MutableStateFlow(false)
    val isSynchronized: StateFlow<Boolean> = _isSynchronized.asStateFlow()

    suspend fun calculateOffset(
        clientSendTime: Long,
        serverRecvTime: Long,
        serverSendTime: Long,
        clientRecvTime: Long
    ): Long {
        val rtt = (serverRecvTime - clientSendTime) + (clientRecvTime - serverSendTime)
        val offsetCandidate = (serverRecvTime - clientSendTime) - (clientRecvTime - serverSendTime) / 2

        // Filter outliers using median absolute deviation
        history.add(offsetCandidate)
        if (history.size > 10) history.removeAt(0)

        val median = history.sorted().let { it[it.size / 2] }
        val mad = history.map { kotlin.math.abs(it - median) }.sorted().let { it[it.size / 2] }

        val newOffset = if (kotlin.math.abs(offsetCandidate - median) <= 3 * mad) {
            // exponential moving average
            if (offsetMs == 0L) offsetCandidate else (0.9 * offsetMs + 0.1 * offsetCandidate).toLong()
        } else {
            offsetMs
        }

        mutex.withLock {
            offsetMs = newOffset
            rttMs = rtt / 2
            _isSynchronized.value = true
        }
        return newOffset
    }

    suspend fun getEstimatedServerTime(): Long {
        return mutex.withLock {
            System.currentTimeMillis() + offsetMs
        }
    }

    fun getRttMs(): Long = rttMs
}