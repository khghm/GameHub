package com.gamehub.server.integration

import com.gamehub.server.modules.TournamentModule
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * تست‌های تورنمنت:
 * - ایجاد تورنمنت
 * - ثبت‌نام کاربران
 * - شروع تورنمنت و تولید براکت
 * - گزارش نتیجه و پیشرفت براکت
 * - توزیع جوایز
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TournamentIntegrationTest {

    private lateinit var client: HttpClient
    private var authToken: String = ""
    private val tournamentModule = TournamentModule

    @BeforeAll
    fun setup() {
        client = HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    when (request.url.encodedPath) {
                        "/api/auth/guest" -> {
                            respond(content = """{"success":true,"token":"tournament-token","guestId":"tournament-user"}""", status = HttpStatusCode.OK)
                        }
                        "/api/tournaments" -> {
                            // شبیه‌سازی لیست تورنمنت‌ها
                            val body = """[{"id":"t1","name":"TestTournament","gameId":"tictactoe","status":"registration","maxParticipants":8,"currentParticipants":2,"entryFeeCoins":0}]"""
                            respond(content = body, status = HttpStatusCode.OK)
                        }
                        "/api/tournaments/t1/register" -> {
                            respond(content = """{"success":true,"message":"Registered"}""", status = HttpStatusCode.OK)
                        }
                        "/api/admin/tournaments/t1/distribute-prizes" -> {
                            respond(content = """{"awarded":true}""", status = HttpStatusCode.OK)
                        }
                        else -> error("Unhandled")
                    }
                }
            }
        }
        runBlocking {
            val resp = client.post("/api/auth/guest") { setBody("""{"deviceId":"tournament"}""") }
            val json = Json.parseToJsonElement(resp.bodyAsText()).jsonObject
            authToken = json["token"]?.jsonPrimitive?.content ?: ""
        }
    }

    @Test
    fun `create tournament via API (simulated)`() = runBlocking {
        // در MockEngine اندپوینت create نداریم، ولی می‌توانیم مستقیم تابع را صدا بزنیم
        val tournament = TournamentModule.createTournament(
            gameId = "tictactoe",
            name = "Integration Test Tournament",
            format = "single_elimination",
            maxParticipants = 4,
            entryFeeCoins = 0,
            prizeDistribution = mapOf("1" to 1.0),
            allowedBehaviorBands = listOf("A", "B"),
            minLevel = 0,
            minElo = 0,
            registrationStart = System.currentTimeMillis(),
            registrationEnd = System.currentTimeMillis() + 3600000,
            startTime = System.currentTimeMillis() + 7200000,
            createdBy = "test"
        )
        assertNotNull(tournament.id)
        assertEquals("tictactoe", tournament.gameId)
    }

    @Test
    fun `register user in tournament and generate bracket`() = runBlocking {
        val tournamentId = "t1"
        val userId = "player-001"
        val userLevel = 5
        val userElo = 1300
        val userBand = "A"

        // ثبت‌نام مستقیم (بدون WebSocket در تست)
        val result = TournamentModule.registerUser(
            tournamentId = tournamentId,
            userId = userId,
            userLevel = userLevel,
            userElo = userElo,
            userBehaviorBand = userBand,
            economyService = null
        )
        assertTrue(result.success)
        assertEquals("registered", result.message)

        // شروع تورنمنت (نیاز به حداقل ۲ شرکت‌کننده)
        val userId2 = "player-002"
        TournamentModule.registerUser(tournamentId, userId2, 5, 1250, "B", null)
        val started = TournamentModule.startTournament(tournamentId)
        assertTrue(started)

        val tournament = TournamentModule.getTournament(tournamentId)
        assertNotNull(tournament?.bracketData)
        assertTrue(tournament?.status == "in_progress")
    }

    @Test
    fun `report match result and advance bracket`() = runBlocking {
        val tournamentId = "t2"
        // ایجاد تورنمنت با ۲ بازیکن
        TournamentModule.createTournament(
            gameId = "tictactoe",
            name = "Bracket Test",
            format = "single_elimination",
            maxParticipants = 2,
            entryFeeCoins = 0,
            prizeDistribution = mapOf("1" to 1.0),
            allowedBehaviorBands = listOf("A"),
            minLevel = 0,
            minElo = 0,
            registrationStart = System.currentTimeMillis(),
            registrationEnd = System.currentTimeMillis() + 3600000,
            startTime = System.currentTimeMillis() + 7200000,
            createdBy = "test"
        )
        val userIdA = "playerA"
        val userIdB = "playerB"
        TournamentModule.registerUser(tournamentId, userIdA, 5, 1200, "A", null)
        TournamentModule.registerUser(tournamentId, userIdB, 5, 1200, "A", null)
        TournamentModule.startTournament(tournamentId)

        // پیدا کردن مسابقه اولین دور
        var match = TournamentModule.getNextPendingMatch(tournamentId)
        assertNotNull(match)
        assertTrue(match?.playerA == userIdA || match?.playerA == userIdB)
        assertTrue(match?.playerB == userIdA || match?.playerB == userIdB)

        // گزارش نتیجه (فرض برنده userIdA)
        val reported = TournamentModule.reportMatchResult(tournamentId, match!!.id, userIdA)
        assertTrue(reported)

        // بعد از گزارش، برنده نهایی باید userIdA باشد و وضعیت completed
        val tournament = TournamentModule.getTournament(tournamentId)
        assertEquals(userIdA, tournament?.winnerId)
        assertEquals("completed", tournament?.status)
    }

    @Test
    fun `get tournament list via API`() = runBlocking {
        val response = client.get("/api/tournaments") {
            header(HttpHeaders.Authorization, "Bearer $authToken")
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val jsonArray = Json.parseToJsonElement(response.bodyAsText()).jsonArray
        assertTrue(jsonArray.isNotEmpty())
        val first = jsonArray[0].jsonObject
        assertEquals("t1", first["id"]?.jsonPrimitive?.content)
    }
}