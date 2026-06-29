// server/src/main/kotlin/com/gamehub/server/modules/WebSocketHandler.kt
package com.gamehub.server.modules

import com.gamehub.games.chess.ChessAction
import com.gamehub.games.farkle.FarkleAction
import com.gamehub.games.esmofamil.EsmoFamilAction
import com.gamehub.games.backgammon.BackgammonAction
import com.gamehub.games.abalone.AbaloneAction
import com.gamehub.games.spadesbaloot.SpadesBalootAction
import com.gamehub.games.othello.OthelloAction
import com.gamehub.games.baltazar.BaltazarAction
import com.gamehub.games.bridge.BridgeAction
import com.gamehub.games.checkers.CheckersAction
import com.gamehub.games.checkers.CheckersEngine
import com.gamehub.games.checkers.CheckersState
import com.gamehub.games.ludo.LudoAction
import com.gamehub.games.monopoly.MonopolyAction
import com.gamehub.games.uno.UnoState
import com.gamehub.games.blokus.BlokusAction
import com.gamehub.games.yahtzee.YahtzeeAction
import com.gamehub.games.battleship.BattleshipAction
import com.gamehub.games.matchmonster.MatchMonsterAction
import com.gamehub.games.soccerstriker.SoccerStrikerAction
import com.gamehub.server.anticheat.AntiCheatService
import com.gamehub.server.cache.PresenceCache
import com.gamehub.server.cache.SessionCache
import com.gamehub.server.matchmaking.MatchmakingService
import com.gamehub.server.modules.GameSessionManager.cacheProvider
import com.gamehub.server.security.AntiReplayFilter
import com.gamehub.server.security.RateLimiter
import com.gamehub.server.serverGameJson
import com.gamehub.shared.cache.CacheProvider
import com.gamehub.shared.core.*
import com.gamehub.shared.engine.GameSnapshot
import com.gamehub.shared.engines.board.BoardAction
import com.gamehub.shared.engines.card.CardAction
import com.gamehub.shared.engines.card.CardColor
import com.gamehub.shared.matchmaking.SkillRating
import com.gamehub.shared.networking.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*

/**
 * مدیریت WebSocket برای بازی‌ها
 *
 * تغییرات جدید فاز 2:
 * - استفاده از ReconnectTokenBroker برای مدیریت توکن‌های reconnect دو لایه
 * - پشتیبانی از deviceIdHash برای Rate Limiting
 * - فراخوانی onPlayerDisconnect/onPlayerReconnect در GameSession
 * - باطل کردن جلسه reconnect پس از قطعی
 */
class WebSocketHandler(
    private val authModule: AuthModule,
    private val matchmakingService: MatchmakingService? = null,
    private val antiCheatService: AntiCheatService? = null,
    private val reconnectTokenBroker: ReconnectTokenBroker? = null,
    private val cacheProvider: CacheProvider,
    private val rateLimiter: RateLimiter
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    suspend fun handle(session: DefaultWebSocketServerSession, userId: String, username: String) {
//        // Debug Checkers serialization
//        try {
//            println("\n🔍 MANUAL CHECKERS SERIALIZATION TEST 🔍")
//            val testPlayers = listOf(PlayerId("user1"), PlayerId("bot1"))
//            val testEngine = CheckersEngine()
//            val testState = testEngine.createInitialState(testPlayers)
//            println("🔍 Test state created successfully: $testState")
//            val testJson = serverGameJson.encodeToString(GameState.serializer(), testState as GameState)
//            println("🔍 Serialized test JSON successfully: $testJson")
//            val deserialized = serverGameJson.decodeFromString(GameState.serializer(), testJson)
//            println("🔍 Deserialized test state successfully: $deserialized\n")
//        } catch (e: Exception) {
//            println("❌ MANUAL CHECKERS TEST FAILED!")
//            e.printStackTrace()
//        }

        // دریافت deviceIdHash از query parameters (برای Rate Limiting)
        val deviceIdHash = session.call.request.queryParameters["deviceId"] ?: ""

        SessionCache.set(userId, "ws-connected", 1800)
        PresenceCache.setInGame(userId)

        // ایجاد جلسه reconnect جدید با استفاده از Token Broker
        val (referenceId, reconnectToken) = reconnectTokenBroker?.createSession(userId, "")
            ?: Pair("", java.util.UUID.randomUUID().toString())

        ReconnectManager.saveToken(userId, "", reconnectToken) // سازگاری با کدهای قدیمی

        val welcomeMsg = """{"type":"welcome","userId":"$userId","username":"$username","reconnectToken":"$reconnectToken"}"""
        session.send(Frame.Text(welcomeMsg))
        println("✅ User $username connected and welcome sent.")

        var currentGameId: String? = null

        try {
            for (frame in session.incoming) {
                if (frame is Frame.Text) {
                    val text = frame.readText()
                    val wsMsg = try {
                        serverGameJson.decodeFromString(WsMessage.serializer(), text)
                    } catch (e: Exception) {
                        println("⚠️ Failed to parse message: $text")
                        null
                    }
                    val nonce = when (wsMsg) {
                        is SubmitMoveMsg -> wsMsg.nonce
                        is ChatMessageMsg -> wsMsg.nonce
                        is MatchmakingRequestMsg -> wsMsg.nonce
                        else -> null
                    }
                    val timestamp = when (wsMsg) {
                        is SubmitMoveMsg -> wsMsg.timestamp
                        is ChatMessageMsg -> wsMsg.timestamp
                        is MatchmakingRequestMsg -> wsMsg.timestamp
                        else -> null
                    }

                    if (nonce != null && timestamp != null) {
                        // ایجاد نمونه AntiReplayFilter (یکبار در کلاس WebSocketHandler)
                        val antiReplay = AntiReplayFilter(cacheProvider)
                        if (!antiReplay.checkAndRecord(nonce, timestamp, userId)) {
                            session.send(Frame.Text("""{"error":"replay_attempt","code":"INVALID_NONCE"}"""))
                            continue
                        }
                    }
                    val rateKey = "user:$userId"
                    if (!rateLimiter.isAllowed(rateKey)) {
                        session.send(Frame.Text("""{"error":"rate_limited","code":"TOO_MANY_REQUESTS","message":"لطفاً کمی صبر کنید"}"""))
                        continue
                    }

                    when (wsMsg) {
                        is MatchmakingRequestMsg -> {
                            when (val req = wsMsg.request) {
                                is SoloRequest -> {
                                    val initialRating = SkillRating(mean = 1200.0, standardDeviation = 0.0)
                                    val mode = "casual"
                                    matchmakingService?.enqueueSolo(userId, req.gameId, mode, initialRating)
                                    println("📥 [$userId] added to queue for ${req.gameId} ($mode)")

                                    val opponentId = matchmakingService?.tryMatchSolo(req.gameId, mode, userId)
                                    if (opponentId != null) {
                                        val gameId = GameSessionManager.createSession(req.gameId, listOf(userId, opponentId))
                                        currentGameId = gameId

                                        val gs = GameSessionManager.getSession(gameId)!!
                                        gs.addPlayerSession(PlayerId(userId), session)

                                        val opponentSession = HubWebSocketHandler.userSessions[opponentId]
                                        if (opponentSession != null) {
                                            gs.addPlayerSession(PlayerId(opponentId), opponentSession)
                                            println("🎮 Both players added to game $gameId")
                                        }

                                        gs.broadcastState()

                                        // به‌روزرسانی جلسه reconnect با gameId واقعی
                                        reconnectTokenBroker?.createSession(userId, gameId)

                                        println("🏗️ New game created: type=${req.gameId}, players=$userId vs $opponentId")

                                        val proposalMsg = MatchProposalMsg(
                                            gameId = gameId,
                                            mode = GameMode.CASUAL,
                                            players = listOf(userId, opponentId),
                                            timeoutSeconds = 30
                                        )
                                        val proposalJson = serverGameJson.encodeToString(WsMessage.serializer(), proposalMsg)
                                        session.send(Frame.Text(proposalJson))
                                        opponentSession?.send(Frame.Text(proposalJson))
                                    } else {
                                        session.send(Frame.Text("""{"type":"waiting","message":"منتظر حریف..."}"""))
                                        println("⏳ $username waiting for opponent in ${req.gameId}")
                                    }
                                }
                                is PartyRequest -> {
                                    println("👥 Party request received but not implemented yet")
                                }
                            }
                        }

                        is ResumeGameMsg -> {
                            val gameId = wsMsg.gameId
                            val token = wsMsg.reconnectToken

                            // استفاده از Token Broker برای اعتبارسنجی
                            val sessionInfo = if (token != null && reconnectTokenBroker != null) {
                                reconnectTokenBroker.validateToken(token, deviceIdHash)
                            } else null

                            val isValid = if (sessionInfo != null) {
                                sessionInfo.gameId == gameId
                            } else {
                                // Fallback به روش قدیمی
                                val tokenValid = ReconnectManager.validateToken(userId, token ?: "") == gameId
                                val sessionCheck = GameSessionManager.getSession(gameId)
                                tokenValid || (sessionCheck != null && sessionCheck.players.contains(PlayerId(userId)))
                            }

                            if (!isValid) {
                                val err = ErrorMsg(code = "INVALID_TOKEN", description = "Reconnect token invalid or missing")
                                session.send(Frame.Text(serverGameJson.encodeToString(WsMessage.serializer(), err)))
                                continue
                            }

                            val existingSession = GameSessionManager.getSession(gameId)
                            if (existingSession != null) {
                                currentGameId = gameId
                                existingSession.onPlayerReconnect(PlayerId(userId))
                                existingSession.addPlayerSession(PlayerId(userId), session)
                                existingSession.broadcastState()
                                println("✅ User $username resumed game $gameId")
                            } else {
                                val snapshotJson = GameSessionManager.cacheProvider?.get("snapshot:$gameId")
                                if (snapshotJson != null) {
                                    val snapshot = serverGameJson.decodeFromString(GameSnapshot.serializer(), snapshotJson)
                                    val newGameId = GameSessionManager.createSession(snapshot.gameType, snapshot.players, gameId)
                                    val newSession = GameSessionManager.getSession(newGameId)
                                    if (newSession != null) {
                                        newSession.restoreFromSnapshot(snapshot)
                                        currentGameId = newGameId
                                        newSession.addPlayerSession(PlayerId(userId), session)
                                        newSession.broadcastState()
                                        // به‌روزرسانی جلسه reconnect
                                        reconnectTokenBroker?.createSession(userId, newGameId)
                                        println("✅ User $username restored game $newGameId from snapshot")
                                    } else {
                                        val err = ErrorMsg(code = "GAME_EXPIRED", description = "Game session expired")
                                        session.send(Frame.Text(serverGameJson.encodeToString(WsMessage.serializer(), err)))
                                    }
                                } else {
                                    val err = ErrorMsg(code = "GAME_EXPIRED", description = "Game session expired")
                                    session.send(Frame.Text(serverGameJson.encodeToString(WsMessage.serializer(), err)))
                                }
                            }
                        }

                        is SubmitMoveMsg -> {
                            val gameId = wsMsg.gameId
                            val movePayload = wsMsg.movePayload
                            val gameType = GameSessionManager.getGameType(gameId) ?: "tictactoe"

                            val action: GameAction? = when (gameType) {
                                "tictactoe", "connectfour", "hex" -> {
                                    val row = movePayload.jsonObject["row"]?.jsonPrimitive?.int
                                    val col = movePayload.jsonObject["col"]?.jsonPrimitive?.int
                                    if (row != null && col != null) BoardAction(row, col) else null
                                }
                                "ludo" -> {
                                    val actionType = movePayload.jsonObject["actionType"]?.jsonPrimitive?.content
                                    when (actionType) {
                                        "roll" -> LudoAction.RollDice
                                        "move" -> {
                                            val pieceIndex = movePayload.jsonObject["pieceIndex"]?.jsonPrimitive?.int
                                            if (pieceIndex != null) LudoAction.MovePiece(pieceIndex) else null
                                        }
                                        else -> null
                                    }
                                }
                                "uno" -> {
                                    val actionType = movePayload.jsonObject["actionType"]?.jsonPrimitive?.content
                                    when (actionType) {
                                        "play" -> {
                                            val cardIndex = movePayload.jsonObject["cardIndex"]?.jsonPrimitive?.int
                                            val state = GameSessionManager.getState(gameId) as? UnoState
                                            val hand = state?.hands?.get(PlayerId(userId))
                                            val card = hand?.cards?.getOrNull(cardIndex ?: -1)
                                            val chosenColorStr = movePayload.jsonObject["chosenColor"]?.jsonPrimitive?.contentOrNull
                                            val chosenColor = when (chosenColorStr?.uppercase()) {
                                                "RED" -> CardColor.RED
                                                "BLUE" -> CardColor.BLUE
                                                "GREEN" -> CardColor.GREEN
                                                "YELLOW" -> CardColor.YELLOW
                                                else -> null
                                            }
                                            if (card != null) CardAction.PlayCard(card, chosenColor) else null
                                        }
                                        "draw" -> CardAction.DrawCard
                                        else -> null
                                    }
                                }
                                "monopoly" -> {
                                    try {
                                        serverGameJson.decodeFromString(MonopolyAction.serializer(), movePayload.toString())
                                    } catch (e: Exception) {
                                        println("❌ Failed to parse MonopolyAction: ${e.message}")
                                        null
                                    }
                                }
                                "chess" -> {
                                    try {
                                        // Try to decode as Move first
                                        serverGameJson.decodeFromString(ChessAction.Move.serializer(), movePayload.toString())
                                    } catch (e: Exception) {
                                        println("❌ Failed to parse ChessAction: ${e.message}")
                                        null
                                    }
                                }
                                "farkle" -> {
                                    try {
                                        // Parse the action type
                                        val actionType = movePayload.jsonObject["actionType"]?.jsonPrimitive?.content
                                        when (actionType) {
                                            "roll" -> FarkleAction.RollDice
                                            "select" -> {
                                                val diceIds = movePayload.jsonObject["diceIds"]?.jsonArray?.map { it.jsonPrimitive.int } ?: emptyList()
                                                FarkleAction.SelectDice(diceIds)
                                            }
                                            "bank" -> FarkleAction.BankScore
                                            "continue" -> FarkleAction.ContinueHotDice
                                            else -> null
                                        }
                                    } catch (e: Exception) {
                                        println("❌ Failed to parse FarkleAction: ${e.message}")
                                        null
                                    }
                                }
                                "esmofamil" -> {
                                    try {
                                        val answersMap = movePayload.jsonObject.mapNotNull { (key, value) ->
                                            val idx = key.toIntOrNull() ?: return@mapNotNull null
                                            val ans = value.jsonPrimitive.contentOrNull
                                            idx to ans
                                        }.toMap()
                                        EsmoFamilAction.SubmitAnswers(answersMap)
                                    } catch (e: Exception) {
                                        println("❌ Failed to parse EsmoFamilAction: ${e.message}")
                                        e.printStackTrace()
                                        null
                                    }
                                }
                                "backgammon" -> {
                                    try {
                                        serverGameJson.decodeFromString(GameAction.serializer(), movePayload.toString()) as BackgammonAction
                                    } catch (e: Exception) {
                                        println("❌ Failed to parse BackgammonAction: ${e.message}")
                                        null
                                    }
                                }
                                "abalone" -> {
                                    try {
                                        serverGameJson.decodeFromString(GameAction.serializer(), movePayload.toString()) as AbaloneAction
                                    } catch (e: Exception) {
                                        println("❌ Failed to parse AbaloneAction: ${e.message}")
                                        null
                                    }
                                }
                                "spades-baloot" -> {
                                    try {
                                        serverGameJson.decodeFromString(GameAction.serializer(), movePayload.toString()) as SpadesBalootAction
                                    } catch (e: Exception) {
                                        println("❌ Failed to parse SpadesBalootAction: ${e.message}")
                                        null
                                    }
                                }
                                "othello" -> {
                                    try {
                                        serverGameJson.decodeFromString(GameAction.serializer(), movePayload.toString()) as OthelloAction
                                    } catch (e: Exception) {
                                        println("❌ Failed to parse OthelloAction: ${e.message}")
                                        null
                                    }
                                }
                                "baltazar" -> {
                                    try {
                                        serverGameJson.decodeFromString(GameAction.serializer(), movePayload.toString()) as BaltazarAction
                                    } catch (e: Exception) {
                                        println("❌ Failed to parse BaltazarAction: ${e.message}")
                                        null
                                    }
                                }
                                "bridge" -> {
                                    try {
                                        serverGameJson.decodeFromString(GameAction.serializer(), movePayload.toString()) as BridgeAction
                                    } catch (e: Exception) {
                                        println("❌ Failed to parse BridgeAction: ${e.message}")
                                        null
                                    }
                                }
                                "checkers" -> {
                                    try {
                                        serverGameJson.decodeFromString(GameAction.serializer(), movePayload.toString()) as CheckersAction
                                    } catch (e: Exception) {
                                        println("❌ Failed to parse CheckersAction: ${e.message}")
                                        null
                                    }
                                }
                                "blokus" -> {
                                    try {
                                        serverGameJson.decodeFromString(GameAction.serializer(), movePayload.toString()) as BlokusAction
                                    } catch (e: Exception) {
                                        println("❌ Failed to parse BlokusAction: ${e.message}")
                                        null
                                    }
                                }
                                "yahtzee" -> {
                                    try {
                                        serverGameJson.decodeFromString(GameAction.serializer(), movePayload.toString()) as YahtzeeAction
                                    } catch (e: Exception) {
                                        println("❌ Failed to parse YahtzeeAction: ${e.message}")
                                        null
                                    }
                                }
                                "battleship" -> {
                                    try {
                                        serverGameJson.decodeFromString(GameAction.serializer(), movePayload.toString()) as BattleshipAction
                                    } catch (e: Exception) {
                                        println("❌ Failed to parse BattleshipAction: ${e.message}")
                                        null
                                    }
                                }
                                "match-monster" -> {
                                    try {
                                        serverGameJson.decodeFromString(GameAction.serializer(), movePayload.toString()) as MatchMonsterAction
                                    } catch (e: Exception) {
                                        println("❌ Failed to parse MatchMonsterAction: ${e.message}")
                                        null
                                    }
                                }
                                "soccer-striker" -> {
                                    try {
                                        serverGameJson.decodeFromString(GameAction.serializer(), movePayload.toString()) as SoccerStrikerAction
                                    } catch (e: Exception) {
                                        println("❌ Failed to parse SoccerStrikerAction: ${e.message}")
                                        null
                                    }
                                }
                                else -> null
                            }

                            if (action != null) {
                                println("🎮 Move from $username in game $gameId: ${action::class.simpleName}")
                                GameSessionManager.submitActionToSession(gameId, PlayerId(userId), action)
                            } else {
                                println("⚠️ Invalid move from $username in game $gameType")
                            }
                        }

                        is SurrenderMsg -> {
                            println("🏳️ Surrender from $username in game ${wsMsg.gameId}")
                            // TODO: implement surrender
                        }

                        is ChatMessageMsg -> {
                            println("💬 Chat from $username: ${wsMsg.content.body}")
                        }

                        else -> {
                            println("⚠️ Unknown message type: ${wsMsg?.type}")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            println("⚠️ خطا در WebSocket کاربر $username: ${e.message}")
            e.printStackTrace()
        } finally {
            println("❌ User $username disconnected")

            // اطلاع به GameSession در مورد قطعی کاربر
            if (currentGameId != null) {
                val sessionObj = GameSessionManager.getSession(currentGameId)
                sessionObj?.onPlayerDisconnect(PlayerId(userId))
            }

            // باطل کردن جلسه reconnect
            reconnectTokenBroker?.revokeAllUserSessions(userId)
            // حذف از کش‌ها
            PresenceCache.removeInGame(userId)
            ReconnectManager.remove(userId)
        }
    }
}