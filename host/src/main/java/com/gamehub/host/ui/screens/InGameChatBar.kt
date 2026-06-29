package com.gamehub.host.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.gamehub.host.viewmodel.GameViewModel
import com.gamehub.host.viewmodel.UserProfile
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

// ------------------- کامپوننت اصلی چت درون بازی -------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InGameChatBar(
    viewModel: GameViewModel,
    localPlayerName: String,
    modifier: Modifier = Modifier
) {
    val messages by viewModel.chatMessages.collectAsState()
    val isMuted by viewModel.isMuted.collectAsState()
    val replyTarget by viewModel.replyTarget.collectAsState()
    var isExpanded by remember { mutableStateOf(false) }
    var inputText by remember { mutableStateOf("") }
    var previewMessage by remember { mutableStateOf<String?>(null) }
    var previewSender by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()
    var profileUsername by remember { mutableStateOf<String?>(null) }
    var showProfilePopup by remember { mutableStateOf(false) }
    var profileData by remember { mutableStateOf<UserProfile?>(null) }
    var showEmojiPicker by remember { mutableStateOf(false) }

    // پیش‌نمایش پیام به مدت ۵ ثانیه در حالت بسته
    LaunchedEffect(messages.size) {
        val last = messages.lastOrNull()
        if (last != null && !isExpanded && !isMuted && last.from != localPlayerName) {
            previewMessage = last.message.take(35) + if (last.message.length > 35) "..." else ""
            previewSender = last.from
            delay(5000)
            if (messages.lastOrNull() == last) {
                previewMessage = null
                previewSender = null
            }
        }
    }

    // اسکرول خودکار به آخرین پیام
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    // دریافت پروفایل کاربر
    fun loadProfile(username: String) {
        viewModel.getUserProfile(username) { profileData = it }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        color = Color(0xFF1E293B),
        shadowElevation = 12.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (!isExpanded) {
                // ---------- حالت بسته (Collapsed) ----------
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clickable { isExpanded = true }
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { viewModel.toggleMute() }, modifier = Modifier.size(36.dp)) {
                        Text(if (isMuted) "🔇" else "💬", fontSize = 20.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    if (previewMessage != null && !isMuted) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = previewSender ?: "",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF38BDF8)
                            )
                            Text(
                                text = previewMessage ?: "",
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.8f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    } else {
                        Text(
                            text = "برای چت کلیک کنید...",
                            color = Color(0xFF94A3B8),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (messages.isNotEmpty()) {
                        Badge(containerColor = Color.Red) {
                            Text("${messages.size}")
                        }
                    }
                }
            } else {
                // ---------- حالت باز (Expanded) ----------
                Column(modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp)) {
                    // هدر
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { isExpanded = false }, modifier = Modifier.size(32.dp)) {
                            Text("▼", color = Color(0xFF38BDF8), fontSize = 16.sp)
                        }
                        Text("چت درون بازی", color = Color.White, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(onClick = { viewModel.toggleMute() }, modifier = Modifier.size(32.dp)) {
                            Text(if (isMuted) "🔇" else "🔔", fontSize = 16.sp)
                        }
                    }
                    HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f))

                    // نوار پاسخ (Reply Target)
                    replyTarget?.let { target ->
                        Surface(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF0F172A)
                        ) {
                            Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("در پاسخ به ${target.from}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                                    Text(target.message, fontSize = 12.sp, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                IconButton(onClick = { viewModel.setReplyTarget(null) }, modifier = Modifier.size(24.dp)) {
                                    Text("✕", color = Color.White, fontSize = 14.sp)
                                }
                            }
                        }
                    }

                    // لیست پیام‌ها
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 8.dp),
                        state = listState
                    ) {
                        itemsIndexed(messages) { _, msg ->
                            ChatMessageBubble(
                                msg = msg,
                                localPlayer = localPlayerName,
                                onReply = { viewModel.setReplyTarget(msg) },
                                onViewProfile = { username ->
                                    profileUsername = username
                                    loadProfile(username)
                                    showProfilePopup = true
                                }
                            )
                        }
                    }

                    // نوار ورودی
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // دکمه ایموجی
                        IconButton(
                            onClick = { showEmojiPicker = true },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Text("😊", fontSize = 20.sp)
                        }

                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            placeholder = { Text("پیام...", color = Color.Gray) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF38BDF8),
                                unfocusedBorderColor = Color.Gray
                            ),
                            shape = RoundedCornerShape(24.dp)
                        )

                        FilledTonalButton(
                            onClick = {
                                if (inputText.isNotBlank()) {
                                    viewModel.sendChatMessage(inputText.trim())
                                    inputText = ""
                                }
                            },
                            modifier = Modifier.height(48.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = Color(0xFF38BDF8)
                            )
                        ) {
                            Text("ارسال", color = Color.White)
                        }
                    }
                }
            }
        }
    }

    // انتخاب ایموجی
    if (showEmojiPicker) {
        EmojiPickerDialog(
            onEmojiSelected = { inputText += it },
            onDismiss = { showEmojiPicker = false }
        )
    }

    // پاپ‌آپ پروفایل کاربر
    if (showProfilePopup && profileUsername != null) {
        ProfilePopupDialog(
            username = profileUsername!!,
            profile = profileData,
            isOnline = true,
            onDismiss = { showProfilePopup = false },
            onAddFriend = { /* TODO */ },
            onInviteToGame = { /* TODO */ },
            onSendMessage = { /* TODO */ }
        )
    }
}

// ------------------- حباب پیام -------------------
@Composable
private fun ChatMessageBubble(
    msg: GameViewModel.ChatMessageItem,
    localPlayer: String,
    onReply: () -> Unit,
    onViewProfile: (String) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectTapGestures(onLongPress = { showMenu = true })
            }
            .padding(vertical = 2.dp),
        horizontalArrangement = if (msg.from == localPlayer) Arrangement.End else Arrangement.Start
    ) {
        // آواتار فرستنده (اگر پیام از خودمان نباشد)
        if (msg.from != localPlayer) {
            Column(
                modifier = Modifier.clickable { onViewProfile(msg.from) },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF475569)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(msg.from.first().uppercase(), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(msg.from, fontSize = 9.sp, color = Color(0xFF94A3B8))
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(horizontalAlignment = if (msg.from == localPlayer) Alignment.End else Alignment.Start) {
            // متن پیام
            Surface(
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (msg.from == localPlayer) 16.dp else 4.dp,
                    bottomEnd = if (msg.from == localPlayer) 4.dp else 16.dp
                ),
                color = if (msg.from == localPlayer) Color(0xFF1D4ED8) else Color(0xFF334155),
                modifier = Modifier.widthIn(max = 240.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(msg.message, fontSize = 14.sp, color = Color.White)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(msg.timestamp)),
                            fontSize = 9.sp,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    }
                }
            }
            // دکمه Reply
            TextButton(
                onClick = onReply,
                modifier = Modifier.height(20.dp).padding(top = 2.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text("پاسخ", fontSize = 10.sp, color = Color(0xFF38BDF8))
            }
        }

        // آواتار خودمان (اگر پیام از خودمان باشد)
        if (msg.from == localPlayer) {
            Spacer(modifier = Modifier.width(8.dp))
            Column(
                modifier = Modifier.clickable { },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E40AF)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(localPlayer.first().uppercase(), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(localPlayer, fontSize = 9.sp, color = Color(0xFF94A3B8))
            }
        }
    }

    // منوی Long-press
    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
        DropdownMenuItem(text = { Text("پاسخ") }, onClick = { showMenu = false; onReply() })
        DropdownMenuItem(text = { Text("گزارش") }, onClick = { showMenu = false })
        DropdownMenuItem(text = { Text("مشاهده پروفایل") }, onClick = { showMenu = false; onViewProfile(msg.from) })
    }
}

// ------------------- دیالوگ انتخاب ایموجی -------------------
@Composable
fun EmojiPickerDialog(
    onEmojiSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val emojis = listOf(
        "😀", "😂", "🤣", "😍", "😎", "🥳", "😢", "😡", "👍", "👎",
        "🔥", "💯", "❤️", "💔", "🎉", "🏆", "⚽", "🎮", "🕹️", "👾",
        "💬", "🤝", "🙏", "💪", "🧠", "👀", "🚀", "⭐", "🌟", "✨",
        "🍕", "☕", "🎵", "🎶", "📱", "💡", "🔑", "🗝️", "🎁", "💎",
        "🐶", "🐱", "🦊", "🐼", "🐨", "🦁", "🐸", "🐵", "🦄", "🐲",
        "🌈", "❄️", "☀️", "🌙", "⚡", "🌊", "🍀", "🌹", "🌸", "🌻"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("انتخاب ایموجی", fontWeight = FontWeight.Bold) },
        text = {
            LazyVerticalGrid(
                columns = GridCells.Fixed(8),
                modifier = Modifier.height(250.dp),
                contentPadding = PaddingValues(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(emojis) { emoji ->
                    Surface(
                        modifier = Modifier
                            .size(36.dp)
                            .clickable { onEmojiSelected(emoji) },
                        shape = RoundedCornerShape(8.dp),
                        color = Color.Transparent
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(emoji, fontSize = 22.sp)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("بستن") } }
    )
}

// ------------------- دیالوگ پروفایل کاربر -------------------
@Composable
fun ProfilePopupDialog(
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
            Box(modifier = Modifier.fillMaxWidth()) {
                // پس‌زمینه گرادیانتی
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

                    // آواتار
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

                    // آمار
                    if (profile != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            StatItem(emoji = "🏆", label = "برد", value = "${profile.wins}", color = Color(0xFFFBBF24))
                            StatItem(emoji = "🎮", label = "بازی", value = "${profile.wins + profile.losses + profile.draws}", color = Color(0xFF3B82F6))
                            StatItem(emoji = "📊", label = "برد %", value = "...", color = Color(0xFF22C55E))
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // دکمه‌های عملیاتی
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ActionButton(emoji = "👤", label = "افزودن دوست", color = Color(0xFF3B82F6), onClick = { onAddFriend(username) }, modifier = Modifier.weight(1f))
                        ActionButton(emoji = "🎮", label = "دعوت", color = Color(0xFF22C55E), onClick = { onInviteToGame(username) }, modifier = Modifier.weight(1f))
                        ActionButton(emoji = "💬", label = "پیام", color = Color(0xFFF97316), onClick = { onSendMessage(username) }, modifier = Modifier.weight(1f))

                    }
                }
            }
        }
    }
}

@Composable
private fun StatItem(
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
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text(label, fontSize = 11.sp, color = Color(0xFF94A3B8))
    }
}

@Composable
private fun ActionButton(
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
            Text(label, fontSize = 10.sp, color = color, fontWeight = FontWeight.Medium)
        }
    }
}