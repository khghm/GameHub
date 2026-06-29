package com.gamehub.shared.matchmaking

import com.gamehub.shared.core.BehaviorBand
import kotlinx.serialization.Serializable

@Serializable
data class QueueEntry(
    val userId: String,               // برای افراد تکی: userId، برای گروه: partyId
    val gameId: String,
    val mode: String,                 // "casual", "ranked"
    val skillRating: SkillRating,
    val band: BehaviorBand,
    val joinedAt: Long,
    val partyId: String? = null,      // اگر عضو یک گروه است
    val partySize: Int = 1,           // تعداد اعضا (1 برای افراد تکی)
    val region: String = "IR"         // برای پشتیبانی آینده (منطقه: IR, EU, NA, ...)
)

@Serializable
data class SkillRating(
    val mean: Double,                 // μ: میانگین مهارت
    val standardDeviation: Double,    // σ: انحراف معیار
    val volatility: Double = 0.06     // σ̃: نوسان مهارت (مقدار پیش‌فرض Glicko-2)
) {
    fun queueScore(waitTimeSeconds: Double = 0.0): Double {
        val baseScore = mean - 3 * standardDeviation
        val waitBonus = (waitTimeSeconds / 60.0).coerceAtMost(5.0) * 50 // حداکثر ۲۵۰ امتیاز برای ۵ دقیقه انتظار
        return baseScore + waitBonus
    }
}