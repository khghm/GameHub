package com.gamehub.server

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import kotlin.system.measureTimeMillis

/**
 * شبیه‌سازی بار سبک – فقط برای تخمین اولیه.
 * این تست به طور پیش‌فرض غیرفعال است (@Disabled) تا در CI اجرا نشود.
 * برای اجرا، @Disabled را حذف کنید.
 */
@Disabled("فقط برای اجرای دستی در محیط توسعه")
class LoadSimulationTest {

    @Test
    fun `simulate 500 concurrent guest logins and matchmaking requests`() = runBlocking {
        val concurrency = 500
        val client = HttpClient() // واقعی (OkHttp)

        val time = measureTimeMillis {
            val channel = Channel<Boolean>(concurrency)
            repeat(concurrency) { i ->
                launch {
                    try {
                        // 1. ثبت مهمان
                        val guestResp = client.post("http://localhost:8080/api/auth/guest") {
                            contentType(ContentType.Application.Json)
                            setBody("""{"deviceId":"load-$i"}""")
                        }
                        val json = Json.parseToJsonElement(guestResp.bodyAsText()).jsonObject
                        val token = json["token"]?.jsonPrimitive?.content ?: return@launch

                        // 2. درخواست مچ‌میکینگ (با تاخیر تصادفی)
                        delay((10..50).random().toLong())
                        val mmResp = client.post("http://localhost:8080/api/matchmaking/join") {
                            header(HttpHeaders.Authorization, "Bearer $token")
                            contentType(ContentType.Application.Json)
                            setBody("""{"gameType":"tictactoe","playerName":"load-$i"}""")
                        }
                        if (mmResp.status == HttpStatusCode.OK) {
                            channel.send(true)
                        } else {
                            channel.send(false)
                        }
                    } catch (e: Exception) {
                        channel.send(false)
                    }
                }
            }
            repeat(concurrency) { channel.receive() }
        }

        println("✅ زمان اجرا برای $concurrency کاربر: ${time}ms")
        // انتظار داریم زمان حدود ۵-۱۰ ثانیه باشد (۵۰۰ کاربر)
    }
}