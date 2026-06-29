package com.gamehub.server.notifications

import com.gamehub.shared.cache.CacheProvider
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class Notification(
    val id: String,
    val type: String,           // "turn_reminder", "game_invite", "match_found", ...
    val title: String,
    val body: String,
    val data: Map<String, String> = emptyMap(),
    val createdAt: Long = System.currentTimeMillis(),
    var isRead: Boolean = false
)

class NotificationService(private val cache: CacheProvider) {
    private val json = Json { ignoreUnknownKeys = true }
    private val NOTIF_INBOX_PREFIX = "notif:inbox:"
    private val NOTIF_COOLDOWN_PREFIX = "notif:cooldown:"
    private val MAX_INBOX_SIZE = 200L

    /**
     * ارسال اعلان به یک کاربر خاص
     * @param userId شناسه کاربر (مهمان یا دائمی)
     * @param notification اعلان
     * @param forcePush اگر true باشد حتی در صورت آنلاین بودن، در صف ذخیره می‌شود
     */
    suspend fun sendNotification(userId: String, notification: Notification, forcePush: Boolean = false) {
        // بررسی خنک‌سازی (برای جلوگیری از اسپم)
        val cooldownKey = "$NOTIF_COOLDOWN_PREFIX${userId}:${notification.type}"
        val lastSent = cache.get(cooldownKey)
        if (lastSent != null && !forcePush) {
            // حداقل ۳۰ ثانیه بین دو اعلان هم‌نوع
            if (System.currentTimeMillis() - lastSent.toLong() < 30000) return
        }

        // ذخیره در صف کاربر
        val inboxKey = "$NOTIF_INBOX_PREFIX$userId"
        val notifJson = json.encodeToString(notification)
        cache.zadd(inboxKey, notification.createdAt.toDouble(), notifJson)
        cache.zremrangebyscore(inboxKey, 0.0, (System.currentTimeMillis() - 30L * 24 * 3600 * 1000).toDouble()) // حذف پیام‌های قدیمی‌تر از ۳۰ روز
        cache.zcard(inboxKey).let { if (it > MAX_INBOX_SIZE) cache.zremrangebyrank(inboxKey, 0, it - MAX_INBOX_SIZE - 1) }

        // به‌روزرسانی خنک‌سازی
        cache.set(cooldownKey, System.currentTimeMillis().toString(), 30)

        // (اختیاری) اگر کاربر در حال حاضر آنلاین است، می‌توانیم از WebSocket مستقیم بفرستیم
        // این کار توسط HubWebSocketHandler انجام می‌شود (بعداً اضافه می‌شود)
    }

    /**
     * دریافت اعلان‌های جدید از زمان مشخص
     * @param userId شناسه کاربر
     * @param sinceTimestamp زمان آخرین دریافت (میلی‌ثانیه)
     * @return لیست اعلان‌های جدید (مرتب نزولی)
     */
    suspend fun getNotificationsSince(userId: String, sinceTimestamp: Long): List<Notification> {
        val inboxKey = "$NOTIF_INBOX_PREFIX$userId"
        val all = cache.zrangebyscore(inboxKey, sinceTimestamp.toDouble() + 0.001, Double.MAX_VALUE, 100)
        return all.mapNotNull { try { json.decodeFromString<Notification>(it) } catch (e: Exception) { null } }
            .sortedByDescending { it.createdAt }
    }

    /**
     * علامت زدن یک اعلان به عنوان خوانده‌شده (اختیاری)
     */
    suspend fun markAsRead(userId: String, notificationId: String) {
        val inboxKey = "$NOTIF_INBOX_PREFIX$userId"
        val all = cache.zrangebyscore(inboxKey, 0.0, Double.MAX_VALUE, MAX_INBOX_SIZE.toInt())
        for (notifJson in all) {
            val notif = try { json.decodeFromString<Notification>(notifJson) } catch (e: Exception) { null } ?: continue
            if (notif.id == notificationId && !notif.isRead) {
                val updated = notif.copy(isRead = true)
                cache.zrem(inboxKey, notifJson)
                cache.zadd(inboxKey, notif.createdAt.toDouble(), json.encodeToString(updated))
                break
            }
        }
    }

    /**
     * دریافت تعداد اعلان‌های خوانده‌نشده (برای نمایش Badge)
     */
    suspend fun getUnreadCount(userId: String): Int {
        val inboxKey = "$NOTIF_INBOX_PREFIX$userId"
        val all = cache.zrangebyscore(inboxKey, 0.0, Double.MAX_VALUE, MAX_INBOX_SIZE.toInt())
        return all.count { notifJson ->
            try {
                !json.decodeFromString<Notification>(notifJson).isRead
            } catch (e: Exception) { false }
        }
    }
}