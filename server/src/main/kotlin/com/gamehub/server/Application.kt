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
import com.gamehub.server.routes.*
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
    val idempotencyManager = com.gamehub.shared.idempotency.IdempotencyManager(cacheProvider)
    val economyService: EconomyService = get()
    val shopService: ShopService = get()
    val antiCheatService: AntiCheatService = get()

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
    val reportService: ReportService = get()
    val gameConfigService: GameConfigService = get()
    val featureFlagService: FeatureFlagService = get()
    val rbacService: RbacService = get()
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

        val settingsService: SettingsService = get()

        // Apply all new route modules
        authRoutes(authModule)
        profileRoutes(authModule, userRepo, ratingService, behaviorService)
        friendsRoutes(authModule, friendsModule, userRepo)
        settingsRoutes(authModule, settingsService)
        matchmakingRoutes(authModule, MatchmakingRestHandler)
        matchHistoryRoutes(authModule)
        notificationRoutes(authModule, notificationService)
        adminRoutes(
            jwtService,
            adminService,
            adminStatsService,
            reportService,
            reportRepository,
            gameConfigService,
            featureFlagService,
            rbacService,
            metricsService,
            shadowPoolManager,
            economyService
        )
        economyRoutes(authModule, economyService, shopService)
        gameActiveRoutes(authModule, cacheProvider)
        testAntiCheatRoutes(antiCheatService)
        clanRoutes(authModule, clanService)
        societyRoutes(authModule, societyService, jwtService)
        tournamentRoutes(
            authModule,
            TournamentModule,
            ratingService,
            behaviorService,
            economyService,
            jwtService
        )

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
