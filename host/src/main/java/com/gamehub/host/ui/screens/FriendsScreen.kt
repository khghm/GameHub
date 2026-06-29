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
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gamehub.host.ui.theme.*
import com.gamehub.host.viewmodel.AuthViewModel
import com.gamehub.host.viewmodel.FriendInfo

@SuppressLint("StateFlowValueCalledInComposition")
@Composable
fun FriendsScreen(
    viewModel: AuthViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    var friendUsername by remember { mutableStateOf("") }
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    val user by viewModel.currentUser.collectAsState()
    val pendingRequests by viewModel.pendingRequests.collectAsState()

    val tabTitles = listOf(
        "🟢 آنلاین",
        "👥 همه",
        "📩 درخواست‌ها (${pendingRequests.size})"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundGradient)
            .padding(20.dp)
    ) {
        // Header
        Text(
            text = "👥 دوستان",
            style = MaterialTheme.typography.headlineLarge,
            color = OnBackground,
            fontWeight = FontWeight.Black,
            fontSize = 32.sp
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Add Friend Section
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
                    value = friendUsername,
                    onValueChange = { friendUsername = it },
                    label = { Text("نام کاربری دوست") },
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
                        if (friendUsername.isNotEmpty()) {
                            viewModel.addFriend(friendUsername)
                            friendUsername = ""
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "افزودن")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("افزودن")
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Tabs
        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = Surface,
            contentColor = Primary,
            divider = {},
            indicator = { tabPositions ->
                TabRowDefaults.Indicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                    height = 3.dp,
                    color = Primary
                )
            }
        ) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = {
                        Text(
                            title,
                            fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Content based on tab
        when (selectedTabIndex) {
            // Online Friends
            0 -> {
                user?.let {
                    val onlineFriends = it.friends.filter { friend -> friend.isOnline }
                    if (onlineFriends.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("هیچ دوستی آنلاین نیست", color = OnSurfaceVariant)
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(onlineFriends) { friend ->
                                FriendCard(friend, onRemove = { viewModel.removeFriend(it.username) })
                            }
                        }
                    }
                }
            }
            // All Friends
            1 -> {
                user?.let {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(it.friends) { friend ->
                            FriendCard(friend, onRemove = { viewModel.removeFriend(it.username) })
                        }
                    }
                }
            }
            // Pending Requests
            2 -> {
                if (pendingRequests.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("هیچ درخواستی ندارید", color = OnSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(pendingRequests) { request ->
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
                                    // Avatar
                                    Box(
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(CircleShape)
                                            .background(
                                                Brush.verticalGradient(
                                                    colors = listOf(
                                                        Primary.copy(alpha = 0.5f), Secondary.copy(alpha = 0.5f)
                                                    )
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = "آواتار",
                                            tint = OnPrimary
                                        )
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(request.username, fontWeight = FontWeight.Bold, color = OnSurface)
                                    }

                                    Row {
                                        Button(
                                            onClick = { viewModel.acceptRequest(request.requestId) },
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Success)
                                        ) {
                                            Icon(Icons.Default.Check, "تایید")
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        OutlinedButton(
                                            onClick = { viewModel.rejectRequest(request.requestId) },
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Icon(Icons.Default.Close, "رد")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FriendCard(
    friend: FriendInfo,
    onRemove: (FriendInfo) -> Unit
) {
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
            // Avatar
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Primary.copy(alpha = 0.5f), Secondary.copy(alpha = 0.5f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "آواتار",
                    tint = OnPrimary
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(friend.displayName ?: friend.username, fontWeight = FontWeight.Bold, color = OnSurface)
                Text(friend.username, fontSize = 12.sp, color = OnSurfaceVariant)
            }

            // Online Status
            if (friend.isOnline) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(Success)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(OnSurfaceVariant)
                )
            }

            Row {
                IconButton(onClick = { /* Open Chat */ }) {
                    Icon(Icons.Default.Email, contentDescription = "چت", tint = Primary)
                }
                IconButton(onClick = { /* Invite */ }) {
                    Icon(Icons.Default.Star, contentDescription = "دعوت", tint = Secondary)
                }
                TextButton(onClick = { onRemove(friend) }) {
                    Text("حذف", color = Error)
                }
            }
        }
    }
}
