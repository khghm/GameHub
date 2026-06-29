package com.gamehub.host.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gamehub.host.ui.theme.*
import com.gamehub.host.viewmodel.PartyViewModel
import com.gamehub.host.viewmodel.PartyMemberUI

@Composable
fun PartyScreen(
    viewModel: PartyViewModel = viewModel(),
    onBack: () -> Unit,
    onStartGame: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val party by viewModel.party.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isLeader = party?.leaderId == viewModel.currentUserId
    var inviteUsername by remember { mutableStateOf("") }

    // Dialog states
    var showLeaveConfirmation by remember { mutableStateOf(false) }
    var showKickConfirmation by remember { mutableStateOf<String?>(null) }
    var selectedGameType by remember { mutableStateOf("ludo") } // Default to ludo

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundGradient)
            .padding(20.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🎉 پارتی",
                style = MaterialTheme.typography.headlineLarge,
                color = OnBackground,
                fontWeight = FontWeight.Black,
                fontSize = 32.sp
            )

            if (party != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Primary.copy(alpha = 0.15f)
                ) {
                    Text(
                        "ID: ${party!!.id}",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        color = Primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (party == null) {
            // Not in a party
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "پارتی",
                        tint = OnSurfaceVariant,
                        modifier = Modifier.size(80.dp)
                    )
                    Text(
                        "هنوز پارتی‌ای ندارید",
                        color = OnSurfaceVariant,
                        fontSize = 18.sp
                    )
                    Button(
                        onClick = { viewModel.createParty() },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary),
                        contentPadding = PaddingValues(horizontal = 32.dp, vertical = 14.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "ایجاد پارتی",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        } else {
            // In party
            // Party Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceVariant)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    // Leader info
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            Tertiary,
                                            Secondary
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = "رهبر",
                                tint = OnPrimary,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Column {
                            Text(
                                "رهبر پارتی",
                                color = OnSurfaceVariant,
                                fontSize = 12.sp
                            )
                            val leader = party!!.members.find { it.userId == party!!.leaderId }
                            Text(
                                leader?.username ?: party!!.leaderId,
                                color = OnSurface,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Members section
                    Text(
                        "اعضا (${party!!.members.size})",
                        color = OnSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(party!!.members) { member ->
                            PartyMemberCard(
                                member = member,
                                isLeader = member.userId == party!!.leaderId,
                                isCurrentUser = member.userId == viewModel.currentUserId,
                                canKick = isLeader && member.userId != viewModel.currentUserId,
                                onKick = { userId -> showKickConfirmation = userId }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Invite section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inviteUsername,
                        onValueChange = { inviteUsername = it },
                        label = { Text("نام کاربری برای دعوت") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = SurfaceVariant.copy(alpha = 0.3f),
                            unfocusedContainerColor = SurfaceVariant.copy(alpha = 0.15f)
                        ),
                        shape = RoundedCornerShape(14.dp)
                    )
                    Button(
                        onClick = {
                            if (inviteUsername.isNotEmpty()) {
                                viewModel.inviteToParty(inviteUsername)
                                inviteUsername = ""
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Secondary)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "دعوت")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isLeader) {
                // Leader options: game selector and start
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            "انتخاب بازی",
                            color = OnSurface,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            GameSelectorChip(
                                text = "🎲 Ludo",
                                selected = selectedGameType == "ludo",
                                onClick = { selectedGameType = "ludo" }
                            )
                            GameSelectorChip(
                                text = "❌ Tic Tac Toe",
                                selected = selectedGameType == "tictactoe",
                                onClick = { selectedGameType = "tictactoe" }
                            )
                            GameSelectorChip(
                                text = "♟️ Chess",
                                selected = selectedGameType == "chess",
                                onClick = { selectedGameType = "chess" }
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = { viewModel.startGame(selectedGameType) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Success),
                            contentPadding = PaddingValues(vertical = 14.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "شروع بازی: $selectedGameType",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = { showLeaveConfirmation = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.ExitToApp, contentDescription = "خروج")
                Spacer(modifier = Modifier.width(8.dp))
                Text("خروج از پارتی")
            }
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Primary)
            }
        }
    }

    // Leave confirmation dialog
    if (showLeaveConfirmation) {
        PremiumAlertDialog(
            onDismiss = { showLeaveConfirmation = false },
            title = "خروج از پارتی؟",
            text = "آیا مطمئن هستید که می‌خواهید از پارتی خارج شوید؟",
            confirmText = "خروج",
            onConfirm = {
                viewModel.leaveParty()
                showLeaveConfirmation = false
            }
        )
    }

    // Kick confirmation dialog
    showKickConfirmation?.let { userIdToKick ->
        PremiumAlertDialog(
            onDismiss = { showKickConfirmation = null },
            title = "اخراج عضو؟",
            text = "آیا مطمئن هستید که می‌خواهید این عضو را اخراج کنید؟",
            confirmText = "اخراج",
            confirmColor = Error,
            onConfirm = {
                viewModel.kickMember(userIdToKick)
                showKickConfirmation = null
            }
        )
    }
}

@Composable
fun PartyMemberCard(
    member: PartyMemberUI,
    isLeader: Boolean,
    isCurrentUser: Boolean,
    canKick: Boolean,
    onKick: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentUser) Primary.copy(alpha = 0.12f) else SurfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            if (isLeader) {
                                Brush.verticalGradient(listOf(Tertiary, Secondary))
                            } else {
                                Brush.verticalGradient(listOf(Primary, Secondary))
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (isLeader) "👑" else "👤",
                        fontSize = 20.sp
                    )
                }

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            member.username,
                            fontWeight = FontWeight.Bold,
                            color = if (isCurrentUser) Primary else OnSurface
                        )
                        if (isLeader) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Tertiary.copy(alpha = 0.3f)
                            ) {
                                Text(
                                    "رهبر",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    color = Tertiary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }

                // Online status
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(if (member.isOnline) Success else OnSurfaceVariant)
                )
            }

            if (canKick) {
                TextButton(onClick = { onKick(member.userId) }) {
                    Text("اخراج", color = Error)
                }
            }
        }
    }
}

@Composable
fun GameSelectorChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium) },
        shape = RoundedCornerShape(12.dp),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = Primary.copy(alpha = 0.25f),
            selectedLabelColor = Primary
        )
    )
}

@Composable
fun PremiumAlertDialog(
    onDismiss: () -> Unit,
    title: String,
    text: String,
    confirmText: String,
    confirmColor: Color = Primary,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                title,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        },
        text = { Text(text, color = OnSurfaceVariant) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = confirmColor)
            ) {
                Text(confirmText, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("لغو")
            }
        },
        containerColor = Surface
    )
}
