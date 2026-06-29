package com.gamehub.server.replay

import com.gamehub.server.repository.GameEventLogRepository
import com.gamehub.server.wal.GameEvent
import java.util.UUID

class ReplayService(private val eventLogRepo: GameEventLogRepository) {
    
    fun getReplayEvents(gameSessionId: UUID): List<GameEvent> {
        return eventLogRepo.findByGameSession(gameSessionId)
    }
    
    fun getReplayEventsFromSequence(gameSessionId: UUID, fromSequence: Long): List<GameEvent> {
        return eventLogRepo.findByGameSession(gameSessionId, fromSequence)
    }
}
