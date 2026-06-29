package com.gamehub.host.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gamehub.host.ui.theme.*
import com.gamehub.host.viewmodel.TournamentUI
import com.gamehub.host.viewmodel.TournamentViewModel

@Composable
fun TournamentListScreen(
    viewModel: TournamentViewModel = viewModel(),
    onBack: () -> Unit,
    onTournamentClick: (String) -> Unit,
    currentUsername: String
) {
    val tournaments by viewModel.tournaments.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedGameType by remember { mutableStateOf("tictactoe") }
    var tournamentName by remember { mutableStateOf("") }
    var entryFee by remember { mutableStateOf("0") }

    LaunchedEffect(Unit) {
        viewModel.playerName = currentUsername
        viewModel.listTournaments()
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
            Text("🏆 تورنمنت‌ها", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = OnBackground)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = { viewModel.listTournaments() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = OnSurfaceVariant)
                }
                IconButton(onClick = { showCreateDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add", tint = Primary)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (tournaments.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                Text("هیچ تورنمنتی یافت نشد", color = OnSurfaceVariant)
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(tournaments) { tournament ->
                    TournamentCard(tournament = tournament, onClick = { onTournamentClick(tournament.id) })
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateTournamentDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { gameType, name, fee ->
                viewModel.createTournament(
                    gameType = gameType,
                    name = name,
                    entryFee = fee.toLongOrNull() ?: 0L
                )
                showCreateDialog = false
            },
            selectedGameType = selectedGameType,
            onGameTypeChange = { selectedGameType = it },
            tournamentName = tournamentName,
            onTournamentNameChange = { tournamentName = it },
            entryFee = entryFee,
            onEntryFeeChange = { entryFee = it }
        )
    }
}

@Composable
fun TournamentCard(tournament: TournamentUI, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(18.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(tournament.name, fontWeight = FontWeight.Bold, color = OnSurface, fontSize = 16.sp)
                Text("🎮 ${tournament.gameId}", fontSize = 13.sp, color = OnSurfaceVariant)
                Text("👥 ${tournament.currentParticipants}/${tournament.maxParticipants}", fontSize = 13.sp, color = OnSurfaceVariant)
                if (tournament.entryFeeCoins > 0) {
                    Text("💰 ${tournament.entryFeeCoins} سکه", fontSize = 13.sp, color = Warning)
                }
            }
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = when (tournament.status) {
                    "waiting" -> Warning.copy(alpha = 0.2f)
                    "registration" -> Primary.copy(alpha = 0.2f)
                    "in_progress" -> Success.copy(alpha = 0.2f)
                    "completed" -> SurfaceVariant.copy(alpha = 0.2f)
                    else -> SurfaceVariant.copy(alpha = 0.2f)
                }
            ) {
                Text(
                    when (tournament.status) {
                        "waiting" -> "در انتظار شروع"
                        "registration" -> "ثبت‌نام"
                        "in_progress" -> "در حال برگزاری"
                        "completed" -> "پایان یافته"
                        else -> tournament.status
                    },
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    fontSize = 12.sp,
                    color = when (tournament.status) {
                        "waiting" -> Warning
                        "registration" -> Primary
                        "in_progress" -> Success
                        "completed" -> OnSurfaceVariant
                        else -> OnSurfaceVariant
                    },
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun CreateTournamentDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String, String) -> Unit,
    selectedGameType: String,
    onGameTypeChange: (String) -> Unit,
    tournamentName: String,
    onTournamentNameChange: (String) -> Unit,
    entryFee: String,
    onEntryFeeChange: (String) -> Unit
) {
    PremiumAlertDialog(
        onDismiss = onDismiss,
        title = "ایجاد تورنمنت جدید",
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("نام تورنمنت:", style = MaterialTheme.typography.bodyMedium, color = OnSurface)
                OutlinedTextField(
                    value = tournamentName,
                    onValueChange = onTournamentNameChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        unfocusedBorderColor = SurfaceVariant,
                        focusedContainerColor = SurfaceVariant.copy(alpha = 0.3f),
                        unfocusedContainerColor = SurfaceVariant.copy(alpha = 0.15f)
                    )
                )
                Text("بازی:", style = MaterialTheme.typography.bodyMedium, color = OnSurface)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("tictactoe", "connectfour", "uno", "ludo").forEach { game ->
                        FilterChip(
                            selected = selectedGameType == game,
                            onClick = { onGameTypeChange(game) },
                            label = { Text(game, fontWeight = if (selectedGameType == game) FontWeight.Bold else FontWeight.Medium) },
                            shape = RoundedCornerShape(12.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Primary.copy(alpha = 0.25f),
                                selectedLabelColor = Primary,
                                containerColor = SurfaceVariant.copy(alpha = 0.2f),
                                labelColor = OnSurfaceVariant
                            )
                        )
                    }
                }
                Text("هزینه ورودی (سکه):", style = MaterialTheme.typography.bodyMedium, color = OnSurface)
                OutlinedTextField(
                    value = entryFee,
                    onValueChange = onEntryFeeChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        unfocusedBorderColor = SurfaceVariant,
                        focusedContainerColor = SurfaceVariant.copy(alpha = 0.3f),
                        unfocusedContainerColor = SurfaceVariant.copy(alpha = 0.15f)
                    )
                )
            }
        },
        confirmText = "ایجاد",
        onConfirm = { onCreate(selectedGameType, tournamentName, entryFee) }
    )
}