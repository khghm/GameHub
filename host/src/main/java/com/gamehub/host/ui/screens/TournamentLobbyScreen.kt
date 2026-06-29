package com.gamehub.host.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gamehub.host.ui.theme.*
import com.gamehub.host.viewmodel.TournamentViewModel

@Composable
fun TournamentLobbyScreen(
    tournamentId: String,
    viewModel: TournamentViewModel = viewModel(),
    onBack: () -> Unit,
    onStartBracket: (String) -> Unit
) {
    val tournament by viewModel.tournament.collectAsState()
    val isConnected by viewModel.isConnected.collectAsState()
    val playerName = viewModel.playerName

    LaunchedEffect(tournamentId) {
        viewModel.subscribe(tournamentId)
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.unsubscribe() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundGradient)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) { Text("← بازگشت", color = OnPrimary, fontSize = 16.sp) }
            Text("🏆 لابی تورنمنت", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = OnBackground)
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (tournament != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(tournament!!.name, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = OnSurface)
                    Text("🎮 ${tournament!!.gameId}", color = OnSurfaceVariant, fontSize = 14.sp)
                    Text("👥 ${tournament!!.currentParticipants}/${tournament!!.maxParticipants}", color = OnSurfaceVariant, fontSize = 14.sp)
                    Text("💰 جایزه: ${tournament!!.prizePoolCoins} سکه", color = Warning, fontSize = 14.sp)
                    if (tournament!!.entryFeeCoins > 0) {
                        Text("هزینه ورودی: ${tournament!!.entryFeeCoins} سکه", color = Secondary, fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = when (tournament!!.status) {
                            "waiting" -> Warning.copy(alpha = 0.2f)
                            "registration" -> Primary.copy(alpha = 0.2f)
                            "in_progress" -> Success.copy(alpha = 0.2f)
                            else -> SurfaceVariant.copy(alpha = 0.2f)
                        }
                    ) {
                        Text(
                            "وضعیت: ${getStatusText(tournament!!.status)}",
                            color = when (tournament!!.status) {
                                "waiting" -> Warning
                                "registration" -> Primary
                                "in_progress" -> Success
                                else -> OnSurfaceVariant
                            },
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (tournament!!.status == "registration" || tournament!!.status == "waiting") {
                Button(
                    onClick = { viewModel.joinTournament(tournamentId) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Success),
                    shape = RoundedCornerShape(16.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                    contentPadding = PaddingValues(vertical = 14.dp)
                ) {
                    Text("ثبت‌نام در تورنمنت", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }

            if (tournament!!.status == "in_progress") {
                Button(
                    onClick = { onStartBracket(tournamentId) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                    shape = RoundedCornerShape(16.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                    contentPadding = PaddingValues(vertical = 14.dp)
                ) {
                    Text("مشاهده براکت", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Primary)
            }
        }
    }
}

private fun getStatusText(status: String): String = when (status) {
    "waiting" -> "در انتظار شروع ثبت‌نام"
    "registration" -> "در حال ثبت‌نام"
    "in_progress" -> "در حال برگزاری"
    "completed" -> "پایان یافته"
    "prizes_distributed" -> "جوایز توزیع شد"
    else -> status
}