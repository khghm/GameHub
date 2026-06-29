package com.gamehub.host.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamehub.host.ui.theme.*
import com.gamehub.host.viewmodel.ChatDisplayMessage
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    localPlayerName: String,
    messages: List<ChatDisplayMessage>,
    onlineUsers: List<String>,
    onSendMessage: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var inputText by remember { mutableStateOf("") }
    var showOnlineUsers by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundGradient)
    ) {
        // Chat header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Surface,
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "💬 چت عمومی",
                    color = OnSurface,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Online users badge
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Success.copy(alpha = 0.15f),
                        modifier = Modifier.clickable { showOnlineUsers = !showOnlineUsers }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Success)
                            )
                            Text(
                                "${onlineUsers.size} آنلاین",
                                color = Success,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }

                    IconButton(onClick = onClose) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "بستن",
                            tint = OnSurfaceVariant
                        )
                    }
                }
            }
        }

        // Online users panel
        if (showOnlineUsers) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 140.dp),
                color = Surface
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "کاربران آنلاین",
                        color = OnSurface,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    ) {
                        onlineUsers.take(12).forEach { user ->
                            val isMe = user == localPlayerName
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isMe) {
                                                Brush.verticalGradient(listOf(Primary, Secondary))
                                            } else {
                                                Brush.verticalGradient(listOf(Secondary, Tertiary))
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        user.first().uppercase(),
                                        color = OnPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    if (isMe) "شما" else user.take(8),
                                    color = OnSurfaceVariant,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }

        // Messages list
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            items(messages) { msg ->
                if (msg.isSystem) {
                    // System message centered
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Surface
                        ) {
                            Text(
                                msg.message,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                fontSize = 12.sp,
                                color = OnSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (msg.isMine) Arrangement.End else Arrangement.Start
                    ) {
                        Row(
                            modifier = Modifier.widthIn(max = 290.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            if (!msg.isMine) {
                                // Avatar
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.verticalGradient(listOf(Secondary, Tertiary))
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        msg.from.first().uppercase(),
                                        color = OnPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Column(
                                horizontalAlignment = if (msg.isMine) Alignment.End else Alignment.Start
                            ) {
                                if (!msg.isMine) {
                                    Text(
                                        msg.from,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = OnSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                }

                                Surface(
                                    shape = RoundedCornerShape(
                                        topStart = 18.dp,
                                        topEnd = 18.dp,
                                        bottomStart = if (msg.isMine) 18.dp else 6.dp,
                                        bottomEnd = if (msg.isMine) 6.dp else 18.dp
                                    ),
                                    color = if (msg.isMine) Primary else Surface
                                ) {
                                    Text(
                                        msg.message,
                                        modifier = Modifier.padding(14.dp),
                                        fontSize = 15.sp,
                                        color = if (msg.isMine) OnPrimary else OnSurface
                                    )
                                }

                                Text(
                                    formatTime(msg.timestamp),
                                    fontSize = 10.sp,
                                    color = OnSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 3.dp)
                                )
                            }

                            if (msg.isMine) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.verticalGradient(listOf(Primary, Secondary))
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        msg.from.first().uppercase(),
                                        color = OnPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Input field
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shadowElevation = 8.dp,
            color = Surface
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("پیام خود را بنویسید...", fontSize = 14.sp) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        unfocusedBorderColor = SurfaceVariant,
                        focusedContainerColor = SurfaceVariant.copy(alpha = 0.3f),
                        unfocusedContainerColor = SurfaceVariant.copy(alpha = 0.15f)
                    )
                )

                FloatingActionButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            onSendMessage(inputText.trim())
                            inputText = ""
                        }
                    },
                    containerColor = Primary,
                    modifier = Modifier.size(52.dp),
                    shape = CircleShape
                ) {
                    Icon(
                        Icons.Default.Send,
                        contentDescription = "ارسال",
                        tint = OnPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

private fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
