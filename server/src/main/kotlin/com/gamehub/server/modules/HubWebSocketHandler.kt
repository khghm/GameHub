// server/src/main/kotlin/com/gamehub/server/modules/HubWebSocketHandler.kt
package com.gamehub.server.modules

import com.gamehub.server.cache.PresenceCache
import com.gamehub.server.cache.SessionCache
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*

class HubWebSocketHandler(
    private val authModule: AuthModule,
    private val friendsModule: FriendsModule,
    private val partyModule: PartyModule
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    suspend fun handle(session: DefaultWebSocketServerSession, userId: String, username: String) {
        // Register session for party broadcasts
        HubWebSocketHandler.userSessions[userId] = session

        SessionCache.set(userId, "ws-hub-connected", 1800)
        // کاربر در هاب آنلاین است (در حال بازی نیست)
        PresenceCache.setOnlineHub(userId)

        session.send(Frame.Text("""{"type":"welcome","userId":"$userId","username":"$username","hub":true}"""))
        println("✅ کاربر $username به هاب وصل شد (Hub WebSocket)")

        try {
            for (frame in session.incoming) {
                if (frame is Frame.Text) {
                    val text = frame.readText()
                    println("📩 پیام هاب از $username: $text")
                    val json = try { Json.parseToJsonElement(text).jsonObject } catch (e: Exception) { null }
                    if (json == null) continue

                    val type = json["type"]?.jsonPrimitive?.content ?: continue

                    when (type) {
                        "party.create" -> {
                            val party = partyModule.createParty(userId, username)
                            session.send(Frame.Text("""{"type":"party.created","party":${Json.encodeToString(partyModule.toClientParty(party))}}"""))
                            println("🎉 Party created by $username, id: ${party.id}")
                        }
                        "party.join" -> {
                            val partyId = json["partyId"]?.jsonPrimitive?.content ?: continue
                            if (partyModule.addMember(partyId, userId, username)) {
                                val updatedParty = partyModule.getParty(partyId)
                                if (updatedParty != null) {
                                    broadcastToParty(partyModule.toClientParty(updatedParty), """{"type":"party.updated","party":${Json.encodeToString(partyModule.toClientParty(updatedParty))}}""")
                                }
                            } else {
                                session.send(Frame.Text("""{"error":"Could not join party"}"""))
                            }
                        }
                        "party.leave" -> {
                            val partyId = json["partyId"]?.jsonPrimitive?.content ?: continue
                            if (partyModule.removeMember(partyId, userId)) {
                                val updatedParty = partyModule.getParty(partyId)
                                if (updatedParty != null) {
                                    broadcastToParty(partyModule.toClientParty(updatedParty), """{"type":"party.updated","party":${Json.encodeToString(partyModule.toClientParty(updatedParty))}}""")
                                } else {
                                    session.send(Frame.Text("""{"type":"party.deleted"}"""))
                                }
                            }
                        }
                        "party.start" -> {
                            val partyId = json["partyId"]?.jsonPrimitive?.content ?: continue
                            val party = partyModule.getParty(partyId)
                            if (party != null && party.leaderId == userId) {
                                partyModule.setState(partyId, "in_queue")
                                broadcastToParty(party, """{"type":"party.started_search"}""")
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            println("⚠️ خطا در Hub WebSocket کاربر $username: ${e.message}")
        } finally {
            println("❌ کاربر $username از هاب قطع شد")
            HubWebSocketHandler.userSessions.remove(userId)
            PresenceCache.removeOnlineHub(userId)  // حذف از مجموعه آنلاین در هاب
        }
    }

    private suspend fun broadcastToParty(party: Party, message: String) {
        party.members.forEach { member ->
            HubWebSocketHandler.userSessions[member.userId]?.let { ws ->
                try { ws.send(Frame.Text(message)) } catch (_: Exception) {}
            }
        }
    }

    companion object {
        val userSessions = java.util.concurrent.ConcurrentHashMap<String, WebSocketSession>()
    }
}