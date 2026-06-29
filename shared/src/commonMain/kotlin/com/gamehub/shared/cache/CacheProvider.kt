// shared/src/commonMain/kotlin/com/gamehub/shared/cache/CacheProvider.kt
package com.gamehub.shared.cache

interface CacheProvider {
    // ---------- Key-Value ----------
    suspend fun set(key: String, value: String, ttlSeconds: Long = 300)
    suspend fun get(key: String): String?
    suspend fun delete(key: String)
    suspend fun exists(key: String): Boolean
    suspend fun expire(key: String, ttlSeconds: Long)

    // ---------- Atomic ----------
    suspend fun setnx(key: String, value: String, ttlSeconds: Long): Boolean
    suspend fun incr(key: String): Long
    suspend fun decr(key: String): Long

    // ---------- Hash ----------
    suspend fun hset(key: String, field: String, value: String)
    suspend fun hget(key: String, field: String): String?
    suspend fun hgetAll(key: String): Map<String, String>
    suspend fun hdel(key: String, vararg fields: String)

    // ---------- Set ----------
    suspend fun sadd(key: String, vararg members: String)
    suspend fun smembers(key: String): Set<String>
    suspend fun srem(key: String, vararg members: String)

    // ---------- Sorted Set ----------
    suspend fun zadd(key: String, score: Double, member: String, ttlSeconds: Long = 0)
    suspend fun zrangebyscore(key: String, minScore: Double, maxScore: Double, limit: Int = 100): List<String>
    suspend fun zremrangebyscore(key: String, minScore: Double, maxScore: Double): Long
    suspend fun zrem(key: String, vararg members: String)
    suspend fun zcard(key: String): Long
    suspend fun zremrangebyrank(key: String, start: Long, stop: Long): Long
    suspend fun zcleanup(key: String): Long // پاک کردن ورودی‌های منقضی شده


    // ---------- List ----------
    suspend fun lpush(key: String, vararg values: String): Long
    suspend fun ltrim(key: String, start: Long, stop: Long)

    // ---------- Pub/Sub ----------
    suspend fun publish(channel: String, message: String)
    suspend fun subscribe(channel: String, handler: (String) -> Unit)

    suspend fun lrange(key: String, start: Long, stop: Long): List<String>
    suspend fun scard(key: String): Long
    suspend fun sismember(key: String, member: String): Boolean
    suspend fun incrBy(key: String, delta: Long): Long

}