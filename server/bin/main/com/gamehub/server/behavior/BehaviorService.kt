package com.gamehub.server.behavior

import com.gamehub.server.repository.BehaviorRepository
import com.gamehub.shared.behavior.BehaviorInfo
import kotlinx.coroutines.delay

class BehaviorService(
    private val behaviorRepository: BehaviorRepository
) {
    companion object {
        const val GRACE_PERIOD_HOURS = 2L
    }

    suspend fun getBehavior(userId: String): BehaviorInfo = behaviorRepository.getOrCreate(userId)

    /**
     * اعمال جریمه (رویداد منفی)
     */
    suspend fun applyPenalty(userId: String, penaltyType: String, matchId: String? = null): BehaviorInfo {
        return applyEvent(userId, penaltyType, true, matchId)
    }

    /**
     * اعمال پاداش (رویداد مثبت)
     */
    suspend fun applyBonus(userId: String, bonusType: String, matchId: String? = null): BehaviorInfo {
        return applyEvent(userId, bonusType, false, matchId)
    }

    private suspend fun applyEvent(userId: String, type: String, isPenalty: Boolean, matchId: String?): BehaviorInfo {
        val delta = getDelta(type, isPenalty)
        if (delta == 0) return behaviorRepository.getOrCreate(userId)

        // محدودیت نرخ برای جریمه‌ها (حداکثر یک بار در ۶ ساعت)
        if (isPenalty) {
            val lastEvent = behaviorRepository.getLastEventTime(userId, type)
            if (lastEvent != null && (System.currentTimeMillis() - lastEvent) < 6 * 3600_000) {
                return behaviorRepository.getOrCreate(userId)
            }
        }

        // محدودیت پاداش روزانه برای اتمام تمیز (حداکثر +۲۰ در روز)
        if (!isPenalty && type == "game_finished_clean") {
            val todayBonus = behaviorRepository.getTodayBonusSum(userId)
            if (todayBonus >= 20) return behaviorRepository.getOrCreate(userId)
        }

        // اعمال تغییر امتیاز
        val newInfo = behaviorRepository.updateScore(userId, delta, type, matchId)

        // اگر باند تغییر کرده، Grace Period را ذخیره می‌کنیم (برای مچ‌میکینگ)
        val oldBand = getBehavior(userId).band
        if (newInfo.band != oldBand) {
            behaviorRepository.saveBandChangeGrace(userId, System.currentTimeMillis() + GRACE_PERIOD_HOURS * 3600_000)
        }

        return newInfo
    }

    /**
     * گرفتن باند مؤثر (با در نظر گرفتن Grace Period)
     * @param userId شناسه کاربر
     * @return باند واقعی که باید در مچ‌میکینگ استفاده شود
     */
    suspend fun getEffectiveBand(userId: String): String {
        val info = getBehavior(userId)
        val graceUntil = behaviorRepository.getBandChangeGrace(userId)
        if (graceUntil != null && graceUntil > System.currentTimeMillis()) {
            // در Grace Period، باند قبلی را برگردان (نیاز به ذخیره باند قبلی داریم)
            val previousBand = behaviorRepository.getPreviousBand(userId)
            return previousBand ?: info.band
        }
        return info.band
    }

    private fun getDelta(type: String, isPenalty: Boolean): Int {
        return when (type) {
            "game_left_ranked" -> -5
            "game_left_casual" -> -3
            "afk" -> -5
            "cheat_confirmed" -> -30
            "report_confirmed" -> -10
            "game_finished_clean" -> 2
            "clean_day" -> 3
            "weekly_bonus" -> 7
            else -> 0
        }
    }
}