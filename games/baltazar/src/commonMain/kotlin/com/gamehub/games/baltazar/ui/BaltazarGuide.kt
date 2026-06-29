package com.gamehub.games.baltazar.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun BaltazarGuide() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text("راهنمای بازی بالتازار", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Text("هدف بازی:", style = MaterialTheme.typography.titleLarge)
        Text("تصرف خانه اصلی حریف! 🏆", style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Text("چطور بازی می‌شود:", style = MaterialTheme.typography.titleMedium)
        Text("1. کلمه‌ای از حروف روی صفحه انتخاب کنید", style = MaterialTheme.typography.bodyMedium)
        Text("2. کلمه را ثبت کنید", style = MaterialTheme.typography.bodyMedium)
        Text("3. خانه‌هایی که به قلمرو شما متصل هستند تصرف می‌شوند!", style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text("قوانین مهم:", style = MaterialTheme.typography.titleMedium)
        Text("- اگر ۳ بار تایم‌اوت کنید باختید!", style = MaterialTheme.typography.bodyMedium)
        Text("- خانه‌های مجاور خانه تصرف شده باز می‌شوند", style = MaterialTheme.typography.bodyMedium)
        Text("- خانه‌های حریف هم می‌توانند آزاد شوند!", style = MaterialTheme.typography.bodyMedium)
    }
}
