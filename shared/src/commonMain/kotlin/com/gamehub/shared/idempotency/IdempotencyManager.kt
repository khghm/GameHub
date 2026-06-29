// shared/src/commonMain/kotlin/com/gamehub/shared/idempotency/IdempotencyManager.kt
package com.gamehub.shared.idempotency

import com.gamehub.shared.cache.CacheProvider
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class IdempotencyManager(private val cache: CacheProvider) {
    private val json = Json { ignoreUnknownKeys = true }
    private val locks = mutableMapOf<String, Mutex>()
    private companion object {
        private const val SALT = "GameHubIdempotencySalt2025"
    }

    @Serializable
    private data class IdempotencyRecord(
        val status: String, // "PROCESSING", "COMPLETED"
        val result: String? = null
    )

    suspend fun <T> execute(
        userId: String,
        actionType: String,
        idempotencyKey: String,
        serializer: KSerializer<T>,
        ttlDays: Int = 90,
        block: suspend () -> T
    ): T {
        val keyHash = simpleHash("$userId:$actionType:$idempotencyKey:$SALT")
        val lock = locks.getOrPut(keyHash) { Mutex() }
        return lock.withLock {
            val existing = cache.get("idemp:$keyHash")
            if (existing != null) {
                val record = json.decodeFromString<IdempotencyRecord>(existing)
                if (record.status == "COMPLETED" && record.result != null) {
                    return@withLock json.decodeFromString(serializer, record.result)
                } else {
                    throw IllegalStateException("Duplicate request in flight: $keyHash")
                }
            }

            // mark as processing
            cache.set("idemp:$keyHash", json.encodeToString(IdempotencyRecord("PROCESSING")), ttlDays * 86400L)

            val result = try {
                block()
            } catch (e: Exception) {
                cache.delete("idemp:$keyHash")
                throw e
            }

            val resultJson = json.encodeToString(serializer, result)
            val record = IdempotencyRecord("COMPLETED", resultJson)
            cache.set("idemp:$keyHash", json.encodeToString(record), ttlDays * 86400L)

            return@withLock result
        }
    }

    // Simple hash for cross-platform (no java.security)
    private fun simpleHash(input: String): String {
        var hash = 0L
        for (c in input) {
            hash = ((hash shl 5) - hash) + c.code
            hash = hash and 0xFFFFFFFFL
        }
        return hash.toString(16)
    }
}