package com.gamehub.server.anticheat

import com.gamehub.shared.cache.CacheProvider
import kotlin.math.abs

class TimeAttestation(private val cache: CacheProvider) {

    suspend fun generateNonce(sessionId: String): Long {
        val nonce = (System.currentTimeMillis() shl 16) + (Math.random() * 65535).toLong()
        cache.set("nonce:$sessionId", nonce.toString(), 60)
        return nonce
    }

    suspend fun verifyMoveTimestamp(
        userId: String,
        sessionId: String,
        clientSendTime: Long,
        serverRecvTime: Long,
        signature: String,
        moveType: String
    ): Boolean {
        // 1. Verify nonce
        val storedNonce = cache.get("nonce:$sessionId")?.toLongOrNull()
        if (storedNonce == null || storedNonce != clientSendTime.shr(16).shl(16)) {
            return false
        }
        cache.delete("nonce:$sessionId")

        // 2. Get time offset for user (from TimeSync)
        val offset = cache.get("time_offset:$userId")?.toLongOrNull() ?: 0L
        val estimatedServerTime = clientSendTime + (System.currentTimeMillis() - clientSendTime) / 2 + offset
        val tolerance = when (moveType) {
            "normal" -> 150L
            "bonus" -> 200L
            else -> 100L
        }
        return abs(estimatedServerTime - serverRecvTime) <= tolerance
    }

    suspend fun updateTimeOffset(userId: String, rtt: Long, serverTime: Long, clientTime: Long) {
        val offset = (serverTime - clientTime) - (rtt / 2)
        val oldOffset = cache.get("time_offset:$userId")?.toLongOrNull() ?: 0L
        val newOffset = (0.9 * oldOffset + 0.1 * offset).toLong()
        cache.set("time_offset:$userId", newOffset.toString(), 3600)
    }
}