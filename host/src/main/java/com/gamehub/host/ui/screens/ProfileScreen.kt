package com.gamehub.host.ui.screens

import android.annotation.SuppressLint
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gamehub.host.ui.theme.*
import com.gamehub.host.viewmodel.AuthViewModel

@SuppressLint("StateFlowValueCalledInComposition")
@Composable
fun ProfileScreen(
    onLogout: () -> Unit,
    viewModel: AuthViewModel = viewModel(),
    onViewMatchHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val user = viewModel.currentUser.value
    var showEditDialog by remember { mutableStateOf(false) }
    var editDisplayName by remember { mutableStateOf(user?.displayName ?: "") }

    LaunchedEffect(Unit) {
        viewModel.loadProfile()
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundGradient),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(20.dp)
    ) {
        // Header with avatar
        item {
            Surface(
                color = Surface,
                shape = RoundedCornerShape(28.dp),
                tonalElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Avatar
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(listOf(Primary, Secondary, Tertiary))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (user?.displayName?.firstOrNull() ?: user?.username?.firstOrNull() ?: "?").toString().uppercase(),
                            fontSize = 46.sp,
                            fontWeight = FontWeight.Bold,
                            color = OnPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = user?.displayName ?: user?.username ?: "Player",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = OnSurface
                    )

                    Text(
                        text = "@${user?.username ?: ""}",
                        fontSize = 15.sp,
                        color = OnSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Stats cards
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = "برد",
                    value = "${user?.wins ?: 0}",
                    emoji = "🏆",
                    color = Success,
                    modifier = Modifier.fillMaxWidth(0.33f)
                )
                StatCard(
                    title = "باخت",
                    value = "${user?.losses ?: 0}",
                    emoji = "📉",
                    color = Error,
                    modifier = Modifier.fillMaxWidth(0.5f)
                )
                StatCard(
                    title = "مساوی",
                    value = "${user?.draws ?: 0}",
                    emoji = "🤝",
                    color = Primary,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Win rate
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                val totalGames = (user?.wins ?: 0) + (user?.losses ?: 0) + (user?.draws ?: 0)
                val winRate = if (totalGames > 0) ((user?.wins ?: 0).toFloat() / totalGames * 100).toInt() else 0

                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "📊 درصد برنده شدن",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = OnSurface
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(SurfaceVariant.copy(alpha = 0.3f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(winRate / 100f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(18.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(Success.copy(alpha = 0.85f), Success)
                                    )
                                )
                        )
                        Text(
                            "$winRate%",
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.Center),
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = OnPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "$totalGames بازی انجام شده",
                        fontSize = 14.sp,
                        color = OnSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Actions
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ProfileButton(
                    text = "ویرایش پروفایل",
                    icon = Icons.Default.Edit,
                    color = Primary,
                    onClick = {
                        editDisplayName = user?.displayName ?: ""
                        showEditDialog = true
                    },
                    modifier = Modifier.fillMaxWidth(0.5f)
                )
                ProfileButton(
                    text = "خروج",
                    icon = Icons.Default.ExitToApp,
                    color = Error,
                    onClick = onLogout,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        item {
            ProfileButton(
                text = "📜 تاریخچه بازی‌ها",
                icon = Icons.Default.DateRange,
                color = Secondary,
                onClick = onViewMatchHistory,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    // Edit display name dialog
    if (showEditDialog) {
        PremiumAlertDialog(
            onDismiss = { showEditDialog = false },
            title = "✏️ ویرایش نام نمایشی",
            text = {
                OutlinedTextField(
                    value = editDisplayName,
                    onValueChange = { editDisplayName = it },
                    label = { Text("نام نمایشی", color = OnSurfaceVariant) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = OnSurface,
                        unfocusedTextColor = OnSurface,
                        focusedBorderColor = Primary,
                        unfocusedBorderColor = SurfaceVariant,
                        cursorColor = Primary,
                        focusedContainerColor = Surface.copy(alpha = 0.8f),
                        unfocusedContainerColor = Surface.copy(alpha = 0.5f)
                    )
                )
            },
            confirmText = "ذخیره",
            onConfirm = {
                if (editDisplayName.isNotEmpty()) {
                    viewModel.updateProfile(editDisplayName)
                    showEditDialog = false
                }
            },
            dismissText = "لغو"
        )
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    emoji: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = emoji, fontSize = 36.sp)
            Spacer(modifier = Modifier.height(10.dp))
            Text(text = value, fontSize = 30.sp, fontWeight = FontWeight.Bold, color = color)
            Text(text = title, fontSize = 13.sp, color = OnSurfaceVariant, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun ProfileButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(60.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color),
        shape = RoundedCornerShape(20.dp),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp)
    ) {
        Icon(icon, contentDescription = null, tint = OnPrimary, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Text(text, color = OnPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
    }
}

@Composable
fun PremiumAlertDialog(
    onDismiss: () -> Unit,
    title: String,
    text: @Composable () -> Unit,
    confirmText: String,
    confirmColor: androidx.compose.ui.graphics.Color = Primary,
    dismissText: String = "لغو",
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                title,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = OnSurface
            )
        },
        text = text,
        confirmButton = {
            Button(
                onClick = onConfirm,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = confirmColor),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(confirmText, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(dismissText, color = OnSurfaceVariant, fontWeight = FontWeight.Medium, fontSize = 14.sp)
            }
        },
        containerColor = Surface,
        shape = RoundedCornerShape(24.dp)
    )
}
