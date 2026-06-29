package com.gamehub.host.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.gamehub.host.R
import com.gamehub.host.network.ApiClient
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Serializable
data class NotificationData(
    val id: String,
    val type: String,
    val title: String,
    val body: String,
    val data: Map<String, String> = emptyMap(),
    val createdAt: Long,
    val isRead: Boolean = false
)

class NotificationManager(private val context: Context, private val authToken: () -> String?) {
    private val apiClient = ApiClient()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var lastPollTimestamp = 0L
    private var isPolling = false
    private val json = Json { ignoreUnknownKeys = true }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "gamehub_notifications",
                "GameHub Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "اعلان‌های نوبت، دعوت به بازی و پیام‌ها"
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    fun startPolling(intervalSeconds: Long = 60) {
        if (isPolling) return
        isPolling = true
        scope.launch {
            while (isPolling) {
                val token = authToken()
                if (!token.isNullOrBlank()) {
                    pollNotifications()
                }
                delay(intervalSeconds * 1000)
            }
        }
    }

    fun stopPolling() {
        isPolling = false
    }

    private suspend fun pollNotifications() {
        val token = authToken() ?: return
        apiClient.setToken(token)
        val response = try {
            apiClient.get("notifications/poll?since=$lastPollTimestamp")
        } catch (e: Exception) {
            return
        }
        val jsonResponse = Json.parseToJsonElement(response).jsonObject
        val list = jsonResponse["notifications"]?.jsonPrimitive?.content ?: return
        val notifications = try {
            json.decodeFromString<List<NotificationData>>(list)
        } catch (e: Exception) { emptyList() }
        if (notifications.isNotEmpty()) {
            lastPollTimestamp = notifications.maxOf { it.createdAt }
            notifications.forEach { showLocalNotification(it) }
        }
    }

    private fun showLocalNotification(notification: NotificationData) {
        val manager = context.getSystemService(NotificationManager::class.java)
        val builder = NotificationCompat.Builder(context, "gamehub_notifications")
            .setSmallIcon(R.drawable.ic_notification) // باید یک آیکون در drawable اضافه کنید
            .setContentTitle(notification.title)
            .setContentText(notification.body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
        manager.notify(notification.id.hashCode(), builder.build())
    }

    // در ApiClient یک متد کمکی اضافه کنید:
    private suspend fun ApiClient.get(path: String): String {
        // کد فعلی get را استفاده کنید (در ApiClient متد getLeaderboard وجود دارد)
        // بهتر است یک متد عمومی GET در ApiClient بسازید
        return ""
    }
}