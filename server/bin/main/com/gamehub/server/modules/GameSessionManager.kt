package com.gamehub.server.modules

import com.gamehub.server.session.GameSession
import com.gamehub.shared.core.*
import com.gamehub.shared.cache.CacheProvider
import com.gamehub.games.tictactoe.TicTacToeEngine
import com.gamehub.games.connectfour.ConnectFourEngine
import com.gamehub.games.ludo.LudoEngine
import com.gamehub.games.chess.ChessEngine
import com.gamehub.games.farkle.FarkleEngine
import com.gamehub.games.esmofamil.EsmoFamilEngine
import com.gamehub.games.backgammon.BackgammonEngine
import com.gamehub.games.abalone.AbaloneEngine
import com.gamehub.games.spadesbaloot.SpadesBalootEngine
import com.gamehub.games.othello.OthelloEngine
import com.gamehub.games.baltazar.BaltazarEngine
import com.gamehub.games.bridge.BridgeEngine
import com.gamehub.games.checkers.CheckersEngine
import com.gamehub.games.blokus.BlokusEngine
import com.gamehub.games.yahtzee.YahtzeeEngine
import com.gamehub.games.nard.NardEngine
import com.gamehub.games.hex.HexEngine
import com.gamehub.games.battleship.BattleshipEngine
import com.gamehub.games.matchmonster.MatchMonsterEngine
import com.gamehub.games.soccerstriker.SoccerStrikerEngine
import java.util.concurrent.ConcurrentHashMap
import com.gamehub.games.monopoly.MonopolyEngine
import com.gamehub.games.uno.UnoEngine
import com.gamehub.server.completion.MatchCompletionService
import com.gamehub.server.repository.GameEventLogRepository
import java.util.UUID

object GameSessionManager {
    private val sessions = ConcurrentHashMap<String, GameSession<*, *, *>>()
    private val gameTypes = ConcurrentHashMap<String, String>()
    var cacheProvider: CacheProvider? = null
    var matchCompletionService: MatchCompletionService? = null
    var eventLogRepository: GameEventLogRepository? = null

    suspend fun createSession(gameType: String, playerIds: List<String>): String {
        val sessionId = UUID.randomUUID().toString().take(8)
        return createSession(gameType, playerIds, sessionId)
    }

    suspend fun createSession(
        gameType: String,
        playerIds: List<String>,
        gameId: String = UUID.randomUUID().toString().take(8),
        botDifficultyLevels: Map<String, Int> = emptyMap()
    ): String {
        val playerIdObjs = playerIds.map { PlayerId(it) }
        val botDifficultyMap = botDifficultyLevels.mapKeys { PlayerId(it.key) }
        val gameSessionId = UUID.randomUUID()

        val session: GameSession<*, *, *> = when (gameType) {
            "tictactoe" -> GameSession(gameId, gameSessionId, TicTacToeEngine(), playerIdObjs, cacheProvider, botDifficultyMap, matchCompletionService, eventLogRepository)
            "uno" -> GameSession(gameId, gameSessionId, UnoEngine(), playerIdObjs, cacheProvider, botDifficultyMap, matchCompletionService, eventLogRepository)
            "connectfour" -> GameSession(gameId, gameSessionId, ConnectFourEngine(), playerIdObjs, cacheProvider, botDifficultyMap, matchCompletionService, eventLogRepository)
            "ludo" -> GameSession(gameId, gameSessionId, LudoEngine(), playerIdObjs, cacheProvider, botDifficultyMap, matchCompletionService, eventLogRepository)
            "monopoly" -> GameSession(gameId, gameSessionId, MonopolyEngine(), playerIdObjs, cacheProvider, botDifficultyMap, matchCompletionService, eventLogRepository)
            "chess" -> GameSession(gameId, gameSessionId, ChessEngine(), playerIdObjs, cacheProvider, botDifficultyMap, matchCompletionService, eventLogRepository)
            "farkle" -> GameSession(gameId, gameSessionId, FarkleEngine(), playerIdObjs, cacheProvider, botDifficultyMap, matchCompletionService, eventLogRepository)
            "esmofamil" -> GameSession(gameId, gameSessionId, EsmoFamilEngine(), playerIdObjs, cacheProvider, botDifficultyMap, matchCompletionService, eventLogRepository)
            "backgammon" -> GameSession(gameId, gameSessionId, BackgammonEngine(), playerIdObjs, cacheProvider, botDifficultyMap, matchCompletionService, eventLogRepository)
            "abalone" -> GameSession(gameId, gameSessionId, AbaloneEngine(), playerIdObjs, cacheProvider, botDifficultyMap, matchCompletionService, eventLogRepository)
            "spades-baloot" -> GameSession(gameId, gameSessionId, SpadesBalootEngine(), playerIdObjs, cacheProvider, botDifficultyMap, matchCompletionService, eventLogRepository)
            "othello" -> GameSession(gameId, gameSessionId, OthelloEngine(), playerIdObjs, cacheProvider, botDifficultyMap, matchCompletionService, eventLogRepository)
            "baltazar" -> GameSession(gameId, gameSessionId, BaltazarEngine(), playerIdObjs, cacheProvider, botDifficultyMap, matchCompletionService, eventLogRepository)
            "bridge" -> GameSession(gameId, gameSessionId, BridgeEngine(), playerIdObjs, cacheProvider, botDifficultyMap, matchCompletionService, eventLogRepository)
            "checkers" -> GameSession(gameId, gameSessionId, CheckersEngine(), playerIdObjs, cacheProvider, botDifficultyMap, matchCompletionService, eventLogRepository)
            "blokus" -> GameSession(gameId, gameSessionId, BlokusEngine(), playerIdObjs, cacheProvider, botDifficultyMap, matchCompletionService, eventLogRepository)
            "yahtzee" -> GameSession(gameId, gameSessionId, YahtzeeEngine(), playerIdObjs, cacheProvider, botDifficultyMap, matchCompletionService, eventLogRepository)
            "nard" -> GameSession(gameId, gameSessionId, NardEngine(), playerIdObjs, cacheProvider, botDifficultyMap, matchCompletionService, eventLogRepository)
            "hex" -> GameSession(gameId, gameSessionId, HexEngine(), playerIdObjs, cacheProvider, botDifficultyMap, matchCompletionService, eventLogRepository)
            "battleship" -> GameSession(gameId, gameSessionId, BattleshipEngine(), playerIdObjs, cacheProvider, botDifficultyMap, matchCompletionService, eventLogRepository)
            "match-monster" -> GameSession(gameId, gameSessionId, MatchMonsterEngine(), playerIdObjs, cacheProvider, botDifficultyMap, matchCompletionService, eventLogRepository)
            "soccer-striker" -> GameSession(gameId, gameSessionId, SoccerStrikerEngine(), playerIdObjs, cacheProvider, botDifficultyMap, matchCompletionService, eventLogRepository)
            else -> throw IllegalArgumentException("Unknown game type: $gameType")
        }
        sessions[gameId] = session
        gameTypes[gameId] = gameType

        // اضافه کردن بازی به لیست بازی‌های فعال هر کاربر
        for (userId in playerIds) {
            addActiveGameForUser(userId, gameId)
        }

        return gameId
    }

    fun getSession(gameId: String): GameSession<*, *, *>? = sessions[gameId]
    fun getGameType(gameId: String): String? = gameTypes[gameId]

    suspend fun removeSession(gameId: String) {
        val session = sessions[gameId]
        if (session != null) {
            // حذف بازی از لیست فعال همه کاربران
            for (playerId in session.players) {
                removeActiveGameForUser(playerId.value, gameId)
            }
        }
        sessions.remove(gameId)
        gameTypes.remove(gameId)
    }

    fun getState(gameId: String): GameState? {
        return (getSession(gameId))?.currentState as? GameState
    }

    suspend fun submitActionToSession(gameId: String, playerId: PlayerId, action: GameAction): Boolean {
        val session = getSession(gameId) ?: return false
        try {
            session.submitActionUnsafe(playerId, action)
            return true
        } catch (e: Exception) {
            return false
        }
    }

    fun getActiveGamesCount(): Int = sessions.size

    // مدیریت لیست بازی‌های فعال کاربر در Redis
    suspend fun addActiveGameForUser(userId: String, gameId: String) {
        cacheProvider?.sadd("user:active_games:$userId", gameId)
        cacheProvider?.expire("user:active_games:$userId", 86400 * 7) // 7 روز
    }

    suspend fun removeActiveGameForUser(userId: String, gameId: String) {
        cacheProvider?.srem("user:active_games:$userId", gameId)
    }
    suspend fun getActiveGamesCount(userId: String): Int {
        return cacheProvider?.scard("user:active_games:$userId")?.toInt() ?: 0
    }

}