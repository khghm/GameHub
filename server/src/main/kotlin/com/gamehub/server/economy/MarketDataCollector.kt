package com.gamehub.server.economy

import com.gamehub.server.repository.MarketDataRepository
import com.gamehub.shared.cache.CacheProvider
import kotlinx.coroutines.*
import java.time.Instant

/**
 * جمع‌آوری داده‌های بازار و محاسبه پیشنهادات قیمت.
 *
 * ## مفاهیم کلیدی:
 *
 * ### demandFactor (ضریب تقاضا)
 * نسبت فروش واقعی یک آیتم در بازه زمانی به فروش مورد انتظار (expectedSales).
 *
 * فرمول: `demandFactor = max(0.2, min(3.0, actualSales / expectedSales))`
 *
 * - اگر demandFactor > 1: تقاضا بیشتر از حد انتظار است → قیمت باید افزایش یابد.
 * - اگر demandFactor < 1: تقاضا کمتر از حد انتظار است → قیمت باید کاهش یابد.
 * - محدوده مجاز: بین 0.2 تا 3.0 (برای جلوگیری از نوسانات شدید).
 *
 * ### minPriceRatio و maxPriceRatio
 * محدوده مجاز تغییر قیمت نسبت به قیمت پایه (basePrice).
 *
 * - `minPriceRatio = 0.8` → قیمت نمی‌تواند کمتر از 80% قیمت پایه شود.
 * - `maxPriceRatio = 2.0` → قیمت نمی‌تواند بیشتر از 200% قیمت پایه شود.
 *
 * این محدودیت‌ها از نوسانات غیرمنطقی جلوگیری می‌کنند.
 *
 * ### demandFactorBase (ضریب حساسیت پایه)
 * میزان تأثیر demandFactor بر قیمت نهایی را کنترل می‌کند.
 *
 * فرمول تغییر قیمت:
 * `priceChangePercent = (demandFactor - 1) * demandFactorBase * 100`
 *
 * مثال‌ها:
 * - demandFactorBase = 0.5 (پیش‌فرض) → اگر تقاضا 2 برابر شود (demandFactor=2)، قیمت 50% افزایش می‌یابد.
 * - demandFactorBase = 1.0 → در same شرایط، قیمت 100% افزایش می‌یابد (حساسیت بیشتر).
 * - demandFactorBase = 0.2 → قیمت فقط 20% افزایش می‌یابد (تغییرات ملایم).
 *
 * ### فعال/غیرفعال بودن (enabled)
 * - اگر enabled = false: فقط داده جمع‌آوری می‌شود، قیمت واقعی تغییری نمی‌کند.
 * - اگر enabled = true: علاوه بر جمع‌آوری داده، قیمت آیتم‌ها نیز به‌روز می‌شود.
 *
 * این طراحی به تیم اجازه می‌دهد پس از چند ماه داده‌کافی، تنها با تغییر تنظیمات، سیستم را فعال کنند.
 */
class MarketDataCollector(
    private val cache: CacheProvider,
    private val repository: MarketDataRepository,
    private val config: MarketSimulatorConfig
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var isRunning = false
    private var currentJob: Job? = null

    companion object {
        private const val CACHE_PREFIX = "market:item:"
        private const val CACHE_TTL_SECONDS = 3600L
    }

    /**
     * شروع فرآیند جمع‌آوری دوره‌ای.
     * اگر enabled = true باشد، علاوه بر جمع‌آوری، قیمت‌ها نیز به‌روز می‌شوند.
     */
    fun start() {
        if (isRunning) {
            println("⚠️ MarketDataCollector already running")
            return
        }
        isRunning = true
        println("📊 MarketDataCollector started (enabled=${config.enabled}, interval=${config.intervalHours}h)")

        currentJob = scope.launch {
            while (isRunning) {
                val startTime = System.currentTimeMillis()
                try {
                    executeCollectionCycle()
                    val duration = System.currentTimeMillis() - startTime
                    println("✅ MarketDataCollector cycle completed in ${duration}ms")
                } catch (e: Exception) {
                    println("❌ MarketDataCollector cycle failed: ${e.message}")
                    e.printStackTrace()
                }
                delay(config.intervalHours * 3600_000L)
            }
        }
    }

    /**
     * توقف فرآیند جمع‌آوری.
     */
    fun stop() {
        isRunning = false
        currentJob?.cancel()
        println("📊 MarketDataCollector stopped")
    }

    /**
     * اجرای یک سیکل کامل جمع‌آوری داده و (در صورت فعال بودن) به‌روزرسانی قیمت.
     */
    private suspend fun executeCollectionCycle() {
        val executionStart = Instant.now()
        var itemsProcessed = 0
        var snapshotsCreated = 0
        var suggestedPricesCreated = 0
        var status = "SUCCESS"
        var errorMessage: String? = null

        try {
            // دریافت لیست تمام آیتم‌های فعال
            val items = getAllActiveItems()
            itemsProcessed = items.size

            val intervalEnd = executionStart
            val intervalStart = intervalEnd.minusSeconds(config.intervalHours * 3600L)

            for (item in items) {
                try {
                    // 1. دریافت آمار فروش در بازه
                    val stats = getSalesStatsForItem(item.id, intervalStart, intervalEnd)
                    if (stats == null || stats.totalSold == 0) {
                        continue // هیچ فروشی در این بازه نداشته
                    }

                    // 2. ذخیره اسنپ‌شات تقاضا (همیشه انجام می‌شود)
                    repository.saveDemandSnapshot(
                        itemId = item.id,
                        snapshotTime = intervalStart,
                        quantitySold = stats.totalSold,
                        averagePriceSold = stats.averagePrice,
                        totalRevenue = stats.totalRevenue,
                        uniqueBuyers = stats.uniqueBuyers
                    )
                    snapshotsCreated++

                    // 3. محاسبه قیمت پیشنهادی
                    val suggestedPrice = calculateSuggestedPrice(item, stats.demandFactor)

                    // 4. ذخیره پیشنهاد قیمت (همیشه انجام می‌شود)
                    repository.saveSuggestedPrice(suggestedPrice)
                    suggestedPricesCreated++

                    // 5. اگر شبیه‌ساز فعال است، قیمت واقعی را به‌روز کن
                    if (config.enabled && suggestedPrice.suggestedPrice != item.currentPrice) {
                        updateItemPrice(item.id, suggestedPrice.suggestedPrice)
                        println("💰 Price updated for ${item.id}: ${item.currentPrice} → ${suggestedPrice.suggestedPrice} (demandFactor=${stats.demandFactor})")
                    } else if (config.enabled) {
                        println("📊 Price unchanged for ${item.id}: ${item.currentPrice} (demandFactor=${stats.demandFactor})")
                    } else {
                        // حالت غیرفعال: فقط لاگ می‌کنیم که چه پیشنهادی داده می‌شد
                        println("📊 [PASSIVE] Suggested price for ${item.id}: ${suggestedPrice.suggestedPrice} (current=${item.currentPrice}, demandFactor=${stats.demandFactor})")
                    }

                } catch (e: Exception) {
                    println("⚠️ Error processing item ${item.id}: ${e.message}")
                    // ادامه می‌دهیم تا آیتم‌های دیگر پردازش شوند
                }
            }

        } catch (e: Exception) {
            status = "FAILED"
            errorMessage = e.message
            println("❌ MarketDataCollector cycle failed: ${e.message}")
            e.printStackTrace()
        } finally {
            // ثبت لاگ اجرا
            val durationMs = System.currentTimeMillis() - executionStart.toEpochMilli()
            repository.logExecution(
                executionTime = executionStart,
                durationMs = durationMs,
                itemsProcessed = itemsProcessed,
                snapshotsCreated = snapshotsCreated,
                suggestedPricesCreated = suggestedPricesCreated,
                status = status,
                errorMessage = errorMessage
            )
        }
    }

    /**
     * محاسبه قیمت پیشنهادی بر اساس ضریب تقاضا و محدودیت‌ها.
     *
     * فرمول:
     * 1. درصد تغییر = (demandFactor - 1) × demandFactorBase × 100
     * 2. قیمت پیشنهادی = قیمت پایه × (1 + درصد تغییر / 100)
     * 3. اعمال محدودیت minPriceRatio و maxPriceRatio
     *
     * @param item اطلاعات آیتم (شامل basePrice و currentPrice)
     * @param demandFactor ضریب تقاضای محاسبه شده (محدوده 0.2 تا 3.0)
     * @return مدل SuggestedPrice کامل
     */
    private suspend fun calculateSuggestedPrice(item: ActiveItem, demandFactor: Double): SuggestedPrice {
        val basePrice = item.basePrice
        val currentPrice = item.currentPrice

        // محاسبه درصد تغییر (مثبت یا منفی)
        val priceChangePercent = (demandFactor - 1.0) * config.demandFactorBase * 100.0

        // محاسبه قیمت جدید
        var newPrice = (basePrice * (1 + priceChangePercent / 100.0)).toLong()

        // اعمال محدودیت‌ها
        val minAllowed = (basePrice * config.minPriceRatio).toLong()
        val maxAllowed = (basePrice * config.maxPriceRatio).toLong()
        newPrice = newPrice.coerceIn(minAllowed, maxAllowed)

        // تعیین دلیل تغییر
        val reason = when {
            demandFactor > 1.2 -> PriceChangeReason.DEMAND_HIGH
            demandFactor < 0.8 -> PriceChangeReason.DEMAND_LOW
            else -> PriceChangeReason.DEMAND_HIGH // fallback
        }

        return SuggestedPrice(
            itemId = item.id,
            suggestedPrice = newPrice,
            reason = reason,
            demandFactor = demandFactor,
            currentPrice = currentPrice,
            basePrice = basePrice,
            minAllowedPrice = minAllowed,
            maxAllowedPrice = maxAllowed,
            createdAt = Instant.now()
        )
    }

    /**
     * دریافت لیست تمام آیتم‌های فعال فروشگاه.
     * در این مرحله از دیتابیس خوانده می‌شود.
     */
    private suspend fun getAllActiveItems(): List<ActiveItem> {
        // TODO: در فاز بعدی از دیتابیس واقعی (item_definitions) بخوانیم
        // فعلاً یک لیست نمونه برمی‌گردانیم
        return listOf(
            ActiveItem("test_item", 100L, 100L),
            ActiveItem("key_chest", 200L, 200L),
            ActiveItem("skin_rare", 500L, 500L)
        )
    }

    /**
     * دریافت آمار فروش یک آیتم در بازه زمانی مشخص.
     * این تابع از جدول inventory_transactions استفاده می‌کند.
     */
    private suspend fun getSalesStatsForItem(itemId: String, start: Instant, end: Instant): ItemSalesStats? {
        // TODO: پیاده‌سازی واقعی با کوئری روی InventoryTransactionsTable
        // در فاز بعدی کامل می‌شود
        return null
    }

    /**
     * به‌روزرسانی قیمت واقعی یک آیتم در دیتابیس و کش.
     * @param itemId شناسه آیتم
     * @param newPrice قیمت جدید
     */
    private suspend fun updateItemPrice(itemId: String, newPrice: Long) {
        // TODO: به‌روزرسانی در جدول item_definitions و کش
        // فعلاً فقط در کش ذخیره می‌کنیم
        cache.set("${CACHE_PREFIX}${itemId}_price", newPrice.toString(), CACHE_TTL_SECONDS)
    }

    /**
     * مدل داخلی برای آیتم فعال.
     */
    private data class ActiveItem(
        val id: String,
        val basePrice: Long,
        val currentPrice: Long
    )
}