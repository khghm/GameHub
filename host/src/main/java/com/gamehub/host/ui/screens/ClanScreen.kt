package com.gamehub.host.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamehub.host.ui.theme.*
import com.gamehub.host.viewmodel.ClanViewModel

@Composable
fun ClanScreen(
    onBack: () -> Unit,
    onOpenChat: (String, String) -> Unit  // channelType, channelId
) {
    val viewModel = remember { ClanViewModel() }
    val myClan by viewModel.myClan.collectAsState()
    val members by viewModel.members.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val message by viewModel.message.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadMyClan()
    }

    var showCreateDialog by remember { mutableStateOf(false) }
    var clanName by remember { mutableStateOf("") }
    var clanTag by remember { mutableStateOf("") }
    var contributeAmount by remember { mutableStateOf("") }

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
            Surface(
                onClick = onBack,
                shape = RoundedCornerShape(14.dp),
                color = SurfaceVariant.copy(alpha = 0.3f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = OnBackground)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("بازگشت", color = OnBackground, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
            }
            Text("👥 کلن", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = OnBackground)
            if (myClan == null) {
                Button(
                    onClick = { showCreateDialog = true },
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("ایجاد کلن")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Primary)
            }
        } else if (myClan != null) {
            // نمایش اطلاعات کلن
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("${myClan!!.name} [${myClan!!.tag}]", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = OnSurface)
                    Text("سطح: ${myClan!!.level}", color = OnSurfaceVariant)
                    Text("اعضا: ${myClan!!.memberCount}/${myClan!!.maxMembers}", color = OnSurfaceVariant)
                    Text("سکه جمع‌آوری شده: ${myClan!!.totalCoinsContributed}", color = Warning)
                    Text("هزینه ارتقا به سطح بعد: ${myClan!!.coinsRequiredForNextLevel}", color = Secondary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { viewModel.upgradeClan() }, shape = RoundedCornerShape(14.dp)) { Text("ارتقا کلن") }
                        Button(onClick = { onOpenChat("clan", myClan!!.id) }, shape = RoundedCornerShape(14.dp)) { Text("چت گروهی") }
                        Button(onClick = { viewModel.leaveClan() }, colors = ButtonDefaults.buttonColors(containerColor = Error), shape = RoundedCornerShape(14.dp)) {
                            Text("ترک کلن")
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = contributeAmount,
                            onValueChange = { contributeAmount = it },
                            label = { Text("مقدار سکه") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Primary,
                                unfocusedBorderColor = SurfaceVariant,
                                focusedContainerColor = SurfaceVariant.copy(alpha = 0.3f),
                                unfocusedContainerColor = SurfaceVariant.copy(alpha = 0.15f)
                            )
                        )
                        Button(
                            onClick = {
                                val amount = contributeAmount.toLongOrNull()
                                if (amount != null && amount > 0) {
                                    viewModel.contributeCoins(amount)
                                    contributeAmount = ""
                                }
                            },
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("کمک سکه")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("اعضای کلن:", color = OnSurface, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(members) { member ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                    ) {
                        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(member.userId, color = OnSurface, fontWeight = FontWeight.Medium)
                            Text(member.role, color = Warning, fontWeight = FontWeight.SemiBold)
                            Text("کمک: ${member.coinsContributed}", color = OnSurfaceVariant, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        } else {
            // بدون کلن
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("شما عضو هیچ کلنی نیستید", color = OnSurfaceVariant, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { showCreateDialog = true },
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        Text("ایجاد کلن جدید", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("ایجاد کلن") },
            text = {
                Column {
                    OutlinedTextField(value = clanName, onValueChange = { clanName = it }, label = { Text("نام کلن") }, singleLine = true)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = clanTag, onValueChange = { clanTag = it }, label = { Text("تگ (حداکثر 4 کاراکتر)") }, singleLine = true)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (clanName.isNotBlank() && clanTag.isNotBlank()) {
                        viewModel.createClan(clanName, clanTag)
                        showCreateDialog = false
                    }
                }) { Text("ایجاد") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text("انصراف") }
            }
        )
    }

    message?.let {
        LaunchedEffect(it) {
            // می‌توانیم با Toast یا Snackbar نمایش دهیم
            viewModel.clearMessage()
        }
    }
}