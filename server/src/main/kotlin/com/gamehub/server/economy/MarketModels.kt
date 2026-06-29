package com.gamehub.server.economy

import kotlinx.serialization.Serializable
import java.time.Instant

/**
 * مدل‌های داده مرتبط با شبیه‌ساز بازار
 * تمام مدل‌ها برای ذخیره‌سازی در دیتابیس و انتقال بین لایه‌ها استفاده می‌شوند.
 */

/**
 * آماری که برای هر آیتم در یک بازه زمانی مشخص محاسبه می‌شود.
 * @property itemId شناسه آیتم
 * @property intervalStart زمان شروع بازه (مثلاً 2025-05-30T00:00:00Z)
 * @property intervalEnd زمان پایان بازه (انحصاری)
 * @property totalSold تعداد کل فروش رفته
 * @property averagePrice میانگین قیمت فروش (کل درآمد / تعداد فروش)
 * @property totalRevenue کل درآمد حاصل از فروش این آیتم
 * @property uniqueBuyers تعداد خریداران یکتا (بر اساس userId)
 * @property demandFactor ضریب تقاضا = (تعداد فروش واقعی) / (تعداد فروش مورد انتظار)
 * @property expectedSales تعداد فروش مورد انتظار در این بازه (از تنظیمات خوانده می‌شود)
 */

data class ItemSalesStats(
    val itemId: String,
    val intervalStart: Instant,
    val intervalEnd: Instant,
    val totalSold: Int,
    val averagePrice: Long,
    val totalRevenue: Long,
    val uniqueBuyers: Int,
    val demandFactor: Double,
    val expectedSales: Int
)

/**
 * پیشنهاد قیمت جدید برای یک آیتم.
 * @property itemId شناسه آیتم
 * @property suggestedPrice قیمت پیشنهادی جدید (بر حسب سکه نرم)
 * @property reason دلیل تغییر قیمت (مقدار از پیش تعریف شده)
 * @property demandFactor ضریب تقاضایی که منجر به این پیشنهاد شده است
 * @property currentPrice قیمت فعلی آیتم در زمان پیشنهاد
 * @property basePrice قیمت پایه آیتم (از جدول item_definitions)
 * @property minAllowedPrice حداقل قیمت مجاز (قیمت پایه × minPriceRatio)
 * @property maxAllowedPrice حداکثر قیمت مجاز (قیمت پایه × maxPriceRatio)
 * @property createdAt زمان تولید پیشنهاد
 */

data class SuggestedPrice(
    val itemId: String,
    val suggestedPrice: Long,
    val reason: PriceChangeReason,
    val demandFactor: Double,
    val currentPrice: Long,
    val basePrice: Long,
    val minAllowedPrice: Long,
    val maxAllowedPrice: Long,
    val createdAt: Instant = Instant.now()
)

/**
 * دلایل ممکن برای تغییر قیمت پیشنهادی.
 */
enum class PriceChangeReason {
    DEMAND_HIGH,      // تقاضا بالاتر از حد انتظار → افزایش قیمت
    DEMAND_LOW,       // تقاضا پایین‌تر از حد انتظار → کاهش قیمت
    INFLATION_ADJUST, // تنظیم بر اساس نرخ تورم کل (در آینده)
    SUPPLY_SHOCK,     // شوک عرضه (موجودی جهانی رو به اتمام)
    MANUAL_OVERRIDE   // تغییر دستی توسط ادمین (در آینده)
}

/**
 * تنظیمات پویای شبیه‌ساز بازار که از دیتابیس خوانده می‌شود.
 * @property enabled فعال بودن شبیه‌ساز (تغییر قیمت واقعی)
 * @property intervalHours بازه زمانی جمع‌آوری داده به ساعت
 * @property demandFactorBase ضریب حساسیت پایه (مقدار بین 0.1 تا 2.0)
 * @property minPriceRatio حداقل نسبت قیمت به قیمت پایه (مثلاً 0.8)
 * @property maxPriceRatio حداکثر نسبت قیمت به قیمت پایه (مثلاً 2.0)
 * @property expectedSalesPerInterval تعداد فروش عادی مورد انتظار در هر بازه
 */
data class MarketSimulatorConfig(
    val enabled: Boolean = false,
    val intervalHours: Int = 24,
    val demandFactorBase: Double = 0.5,
    val minPriceRatio: Double = 0.8,
    val maxPriceRatio: Double = 2.0,
    val expectedSalesPerInterval: Int = 50
)

/**
 * نتیجه یک اجرای دوره‌ای شبیه‌ساز.
 * @param executionTime زمان شروع اجرا
 * @param durationMs مدت زمان اجرا به میلی‌ثانیه
 * @param itemsProcessed تعداد آیتم‌های پردازش شده
 * @param snapshotsCreated تعداد اسنپ‌شات‌های ذخیره شده
 * @param suggestedPricesCreated تعداد پیشنهادات قیمت ایجاد شده (در صورت فعال بودن)
 * @param status وضعیت اجرا (SUCCESS, PARTIAL, FAILED)
 * @param errorMessage پیام خطا (در صورت وجود)
 */
data class ExecutionLog(
    val executionTime: Instant,
    val durationMs: Long,
    val itemsProcessed: Int,
    val snapshotsCreated: Int,
    val suggestedPricesCreated: Int,
    val status: String,
    val errorMessage: String? = null
)