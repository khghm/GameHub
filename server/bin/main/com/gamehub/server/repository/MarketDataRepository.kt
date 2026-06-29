package com.gamehub.server.repository

import com.gamehub.server.economy.ItemSalesStats
import com.gamehub.server.economy.PriceChangeReason
import com.gamehub.server.economy.SuggestedPrice
import com.gamehub.server.persistence.DatabaseFactory.dbQuery
import com.gamehub.shared.cache.CacheProvider
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant

/**
 * مخزن داده‌های شبیه‌ساز بازار.
 * تمام عملیات دیتابیسی مربوط به market_demand_snapshot، market_suggested_prices و
 * market_simulator_execution_log در این کلاس متمرکز شده است.
 */
class MarketDataRepository(
    private val cache: CacheProvider
) {

    // ==================== جداول ====================
    private object MarketDemandSnapshotTable : Table("market_demand_snapshot") {
        val id = long("id").autoIncrement()
        val itemId = varchar("item_id", 50)
        val snapshotTime = timestamp("snapshot_time")
        val quantitySold = integer("quantity_sold")
        val averagePriceSold = long("average_price_sold")
        val totalRevenue = long("total_revenue")
        val uniqueBuyers = integer("unique_buyers")
        val createdAt = timestamp("created_at").default(Instant.now())
        override val primaryKey = PrimaryKey(id)
    }

    private object MarketSuggestedPricesTable : Table("market_suggested_prices") {
        val id = long("id").autoIncrement()
        val itemId = varchar("item_id", 50)
        val suggestedPrice = long("suggested_price")
        val reason = varchar("reason", 255)
        val demandFactor = double("demand_factor")
        val currentPrice = long("current_price")
        val createdAt = timestamp("created_at").default(Instant.now())
        override val primaryKey = PrimaryKey(id)
    }

    private object ExecutionLogTable : Table("market_simulator_execution_log") {
        val id = long("id").autoIncrement()
        val executionTime = timestamp("execution_time")
        val durationMs = long("duration_ms")
        val itemsProcessed = integer("items_processed")
        val snapshotsCreated = integer("snapshots_created")
        val suggestedPricesCreated = integer("suggested_prices_created")
        val status = varchar("status", 20)
        val errorMessage = text("error_message").nullable()
        val createdAt = timestamp("created_at").default(Instant.now())
        override val primaryKey = PrimaryKey(id)
    }

    // ==================== عملیات اسنپ‌شات ====================

    /**
     * ذخیره یک اسنپ‌شات تقاضا در دیتابیس.
     * @param itemId شناسه آیتم
     * @param snapshotTime زمان شروع بازه
     * @param quantitySold تعداد فروش رفته
     * @param averagePriceSold میانگین قیمت فروش
     * @param totalRevenue کل درآمد
     * @param uniqueBuyers تعداد خریداران یکتا
     */
    suspend fun saveDemandSnapshot(
        itemId: String,
        snapshotTime: Instant,
        quantitySold: Int,
        averagePriceSold: Long,
        totalRevenue: Long,
        uniqueBuyers: Int
    ) = dbQuery {
        MarketDemandSnapshotTable.insert {
            it[MarketDemandSnapshotTable.itemId] = itemId
            it[MarketDemandSnapshotTable.snapshotTime] = snapshotTime
            it[MarketDemandSnapshotTable.quantitySold] = quantitySold
            it[MarketDemandSnapshotTable.averagePriceSold] = averagePriceSold
            it[MarketDemandSnapshotTable.totalRevenue] = totalRevenue
            it[MarketDemandSnapshotTable.uniqueBuyers] = uniqueBuyers
        }
    }

    /**
     * بازیابی آخرین اسنپ‌شات برای یک آیتم (برای محاسبه روند).
     */
    suspend fun getLastSnapshot(itemId: String): ResultRow? = dbQuery {
        MarketDemandSnapshotTable
            .selectAll()
            .where { MarketDemandSnapshotTable.itemId eq itemId }
            .orderBy(MarketDemandSnapshotTable.snapshotTime to SortOrder.DESC)
            .limit(1)
            .singleOrNull()
    }

    // ==================== عملیات پیشنهاد قیمت ====================

    /**
     * ذخیره یک پیشنهاد قیمت جدید (برای استفاده در آینده).
     * @param suggestedPrice مدل کامل پیشنهاد قیمت
     */
    suspend fun saveSuggestedPrice(suggestedPrice: SuggestedPrice) = dbQuery {
        MarketSuggestedPricesTable.insert {
            it[MarketSuggestedPricesTable.itemId] = suggestedPrice.itemId
            it[MarketSuggestedPricesTable.suggestedPrice] = suggestedPrice.suggestedPrice
            it[MarketSuggestedPricesTable.reason] = suggestedPrice.reason.name
            it[MarketSuggestedPricesTable.demandFactor] = suggestedPrice.demandFactor
            it[MarketSuggestedPricesTable.currentPrice] = suggestedPrice.currentPrice
        }
    }

    /**
     * بازیابی آخرین پیشنهاد قیمت برای یک آیتم.
     */
    suspend fun getLastSuggestedPrice(itemId: String): SuggestedPrice? = dbQuery {
        MarketSuggestedPricesTable
            .selectAll()
            .where { MarketSuggestedPricesTable.itemId eq itemId }
            .orderBy(MarketSuggestedPricesTable.createdAt to SortOrder.DESC)
            .limit(1)
            .singleOrNull()
            ?.let { row ->
                SuggestedPrice(
                    itemId = row[MarketSuggestedPricesTable.itemId],
                    suggestedPrice = row[MarketSuggestedPricesTable.suggestedPrice],
                    reason = PriceChangeReason.valueOf(row[MarketSuggestedPricesTable.reason]),
                    demandFactor = row[MarketSuggestedPricesTable.demandFactor],
                    currentPrice = row[MarketSuggestedPricesTable.currentPrice],
                    basePrice = 0L, // در آینده از item_definitions پر می‌شود
                    minAllowedPrice = 0L,
                    maxAllowedPrice = 0L,
                    createdAt = row[MarketSuggestedPricesTable.createdAt]
                )
            }
    }

    // ==================== عملیات لاگ اجرا ====================

    /**
     * ثبت لاگ اجرای یک دوره شبیه‌ساز.
     */
    suspend fun logExecution(
        executionTime: Instant,
        durationMs: Long,
        itemsProcessed: Int,
        snapshotsCreated: Int,
        suggestedPricesCreated: Int,
        status: String,
        errorMessage: String? = null
    ) = dbQuery {
        ExecutionLogTable.insert {
            it[ExecutionLogTable.executionTime] = executionTime
            it[ExecutionLogTable.durationMs] = durationMs
            it[ExecutionLogTable.itemsProcessed] = itemsProcessed
            it[ExecutionLogTable.snapshotsCreated] = snapshotsCreated
            it[ExecutionLogTable.suggestedPricesCreated] = suggestedPricesCreated
            it[ExecutionLogTable.status] = status
            it[ExecutionLogTable.errorMessage] = errorMessage
        }
    }

    // ==================== عملیات کمکی ====================

    /**
     * دریافت آمار فروش یک آیتم در بازه زمانی مشخص با استفاده از جدول تراکنش‌ها.
     * @param itemId شناسه آیتم
     * @param intervalStart زمان شروع بازه (شامل)
     * @param intervalEnd زمان پایان بازه (انحصاری)
     * @return آمار فروش (یا null اگر هیچ فروشی نبوده باشد)
     */
    suspend fun getSalesStatsForItem(
        itemId: String,
        intervalStart: Instant,
        intervalEnd: Instant
    ): ItemSalesStats? {
        // این تابع باید به جدول inventory_transactions دسترسی داشته باشد
        // پیاده‌سازی واقعی در فاز بعدی با استفاده از InventoryTransactionsTable انجام می‌شود
        // فعلاً یک پیاده‌سازی نمونه برمی‌گردانیم (در فایل نهایی با کوئری واقعی جایگزین می‌شود)
        return null
    }
}