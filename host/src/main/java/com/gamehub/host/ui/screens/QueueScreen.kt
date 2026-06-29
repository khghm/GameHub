package com.gamehub.host.ui.screens

import android.app.Activity
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gamehub.host.network.MatchmakingClient
import com.gamehub.host.viewmodel.AuthViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*

@Composable
fun QueueScreen(
    gameType: String,
    playerName: String,
    onMatchFound: (String, String,String) -> Unit,
    onCancel: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "queue")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.2f,
        animationSpec = infiniteRepeatable(animation = tween(1000), repeatMode = RepeatMode.Reverse),
        label = "scale"
    )

    var matchGameId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        matchGameId = null
    }
    val scope = rememberCoroutineScope()

    val activity = LocalContext.current as Activity
    val authViewModel: AuthViewModel = viewModel(viewModelStoreOwner = activity as ViewModelStoreOwner)
    val client = remember { MatchmakingClient() }
    val finalPlayerName = remember(playerName) {
        if (playerName.isBlank() || playerName == "player1") "Player_${(1000..9999).random()}" else playerName
    }

    LaunchedEffect(gameType, finalPlayerName) {
        val client = HttpClient(OkHttp)
        val token = com.gamehub.host.network.GlobalAuth.token ?: ""
        try {
            while (true) {
                val serverIp = com.gamehub.host.BuildConfig.SERVER_IP
                android.util.Log.d("Matchmaking", "Sending request for gameType=$gameType")
                val response: HttpResponse = client.post("http://$serverIp:8080/api/matchmaking/join") {
                    header("Authorization", "Bearer $token")
                    contentType(ContentType.Application.Json)
                    setBody("""{"gameType":"$gameType","playerName":"$finalPlayerName"}""")
                }
                val body = response.bodyAsText()
                if (!body.trimStart().startsWith("{")) {
                    // پاسخ JSON نیست (احتمالاً خطا). دوباره تلاش کن
                    delay(1500)
                    continue
                }
                val json = try {
                    Json.parseToJsonElement(body).jsonObject
                } catch (e: Exception) {
                    delay(1500)
                    continue
                }
                val status = json["status"]?.jsonPrimitive?.content

                if (status == "matched") {
                    val gameId = json["gameId"]?.jsonPrimitive?.content
                    if (gameId != null && gameId != "waiting") {
                        matchGameId = gameId
                        break  // از حلقه خارج شو
                    }
                }
                delay(1500) // ۱.۵ ثانیه صبر کن و دوباره تلاش کن
            }
        } finally {
            client.close()
        }
    }

    LaunchedEffect(matchGameId) {
        matchGameId?.let { gameId ->
            withContext(Dispatchers.IO) {
                client.disconnect()
            }
            val instanceKey = java.util.UUID.randomUUID().toString()
            onMatchFound(gameId, "opponent", instanceKey)   // به تابع onMatchFound تغییر دهید
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            scope.launch { client.disconnect() }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
            .background(Brush.verticalGradient(colors = listOf(Color(0xFF1A237E), Color(0xFF0D47A1)))),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Box(modifier = Modifier.size(100.dp).scale(scale).clip(CircleShape).background(Color(0xFF1565C0)), contentAlignment = Alignment.Center) {
                Text("🔍", fontSize = 48.sp)
            }
            Spacer(Modifier.height(32.dp))
            Text("در حال جستجوی حریف...", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text("بازی: $gameType", fontSize = 16.sp, color = Color.White.copy(alpha = 0.7f))
            Text("بازیکن: $finalPlayerName", fontSize = 14.sp, color = Color.White.copy(alpha = 0.5f))
            Spacer(Modifier.height(48.dp))
            Button(onClick = onCancel, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)), shape = RoundedCornerShape(12.dp), modifier = Modifier.height(50.dp)) {
                Text("لغو", fontSize = 18.sp)
            }
        }
    }
}