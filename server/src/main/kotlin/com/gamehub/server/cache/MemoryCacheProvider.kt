// server/src/main/kotlin/com/gamehub/server/cache/MemoryCacheProvider.kt
package com.gamehub.server.cache

import com.gamehub.shared.cache.CacheProvider
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class MemoryCacheProvider : CacheProvider {
    private data class Entry(val value: String, val expiry: Long)
    private data class SortedSetEntry(val member: String, val expiry: Long)
    private val store = ConcurrentHashMap<String, Entry>()
    private val hashStore = ConcurrentHashMap<String, ConcurrentHashMap<String, String>>()
    private val setStore = ConcurrentHashMap<String, MutableSet<String>>()
    private val sortedSetStore = ConcurrentHashMap<String, java.util.concurrent.ConcurrentSkipListMap<Double, MutableSet<SortedSetEntry>>>()
    private val listStore = ConcurrentHashMap<String, MutableList<String>>()
    private val locks = ConcurrentHashMap<String, Mutex>()
    private val subscribers = ConcurrentHashMap<String, MutableList<(String) -> Unit>>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private fun getLock(key: String): Mutex = locks.getOrPut(key) { Mutex() }

    override suspend fun set(key: String, value: String, ttlSeconds: Long) {
        val expiry = if (ttlSeconds > 0) System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(ttlSeconds) else Long.MAX_VALUE
        store[key] = Entry(value, expiry)
    }

    override suspend fun get(key: String): String? = store[key]?.takeIf { it.expiry > System.currentTimeMillis() }?.value

    override suspend fun delete(key: String) {
        store.remove(key)
        hashStore.remove(key)
        setStore.remove(key)
        sortedSetStore.remove(key)
        listStore.remove(key)
    }

    override suspend fun exists(key: String): Boolean = get(key) != null

    override suspend fun expire(key: String, ttlSeconds: Long) {
        val entry = store[key] ?: return
        store[key] = entry.copy(expiry = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(ttlSeconds))
    }

    override suspend fun setnx(key: String, value: String, ttlSeconds: Long): Boolean = getLock(key).withLock {
        if (exists(key)) false else {
            set(key, value, ttlSeconds)
            true
        }
    }

    override suspend fun incr(key: String): Long = getLock(key).withLock {
        val current = (get(key)?.toLongOrNull() ?: 0) + 1
        set(key, current.toString())
        current
    }

    override suspend fun decr(key: String): Long = getLock(key).withLock {
        val current = (get(key)?.toLongOrNull() ?: 0) - 1
        set(key, current.toString())
        current
    }

    override suspend fun hset(key: String, field: String, value: String) {
        val map = hashStore.getOrPut(key) { ConcurrentHashMap() }
        map[field] = value
    }

    override suspend fun hget(key: String, field: String): String? = hashStore[key]?.get(field)

    override suspend fun hgetAll(key: String): Map<String, String> = hashStore[key]?.toMap() ?: emptyMap()

    override suspend fun hdel(key: String, vararg fields: String) {
        hashStore[key]?.let { map -> fields.forEach { map.remove(it) } }
    }

    override suspend fun sadd(key: String, vararg members: String) {
        setStore.getOrPut(key) { mutableSetOf() }.addAll(members)
    }

    override suspend fun smembers(key: String): Set<String> = setStore[key]?.toSet() ?: emptySet()

    override suspend fun srem(key: String, vararg members: String) {
        setStore[key]?.removeAll(members.toSet())
    }

    override suspend fun zadd(key: String, score: Double, member: String, ttlSeconds: Long) {
        val expiry = if (ttlSeconds > 0) System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(ttlSeconds) else Long.MAX_VALUE
        val map = sortedSetStore.getOrPut(key) { java.util.concurrent.ConcurrentSkipListMap() }
        // ابتدا member از همه ورودی قدیمی از همه scoreها پاک می‌شوند
        zrem(key, member)
        val set = map.getOrPut(score) { java.util.concurrent.ConcurrentHashMap.newKeySet() }
        set.add(SortedSetEntry(member, expiry))
    }

    private fun cleanupExpiredSortedSetEntries(map: java.util.concurrent.ConcurrentSkipListMap<Double, MutableSet<SortedSetEntry>>) {
        val now = System.currentTimeMillis()
        val iterator = map.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            val entriesToRemove: MutableList<SortedSetEntry> = mutableListOf()
            for (sEntry in entry.value) {
                if (sEntry.expiry <= now) {
                    entriesToRemove.add(sEntry)
                }
            }
            entry.value.removeAll(entriesToRemove)
            if (entry.value.isEmpty()) {
                iterator.remove()
            }
        }
    }

    override suspend fun zrangebyscore(key: String, minScore: Double, maxScore: Double, limit: Int): List<String> {
        val map = sortedSetStore[key] ?: return emptyList()
        cleanupExpiredSortedSetEntries(map)
        val result = mutableListOf<String>()
        var count = 0
        val subMap = map.subMap(minScore, true, maxScore, true)
        for ((score, entries) in subMap) {
            for (entry in entries) {
                if (count >= limit) break
                result.add(entry.member)
                count++
            }
            if (count >= limit) break
        }
        return result
    }

    override suspend fun zremrangebyscore(key: String, minScore: Double, maxScore: Double): Long {
        val map = sortedSetStore[key] ?: return 0
        var removed = 0L
        val toRemove = map.subMap(minScore, true, maxScore, true).toList()
        toRemove.forEach { (score, members) ->
            removed += members.size
            map.remove(score)
        }
        return removed
    }

    override suspend fun zrem(key: String, vararg members: String) {
        val map = sortedSetStore[key] ?: return
        val memberSet = members.toSet()
        for (score in map.keys.toList()) {
            val entries = map[score] ?: continue
            entries.removeIf { it.member in memberSet }
            if (entries.isEmpty()) {
                map.remove(score)
            }
        }
    }

    override suspend fun zcard(key: String): Long {
        val map = sortedSetStore[key] ?: return 0L
        cleanupExpiredSortedSetEntries(map)
        return map.values.sumOf { it.size }.toLong()
    }

    override suspend fun zcleanup(key: String): Long {
        val map = sortedSetStore[key] ?: return 0L
        val before = zcard(key)
        cleanupExpiredSortedSetEntries(map)
        val after = zcard(key)
        return before - after
    }

    override suspend fun lpush(key: String, vararg values: String): Long {
        val list = listStore.getOrPut(key) { mutableListOf() }
        for (value in values.reversed()) {
            list.add(0, value)
        }
        return list.size.toLong()
    }

    override suspend fun ltrim(key: String, start: Long, stop: Long) {
        val list = listStore[key] ?: return
        val newList = list.drop(start.toInt()).take((stop - start + 1).toInt())
        listStore[key] = newList.toMutableList()
    }

    override suspend fun publish(channel: String, message: String) {
        subscribers[channel]?.forEach { handler ->
            scope.launch { try { handler(message) } catch (_: Exception) {} }
        }
    }

    override suspend fun subscribe(channel: String, handler: (String) -> Unit) {
        subscribers.getOrPut(channel) { mutableListOf() }.add(handler)
    }

    // اضافه کردن توابع جدید
    override suspend fun lrange(key: String, start: Long, stop: Long): List<String> {
        val list = listStore[key] ?: return emptyList()
        val startIdx = start.toInt()
        val endIdx = if (stop < 0) list.size + stop.toInt() else stop.toInt()
        return if (startIdx < 0 || startIdx >= list.size) emptyList()
        else list.subList(startIdx, minOf(endIdx + 1, list.size))
    }

    override suspend fun zremrangebyrank(key: String, start: Long, stop: Long): Long {
        val map = sortedSetStore[key] ?: return 0L
        var removed = 0L
        val iterator = map.entries.iterator()
        var index = 0L
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (index in start..stop) {
                removed += entry.value.size
                iterator.remove()
            }
            index++
        }
        return removed
    }
    override suspend fun incrBy(key: String, delta: Long): Long {
        val lock = getLock(key)
        return lock.withLock {
            val current = (get(key)?.toLongOrNull() ?: 0) + delta
            set(key, current.toString())
            current
        }
    }

    override suspend fun scard(key: String): Long = setStore[key]?.size?.toLong() ?: 0L

    override suspend fun sismember(key: String, member: String): Boolean = setStore[key]?.contains(member) == true
}
