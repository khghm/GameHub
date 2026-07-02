
package com.gamehub.server.routes

import com.gamehub.server.admin.*
import com.gamehub.server.admin.dto.AdminUserListResponse
import com.gamehub.server.anticheat.AntiCheatService
import com.gamehub.server.anticheat.ShadowPoolManager
import com.gamehub.server.economy.EconomyService
import com.gamehub.server.featureflags.FeatureFlagService
import com.gamehub.server.featureflags.CreateFeatureFlagRequest
import com.gamehub.server.featureflags.UpdateFeatureFlagRequest
import com.gamehub.server.repository.ReportRepository
import com.gamehub.server.security.JwtService
import com.gamehub.shared.game.GameConfigCreateRequest
import com.gamehub.shared.game.GameConfigUpdateRequest
import com.gamehub.shared.report.ReportDecision
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.http.*
import io.ktor.server.plugins.origin
import kotlinx.serialization.encodeToString

fun Route.adminRoutes(
    jwtService: JwtService,
    adminService: AdminService,
    adminStatsService: AdminStatsService,
    reportService: ReportService,
    reportRepository: ReportRepository,
    gameConfigService: GameConfigService,
    featureFlagService: FeatureFlagService,
    rbacService: RbacService,
    metricsService: MetricsService,
    shadowPoolManager: ShadowPoolManager,
    economyService: EconomyService
) {
    route("/api/admin") {
        post("/login") {
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

        get("/users") {
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
            call.respondText(com.gamehub.server.serverGameJson.encodeToString(response), ContentType.Application.Json)
        }

        post("/users/ban") {
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
            val success = adminService.banUser(java.util.UUID.fromString(claims.userId), claims.username, targetUserId, reason, durationHours)
            call.respondText("""{"success":$success}""", ContentType.Application.Json)
        }

        post("/users/unban") {
            val params = call.receive<Map<String, String>>()
            val targetUserId = params["userId"] ?: ""
            val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: ""
            val claims = jwtService.verifyToken(token)
            if (claims == null || claims.type != "admin") {
                call.respondText("""{"error":"Unauthorized"}""", ContentType.Application.Json)
                return@post
            }
            val success = adminService.unbanUser(java.util.UUID.fromString(claims.userId), claims.username, targetUserId)
            call.respondText("""{"success":$success}""", ContentType.Application.Json)
        }

        get("/audit") {
            val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: ""
            val claims = jwtService.verifyToken(token)
            if (claims == null || claims.type != "admin") {
                call.respondText("""{"error":"Unauthorized"}""", ContentType.Application.Json)
                return@get
            }
            val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 100
            val offset = call.request.queryParameters["offset"]?.toIntOrNull() ?: 0
            val logs = adminService.getAuditLog(limit, offset)
            val json = com.gamehub.server.serverGameJson.encodeToString(logs)
            call.respondText(json, ContentType.Application.Json)
        }

        route("/feature-flags") {
            get {
                val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: ""
                val claims = jwtService.verifyToken(token)
                if (claims == null || claims.type != "admin") {
                    call.respondText("""{"error":"Unauthorized"}""", ContentType.Application.Json)
                    return@get
                }
                val environment = call.request.queryParameters["environment"]
                val flags = featureFlagService.getAllFeatureFlags(environment)
                call.respondText(com.gamehub.server.serverGameJson.encodeToString(flags), ContentType.Application.Json)
            }

            post {
                val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: ""
                val claims = jwtService.verifyToken(token)
                if (claims == null || claims.type != "admin") {
                    call.respondText("""{"error":"Unauthorized"}""", ContentType.Application.Json)
                    return@post
                }
                val request = call.receive<CreateFeatureFlagRequest>()
                try {
                    val flag = featureFlagService.createFeatureFlag(request)
                    call.respondText(com.gamehub.server.serverGameJson.encodeToString(flag), ContentType.Application.Json)
                } catch (e: IllegalArgumentException) {
                    call.respondText("{\"error\":\"${e.message}\"}", status = HttpStatusCode.BadRequest, contentType = ContentType.Application.Json)
                }
            }

            patch("{id}") {
                val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: ""
                val claims = jwtService.verifyToken(token)
                if (claims == null || claims.type != "admin") {
                    call.respondText("""{"error":"Unauthorized"}""", ContentType.Application.Json)
                    return@patch
                }
                val id = call.parameters["id"] ?: return@patch call.respondText("""{"error":"Missing id"}""", status = HttpStatusCode.BadRequest, contentType = ContentType.Application.Json)
                val request = call.receive<UpdateFeatureFlagRequest>()
                val flag = featureFlagService.updateFeatureFlag(java.util.UUID.fromString(id), request)
                if (flag != null) {
                    call.respondText(com.gamehub.server.serverGameJson.encodeToString(flag), ContentType.Application.Json)
                } else {
                    call.respondText("""{"error":"Flag not found"}""", status = HttpStatusCode.NotFound, contentType = ContentType.Application.Json)
                }
            }

            delete("{id}") {
                val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: ""
                val claims = jwtService.verifyToken(token)
                if (claims == null || claims.type != "admin") {
                    call.respondText("""{"error":"Unauthorized"}""", ContentType.Application.Json)
                    return@delete
                }
                val id = call.parameters["id"] ?: return@delete call.respondText("""{"error":"Missing id"}""", status = HttpStatusCode.BadRequest, contentType = ContentType.Application.Json)
                val success = featureFlagService.deleteFeatureFlag(java.util.UUID.fromString(id))
                call.respondText("""{"success":$success}""", ContentType.Application.Json)
            }
        }

        get("/stats") {
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
            call.respondText(com.gamehub.server.serverGameJson.encodeToString(response), ContentType.Application.Json)
        }

        get("/my-permissions") {
            val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: ""
            val claims = jwtService.verifyToken(token)
            if (claims == null || claims.type != "admin") {
                call.respondText("""{"error":"Unauthorized"}""", ContentType.Application.Json)
                return@get
            }
            val adminId = java.util.UUID.fromString(claims.userId)
            val permissions = rbacService.getUserPermissions(adminId)
            call.respondText(com.gamehub.server.serverGameJson.encodeToString(permissions), ContentType.Application.Json)
        }

        get("/users/{userId}/permissions") {
            val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: ""
            val claims = jwtService.verifyToken(token)
            if (claims == null || claims.type != "admin") {
                call.respondText("""{"error":"Unauthorized"}""", ContentType.Application.Json)
                return@get
            }
            val targetUserId = call.parameters["userId"] ?: return@get call.respondText("""{"error":"Missing userId"}""")
            if (!rbacService.userHasPermission(java.util.UUID.fromString(claims.userId), "admins:view")) {
                call.respondText("""{"error":"Forbidden"}""", ContentType.Application.Json)
                return@get
            }
            val perms = rbacService.getUserPermissions(java.util.UUID.fromString(targetUserId))
            call.respondText(com.gamehub.server.serverGameJson.encodeToString(perms), ContentType.Application.Json)
        }

        get("/roles") {
            val roles = listOf(
                mapOf("name" to "super_admin", "description" to "Full access"),
                mapOf("name" to "admin", "description" to "Administrative access"),
                mapOf("name" to "moderator", "description" to "Manage users and reports"),
                mapOf("name" to "support", "description" to "View users and reports")
            )
            call.respondText(com.gamehub.server.serverGameJson.encodeToString(roles), ContentType.Application.Json)
        }

        get("/permissions") {
            val allPermissions = rbacService.getAllPermissionsList()
            call.respondText(com.gamehub.server.serverGameJson.encodeToString(allPermissions), ContentType.Application.Json)
        }

        get("/metrics") {
            val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: ""
            val claims = jwtService.verifyToken(token)
            if (claims == null || claims.type != "admin") {
                call.respondText("""{"error":"Unauthorized"}""", ContentType.Application.Json)
                return@get
            }
            val metrics = metricsService.getCurrentMetrics()
            call.respondText(com.gamehub.server.serverGameJson.encodeToString(metrics), ContentType.Application.Json)
        }

        get("/reports") {
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
            call.respondText(com.gamehub.server.serverGameJson.encodeToString(response), ContentType.Application.Json)
        }

        post("/reports/{id}/review") {
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

        route("/game-configs") {
            get {
                val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: ""
                val claims = jwtService.verifyToken(token)
                if (claims == null || claims.type != "admin") {
                    call.respondText("""{"error":"Unauthorized"}""", ContentType.Application.Json)
                    return@get
                }
                val gameId = call.request.queryParameters["gameId"]
                val configs = gameConfigService.getAllConfigs(gameId)
                call.respondText(com.gamehub.server.serverGameJson.encodeToString(configs), ContentType.Application.Json)
            }

            get("{gameId}/{mode}") {
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
                    call.respondText(com.gamehub.server.serverGameJson.encodeToString(config), ContentType.Application.Json)
                } else {
                    call.respondText("""{"error":"Config not found"}""", ContentType.Application.Json)
                }
            }

            post {
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
                call.respondText(com.gamehub.server.serverGameJson.encodeToString(config), ContentType.Application.Json)
            }

            put("{id}") {
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

            get("{gameId}/{mode}/history") {
                val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: ""
                val claims = jwtService.verifyToken(token)
                if (claims == null || claims.type != "admin") {
                    call.respondText("""{"error":"Unauthorized"}""", ContentType.Application.Json)
                    return@get
                }
                val gameId = call.parameters["gameId"] ?: return@get call.respondText("""{"error":"Missing gameId"}""")
                val mode = call.parameters["mode"] ?: return@get call.respondText("""{"error":"Missing mode"}""")
                val history = gameConfigService.getConfigHistory(gameId, mode)
                call.respondText(com.gamehub.server.serverGameJson.encodeToString(history), ContentType.Application.Json)
            }
        }

        route("/cheat-attempts") {
            get {
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
                val attempts = emptyList<Map<String, Any>>()
                call.respondText(com.gamehub.server.serverGameJson.encodeToString(mapOf("attempts" to attempts, "total" to 0)), ContentType.Application.Json)
            }
        }

        route("/shadow-pool") {
            get {
                val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: ""
                val claims = jwtService.verifyToken(token)
                if (claims == null || claims.type != "admin") {
                    call.respondText("""{"error":"Unauthorized"}""", ContentType.Application.Json)
                    return@get
                }
                val users = shadowPoolManager.getShadowPoolUsers().toList()
                call.respondText(com.gamehub.server.serverGameJson.encodeToString(mapOf("users" to users)), ContentType.Application.Json)
            }

            post("/remove") {
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
        }
    }

    get("/api/feature-flags/{key}/enabled") {
        val key = call.parameters["key"] ?: return@get call.respondText("""{"error":"Missing key"}""", status = HttpStatusCode.BadRequest, contentType = ContentType.Application.Json)
        val environment = call.request.queryParameters["environment"]
        val enabled = featureFlagService.isFeatureEnabled(key, environment)
        call.respondText("""{"enabled":$enabled}""", ContentType.Application.Json)
    }
}

