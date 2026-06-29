package com.gamehub.server.di

import com.gamehub.server.admin.AdminService
import com.gamehub.server.admin.AdminStatsService
import com.gamehub.server.admin.AlertService
import com.gamehub.server.admin.GameConfigService
import com.gamehub.server.admin.MetricsService
import com.gamehub.server.admin.RbacService
import com.gamehub.server.admin.ReportService
import com.gamehub.server.admin.repository.AdminRepository
import com.gamehub.server.anticheat.AntiCheatService
import com.gamehub.server.anticheat.BotBehaviorDetector
import com.gamehub.server.anticheat.CollusionDetector
import com.gamehub.server.anticheat.LagSwitchDetector
import com.gamehub.server.anticheat.MacroDetector
import com.gamehub.server.anticheat.PenaltyService
import com.gamehub.server.anticheat.ShadowPoolManager
import com.gamehub.server.anticheat.SpeedHackDetector
import com.gamehub.server.anticheat.TimeAttestation
import com.gamehub.server.behavior.BehaviorService
import com.gamehub.server.bot.BotProfileGenerator
import com.gamehub.server.bot.BotProfileRepository
import com.gamehub.server.bot.BotRotationScheduler
import com.gamehub.server.bot.BotStrategyRegistry
import com.gamehub.server.bot.CentralBotManager
import com.gamehub.server.cache.CacheProviderFactory
import com.gamehub.server.cache.CircuitBreakerCacheProvider
import com.gamehub.server.cache.PresenceCache
import com.gamehub.server.cache.SessionCache
import com.gamehub.server.clan.ClanService
import com.gamehub.server.completion.MatchCompletionService
import com.gamehub.server.economy.EconomyLoopService
import com.gamehub.server.economy.EconomyService
import com.gamehub.server.economy.ShopService

import com.gamehub.server.matchmaking.MatchmakingService
import com.gamehub.server.modules.ReconnectTokenBroker
import com.gamehub.server.notifications.NotificationService
import com.gamehub.server.persistence.DatabaseConfig
import com.gamehub.server.persistence.DatabaseFactory
import com.gamehub.server.rating.RatingService
import com.gamehub.server.featureflags.FeatureFlagService
import com.gamehub.server.repository.BehaviorRepository
import com.gamehub.server.repository.FeatureFlagRepository
import com.gamehub.server.repository.FriendshipRepository
import com.gamehub.server.repository.GameConfigRepository
import com.gamehub.server.repository.GameEventLogRepository
import com.gamehub.server.repository.MarketDataRepository
import com.gamehub.server.repository.MatchHistoryRepository
import com.gamehub.server.repository.PartyRepository
import com.gamehub.server.repository.RatingRepository
import com.gamehub.server.repository.ReportRepository
import com.gamehub.server.repository.UserRepository
import com.gamehub.server.repository.UserTutorialRepository
import com.gamehub.server.replay.ReplayService
import com.gamehub.server.wal.WriteAheadLogService
import com.gamehub.server.wal.WalManager
import com.gamehub.server.security.JwtConfig
import com.gamehub.server.security.JwtService
import com.gamehub.server.security.RateLimiter
import com.gamehub.server.security.ReconnectRateLimiter
import com.gamehub.server.security.TokenBlacklist
import com.gamehub.server.settings.SettingsService
import com.gamehub.server.society.SocietyService
import com.gamehub.shared.idempotency.IdempotencyManager
import com.typesafe.config.ConfigFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.dsl.bind
import org.koin.dsl.module

val appModule = module {
    single { ConfigFactory.load() }
    single { CoroutineScope(SupervisorJob() + Dispatchers.Default) }

    // Database
    single {
        val config = get<com.typesafe.config.Config>()
        DatabaseConfig(
            driver = config.getString("database.driver"),
            url = config.getString("database.url"),
            user = config.getString("database.user"),
            password = config.getString("database.password"),
            maxPoolSize = config.getInt("database.maxPoolSize")
        )
    }
    single {
        DatabaseFactory.init(get())
        DatabaseFactory
    }

    // Repositories
    single { UserRepository() }
    single { FriendshipRepository() }
    single { PartyRepository() }
    single { RatingRepository() }
    single { BehaviorRepository() }
    single { MatchHistoryRepository() }
    single { ReportRepository() }
    single { GameConfigRepository() }
    single { MarketDataRepository(get()) }
    single { AdminRepository() }
    single { BotProfileRepository() }
    single { GameEventLogRepository() }
    single { FeatureFlagRepository() }
    single { UserTutorialRepository() }

    // Feature Flags
    single { FeatureFlagService(get()) }
    
    // WAL (Write-Ahead Log)
    single { WriteAheadLogService(get()) }
    single { WalManager(get(), get(), get()) }

    // Cache
    single {
        val config = get<com.typesafe.config.Config>()
        val redisEnabled = config.getBoolean("redis.enabled")
        val redisUrl = if (redisEnabled) config.getString("redis.url") else null
        val baseCache = CacheProviderFactory.create(redisUrl)
        CircuitBreakerCacheProvider(baseCache)
    } bind com.gamehub.shared.cache.CacheProvider::class
    single { TokenBlacklist(get()) }
    single { SessionCache.init(get()); SessionCache }
    single { PresenceCache.init(get()); PresenceCache }
    single { IdempotencyManager(get()) }

    // JWT & Security
    single {
        val config = get<com.typesafe.config.Config>()
        JwtConfig(
            secret = config.getString("jwt.secret"),
            issuer = config.getString("jwt.issuer"),
            accessTokenValidityMs = config.getLong("jwt.accessTokenValidityMs"),
            refreshTokenValidityMs = config.getLong("jwt.refreshTokenValidityMs")
        )
    }
    single { JwtService(get(), get()) }
    single { RateLimiter(get(), capacity = 60, refillRate = 30, windowSeconds = 60) }
    single { ReconnectRateLimiter(get()) }
    single { ReconnectTokenBroker(get(), get(), get()) }

    // Bot Services
    single { BotStrategyRegistry }
    single { BotProfileGenerator }
    single { CentralBotManager(get(), get()) }
    single { BotRotationScheduler(get(), get()) }

    // Game Services
    single { RatingService(get()) }
    single { BehaviorService(get()) }
    single { ShadowPoolManager(get(), get()) }
    single { MatchmakingService(get(), get(), get(), get()) }
    single { MatchCompletionService(get(), get(), get()) }
    single { EconomyLoopService(get()) }
    single { EconomyService(get(), get(), get(), get()) }
    single { ShopService(get(), get(), get()) }
    single { TimeAttestation(get()) }
    single { SpeedHackDetector(get()) }
    single { LagSwitchDetector(get()) }
    single { MacroDetector(get()) }
    single { CollusionDetector(get(), get()) }
    single { PenaltyService(get(), get(), get(), get()) }
    single { BotBehaviorDetector(get()) }
    single {
        AntiCheatService(
            get(), get(), get(), get(), get(), get(), get(), get()
        )
    }
    single { SettingsService(get()) }
    single { NotificationService(get()) }
    single { ClanService(get(), get()) }
    single { SocietyService(get(), get(), get(), get()) }

    // Admin Services
    single { AdminService(get(), get(), get()) }
    single { AdminStatsService(get(), get()) }
    single { AlertService(get()) }
    single { MetricsService(get()) }
    single { RbacService(get()) }
    single { ReportService(get(), get(), get()) }
    single { GameConfigService(get()) }
    
    // New Features
    single { ReplayService(get()) }
}
