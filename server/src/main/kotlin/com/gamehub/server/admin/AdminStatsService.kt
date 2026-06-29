package com.gamehub.server.admin

import com.gamehub.server.cache.PresenceCache
import com.gamehub.server.modules.GameSessionManager
import com.gamehub.server.repository.MatchHistoryRepository
import com.gamehub.server.repository.UserRepository
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.OffsetDateTime

class AdminStatsService(
    private val userRepository: UserRepository,
    private val matchHistoryRepository: MatchHistoryRepository? = null
) {
    suspend fun getOnlineHubUsersCount(): Int = PresenceCache.getOnlineHubCount()
    suspend fun getInGameUsersCount(): Int = PresenceCache.getInGameCount()
    suspend fun getActiveGamesCount(): Int = GameSessionManager.getActiveGamesCount()

    suspend fun getFinishedGamesToday(): Int = matchHistoryRepository?.countMatchesAfter(getStartOfDay()) ?: 0

    suspend fun getTotalUsersCount(): Int = userRepository.getTotalUsersCount(null)

    suspend fun getNewUsersToday(): Int = userRepository.getUsersRegisteredAfter(getStartOfDayOffset())

    suspend fun getTotalCoinsInCirculation(): Long = userRepository.getTotalSoftCurrency()

    suspend fun getServerHealth(): Map<String, Any> {
        val dbOk = try {
            userRepository.getTotalUsersCount(null)
            true
        } catch (e: Exception) {
            false
        }
        return mapOf("status" to dbOk)
    }

    private fun getStartOfDay(): Instant = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant()
    private fun getStartOfDayOffset(): OffsetDateTime = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime()
}