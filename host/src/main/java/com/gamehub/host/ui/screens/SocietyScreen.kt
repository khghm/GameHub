package com.gamehub.host.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.gamehub.host.viewmodel.SocietyViewModel

@Composable
fun SocietyScreen(
    onBack: () -> Unit,
    onOpenChat: (String, String) -> Unit
) {
    val viewModel = remember { SocietyViewModel() }
    val societies by viewModel.societies.collectAsState()
    val currentSociety by viewModel.currentSociety.collectAsState()
    val members by viewModel.members.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val message by viewModel.message.collectAsState()
    var selectedSocietyId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadAllSocieties()
    }

    LaunchedEffect(selectedSocietyId) {
        if (selectedSocietyId != null) {
            viewModel.loadSociety(selectedSocietyId!!)
        } else {
            viewModel.loadAllSocieties()
        }
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
            Surface(
                onClick = {
                    if (currentSociety != null) selectedSocietyId = null
                    else onBack()
                },
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
            Text("🏛️ انجمن‌ها", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = OnBackground)
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Primary)
            }
        } else if (currentSociety != null) {
            // نمایش جزئیات یک انجمن
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(currentSociety!!.name, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = OnSurface)
                    Text("تعداد اعضا: ${currentSociety!!.memberCount}/${currentSociety!!.maxMembers}", color = OnSurfaceVariant)
                    Text("نوع عضویت: ${currentSociety!!.membershipType}", color = OnSurfaceVariant)
                    currentSociety!!.description?.let {
                        Text(it, color = OnSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { onOpenChat("society", currentSociety!!.id) }, shape = RoundedCornerShape(14.dp)) { Text("چت گروهی") }
                        Button(onClick = { viewModel.leaveSociety(currentSociety!!.id) }, colors = ButtonDefaults.buttonColors(containerColor = Error), shape = RoundedCornerShape(14.dp)) {
                            Text("ترک انجمن")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("اعضای انجمن:", color = OnSurface, fontWeight = FontWeight.Bold, fontSize = 16.sp)
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
                            Text(member.status, color = OnSurfaceVariant, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        } else {
            // لیست همه انجمن‌ها
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(societies) { society ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedSocietyId = society.id },
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Row(modifier = Modifier.padding(20.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(society.name, fontWeight = FontWeight.Bold, color = OnSurface, fontSize = 16.sp)
                                Text("اعضا: ${society.memberCount}/${society.maxMembers}", fontSize = 13.sp, color = OnSurfaceVariant)
                            }
                            Button(onClick = { viewModel.joinSociety(society.id) }, shape = RoundedCornerShape(14.dp)) {
                                Text("عضویت")
                            }
                        }
                    }
                }
            }
        }
    }

    message?.let {
        LaunchedEffect(it) {
            viewModel.clearMessage()
        }
    }
}