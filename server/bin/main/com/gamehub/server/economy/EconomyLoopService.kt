// server/src/main/kotlin/com/gamehub/server/economy/EconomyLoopService.kt
package com.gamehub.server.economy

import com.gamehub.shared.cache.CacheProvider

/**
 * سرویس رصد جریان سکه (فقط برای آمار، بدون اعمال هیچ تغییری در موجودی کاربران)
 */
class EconomyLoopService(
    private val cache: CacheProvider
) {
    // ثبت جریان سکه (برای آمار تورم)
    suspend fun recordFlow(delta: Long) {
        if (delta == 0L) return
        val minuteBucket = getCurrentMinuteBucket()
        if (delta > 0) {
            cache.incrBy("$minuteBucket:inflow", delta)
        } else {
            cache.incrBy("$minuteBucket:outflow", -delta)
        }
        cache.expire("$minuteBucket:inflow", 7200)
        cache.expire("$minuteBucket:outflow", 7200)
    }

    private fun getCurrentMinuteBucket(): String {
        val minute = System.currentTimeMillis() / 60000
        return "economy:flow:minute:$minute"
    }
}