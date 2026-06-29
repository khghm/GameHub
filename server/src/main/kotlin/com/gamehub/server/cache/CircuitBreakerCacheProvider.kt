// server/src/main/kotlin/com/gamehub/server/cache/CircuitBreakerCacheProvider.kt
package com.gamehub.server.cache

import com.gamehub.shared.cache.CacheProvider
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig
import io.github.resilience4j.kotlin.circuitbreaker.executeSuspendFunction
import java.time.Duration

class CircuitBreakerCacheProvider(
    private val delegate: CacheProvider,
    circuitBreakerName: String = "cache-circuit-breaker"
) : CacheProvider {

    private val circuitBreakerConfig = CircuitBreakerConfig.custom()
        .failureRateThreshold(50f) // 50% failure rate to open circuit
        .waitDurationInOpenState(Duration.ofSeconds(10)) // Wait 10 seconds before trying to close
        .permittedNumberOfCallsInHalfOpenState(3) // Allow 3 calls in half-open state
        .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
        .slidingWindowSize(10) // Check last 10 calls
        .build()

    private val circuitBreaker = CircuitBreaker.of(circuitBreakerName, circuitBreakerConfig)

    private suspend fun <T> withCircuitBreaker(block: suspend () -> T): T {
        return circuitBreaker.executeSuspendFunction { block() }
    }

    override suspend fun set(key: String, value: String, ttlSeconds: Long) = withCircuitBreaker {
        delegate.set(key, value, ttlSeconds)
    }

    override suspend fun get(key: String): String? = withCircuitBreaker {
        delegate.get(key)
    }

    override suspend fun delete(key: String) = withCircuitBreaker {
        delegate.delete(key)
    }

    override suspend fun exists(key: String): Boolean = withCircuitBreaker {
        delegate.exists(key)
    }

    override suspend fun expire(key: String, ttlSeconds: Long) = withCircuitBreaker {
        delegate.expire(key, ttlSeconds)
    }

    override suspend fun setnx(key: String, value: String, ttlSeconds: Long): Boolean = withCircuitBreaker {
        delegate.setnx(key, value, ttlSeconds)
    }

    override suspend fun incr(key: String): Long = withCircuitBreaker {
        delegate.incr(key)
    }

    override suspend fun decr(key: String): Long = withCircuitBreaker {
        delegate.decr(key)
    }

    override suspend fun hset(key: String, field: String, value: String) = withCircuitBreaker {
        delegate.hset(key, field, value)
    }

    override suspend fun hget(key: String, field: String): String? = withCircuitBreaker {
        delegate.hget(key, field)
    }

    override suspend fun hgetAll(key: String): Map<String, String> = withCircuitBreaker {
        delegate.hgetAll(key)
    }

    override suspend fun hdel(key: String, vararg fields: String) = withCircuitBreaker {
        delegate.hdel(key, *fields)
    }

    override suspend fun sadd(key: String, vararg members: String) = withCircuitBreaker {
        delegate.sadd(key, *members)
    }

    override suspend fun smembers(key: String): Set<String> = withCircuitBreaker {
        delegate.smembers(key)
    }

    override suspend fun srem(key: String, vararg members: String) = withCircuitBreaker {
        delegate.srem(key, *members)
    }

    override suspend fun zadd(key: String, score: Double, member: String, ttlSeconds: Long) = withCircuitBreaker {
        delegate.zadd(key, score, member, ttlSeconds)
    }

    override suspend fun zrangebyscore(key: String, minScore: Double, maxScore: Double, limit: Int): List<String> = withCircuitBreaker {
        delegate.zrangebyscore(key, minScore, maxScore, limit)
    }

    override suspend fun zremrangebyscore(key: String, minScore: Double, maxScore: Double): Long = withCircuitBreaker {
        delegate.zremrangebyscore(key, minScore, maxScore)
    }

    override suspend fun zrem(key: String, vararg members: String) = withCircuitBreaker {
        delegate.zrem(key, *members)
    }

    override suspend fun zcard(key: String): Long = withCircuitBreaker {
        delegate.zcard(key)
    }

    override suspend fun zremrangebyrank(key: String, start: Long, stop: Long): Long = withCircuitBreaker {
        delegate.zremrangebyrank(key, start, stop)
    }

    override suspend fun zcleanup(key: String): Long = withCircuitBreaker {
        delegate.zcleanup(key)
    }

    override suspend fun lpush(key: String, vararg values: String): Long = withCircuitBreaker {
        delegate.lpush(key, *values)
    }

    override suspend fun ltrim(key: String, start: Long, stop: Long) = withCircuitBreaker {
        delegate.ltrim(key, start, stop)
    }

    override suspend fun publish(channel: String, message: String) = withCircuitBreaker {
        delegate.publish(channel, message)
    }

    override suspend fun subscribe(channel: String, handler: (String) -> Unit) = withCircuitBreaker {
        delegate.subscribe(channel, handler)
    }

    override suspend fun lrange(key: String, start: Long, stop: Long): List<String> = withCircuitBreaker {
        delegate.lrange(key, start, stop)
    }

    override suspend fun scard(key: String): Long = withCircuitBreaker {
        delegate.scard(key)
    }

    override suspend fun sismember(key: String, member: String): Boolean = withCircuitBreaker {
        delegate.sismember(key, member)
    }

    override suspend fun incrBy(key: String, delta: Long): Long = withCircuitBreaker {
        delegate.incrBy(key, delta)
    }
}
