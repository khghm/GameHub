package com.gamehub.server.modules

import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.*

class TournamentWebSocketHandler {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    suspend fun handle(session: DefaultWebSocketServerSession) {
        var subscribedTournamentId: String? = null
        try {
            for (frame in session.incoming) {
                if (frame is Frame.Text) {
                    val text = frame.readText()
                    println("Tournament WebSocket received: $text")
                    val json = try {
                        Json.parseToJsonElement(text).jsonObject
                    } catch (e: Exception) {
                        continue
                    }
                    val action = json["action"]?.jsonPrimitive?.content

                    when (action) {
                        "create" -> {
                            val gameId = json["gameType"]?.jsonPrimitive?.content ?: continue
                            val name = json["name"]?.jsonPrimitive?.content ?: "Tournament"
                            val format = json["format"]?.jsonPrimitive?.content ?: "single_elimination"
                            val maxParticipants = json["maxPlayers"]?.jsonPrimitive?.int ?: 8
                            val entryFeeCoins = json["entryFee"]?.jsonPrimitive?.long ?: 0L
                            val prizeDistributionJson = json["prizeDistribution"]?.jsonObject
                            val prizeDistribution = prizeDistributionJson?.mapValues { it.value.jsonPrimitive.content.toDouble() } ?: mapOf("1" to 1.0)
                            val allowedBands = json["allowedBands"]?.jsonArray?.map { it.jsonPrimitive.content } ?: listOf("A", "B")
                            val minLevel = json["minLevel"]?.jsonPrimitive?.int ?: 0
                            val minElo = json["minElo"]?.jsonPrimitive?.int ?: 0
                            val registrationStart = json["registrationStart"]?.jsonPrimitive?.long ?: System.currentTimeMillis()
                            val registrationEnd = json["registrationEnd"]?.jsonPrimitive?.long ?: (System.currentTimeMillis() + 86400000L)
                            val startTime = json["startTime"]?.jsonPrimitive?.long ?: (System.currentTimeMillis() + 86400000L)
                            val createdBy = json["createdBy"]?.jsonPrimitive?.content ?: "system"

                            val tournament = TournamentModule.createTournament(
                                gameId = gameId,
                                name = name,
                                format = format,
                                maxParticipants = maxParticipants,
                                entryFeeCoins = entryFeeCoins,
                                prizeDistribution = prizeDistribution,
                                allowedBehaviorBands = allowedBands,
                                minLevel = minLevel,
                                minElo = minElo,
                                registrationStart = registrationStart,
                                registrationEnd = registrationEnd,
                                startTime = startTime,
                                createdBy = createdBy
                            )
                            session.send(Frame.Text(TournamentModule.serializeTournament(tournament)))
                        }

                        "list" -> {
                            val tournaments = TournamentModule.getAllTournaments()
                            val listJson = tournaments.map { TournamentModule.serializeTournament(it) }.joinToString(",", "[", "]")
                            session.send(Frame.Text("""{"type":"list","tournaments":$listJson}"""))
                        }

                        "join" -> {
                            val tournamentId = json["tournamentId"]?.jsonPrimitive?.content ?: continue
                            val playerName = json["playerName"]?.jsonPrimitive?.content ?: continue
                            // در اینجا ثبت‌نام موقتاً فقط با یک کاربر ساده انجام می‌شود
                            val result = RegistrationResult(
                                success = true,
                                message = "Joined tournament (simulated)",
                                isWaitlist = false
                            )
                            session.send(Frame.Text(TournamentModule.serializeRegistrationResult(result)))
                            TournamentModule.broadcastTournamentUpdate(tournamentId)
                        }

                        "start" -> {
                            val tournamentId = json["tournamentId"]?.jsonPrimitive?.content ?: continue
                            val success = TournamentModule.startTournament(tournamentId)
                            session.send(Frame.Text("""{"success":$success}"""))
                        }

                        "subscribe" -> {
                            val tournamentId = json["tournamentId"]?.jsonPrimitive?.content ?: continue
                            subscribedTournamentId = tournamentId
                            TournamentModule.addSubscriber(tournamentId, session)
                            val tournament = TournamentModule.getTournament(tournamentId)
                            if (tournament != null) {
                                session.send(Frame.Text(TournamentModule.serializeTournament(tournament)))
                            }
                        }

                        "unsubscribe" -> {
                            subscribedTournamentId?.let {
                                TournamentModule.removeSubscriber(it, session)
                            }
                        }

                        "report_result" -> {
                            val tournamentId = json["tournamentId"]?.jsonPrimitive?.content ?: continue
                            val matchId = json["matchId"]?.jsonPrimitive?.content ?: continue
                            val winnerId = json["winnerId"]?.jsonPrimitive?.content ?: continue
                            val success = TournamentModule.reportMatchResult(tournamentId, matchId, winnerId)
                            session.send(Frame.Text("""{"success":$success}"""))
                        }
                        "start_match" -> {
                            val tournamentId = json["tournamentId"]?.jsonPrimitive?.content ?: continue
                            val matchId = json["matchId"]?.jsonPrimitive?.content ?: continue
                            val gameSessionId = TournamentModule.startMatch(tournamentId, matchId)
                            if (gameSessionId != null) {
                                session.send(Frame.Text("""{"type":"match_started","gameSessionId":"$gameSessionId"}"""))
                            } else {
                                session.send(Frame.Text("""{"type":"error","message":"Failed to start match"}"""))
                            }
                        }

                        "my_match" -> {
                            val tournamentId = json["tournamentId"]?.jsonPrimitive?.content ?: continue
                            val userId = json["userId"]?.jsonPrimitive?.content ?: continue
                            val matchInfo = TournamentModule.getMatchForPlayer(tournamentId, userId)
                            if (matchInfo != null) {
                                val (gameSessionId, gameType) = matchInfo
                                session.send(Frame.Text("""{"type":"match_info","gameSessionId":"$gameSessionId","gameType":"$gameType"}"""))
                            } else {
                                session.send(Frame.Text("""{"type":"match_info","gameSessionId":null}"""))
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            println("Tournament WebSocket error: ${e.message}")
        } finally {
            subscribedTournamentId?.let {
                TournamentModule.removeSubscriber(it, session)
            }
        }
    }
}