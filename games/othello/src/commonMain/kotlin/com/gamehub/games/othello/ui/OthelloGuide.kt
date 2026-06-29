package com.gamehub.games.othello.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun OthelloGuide() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("راهنمای بازی اتللو", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Text("هدف بازی: داشتن بیشتر مهره‌های رنگ خود در پایان بازی", style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Text("حرکت: در هر نوبت، مهره‌ای از رنگ خود را در خانه‌ای قرار می‌دهید که حداقل یک مهره حریف را در یک خط مستقیم بین مهره جدید و یک مهره خودی محصور کند", style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text("برگشت مهره‌ها: تمام مهره‌های محصور شده به رنگ شما تغییر می‌کنند", style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text("پاس: اگر حرکتی ندارید، نوبت به حریف منتقل می‌شود", style = MaterialTheme.typography.bodyMedium)
    }
}
