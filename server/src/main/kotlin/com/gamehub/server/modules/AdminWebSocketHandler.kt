package com.gamehub.server.modules

import com.gamehub.server.admin.AdminStatsService
import com.gamehub.server.security.JwtService
import com.gamehub.server.serverGameJson
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.serialization.encodeToString

class AdminWebSocketHandler(
    private val jwtService: JwtService,
    private val adminStatsService: AdminStatsService
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    suspend fun handle(session: DefaultWebSocketServerSession) {
        val token = session.call.request.queryParameters["token"] ?: ""
        val claims = jwtService.verifyToken(token)
        if (claims == null || claims.type != "admin") {
            session.close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Unauthorized"))
            return
        }
        println("✅ Admin WebSocket connected: ${claims.username}")

        val job = scope.launch {
            while (isActive) {
                delay(2000)
                try {
                    val stats = mapOf(
                        "onlineHubUsers" to adminStatsService.getOnlineHubUsersCount(),
                        "inGameUsers" to adminStatsService.getInGameUsersCount(),
                        "activeGames" to adminStatsService.getActiveGamesCount()
                    )
                    val msg = serverGameJson.encodeToString(stats)
                    session.send(Frame.Text(msg))
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    println("⚠️ Admin WebSocket send error: ${e.message}")
                    // ادامه می‌دهیم، اتصال را نمی‌بندیم
                }
            }
        }

        try {
            for (frame in session.incoming) {
                if (frame is Frame.Text && frame.readText() == "ping") {
                    session.send(Frame.Text("pong"))
                }
            }
        } catch (e: Exception) {
            println("⚠️ Admin WebSocket receive error: ${e.message}")
        } finally {
            job.cancel()
            println("❌ Admin WebSocket disconnected: ${claims.username}")
        }
    }
}