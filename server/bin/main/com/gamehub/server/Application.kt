// server/src/main/kotlin/com/gamehub/server/Application.kt
package com.gamehub.server

import at.favre.lib.crypto.bcrypt.BCrypt
import com.gamehub.server.admin.AdminService
import com.gamehub.server.admin.AdminStatsService
import com.gamehub.server.admin.AlertService
import com.gamehub.server.admin.GameConfigService
import com.gamehub.server.featureflags.CreateFeatureFlagRequest
import com.gamehub.server.featureflags.FeatureFlagService
import com.gamehub.server.featureflags.UpdateFeatureFlagRequest
import com.gamehub.server.admin.MetricsService
import com.gamehub.server.admin.RbacService
import com.gamehub.server.admin.ReportService
import com.gamehub.server.admin.dto.AdminUserListResponse
import com.gamehub.server.admin.repository.AdminRepository
import com.gamehub.server.anticheat.AntiCheatService
import com.gamehub.server.bot.BotRotationScheduler
import com.gamehub.server.bot.CentralBotManager
import com.gamehub.server.cache.CircuitBreakerCacheProvider
import com.gamehub.server.clan.ClanService
import com.gamehub.server.completion.MatchCompletionService
import com.gamehub.server.di.appModule
import com.gamehub.server.economy.EconomyService
import com.gamehub.server.economy.ShopService
import com.gamehub.server.matchmaking.MatchmakingService
import com.gamehub.server.modules.*
import com.gamehub.server.notifications.NotificationService
import com.gamehub.server.persistence.DatabaseFactory
import com.gamehub.server.rating.RatingService
import com.gamehub.server.repository.UserRepository
import com.gamehub.server.security.JwtService
import com.gamehub.server.settings.SettingsService
import com.gamehub.server.society.MembershipCondition
import com.gamehub.server.society.SocietyService
import com.gamehub.shared.engine.GameSnapshot
import com.gamehub.shared.game.GameConfigCreateRequest
import com.gamehub.shared.game.GameConfigUpdateRequest
import com.gamehub.shared.report.ReportDecision
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.metrics.micrometer.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.serialization.kotlinx.json.*
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics
import io.micrometer.core.instrument.binder.system.DiskSpaceMetrics
import io.micrometer.core.instrument.binder.system.ProcessorMetrics
import io.micrometer.core.instrument.binder.system.UptimeMetrics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.flywaydb.core.Flyway
import org.koin.ktor.ext.get
import org.koin.ktor.ext.inject
import java.io.File
import java.util.UUID
import io.ktor.server.plugins.origin
import io.ktor.http.HttpMethod
import io.ktor.http.HttpHeaders
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.websocket.CloseReason
import io.ktor.websocket.close
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import kotlinx.coroutines.runBlocking
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.application.*
import javax.net.ssl.SSLContext


// Metrics Registry
val appMicrometerRegistry: PrometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)


fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module).start(wait = true)
}

fun Application.module() {
    // Install Koin first!
    install(Koin) {
        slf4jLogger()
        modules(appModule)
    }

    // Get dependencies from Koin
    val config: com.typesafe.config.Config = get()
    val scope: CoroutineScope = get()
    val databaseConfig: com.gamehub.server.persistence.DatabaseConfig = get()
    DatabaseFactory.init(databaseConfig)

    Flyway.configure()
        .dataSource(databaseConfig.url, databaseConfig.user, databaseConfig.password)
        .load()
        .migrate()

    val cacheProvider: CircuitBreakerCacheProvider = get()
    val botStrategyRegistry: com.gamehub.server.bot.BotStrategyRegistry = get()
    GameSessionManager.cacheProvider = cacheProvider
    com.gamehub.server.cache.SessionCache.init(cacheProvider)
    com.gamehub.server.cache.PresenceCache.init(cacheProvider)

    val botProfileRepository: com.gamehub.server.bot.BotProfileRepository = get()
    val centralBotManager: CentralBotManager = get()
    MatchmakingRestHandler.centralBotManager = centralBotManager
    scope.launch {
        val games = listOf("tictactoe", "connectfour", "uno", "ludo", "monopoly", "chess", "farkle", "esmofamil", "backgammon", "abalone", "spades-baloot", "othello", "baltazar", "bridge", "checkers", "blokus", "yahtzee", "hex", "battleship", "match-monster", "soccer-striker")
        for (gameId in games) {
            val existingBots = botProfileRepository.findAllActive(gameId)
            if (existingBots.size < 10) {
                for (difficulty in 1..10) {
                    repeat(2) {
                        centralBotManager.createShadowBot(gameId, difficulty)
                    }
                }
            }
        }
        centralBotManager.initializePool()
        println("🤖 Bot pool initialized with shadow bots")
    }
    val botRotationScheduler: BotRotationScheduler = get()
    botRotationScheduler.start()
    scope.launch {
        try {
            centralBotManager.initializePool()
            println("🤖 Bot pool initialized successfully")
        } catch (e: Exception) {
            println("⚠️ Failed to initialize bot pool: ${e.message}")
        }
    }
    val ratingService: RatingService = get()
    val behaviorService: com.gamehub.server.behavior.BehaviorService = get()
    val shadowPoolManager: com.gamehub.server.anticheat.ShadowPoolManager = get()
    val matchmakingService: MatchmakingService = get()
    val userTutorialRepository: com.gamehub.server.repository.UserTutorialRepository = get()
    val matchHistoryRepo: com.gamehub.server.repository.MatchHistoryRepository = get()
    val matchCompletionService: MatchCompletionService = get()
    GameSessionManager.matchCompletionService = matchCompletionService

    val eventLogRepo: com.gamehub.server.repository.GameEventLogRepository = get()
    GameSessionManager.eventLogRepository = eventLogRepo
    MatchmakingRestHandler.service = matchmakingService
    MatchmakingRestHandler.ratingService = ratingService
    MatchmakingRestHandler.userTutorialRepository = userTutorialRepository

    val jwtService: JwtService = get()
    ReconnectManager.init(cacheProvider, jwtService)

    val userRepo: UserRepository = get()
    val authModule = AuthModule(userRepo, jwtService, cacheProvider)
    val friendshipRepo: com.gamehub.server.repository.FriendshipRepository = get()
    val friendsModule = FriendsModule(friendshipRepo)
    val partyRepository: com.gamehub.server.repository.PartyRepository = get()
    val partyModule = PartyModule(cacheProvider, partyRepository)
    // ========== Economy Endpoints ==========
    val idempotencyManager = com.gamehub.shared.idempotency.IdempotencyManager(cacheProvider)
    val economyService: EconomyService = get()
    val shopService: ShopService = get()
    // Anti-cheat components
    val antiCheatService: AntiCheatService = get()

    // Admin
    val adminRepository: AdminRepository = get()
    runBlocking {
        val existing = adminRepository.findByUsername("admin")
        if (existing == null) {
            val hash = BCrypt.withDefaults().hashToString(12, "admin123".toCharArray())
            adminRepository.createAdmin("admin", hash, "super_admin")
            println("✅ Admin created: username=admin, password=admin123")
        }
    }
    val adminService: AdminService = get()
    val adminStatsService: AdminStatsService = get()
    val notificationService: NotificationService = get()
    // ========== Report Management APIs ==========
    val reportService: ReportService = get()
    // ========== Game Configuration Management APIs ==========
    val gameConfigService: GameConfigService = get()
    val featureFlagService: FeatureFlagService = get()
    // ========== RBAC Management APIs ==========
    val rbacService: RbacService = get()
    // ========== Metrics API ==========
    val metricsService: MetricsService = get()
    val alertService: AlertService = get()
    alertService.start()
    val clanService: ClanService = get()
    val societyService: SocietyService = get()
    val reportRepository: com.gamehub.server.repository.ReportRepository = get()
    val reconnectTokenBroker: ReconnectTokenBroker = get()
    val rateLimiter: com.gamehub.server.security.RateLimiter = get()

    val json = Json { ignoreUnknownKeys = true; isLenient = true }

    // Install Micrometer Metrics
    install(MicrometerMetrics) {
        registry = appMicrometerRegistry
        meterBinders = listOf(
            ClassLoaderMetrics(),
            JvmMemoryMetrics(),
            JvmGcMetrics(),
            ProcessorMetrics(),
            JvmThreadMetrics(),
            UptimeMetrics(),
            DiskSpaceMetrics(File(System.getProperty("user.dir")))
        )
    }

    install(ContentNegotiation) { json(json) }
    install(CORS) {
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)
        allowCredentials = true
        anyHost()
    }
    install(WebSockets)
    install(io.ktor.server.plugins.statuspages.StatusPages) {
        exception<Throwable> { call, cause ->
            call.respondText(
                text = "Error: ${cause.message}\n${cause.stackTraceToString()}",
                contentType = ContentType.Text.Plain
            )
        }
    }

    // ========== 8. Routing ==========
    routing {
        // Prometheus Metrics Endpoint
        get("/metrics") {
            call.respondText(appMicrometerRegistry.scrape(), ContentType.Text.Plain)
        }

        // Replay Module (Event Sourcing Endpoints)
        val replayService: com.gamehub.server.replay.ReplayService = get()
        replayModule(replayService, authModule)


        // Match History Endpoint
        get("/api/matchhistory/{username}") {
            try {
                println("🌐 /api/matchhistory/{username} CALLED!")
                val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: ""
                println("🌐 Got token: ${if (token.isNotEmpty()) "****" else "empty"}")
                val user = authModule.validateToken(token)
                println("🌐 authModule.validateToken returned: $user")
                if (user == null) {
                    return@get call.respondText("{\"error\":\"Unauthorized\"}", ContentType.Application.Json)
                }
                val username = call.parameters["username"] ?: ""
                println("🌐 Requested username: $username, User from token: ${user.username}, ${user.id}")
                val history = MatchHistoryModule.getHistoryForUser(user.id)
                println("🌐 Returning ${history.size} matches!")
                val json = serverGameJson.encodeToString(history)
                println("🌐 JSON response: $json")
                call.respondText(json, ContentType.Application.Json)
            } catch (e: Exception) {
                println("🌐 ERROR IN API: ${e.message}")
                e.printStackTrace()
                call.respondText("{\"error\":\"Internal server error\",\"message\":\"${e.message}\"}", status = io.ktor.http.HttpStatusCode.InternalServerError, contentType = ContentType.Application.Json)
            }
        }

        val settingsService: SettingsService = get()

        get("/api/settings") {
            val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: ""
            val user = authModule.validateToken(token) ?: return@get call.respondText("{\"error\":\"Unauthorized\"}", ContentType.Application.Json)
            val settings = settingsService.getUserSettings(user.id)
            call.respondText(serverGameJson.encodeToString(settings), ContentType.Application.Json)
        }

        patch("/api/settings") {
            val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: ""
            val user = authModule.validateToken(token) ?: return@patch call.respondText("{\"error\":\"Unauthorized\"}", ContentType.Application.Json)
            val patch = call.receive<JsonObject>()
            val newSettings = settingsService.updateUserSettings(user.id, patch)
            call.respondText(serverGameJson.encodeToString(newSettings), ContentType.Application.Json)
        }

        post("/api/auth/guest") {
            val params = call.receive<Map<String, String>>()
            val deviceId = params["deviceId"] ?: ""
            val ip = call.request.headers["X-Forwarded-For"] ?: call.request.origin.remoteHost
            val result = authModule.guestLogin(deviceId, ip)
            call.respondText(result, ContentType.Application.Json)
        }

        post("/api/register") {
            val params = call.receive<Map<String, String>>()
            val username = params["username"] ?: ""
            val password = params["password"] ?: ""
            val displayName = params["displayName"] ?: username
            val result = authModule.register(username, password, displayName)
            call.respondText(result, ContentType.Application.Json)
        }

        post("/api/login") {
            val params = call.receive<Map<String, String>>()
            val username = params["username"] ?: ""
            val password = params["password"] ?: ""
            val result = authModule.login(username, password)
            call.respondText(result, ContentType.Application.Json)
        }

        get("/api/profile") {
            val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: ""
            val user = authModule.validateToken(token) ?: return@get call.respondText("{\"error\":\"Unauthorized\"}", ContentType.Application.Json)
            val profile = userRepo.findById(UUID.fromString(user.id))
            if (profile != null) {
                call.respondText(
                    """{"username":"${profile.username}","displayName":"${profile.displayName ?: profile.username}","avatar":"${profile.avatarUrl ?: ""}","wins":0,"losses":0,"draws":0}""",
                    ContentType.Application.Json
                )
            } else {
                call.respondText("{\"error\":\"User not found\"}", ContentType.Application.Json)
            }
        }

        post("/api/friends/request") {
            val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: ""
            val currentUser = authModule.validateToken(token) ?: return@post call.respondText("{\"error\":\"Unauthorized\"}", ContentType.Application.Json)
            val params = call.receive<Map<String, String>>()
            val friendUsername = params["friendUsername"] ?: ""
            val result = friendsModule.sendRequest(UUID.fromString(currentUser.id), friendUsername)
            call.respondText(json.encodeToString(result), ContentType.Application.Json)
        }

        post("/api/friends/accept") {
            val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: ""
            val currentUser = authModule.validateToken(token) ?: return@post call.respondText("{\"error\":\"Unauthorized\"}", ContentType.Application.Json)
            val params = call.receive<Map<String, String>>()
            val requestId = UUID.fromString(params["requestId"] ?: "")
            val result = friendsModule.acceptRequest(UUID.fromString(currentUser.id), requestId)
            call.respondText(json.encodeToString(result), ContentType.Application.Json)
        }

        post("/api/friends/reject") {
            val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: ""
            val currentUser = authModule.validateToken(token) ?: return@post call.respondText("{\"error\":\"Unauthorized\"}", ContentType.Application.Json)
            val params = call.receive<Map<String, String>>()
            val requestId = UUID.fromString(params["requestId"] ?: "")
            val result = friendsModule.rejectRequest(UUID.fromString(currentUser.id), requestId)
            call.respondText(json.encodeToString(result), ContentType.Application.Json)
        }

        get("/api/friends") {
            val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: ""
            val currentUser = authModule.validateToken(token) ?: return@get call.respondText("{\"error\":\"Unauthorized\"}", ContentType.Application.Json)
            val friends = friendsModule.getFriends(UUID.fromString(currentUser.id))
            val friendsJson = json.encodeToString(friends)
            call.respondText("""{"friends":$friendsJson}""", ContentType.Application.Json)
        }

        get("/api/friends/pending") {
            val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: ""
            val currentUser = authModule.validateToken(token) ?: return@get call.respondText("{\"error\":\"Unauthorized\"}", ContentType.Application.Json)
            val pending = friendsModule.getPendingRequests(UUID.fromString(currentUser.id))
            call.respondText("""{"pending":${json.encodeToString(pending)}}""", ContentType.Application.Json)
        }

        delete("/api/friends/remove") {
            val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: ""
            val currentUser = authModule.validateToken(token) ?: return@delete call.respondText("{\"error\":\"Unauthorized\"}", ContentType.Application.Json)
            val params = call.receive<Map<String, String>>()
            val friendUsername = params["friendUsername"] ?: ""

            val friendUser = userRepo.findByUsername(friendUsername)
            if (friendUser == null) {
                return@delete call.respondText("{\"success\":false,\"message\":\"Friend not found\"}", ContentType.Application.Json)
            }

            val result = friendsModule.unfriend(UUID.fromString(currentUser.id), friendUser.id!!)
            call.respondText(json.encodeToString(result), ContentType.Application.Json)
        }

        post("/api/matchmaking/join") {
            println("📡 Matchmaking request received!")
            val params = call.receive<Map<String, String>>()
            val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: ""
            val user = authModule.validateToken(token) ?: return@post call.respondText("""{"error":"Unauthorized"}""", ContentType.Application.Json)
            val gameType = params["gameType"] ?: "tictactoe"
            val mode = params["mode"] ?: "casual"
            val result = MatchmakingRestHandler.joinQueue(user.id, user.username, gameType, mode)
            call.respondText(result, ContentType.Application.Json)
        }

        // ======================== Admin API ========================
        post("/api/admin/login") {
            val params = call.receive<Map<String, String>>()
            val username = params["username"] ?: ""
            val password = params["password"] ?: ""
            val ip = call.request.headers["X-Forwarded-For"] ?: call.request.origin.remoteHost
            val userAgent = call.request.headers["User-Agent"] ?: ""
            val admin = adminService.login(username, password, ip, userAgent)
            if (admin != null) {
                val ipHash = jwtService.hashIp(ip)
                val token = jwtService.createAdminToken(admin.id.toString(), admin.username, admin.role, ipHash)
                call.respondText("""{"success":true,"token":"$token","role":"${admin.role}"}""", ContentType.Application.Json)
            } else {
                call.respondText("""{"success":false,"message":"Invalid credentials"}""", ContentType.Application.Json)
            }
        }

        get("/api/admin/users") {
            val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: ""
            val claims = jwtService.verifyToken(token)
            if (claims == null || claims.type != "admin") {
                call.respondText("""{"error":"Unauthorized"}""", ContentType.Application.Json)
                return@get
            }
            val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
            val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 20
            val search = call.request.queryParameters["search"]
            val users = adminService.getUsers(page, pageSize, search)
            val total = adminService.getTotalUsersCount(search)
            val response = AdminUserListResponse(users = users, total = total)
            call.respondText(serverGameJson.encodeToString(response), ContentType.Application.Json)
        }

        post("/api/admin/users/ban") {
            val params = call.receive<Map<String, String>>()
            val targetUserId = params["userId"] ?: ""
            val reason = params["reason"] ?: ""
            val durationHours = params["durationHours"]?.toIntOrNull()
            val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: ""
            val claims = jwtService.verifyToken(token)
            if (claims == null || claims.type != "admin") {
                call.respondText("""{"error":"Unauthorized"}""", ContentType.Application.Json)
                return@post
            }
            val result = adminService.banUser(UUID.fromString(claims.userId), claims.username, targetUserId, reason, durationHours)
            call.respondText("""{"success":$result}""", ContentType.Application.Json)
        }

        post("/api/admin/users/unban") {
            val params = call.receive<Map<String, String>>()
            val targetUserId = params["userId"] ?: ""
            val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: ""
            val claims = jwtService.verifyToken(token)
            if (claims == null || claims.type != "admin") {
                call.respondText("""{"error":"Unauthorized"}""", ContentType.Application.Json)
                return@post
            }
            val result = adminService.unbanUser(UUID.fromString(claims.userId), claims.username, targetUserId)
            call.respondText("""{"success":$result}""", ContentType.Application.Json)
        }

        get("/api/admin/audit") {
            val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: ""
            val claims = jwtService.verifyToken(token)
            if (claims == null || claims.type != "admin") {
                call.respondText("""{"error":"Unauthorized"}""", ContentType.Application.Json)
                return@get
            }
            val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 100
            val offset = call.request.queryParameters["offset"]?.toIntOrNull() ?: 0
            val logs = adminService.getAuditLog(limit, offset)
            val json = serverGameJson.encodeToString(logs)
            call.respondText(json, ContentType.Application.Json)
        }

        // ========== Feature Flags Management APIs ==========
        // Get all feature flags
        get("/api/admin/feature-flags") {
            val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: ""
            val claims = jwtService.verifyToken(token)
            if (claims == null || claims.type != "admin") {
                call.respondText("""{"error":"Unauthorized"}""", ContentType.Application.Json)
                return@get
            }
            val environment = call.request.queryParameters["environment"]
            val flags = featureFlagService.getAllFeatureFlags(environment)
            call.respondText(serverGameJson.encodeToString(flags), ContentType.Application.Json)
        }

        // Create a new feature flag
        post("/api/admin/feature-flags") {
            val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: ""
            val claims = jwtService.verifyToken(token)
            if (claims == null || claims.type != "admin") {
                call.respondText("""{"error":"Unauthorized"}""", ContentType.Application.Json)
                return@post
            }
            val request = call.receive<CreateFeatureFlagRequest>()
            try {
                val flag = featureFlagService.createFeatureFlag(request)
                call.respondText(serverGameJson.encodeToString(flag), ContentType.Application.Json)
            } catch (e: IllegalArgumentException) {
                call.respondText("{\"error\":\"${e.message}\"}", status = HttpStatusCode.BadRequest, contentType = ContentType.Application.Json)
            }
        }

        // Update a feature flag
        patch("/api/admin/feature-flags/{id}") {
            val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: ""
            val claims = jwtService.verifyToken(token)
            if (claims == null || claims.type != "admin") {
                call.respondText("""{"error":"Unauthorized"}""", ContentType.Application.Json)
                return@patch
            }
            val id = call.parameters["id"] ?: return@patch call.respondText("""{"error":"Missing id"}""", status = HttpStatusCode.BadRequest, contentType = ContentType.Application.Json)
            val request = call.receive<UpdateFeatureFlagRequest>()
            val flag = featureFlagService.updateFeatureFlag(UUID.fromString(id), request)
            if (flag != null) {
                call.respondText(serverGameJson.encodeToString(flag), ContentType.Application.Json)
            } else {
                call.respondText("""{"error":"Flag not found"}""", status = HttpStatusCode.NotFound, contentType = ContentType.Application.Json)
            }
        }

        // Delete a feature flag
        delete("/api/admin/feature-flags/{id}") {
            val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: ""
            val claims = jwtService.verifyToken(token)
            if (claims == null || claims.type != "admin") {
                call.respondText("""{"error":"Unauthorized"}""", ContentType.Application.Json)
                return@delete
            }
            val id = call.parameters["id"] ?: return@delete call.respondText("""{"error":"Missing id"}""", status = HttpStatusCode.BadRequest, contentType = ContentType.Application.Json)
            val success = featureFlagService.deleteFeatureFlag(UUID.fromString(id))
            call.respondText("""{"success":$success}""", ContentType.Application.Json)
        }

        // Public endpoint to check if a feature flag is enabled (for clients)
        get("/api/feature-flags/{key}/enabled") {
            val key = call.parameters["key"] ?: return@get call.respondText("""{"error":"Missing key"}""", status = HttpStatusCode.BadRequest, contentType = ContentType.Application.Json)
            val environment = call.request.queryParameters["environment"]
            val enabled = featureFlagService.isFeatureEnabled(key, environment)
            call.respondText("""{"enabled":$enabled}""", ContentType.Application.Json)
        }
        // ========== Admin Dashboard APIs ==========
//        get("/api/admin/dashboard/stats") {
//            val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: ""
//            val claims = jwtService.verifyToken(token)
//            if (claims == null || claims.type != "admin") {
//                call.respondText("""{"error":"Unauthorized"}""", ContentType.Application.Json)
//                return@get
//            }
//            val online = adminStatsService.getOnlineUsersCount()
//            val activeGames = adminStatsService.getActiveGamesCount()
//            val totalUsers = adminStatsService.getTotalUsersCount()
//            val newUsersToday = adminStatsService.getNewUsersToday()
//            val gamesToday = adminStatsService.getGamesPlayedToday()
//            val totalCoins = adminStatsService.getTotalCoinsInCirculation()
//            val queueSizes = adminStatsService.getQueueSizes()
//            val health = adminStatsService.getServerHealth()
//            val response = mapOf(
//                "onlineUsers" to online,
//                "activeGames" to activeGames,
//                "totalUsers" to totalUsers,
//                "newUsersToday" to newUsersToday,
//                "gamesToday" to gamesToday,
//                "totalCoins" to totalCoins,
//                "queueSizes" to queueSizes,
//                "health" to health
//            )
//            call.respondText(serverGameJson.encodeToString(response), ContentType.Application.Json)
//        }
        get("/api/admin/stats") {
            val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: ""
            val claims = jwtService.verifyToken(token)
            if (claims == null || claims.type != "admin") {
                call.respondText("""{"error":"Unauthorized"}""", ContentType.Application.Json)
                return@get
            }
            val onlineHub = adminStatsService.getOnlineHubUsersCount()
            val inGame = adminStatsService.getInGameUsersCount()
            val activeGames = adminStatsService.getActiveGamesCount()
            val finishedGamesToday = adminStatsService.getFinishedGamesToday()
            val totalUsers = adminStatsService.getTotalUsersCount()
            val newUsersToday = adminStatsService.getNewUsersToday()
            val totalCoins = adminStatsService.getTotalCoinsInCirculation()
            val health = adminStatsService.getServerHealth()
            val response = mapOf(
                "onlineHubUsers" to onlineHub,
                "inGameUsers" to inGame,
                "activeGames" to activeGames,
                "finishedGamesToday" to finishedGamesToday,
                "totalUsers" to totalUsers,
                "newUsersToday" to newUsersToday,
                "totalCoins" to totalCoins,
                "health" to health
            )
            call.respondText(serverGameJson.encodeToString(response), ContentType.Application.Json)
        }
        // Get current admin's permissions
        get("/api/admin/my-permissions") {
            val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: ""
            val claims = jwtService.verifyToken(token)
            if (claims == null || claims.type != "admin") {
                call.respondText("""{"error":"Unauthorized"}""", ContentType.Application.Json)
                return@get
            }
            val adminId = UUID.fromString(claims.userId)
            val permissions = rbacService.getUserPermissions(adminId)
            call.respondText(serverGameJson.encodeToString(permissions), ContentType.Application.Json)
        }

// Get permissions for a specific admin
        get("/api/admin/users/{userId}/permissions") {
            val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: ""
            val claims = jwtService.verifyToken(token)
            if (claims == null || claims.type != "admin") {
                call.respondText("""{"error":"Unauthorized"}""", ContentType.Application.Json)
                return@get
            }
            val targetUserId = call.parameters["userId"] ?: return@get call.respondText("""{"error":"Missing userId"}""")
            // فقط ادمین با دسترسی admins:view می‌تواند ببیند
            if (!rbacService.userHasPermission(UUID.fromString(claims.userId), "admins:view")) {
                call.respondText("""{"error":"Forbidden"}""", ContentType.Application.Json)
                return@get
            }
            val perms = rbacService.getUserPermissions(UUID.fromString(targetUserId))
            call.respondText(serverGameJson.encodeToString(perms), ContentType.Application.Json)
        }


// Get all roles (static list for frontend)
        get("/api/admin/roles") {
            val roles = listOf(
                mapOf("name" to "super_admin", "description" to "Full access"),
                mapOf("name" to "admin", "description" to "Administrative access"),
                mapOf("name" to "moderator", "description" to "Manage users and reports"),
                mapOf("name" to "support", "description" to "View users and reports")
            )
            call.respondText(serverGameJson.encodeToString(roles), ContentType.Application.Json)
        }

// Get all permissions (static list)
        get("/api/admin/permissions") {
            val allPermissions = rbacService.getAllPermissionsList()
            call.respondText(serverGameJson.encodeToString(allPermissions), ContentType.Application.Json)
        }

        get("/api/admin/metrics") {
            val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: ""
            val claims = jwtService.verifyToken(token)
            if (claims == null || claims.type != "admin") {
                call.respondText("""{"error":"Unauthorized"}""", ContentType.Application.Json)
                return@get
            }
            val metrics = metricsService.getCurrentMetrics()
            call.respondText(serverGameJson.encodeToString(metrics), ContentType.Application.Json)
        }

        // اندپوینت دریافت اعلان‌های جدید (Polling)
        get("/api/notifications/poll") {
            val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: ""
            val user = authModule.validateToken(token) ?: return@get call.respondText("{\"error\":\"Unauthorized\"}", ContentType.Application.Json)
            val since = call.request.queryParameters["since"]?.toLongOrNull() ?: 0L
            val notifications = notificationService.getNotificationsSince(user.id, since)
            call.respondText(serverGameJson.encodeToString(notifications), ContentType.Application.Json)
        }

// اندپوینت علامت زدن خوانده‌شده (اختیاری)
        post("/api/notifications/read/{id}") {
            val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: ""
            val user = authModule.validateToken(token) ?: return@post call.respondText("{\"error\":\"Unauthorized\"}", ContentType.Application.Json)
            val id = call.parameters["id"] ?: return@post call.respondText("{\"error\":\"Missing id\"}", ContentType.Application.Json)
            notificationService.markAsRead(user.id, id)
            call.respondText("{\"success\":true}", ContentType.Application.Json)
        }

// اندپوینت دریافت تعداد اعلان‌های خوانده‌نشده (برای Badge)
        get("/api/notifications/unread") {
            val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: ""
            val user = authModule.validateToken(token) ?: return@get call.respondText("{\"error\":\"Unauthorized\"}", ContentType.Application.Json)
            val count = notificationService.getUnreadCount(user.id)
            call.respondText("{\"count\":$count}", ContentType.Application.Json)
        }
        post("/api/auth/attest/request") {
            val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: ""
            val user = authModule.validateToken(token) ?: return@post call.respondText("{\"error\":\"Unauthorized\"}", ContentType.Application.Json)
            val challengeId = authModule.requestAttestationChallenge(user.id)
            call.respondText("""{"challengeId":"$challengeId"}""", ContentType.Application.Json)
        }

        post("/api/auth/attest/verify") {
            val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: ""
            val user = authModule.validateToken(token) ?: return@post call.respondText("{\"error\":\"Unauthorized\"}", ContentType.Application.Json)
            val params = call.receive<Map<String, String>>()
            val challengeId = params["challengeId"] ?: return@post call.respondText("{\"error\":\"Missing challengeId\"}", ContentType.Application.Json)
            val signature = params["signature"] ?: return@post call.respondText("{\"error\":\"Missing signature\"}", ContentType.Application.Json)
            val publicKey = params["publicKey"] ?: return@post call.respondText("{\"error\":\"Missing publicKey\"}", ContentType.Application.Json)
            val verified = authModule.verifyAttestation(user.id, challengeId, signature, publicKey)
            if (verified) {
                call.respondText("{\"success\":true,\"message\":\"Device attested\"}", ContentType.Application.Json)
            } else {
                call.respondText("{\"success\":false,\"message\":\"Verification failed\"}", ContentType.Application.Json)
            }
        }
        get("/api/user/rating") {
            val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: ""
            val user = authModule.validateToken(token) ?: return@get call.respondText("{\"error\":\"Unauthorized\"}", ContentType.Application.Json)
            val gameId = call.request.queryParameters["gameId"] ?: "tictactoe"
            val rating = ratingService.getRating(user.id, gameId)
            call.respondText(serverGameJson.encodeToString(rating), ContentType.Application.Json)
        }

        get("/api/user/behavior") {
            val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: ""
            val user = authModule.validateToken(token) ?: return@get call.respondText("{\"error\":\"Unauthorized\"}", ContentType.Application.Json)
            val behavior = behaviorService.getBehavior(user.id)
            call.respondText(serverGameJson.encodeToString(behavior), ContentType.Application.Json)
        }
        // اندپوینت تست ضد تقلب (فقط برای توسعه)
        post("/api/test/cheat") {
            val params = call.receive<Map<String, String>>()
            val userId = params["userId"] ?: return@post call.respondText("{\"error\":\"Missing userId\"}", ContentType.Application.Json)
            val cheatType = params["cheatType"] ?: return@post call.respondText("{\"error\":\"Missing cheatType\"}", ContentType.Application.Json)
            val gameId = params["gameId"] ?: "test_game"
            val matchId = params["matchId"] ?: "test_match"

            when (cheatType) {
                "speed" -> {
                    // شبیه‌سازی اسپید هک: زمان حرکت 10 میلی‌ثانیه (خیلی سریع)
                    val clientSendTime = System.currentTimeMillis()
                    val serverRecvTime = clientSendTime + 10 // فقط 10 میلی‌ثانیه اختلاف
                    antiCheatService.checkMove(
                        userId = userId,
                        sessionId = "test_session",
                        gameId = gameId,
                        matchId = matchId,
                        moveType = "normal",
                        clientSendTime = clientSendTime,
                        serverRecvTime = serverRecvTime,
                        signature = "",
                        reactionMs = 5
                    )
                }
                "lag" -> {
                    // شبیه‌سازی لگ سوئیچ: ارسال 4 RTT با نوسان زیاد
                    val rtts = listOf(50L, 300L, 50L, 350L, 50L)
                    for (rtt in rtts) {
                        antiCheatService.checkLagSwitch(userId, matchId, rtt)
                    }
                }
                "macro" -> {
                    // شبیه‌سازی ماکرو: 10 واکنش با زمان ثابت 50 میلی‌ثانیه
                    repeat(10) {
                        antiCheatService.checkMove(
                            userId = userId,
                            sessionId = "test_session",
                            gameId = gameId,
                            matchId = matchId,
                            moveType = "normal",
                            clientSendTime = System.currentTimeMillis(),
                            serverRecvTime = System.currentTimeMillis(),
                            signature = "",
                            reactionMs = 50
                        )
                    }
                }
                "collusion" -> {
                    // شبیه‌سازی تبانی: دو کاربر با نرخ برد بالا
                    val otherUserId = params["otherUserId"] ?: return@post call.respondText("{\"error\":\"Missing otherUserId\"}")
                    // ابتدا چند بازی ثبت می‌کنیم (در دیتابیس)
                    for (i in 1..10) {
                        // شبیه‌سازی برد متوالی userId مقابل otherUserId
                        // (در پیاده‌سازی واقعی نیاز به ذخیره در match_history دارد)
                    }
                    antiCheatService.checkCollusion(userId, otherUserId, gameId, matchId)
                }
                else -> return@post call.respondText("{\"error\":\"Unknown cheatType\"}")
            }
            call.respondText("{\"success\":true,\"message\":\"Cheat simulation triggered\"}", ContentType.Application.Json)
        }
        // ========== Economy Endpoints ==========
        get("/api/wallet") {
            val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: ""
            val user = authModule.validateToken(token) ?: return@get call.respondText("{\"error\":\"Unauthorized\"}", ContentType.Application.Json)
            val wallet = economyService.getWallet(user.id)
            call.respondText(serverGameJson.encodeToString(wallet), ContentType.Application.Json)
        }

        // ========== Shop Endpoints (با استفاده از ShopService) ==========

        post("/api/shop/buy") {
            val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: ""
            val user = authModule.validateToken(token) ?: return@post call.respondText("{\"error\":\"Unauthorized\"}", ContentType.Application.Json)
            val params = call.receive<Map<String, String>>()
            val itemId = params["itemId"] ?: return@post call.respondText("{\"error\":\"Missing itemId\"}", ContentType.Application.Json)
            val quantity = params["quantity"]?.toIntOrNull() ?: 1
            val idempotencyKey = params["idempotencyKey"] ?: UUID.randomUUID().toString()
            try {
                val result = shopService.purchaseItem(user.id, itemId, quantity, idempotencyKey)
                call.respondText(serverGameJson.encodeToString(result), ContentType.Application.Json)
            } catch (e: IllegalArgumentException) {
                call.respondText(
                    text = "{\"error\":\"${e.message}\"}",
                    status = HttpStatusCode.BadRequest,
                    contentType = ContentType.Application.Json
                )
            } catch (e: IllegalStateException) {
                call.respondText(
                    text = "{\"error\":\"${e.message}\"}",
                    status = HttpStatusCode.BadRequest,
                    contentType = ContentType.Application.Json
                )
            }
        }

        post("/api/refund") {
            val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: ""
            val user = authModule.validateToken(token) ?: return@post call.respondText("{\"error\":\"Unauthorized\"}", ContentType.Application.Json)
            val params = call.receive<Map<String, String>>()
            val purchaseId = params["purchaseId"] ?: return@post call.respondText("{\"error\":\"Missing purchaseId\"}", ContentType.Application.Json)
            val idempotencyKey = params["idempotencyKey"] ?: UUID.randomUUID().toString()
            try {
                val result = shopService.refundPurchase(user.id, purchaseId, idempotencyKey)
                call.respondText(serverGameJson.encodeToString(result), ContentType.Application.Json)
            } catch (e: IllegalStateException) {
                call.respondText(
                    text = "{\"error\":\"${e.message}\"}",
                    status = HttpStatusCode.BadRequest,
                    contentType = ContentType.Application.Json
                )
            }
        }

        post("/api/gift/coin") {
            val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: ""
            val user = authModule.validateToken(token) ?: return@post call.respondText("{\"error\":\"Unauthorized\"}", ContentType.Application.Json)
            val params = call.receive<Map<String, String>>()
            val toUserId = params["toUserId"] ?: return@post call.respondText("{\"error\":\"Missing toUserId\"}", ContentType.Application.Json)
            val amount = params["amount"]?.toLongOrNull() ?: return@post call.respondText("{\"error\":\"Missing amount\"}", ContentType.Application.Json)
            val message = params["message"]
            val idempotencyKey = params["idempotencyKey"] ?: UUID.randomUUID().toString()
            try {
                val result = economyService.giftCoins(user.id, toUserId, amount, message, idempotencyKey)
                call.respondText(serverGameJson.encodeToString(result), ContentType.Application.Json)
            } catch (e: IllegalArgumentException) {
                call.respondText(
                    text = "{\"error\":\"${e.message}\"}",
                    status = HttpStatusCode.BadRequest,
                    contentType = ContentType.Application.Json
                )
            } catch (e: IllegalStateException) {
                call.respondText(
                    text = "{\"error\":\"${e.message}\"}",
                    status = HttpStatusCode.BadRequest,
                    contentType = ContentType.Application.Json
                )
            }
        }
        get("/api/games/active") {
            val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: ""
            val user = authModule.validateToken(token) ?: return@get call.respondText("{\"error\":\"Unauthorized\"}", ContentType.Application.Json)
            val activeGamesSet = cacheProvider.smembers("user:active_games:${user.id}")
            val gameInfos = mutableListOf<Map<String, Any>>()
            for (gameId in activeGamesSet) {
                val snapshotJson = cacheProvider.get("snapshot:$gameId")
                if (snapshotJson != null) {
                    val snapshot = serverGameJson.decodeFromString(GameSnapshot.serializer(), snapshotJson)
                    // استخراج اطلاعات خلاصه مانند آخرین نوبت، زمان آخرین فعالیت و ...
                    gameInfos.add(mapOf(
                        "gameId" to gameId,
                        "gameType" to snapshot.gameType,
                        "players" to snapshot.players,
                        "lastUpdate" to snapshot.version // می‌توان timestamp جدا ذخیره کرد
                    ))
                }
            }
            call.respondText(serverGameJson.encodeToString(gameInfos), ContentType.Application.Json)
        }
        get("/api/admin/reports") {
            val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: ""
            val claims = jwtService.verifyToken(token)
            if (claims == null || claims.type != "admin") {
                call.respondText("""{"error":"Unauthorized"}""", ContentType.Application.Json)
                return@get
            }
            val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
            val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 20
            val status = call.request.queryParameters["status"]
            val violationType = call.request.queryParameters["violationType"]
            val fromDate = call.request.queryParameters["fromDate"]?.toLongOrNull()
            val toDate = call.request.queryParameters["toDate"]?.toLongOrNull()
            val reporterId = call.request.queryParameters["reporterId"]
            val reportedUserId = call.request.queryParameters["reportedUserId"]
            val minReporterScore = call.request.queryParameters["minReporterScore"]?.toIntOrNull()

            val offset = (page - 1) * pageSize
            val reports = reportRepository.getReports(status, violationType, fromDate, toDate, reporterId, reportedUserId, minReporterScore, offset, pageSize)
            val total = reportRepository.getReportsCount(status, violationType, fromDate, toDate, reporterId, reportedUserId, minReporterScore)
            val response = mapOf("reports" to reports, "total" to total)
            call.respondText(serverGameJson.encodeToString(response), ContentType.Application.Json)
        }

        post("/api/admin/reports/{id}/review") {
            val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: ""
            val claims = jwtService.verifyToken(token)
            if (claims == null || claims.type != "admin") {
                call.respondText("""{"error":"Unauthorized"}""", ContentType.Application.Json)
                return@post
            }
            val reportId = call.parameters["id"]?.toLongOrNull() ?: return@post call.respondText("""{"error":"Invalid report id"}""")
            val params = call.receive<Map<String, Any>>()
            val decisionStr = params["decision"] as? String ?: return@post call.respondText("""{"error":"Missing decision"}""")
            val reason = params["reason"] as? String ?: ""
            val durationHours = (params["durationHours"] as? Number)?.toInt()
            val decision = try { ReportDecision.valueOf(decisionStr) } catch (e: Exception) { ReportDecision.none }
            val success = reportService.applyDecision(reportId, decision, reason, claims.userId, durationHours)
            call.respondText("""{"success":$success}""", ContentType.Application.Json)
        }

        // Get all configs (optionally filtered by gameId)
        get("/api/admin/game-configs") {
            val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: ""
            val claims = jwtService.verifyToken(token)
            if (claims == null || claims.type != "admin") {
                call.respondText("""{"error":"Unauthorized"}""", ContentType.Application.Json)
                return@get
            }
            val gameId = call.request.queryParameters["gameId"]
            val configs = gameConfigService.getAllConfigs(gameId)
            call.respondText(serverGameJson.encodeToString(configs), ContentType.Application.Json)
        }

// Get specific active config for a game mode
        get("/api/admin/game-configs/{gameId}/{mode}") {
            val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: ""
            val claims = jwtService.verifyToken(token)
            if (claims == null || claims.type != "admin") {
                call.respondText("""{"error":"Unauthorized"}""", ContentType.Application.Json)
                return@get
            }
            val gameId = call.parameters["gameId"] ?: return@get call.respondText("""{"error":"Missing gameId"}""")
            val mode = call.parameters["mode"] ?: return@get call.respondText("""{"error":"Missing mode"}""")
            val config = gameConfigService.getConfig(gameId, mode)
            if (config != null) {
                call.respondText(serverGameJson.encodeToString(config), ContentType.Application.Json)
            } else {
                call.respondText("""{"error":"Config not found"}""", ContentType.Application.Json)
            }
        }

// Create or update a config (if existing, will deactivate old and create new)
        post("/api/admin/game-configs") {
            val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: ""
            val claims = jwtService.verifyToken(token)
            if (claims == null || claims.type != "admin") {
                call.respondText("""{"error":"Unauthorized"}""", ContentType.Application.Json)
                return@post
            }
            val request = call.receive<GameConfigCreateRequest>()
            val config = gameConfigService.createConfig(
                gameId = request.gameId,
                mode = request.mode,
                config = request.config,
                createdBy = claims.username
            )
            call.respondText(serverGameJson.encodeToString(config), ContentType.Application.Json)
        }

// Update an existing config (increments version)
        put("/api/admin/game-configs/{id}") {
            val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: ""
            val claims = jwtService.verifyToken(token)
            if (claims == null || claims.type != "admin") {
                call.respondText("""{"error":"Unauthorized"}""", ContentType.Application.Json)
                return@put
            }
            val id = call.parameters["id"]?.toLongOrNull() ?: return@put call.respondText("""{"error":"Invalid id"}""")
            val request = call.receive<GameConfigUpdateRequest>()
            val success = gameConfigService.updateConfig(id, request.config, request.version, claims.username)
            if (success) {
                call.respondText("""{"success":true}""", ContentType.Application.Json)
            } else {
                call.respondText("""{"success":false,"error":"Update failed"}""", ContentType.Application.Json)
            }
        }

// Get version history for a game mode
        get("/api/admin/game-configs/{gameId}/{mode}/history") {
            val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: ""
            val claims = jwtService.verifyToken(token)
            if (claims == null || claims.type != "admin") {
                call.respondText("""{"error":"Unauthorized"}""", ContentType.Application.Json)
                return@get
            }
            val gameId = call.parameters["gameId"] ?: return@get call.respondText("""{"error":"Missing gameId"}""")
            val mode = call.parameters["mode"] ?: return@get call.respondText("""{"error":"Missing mode"}""")
            val history = gameConfigService.getConfigHistory(gameId, mode)
            call.respondText(serverGameJson.encodeToString(history), ContentType.Application.Json)
        }

        // ========== مدیریت ضد تقلب (Anti-Cheat Management) ==========

// دریافت لیست تخلفات با فیلتر
        get("/api/admin/cheat-attempts") {
            val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: ""
            val claims = jwtService.verifyToken(token)
            if (claims == null || claims.type != "admin") {
                call.respondText("""{"error":"Unauthorized"}""", ContentType.Application.Json)
                return@get
            }
            val userId = call.request.queryParameters["userId"]
            val violationType = call.request.queryParameters["violationType"]
            val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 100
            val offset = call.request.queryParameters["offset"]?.toIntOrNull() ?: 0

            // نیاز به CheatAttemptRepository – اگر ندارید، موقتاً یک لیست خالی برگردانید
            // برای جلوگیری از خطای 404، حداقل یک پاسخ JSON معتبر برگردانید
            val attempts = emptyList<Map<String, Any>>()
            call.respondText(serverGameJson.encodeToString(mapOf("attempts" to attempts, "total" to 0)), ContentType.Application.Json)
        }

// دریافت وضعیت Shadow Pool
        get("/api/admin/shadow-pool") {
            val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: ""
            val claims = jwtService.verifyToken(token)
            if (claims == null || claims.type != "admin") {
                call.respondText("""{"error":"Unauthorized"}""", ContentType.Application.Json)
                return@get
            }
            val users = shadowPoolManager.getShadowPoolUsers().toList()
            call.respondText(serverGameJson.encodeToString(mapOf("users" to users)), ContentType.Application.Json)
        }

// حذف کاربر از Shadow Pool
        post("/api/admin/shadow-pool/remove") {
            val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: ""
            val claims = jwtService.verifyToken(token)
            if (claims == null || claims.type != "admin") {
                call.respondText("""{"error":"Unauthorized"}""", ContentType.Application.Json)
                return@post
            }
            val params = call.receive<Map<String, String>>()
            val userId = params["userId"] ?: return@post call.respondText("""{"error":"Missing userId"}""")
            shadowPoolManager.removeFromShadowPool(userId)
            call.respondText("""{"success":true}""", ContentType.Application.Json)
        }
        // ========== Tournament Endpoints ==========

// دریافت لیست تورنمنت‌ها
        get("/api/tournaments") {
            val tournaments = TournamentModule.getAllTournaments()
            call.respondText(serverGameJson.encodeToString(tournaments), ContentType.Application.Json)
        }

// دریافت اطلاعات یک تورنمنت
        get("/api/tournaments/{id}") {
            val id = call.parameters["id"] ?: return@get call.respondText("{\"error\":\"Missing id\"}")
            val tournament = TournamentModule.getTournament(id)
            if (tournament != null) {
                call.respondText(serverGameJson.encodeToString(tournament), ContentType.Application.Json)
            } else {
                call.respondText("{\"error\":\"Tournament not found\"}", ContentType.Application.Json)
            }
        }

// ثبت‌نام در تورنمنت
        post("/api/tournaments/{id}/register") {
            val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: ""
            val user = authModule.validateToken(token) ?: return@post call.respondText("{\"error\":\"Unauthorized\"}")

            val tournamentId = call.parameters["id"] ?: return@post call.respondText("{\"error\":\"Missing tournament id\"}")

            // دریافت اطلاعات کاربر برای بررسی شرایط
            val userRating = ratingService.getRating(user.id, "tictactoe") // TODO: gameId از تورنمنت
            val userBehavior = behaviorService.getBehavior(user.id)

            val result = TournamentModule.registerUser(
                tournamentId = tournamentId,
                userId = user.id,
                userLevel = userRating.gamesPlayed / 10 + 1, // تخمین سطح
                userElo = userRating.rating,
                userBehaviorBand = userBehavior.band,
                economyService = economyService
            )

            call.respondText(serverGameJson.encodeToString(result), ContentType.Application.Json)
        }

// لغو ثبت‌نام
        delete("/api/tournaments/{id}/register") {
            val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: ""
            val user = authModule.validateToken(token) ?: return@delete call.respondText("{\"error\":\"Unauthorized\"}")

            val tournamentId = call.parameters["id"] ?: return@delete call.respondText("{\"error\":\"Missing tournament id\"}")

            val success = TournamentModule.cancelRegistration(tournamentId, user.id, economyService)
            call.respondText("""{"success":$success}""", ContentType.Application.Json)
        }
        // توزیع جوایز تورنمنت (فقط ادمین یا پس از پایان خودکار)
        post("/api/admin/tournaments/{id}/distribute-prizes") {
            val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: ""
            val claims = jwtService.verifyToken(token)
            if (claims == null || claims.type != "admin") {
                call.respondText("""{"error":"Unauthorized"}""", ContentType.Application.Json)
                return@post
            }
            val tournamentId = call.parameters["id"] ?: return@post call.respondText("""{"error":"Missing id"}""")
            val awarded = TournamentModule.distributePrizes(tournamentId, economyService)
            call.respondText(serverGameJson.encodeToString(mapOf("awarded" to awarded)), ContentType.Application.Json)
        }
        // ========== Clan Endpoints ==========

// ایجاد کلن
        post("/api/clans/create") {
            val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: ""
            val user = authModule.validateToken(token) ?: return@post call.respondText("{\"error\":\"Unauthorized\"}")
            val params = call.receive<Map<String, String>>()
            val name = params["name"] ?: return@post call.respondText("{\"error\":\"Missing name\"}")
            val tag = params["tag"] ?: return@post call.respondText("{\"error\":\"Missing tag\"}")
            val clan = clanService.createClan(user.id, name, tag)
            if (clan != null) {
                call.respondText(serverGameJson.encodeToString(clan), ContentType.Application.Json)
            } else {
                call.respondText("{\"error\":\"Clan creation failed\"}", ContentType.Application.Json)
            }
        }

// پیوستن به کلن
        post("/api/clans/{id}/join") {
            val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: ""
            val user = authModule.validateToken(token) ?: return@post call.respondText("{\"error\":\"Unauthorized\"}")
            val clanId = call.parameters["id"] ?: return@post call.respondText("{\"error\":\"Missing clan id\"}")
            val success = clanService.joinClan(user.id, clanId)
            call.respondText("""{"success":$success}""", ContentType.Application.Json)
        }

// ترک کلن
        post("/api/clans/{id}/leave") {
            val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: ""
            val user = authModule.validateToken(token) ?: return@post call.respondText("{\"error\":\"Unauthorized\"}")
            val clanId = call.parameters["id"] ?: return@post call.respondText("{\"error\":\"Missing clan id\"}")
            val success = clanService.leaveClan(user.id, clanId)
            call.respondText("""{"success":$success}""", ContentType.Application.Json)
        }

// ارتقا کلن
        post("/api/clans/{id}/upgrade") {
            val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: ""
            val user = authModule.validateToken(token) ?: return@post call.respondText("{\"error\":\"Unauthorized\"}")
            val clanId = call.parameters["id"] ?: return@post call.respondText("{\"error\":\"Missing clan id\"}")
            val success = clanService.upgradeClan(clanId, user.id)
            call.respondText("""{"success":$success}""", ContentType.Application.Json)
        }

// کمک سکه به صندوق کلن
        post("/api/clans/{id}/contribute") {
            val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: ""
            val user = authModule.validateToken(token) ?: return@post call.respondText("{\"error\":\"Unauthorized\"}")
            val clanId = call.parameters["id"] ?: return@post call.respondText("{\"error\":\"Missing clan id\"}")
            val params = call.receive<Map<String, String>>()
            val amount = params["amount"]?.toLongOrNull() ?: return@post call.respondText("{\"error\":\"Missing amount\"}")
            val success = clanService.contributeCoins(user.id, clanId, amount)
            call.respondText("""{"success":$success}""", ContentType.Application.Json)
        }

// دریافت اطلاعات کلن
        get("/api/clans/{id}") {
            val clanId = call.parameters["id"] ?: return@get call.respondText("{\"error\":\"Missing clan id\"}")
            val clan = clanService.getClanInfo(clanId)
            if (clan != null) {
                call.respondText(serverGameJson.encodeToString(clan), ContentType.Application.Json)
            } else {
                call.respondText("{\"error\":\"Clan not found\"}", ContentType.Application.Json)
            }
        }

// دریافت اعضای کلن
        get("/api/clans/{id}/members") {
            val clanId = call.parameters["id"] ?: return@get call.respondText("{\"error\":\"Missing clan id\"}")
            val members = clanService.getClanMembers(clanId)
            call.respondText(serverGameJson.encodeToString(members), ContentType.Application.Json)
        }

// دریافت کلن کاربر جاری
        get("/api/user/clan") {
            val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: ""
            val user = authModule.validateToken(token) ?: return@get call.respondText("{\"error\":\"Unauthorized\"}")
            val clan = clanService.getUserClan(user.id)
            if (clan != null) {
                call.respondText(serverGameJson.encodeToString(clan), ContentType.Application.Json)
            } else {
                call.respondText("{\"clan\":null}", ContentType.Application.Json)
            }
        }
        // ========== Society Endpoints ==========

// ایجاد انجمن (فقط ادمین)
        post("/api/admin/societies") {
            val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: ""
            val claims = jwtService.verifyToken(token)
            if (claims == null || claims.type != "admin") {
                call.respondText("""{"error":"Unauthorized"}""", ContentType.Application.Json)
                return@post
            }
            val params = call.receive<Map<String, Any>>()
            val name = params["name"] as? String ?: return@post call.respondText("{\"error\":\"Missing name\"}")
            val description = params["description"] as? String
            val maxMembers = (params["maxMembers"] as? Number)?.toInt() ?: 50000
            val membershipType = params["membershipType"] as? String ?: "open"
            val conditionMap = params["membershipCondition"] as? Map<*, *>
            val condition = conditionMap?.let {
                MembershipCondition(
                    minLevel = it["minLevel"] as? Int,
                    minElo = it["minElo"] as? Int,
                    allowedBehaviorBands = it["allowedBehaviorBands"] as? List<String>,
                    minGamesPlayed = it["minGamesPlayed"] as? Int,
                    minWinRate = it["minWinRate"] as? Double
                )
            }
            val society = societyService.createSociety(name, description, claims.userId, maxMembers, membershipType, condition)
            if (society != null) {
                call.respondText(serverGameJson.encodeToString(society), ContentType.Application.Json)
            } else {
                call.respondText("{\"error\":\"Creation failed\"}", ContentType.Application.Json)
            }
        }

// دریافت لیست انجمن‌ها
        get("/api/societies") {
            val societies = societyService.getAllSocieties()
            call.respondText(serverGameJson.encodeToString(societies), ContentType.Application.Json)
        }

// دریافت اطلاعات یک انجمن
        get("/api/societies/{id}") {
            val id = call.parameters["id"] ?: return@get call.respondText("{\"error\":\"Missing id\"}")
            val society = societyService.getSociety(id)
            if (society != null) {
                call.respondText(serverGameJson.encodeToString(society), ContentType.Application.Json)
            } else {
                call.respondText("{\"error\":\"Not found\"}", ContentType.Application.Json)
            }
        }

// درخواست عضویت در انجمن
        post("/api/societies/{id}/join") {
            val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: ""
            val user = authModule.validateToken(token) ?: return@post call.respondText("{\"error\":\"Unauthorized\"}")
            val id = call.parameters["id"] ?: return@post call.respondText("{\"error\":\"Missing id\"}")
            val success = societyService.requestJoin(user.id, id)
            call.respondText("""{"success":$success}""", ContentType.Application.Json)
        }

// خروج از انجمن
        post("/api/societies/{id}/leave") {
            val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: ""
            val user = authModule.validateToken(token) ?: return@post call.respondText("{\"error\":\"Unauthorized\"}")
            val id = call.parameters["id"] ?: return@post call.respondText("{\"error\":\"Missing id\"}")
            val success = societyService.leaveSociety(user.id, id)
            call.respondText("""{"success":$success}""", ContentType.Application.Json)
        }

// تأیید عضویت (فقط ادمین یا صاحب انجمن)
        post("/api/societies/{id}/approve/{userId}") {
            val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: ""
            val admin = authModule.validateToken(token) ?: return@post call.respondText("{\"error\":\"Unauthorized\"}")
            val id = call.parameters["id"] ?: return@post call.respondText("{\"error\":\"Missing society id\"}")
            val userId = call.parameters["userId"] ?: return@post call.respondText("{\"error\":\"Missing user id\"}")
            val success = societyService.approveMember(admin.id, id, userId)
            call.respondText("""{"success":$success}""", ContentType.Application.Json)
        }

// رد عضویت
        post("/api/societies/{id}/reject/{userId}") {
            val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: ""
            val admin = authModule.validateToken(token) ?: return@post call.respondText("{\"error\":\"Unauthorized\"}")
            val id = call.parameters["id"] ?: return@post call.respondText("{\"error\":\"Missing society id\"}")
            val userId = call.parameters["userId"] ?: return@post call.respondText("{\"error\":\"Missing user id\"}")
            val success = societyService.rejectMember(admin.id, id, userId)
            call.respondText("""{"success":$success}""", ContentType.Application.Json)
        }

// دریافت اعضای انجمن
        get("/api/societies/{id}/members") {
            val id = call.parameters["id"] ?: return@get call.respondText("{\"error\":\"Missing id\"}")
            val members = societyService.getSocietyMembers(id)
            call.respondText(serverGameJson.encodeToString(members), ContentType.Application.Json)
        }
        // ایجاد اتاق بازی خصوصی برای اعضای کلن
        post("/api/clans/{id}/game") {
            val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: ""
            val user = authModule.validateToken(token) ?: return@post call.respondText("{\"error\":\"Unauthorized\"}")
            val clanId = call.parameters["id"] ?: return@post call.respondText("{\"error\":\"Missing clan id\"}")
            val params = call.receive<Map<String, String>>()
            val gameType = params["gameType"] ?: "tictactoe"

            // بررسی عضویت کاربر در کلن
            val userClan = clanService.getUserClan(user.id)
            if (userClan?.id != clanId) {
                return@post call.respondText("{\"error\":\"Not a member of this clan\"}")
            }

            // دریافت لیست اعضای آنلاین کلن (اختیاری، برای دعوت)
            // ایجاد اتاق خصوصی با GameSessionManager
            val gameId = GameSessionManager.createSession(gameType, listOf(user.id)) // ابتدا فقط خودش
            // (در عمل، می‌توانید اتاق را با ظرفیت بیشتر ایجاد کنید و سایر اعضا را دعوت کنید)

            call.respondText("""{"gameId":"$gameId"}""", ContentType.Application.Json)
        }

// مشابه برای انجمن
        post("/api/societies/{id}/game") {
            val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: ""
            val user = authModule.validateToken(token) ?: return@post call.respondText("{\"error\":\"Unauthorized\"}")
            val societyId = call.parameters["id"] ?: return@post call.respondText("{\"error\":\"Missing society id\"}")
            val params = call.receive<Map<String, String>>()
            val gameType = params["gameType"] ?: "tictactoe"

            val userSocieties = societyService.getUserSocieties(user.id)
            if (!userSocieties.any { it.id == societyId }) {
                return@post call.respondText("{\"error\":\"Not a member of this society\"}")
            }

            val gameId = GameSessionManager.createSession(gameType, listOf(user.id))
            call.respondText("""{"gameId":"$gameId"}""", ContentType.Application.Json)
        }

        // ========== Game Configs (Admin) ==========
        get("/api/admin/game-configs") {
            val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: ""
            val claims = jwtService.verifyToken(token)
            if (claims == null || claims.type != "admin") {
                call.respondText("""{"error":"Unauthorized"}""", ContentType.Application.Json)
                return@get
            }
            val gameId = call.request.queryParameters["gameId"]
            val configs = gameConfigService.getAllConfigs(gameId)
            call.respondText(serverGameJson.encodeToString(configs), ContentType.Application.Json)
        }

        get("/api/admin/game-configs/{gameId}/{mode}") {
            val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: ""
            val claims = jwtService.verifyToken(token)
            if (claims == null || claims.type != "admin") {
                call.respondText("""{"error":"Unauthorized"}""", ContentType.Application.Json)
                return@get
            }
            val gameId = call.parameters["gameId"] ?: return@get call.respondText("""{"error":"Missing gameId"}""")
            val mode = call.parameters["mode"] ?: return@get call.respondText("""{"error":"Missing mode"}""")
            val config = gameConfigService.getConfig(gameId, mode)
            if (config != null) call.respondText(serverGameJson.encodeToString(config), ContentType.Application.Json)
            else call.respondText("""{"error":"Config not found"}""", ContentType.Application.Json)
        }

        post("/api/admin/game-configs") {
            val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: ""
            val claims = jwtService.verifyToken(token)
            if (claims == null || claims.type != "admin") {
                call.respondText("""{"error":"Unauthorized"}""", ContentType.Application.Json)
                return@post
            }
            val request = call.receive<GameConfigCreateRequest>()
            val config = gameConfigService.createConfig(request.gameId, request.mode, request.config, claims.username)
            call.respondText(serverGameJson.encodeToString(config), ContentType.Application.Json)
        }

        put("/api/admin/game-configs/{id}") {
            val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: ""
            val claims = jwtService.verifyToken(token)
            if (claims == null || claims.type != "admin") {
                call.respondText("""{"error":"Unauthorized"}""", ContentType.Application.Json)
                return@put
            }
            val id = call.parameters["id"]?.toLongOrNull() ?: return@put call.respondText("""{"error":"Invalid id"}""")
            val request = call.receive<GameConfigUpdateRequest>()
            val success = gameConfigService.updateConfig(id, request.config, request.version, claims.username)
            call.respondText(if (success) """{"success":true}""" else """{"success":false,"error":"Update failed"}""", ContentType.Application.Json)
        }

        get("/api/admin/game-configs/{gameId}/{mode}/history") {
            val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: ""
            val claims = jwtService.verifyToken(token)
            if (claims == null || claims.type != "admin") {
                call.respondText("""{"error":"Unauthorized"}""", ContentType.Application.Json)
                return@get
            }
            val gameId = call.parameters["gameId"] ?: return@get call.respondText("""{"error":"Missing gameId"}""")
            val mode = call.parameters["mode"] ?: return@get call.respondText("""{"error":"Missing mode"}""")
            val history = gameConfigService.getConfigHistory(gameId, mode)
            call.respondText(serverGameJson.encodeToString(history), ContentType.Application.Json)
        }

        webSocket("/game") {
            val token = call.request.queryParameters["token"] ?: ""
            val user = authModule.validateToken(token)
            if (user == null) {
                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "توکن نامعتبر"))
                return@webSocket
            }
            val handler = WebSocketHandler(
                authModule,
                matchmakingService,
                antiCheatService,
                reconnectTokenBroker,
                cacheProvider,
                rateLimiter
            )
            handler.handle(this, user.id, user.username)
        }
        webSocket("/ws/admin") {
            val token = call.request.queryParameters["token"] ?: ""
            val claims = jwtService.verifyToken(token)
            if (claims == null || claims.type != "admin") {
                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Unauthorized"))
                return@webSocket
            }
            val adminHandler = AdminWebSocketHandler(jwtService, adminStatsService)
            adminHandler.handle(this)
        }

        webSocket("/ws/hub") {
            val token = call.request.queryParameters["token"] ?: ""
            val user = authModule.validateToken(token)
            if (user == null) {
                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "توکن نامعتبر"))
                return@webSocket
            }
            val handler = HubWebSocketHandler(authModule, friendsModule, partyModule)
            handler.handle(this, user.id, user.username)
        }

        webSocket("/chat") {
            val handler = ChatWebSocketHandler()
            handler.handle(this)
        }

        webSocket("/tournament") {
            val handler = TournamentWebSocketHandler()
            handler.handle(this)
        }
    }
}