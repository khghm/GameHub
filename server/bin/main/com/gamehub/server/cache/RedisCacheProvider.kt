// server/src/main/kotlin/com/gamehub/server/cache/RedisCacheProvider.kt
package com.gamehub.server.cache

import com.gamehub.shared.cache.CacheProvider
import io.lettuce.core.*
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.sync.RedisCommands
import io.lettuce.core.pubsub.RedisPubSubAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RedisCacheProvider(private val redisUri: String = "redis://localhost:6379") : CacheProvider {

    private var client: RedisClient? = null
    private var connection: StatefulRedisConnection<String, String>? = null
    private lateinit var sync: RedisCommands<String, String>

    init {
        reconnect()
    }

    private fun reconnect() {
        try {
            client = RedisClient.create(redisUri)
            connection = client?.connect()
            sync = connection!!.sync()
        } catch (e: Exception) {
            throw RuntimeException("Failed to connect to Redis at $redisUri", e)
        }
    }

    private suspend fun <T> executeWithRetry(block: (RedisCommands<String, String>) -> T): T {
        return withContext(Dispatchers.IO) {
            try {
                if (!sync.isOpen) reconnect()
                block(sync)
            } catch (e: Exception) {
                reconnect()
                block(sync)
            }
        }
    }

    override suspend fun set(key: String, value: String, ttlSeconds: Long) = executeWithRetry {
        if (ttlSeconds > 0) it.setex(key, ttlSeconds, value) else it.set(key, value)
        Unit
    }

    override suspend fun get(key: String): String? = executeWithRetry { it.get(key) }

    override suspend fun delete(key: String) = executeWithRetry {
        it.del(key)
        Unit
    }

    override suspend fun exists(key: String): Boolean = executeWithRetry { it.exists(key) > 0 }

    override suspend fun expire(key: String, ttlSeconds: Long) = executeWithRetry {
        it.expire(key, ttlSeconds)
        Unit
    }

    override suspend fun setnx(key: String, value: String, ttlSeconds: Long): Boolean = executeWithRetry {
        val result = it.setnx(key, value)
        if (result && ttlSeconds > 0) {
            it.expire(key, ttlSeconds)
        }
        result
    }

    override suspend fun incr(key: String): Long = executeWithRetry { it.incr(key) }

    override suspend fun decr(key: String): Long = executeWithRetry { it.decr(key) }

    override suspend fun hset(key: String, field: String, value: String) = executeWithRetry {
        it.hset(key, field, value)
        Unit
    }

    override suspend fun hget(key: String, field: String): String? = executeWithRetry { it.hget(key, field) }

    override suspend fun hgetAll(key: String): Map<String, String> = executeWithRetry { it.hgetall(key) }

    override suspend fun hdel(key: String, vararg fields: String) = executeWithRetry {
        it.hdel(key, *fields)
        Unit
    }

    override suspend fun sadd(key: String, vararg members: String) = executeWithRetry {
        it.sadd(key, *members)
        Unit
    }

    override suspend fun smembers(key: String): Set<String> = executeWithRetry { it.smembers(key) }

    override suspend fun srem(key: String, vararg members: String) = executeWithRetry {
        it.srem(key, *members)
        Unit
    }

    override suspend fun zadd(key: String, score: Double, member: String, ttlSeconds: Long) = executeWithRetry {
        it.zadd(key, score, member)
        if (ttlSeconds > 0) {
            // برای تنظیم TTL روی تک تک اعضای zset، می‌توان از PEXPIRE استفاده کرد
            // اما Lettuce به طور مستقیم این کار را ساده نمی‌کند، لذا از zadd و expire کل key استفاده می‌کنیم
            // یا از یک hash برای ردیابی TTL استفاده می‌کنیم، اما برای سادگی در اینجا
            // expire کل key را تنظیم می‌کنیم، در نسخه‌های بعدی بهبود می‌یابد
            val currentTtl = it.ttl(key)
            if (currentTtl == -1L || currentTtl < ttlSeconds) {
                it.expire(key, ttlSeconds)
            }
        }
        Unit
    }

    override suspend fun zrangebyscore(key: String, minScore: Double, maxScore: Double, limit: Int): List<String> = executeWithRetry {
        it.zrangebyscore(key, Range.create(minScore, maxScore), Limit.create(0, limit.toLong()))
    }

    override suspend fun zremrangebyscore(key: String, minScore: Double, maxScore: Double): Long = executeWithRetry {
        it.zremrangebyscore(key, Range.create(minScore, maxScore))
    }

    override suspend fun zrem(key: String, vararg members: String) = executeWithRetry {
        it.zrem(key, *members)
        Unit
    }
    override suspend fun zcard(key: String): Long = executeWithRetry {
        it.zcard(key)
    }

    override suspend fun zcleanup(key: String): Long = executeWithRetry {
        // در Redis، پاک کردن ورودی‌های منقضی شده به طور خودکار انجام می‌شود
        // اما برای یکپارچگی با MemoryCacheProvider، تابع را پیاده‌سازی می‌کنیم
        0L
    }

    override suspend fun lpush(key: String, vararg values: String): Long = executeWithRetry {
        it.lpush(key, *values)
    }

    override suspend fun ltrim(key: String, start: Long, stop: Long) = executeWithRetry {
        it.ltrim(key, start, stop)
        Unit
    }

    override suspend fun publish(channel: String, message: String) = executeWithRetry {
        it.publish(channel, message)
        Unit
    }

    override suspend fun subscribe(channel: String, handler: (String) -> Unit) {
        withContext(Dispatchers.IO) {
            Thread {
                try {
                    val pubSubClient = RedisClient.create(redisUri)
                    val pubSubConnection = pubSubClient.connectPubSub()
                    pubSubConnection.addListener(object : RedisPubSubAdapter<String, String>() {
                        override fun message(ch: String, message: String) {
                            if (ch == channel) {
                                handler(message)
                            }
                        }
                    })
                    pubSubConnection.sync().subscribe(channel)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }.start()
        }
    }
    override suspend fun lrange(key: String, start: Long, stop: Long): List<String> = executeWithRetry {
        it.lrange(key, start, stop)
    }

    override suspend fun scard(key: String): Long = executeWithRetry {
        it.scard(key)
    }

    override suspend fun sismember(key: String, member: String): Boolean = executeWithRetry {
        it.sismember(key, member)
    }
    override suspend fun zremrangebyrank(key: String, start: Long, stop: Long): Long = executeWithRetry {
        it.zremrangebyrank(key, start, stop)
    }
    override suspend fun incrBy(key: String, delta: Long): Long = executeWithRetry {
        it.incrby(key, delta)
    }
}