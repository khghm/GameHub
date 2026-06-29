package com.gamehub.host.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.gamehub.host.viewmodel.UserProfile

@Composable
fun ProfilePopup(
    username: String,
    profile: UserProfile?,
    isOnline: Boolean = false,
    onDismiss: () -> Unit,
    onAddFriend: (String) -> Unit,
    onInviteToGame: (String) -> Unit,
    onSendMessage: (String) -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color(0xFF3B82F6), Color(0xFF1D4ED8))
                            )
                        )
                )

                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(40.dp))

                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF334155))
                            .border(4.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = username.first().uppercase(),
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    if (isOnline) {
                        Box(
                            modifier = Modifier
                                .offset(x = 30.dp, y = (-20).dp)
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF22C55E))
                                .border(2.dp, Color.White, CircleShape)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = profile?.displayName ?: username,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "@$username",
                        fontSize = 14.sp,
                        color = Color(0xFF94A3B8)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (profile != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            StatItem(emoji = "🏆", label = "Wins", value = "${profile.wins}", color = Color(0xFFFBBF24))
                            StatItem(emoji = "🎮", label = "Played", value = "${profile.wins + profile.losses + profile.draws}", color = Color(0xFF3B82F6))
                            StatItem(emoji = "📊", label = "Win Rate", value = "${
                                if (profile.wins + profile.losses + profile.draws > 0)
                                    ((profile.wins.toFloat() / (profile.wins + profile.losses + profile.draws)) * 100).toInt()
                                else 0
                            }%", color = Color(0xFF22C55E))
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ActionButton(emoji = "👤", label = "Add Friend", color = Color(0xFF3B82F6), onClick = { onAddFriend(username) }, modifier = Modifier.weight(1f))
                        ActionButton(emoji = "🎮", label = "Invite", color = Color(0xFF22C55E), onClick = { onInviteToGame(username) }, modifier = Modifier.weight(1f))
                        ActionButton(emoji = "💬", label = "Message", color = Color(0xFFF97316), onClick = { onSendMessage(username) }, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun StatItem(
    emoji: String,
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = emoji, fontSize = 24.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = Color(0xFF94A3B8)
        )
    }
}

@Composable
fun ActionButton(
    emoji: String,
    label: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color.copy(alpha = 0.2f)),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = emoji, fontSize = 18.sp)
            Text(
                text = label,
                fontSize = 10.sp,
                color = color,
                fontWeight = FontWeight.Medium
            )
        }
    }
}