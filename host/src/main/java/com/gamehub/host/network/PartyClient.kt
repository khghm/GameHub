package com.gamehub.host.network

import com.gamehub.host.viewmodel.PartyMemberUI
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.delay
import kotlin.math.pow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

sealed class PartyEvent {
    data class PartyCreated(
        val partyId: String,
        val leaderId: String,
        val members: List<PartyMemberUI>
    ) : PartyEvent()
    data class PartyUpdated(
        val members: List<PartyMemberUI>,
        val state: String
    ) : PartyEvent()
    object PartyDeleted : PartyEvent()
    object Connected : PartyEvent()
    data class Error(val message: String) : PartyEvent()
}

class PartyClient {
    private val client = HttpClient(OkHttp) { install(WebSockets) }
    private var session: WebSocketSession? = null
    private val _events = MutableSharedFlow<PartyEvent>(replay = 5) // Increased replay to handle potential missed events during reconnect
    val events: SharedFlow<PartyEvent> = _events
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    suspend fun connect(url: String) {
        var attempt = 0
        while (scope.isActive) {
            try {
                session = client.webSocketSession(url)
                _events.emit(PartyEvent.Connected) // Indicate connection
                for (frame in session!!.incoming) {
                    if (frame is Frame.Text) {
                        val text = frame.readText()
                        val obj = Json.parseToJsonElement(text).jsonObject
                        val type = obj["type"]?.jsonPrimitive?.content
                        when (type) {
                            "party.created" -> {
                                val party = obj["party"]?.jsonObject ?: continue
                                val partyId = party["id"]?.jsonPrimitive?.content ?: ""
                                val leaderId = party["leaderId"]?.jsonPrimitive?.content ?: ""
                                val members = party["members"]?.jsonArray?.map { it.jsonObject } ?: emptyList()
                                val partyMembers = members.map { memberJson ->
                                    PartyMemberUI(
                                        userId = memberJson["userId"]?.jsonPrimitive?.content ?: "",
                                        username = memberJson["username"]?.jsonPrimitive?.content ?: "",
                                        isOnline = memberJson["isOnline"]?.jsonPrimitive?.boolean ?: false
                                    )
                                }
                                _events.emit(PartyEvent.PartyCreated(partyId, leaderId, partyMembers))
                            }
                            "party.updated" -> {
                                val party = obj["party"]?.jsonObject ?: continue
                                val members = party["members"]?.jsonArray?.map { it.jsonObject } ?: emptyList()
                                val partyMembers = members.map { memberJson ->
                                    PartyMemberUI(
                                        userId = memberJson["userId"]?.jsonPrimitive?.content ?: "",
                                        username = memberJson["username"]?.jsonPrimitive?.content ?: "",
                                        isOnline = memberJson["isOnline"]?.jsonPrimitive?.boolean ?: false
                                    )
                                }
                                val state = party["state"]?.jsonPrimitive?.content ?: ""
                                _events.emit(PartyEvent.PartyUpdated(partyMembers, state))
                            }
                            "party.deleted" -> _events.emit(PartyEvent.PartyDeleted)
                            "error" -> _events.emit(PartyEvent.Error(obj["message"]?.jsonPrimitive?.content ?: "Unknown error"))
                        }
                    }
                }
            } catch (e: CancellationException) {
                // Coroutine was cancelled, rethrow to propagate cancellation
                throw e
            } catch (e: Exception) {
                _events.emit(PartyEvent.Error("WebSocket Disconnected: ${e.message}. Reconnecting..."))
                delay(1000L * (2.0.pow(attempt).toLong().coerceAtMost(30))) // Exponential backoff up to 30 seconds
                attempt++
            }
        }
    }

    suspend fun send(message: String) {
        session?.send(Frame.Text(message))
    }

    suspend fun disconnect() {
        session?.close()
        client.close()
    }
}