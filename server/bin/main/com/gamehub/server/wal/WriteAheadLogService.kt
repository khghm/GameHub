package com.gamehub.server.wal

import com.gamehub.server.repository.GameEventLogRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class WriteAheadLogService(
    private val repository: GameEventLogRepository
) {
    
    private val sessionCaches = ConcurrentHashMap<UUID, SessionEventCache>()
    
    suspend fun logEvent(
        gameSessionId: UUID,
        gameType: String,
        eventType: String,
        playerId: UUID?,
        payload: JsonObject
    ): GameEvent = withContext(Dispatchers.IO) {
        val sequenceNumber = repository.getNextSequenceNumber(gameSessionId)
        val event = GameEvent(
            eventId = UUID.randomUUID(),
            gameSessionId = gameSessionId,
            gameType = gameType,
            eventType = eventType,
            playerId = playerId.toString(),
            timestamp = Instant.now(),
            sequenceNumber = sequenceNumber,
            payload = payload
        )
        
        repository.insert(event)
        
        val cache = sessionCaches.computeIfAbsent(gameSessionId) { SessionEventCache() }
        cache.addEvent(event)
        
        event
    }
    
    suspend fun getEventsForSession(
        gameSessionId: UUID,
        fromSequence: Long = 0
    ): List<GameEvent> = withContext(Dispatchers.IO) {
        val cache = sessionCaches[gameSessionId]
        val cachedEvents = cache?.getEventsFrom(fromSequence) ?: emptyList()
        
        if (cachedEvents.isNotEmpty() && cachedEvents.first().sequenceNumber == fromSequence + 1) {
            cachedEvents
        } else {
            repository.findByGameSession(gameSessionId, fromSequence)
        }
    }
    
    suspend fun getUnappliedEvents(gameSessionId: UUID): List<GameEvent> = withContext(Dispatchers.IO) {
        repository.findUnappliedEvents(gameSessionId)
    }
    
    suspend fun markEventAsApplied(eventId: UUID): Boolean = withContext(Dispatchers.IO) {
        repository.markAsApplied(eventId)
    }
    
    suspend fun markEventsAsApplied(eventIds: List<UUID>): Int = withContext(Dispatchers.IO) {
        repository.markBatchAsApplied(eventIds)
    }
    
    suspend fun replayEvents(
        gameSessionId: UUID,
        fromSequence: Long = 0,
        handler: suspend (GameEvent) -> Unit
    ): Int = withContext(Dispatchers.IO) {
        val events = repository.findByGameSession(gameSessionId, fromSequence)
        events.forEach { event ->
            handler(event)
        }
        events.size
    }
    
    suspend fun validateEvent(event: GameEvent): Boolean = withContext(Dispatchers.IO) {
        val storedEvent = repository.findByEventId(event.eventId) ?: return@withContext false
        storedEvent.computeChecksum() == event.computeChecksum()
    }
    
    suspend fun recoverSession(gameSessionId: UUID): List<GameEvent> = withContext(Dispatchers.IO) {
        val events = repository.findByGameSession(gameSessionId)
        sessionCaches[gameSessionId] = SessionEventCache().apply {
            events.forEach { addEvent(it) }
        }
        events
    }
    
    fun clearSessionCache(gameSessionId: UUID) {
        sessionCaches.remove(gameSessionId)
    }
    
    fun clearAllCaches() {
        sessionCaches.clear()
    }
    
    private class SessionEventCache {
        private val events = mutableListOf<GameEvent>()
        private val eventMap = mutableMapOf<UUID, GameEvent>()
        
        @Synchronized
        fun addEvent(event: GameEvent) {
            events.add(event)
            eventMap[event.eventId] = event
        }
        
        @Synchronized
        fun getEventsFrom(fromSequence: Long): List<GameEvent> {
            return events.filter { it.sequenceNumber > fromSequence }
        }
    }
}
