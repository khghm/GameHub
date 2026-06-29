package com.gamehub.host.network

import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class TournamentClient {
    private val client = HttpClient(OkHttp) { install(WebSockets) }
    private var session: WebSocketSession? = null
    private val _events = MutableSharedFlow<String>(replay = 10)
    val events: SharedFlow<String> = _events
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    suspend fun connect(url: String) {
        try {
            session = client.webSocketSession(url)
            scope.launch {
                for (frame in session!!.incoming) {
                    if (frame is Frame.Text) {
                        _events.emit(frame.readText())
                    }
                }
            }
        } catch (e: Exception) {
            _events.emit("""{"type":"error","message":"${e.message}"}""")
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