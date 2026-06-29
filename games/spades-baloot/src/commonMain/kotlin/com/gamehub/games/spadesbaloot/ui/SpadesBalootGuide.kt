package com.gamehub.games.spadesbaloot.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun SpadesBalootGuide() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "راهنمای اسپیدز بلوت",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text("هدف بازی: تیمی که زودتر به ۵۰۰ امتیاز برسد برنده است!")
        Text("قوانین کلیدی:")
        Text("۱. خال پیک (♠) همواره حکم است!")
        Text("۲. باید خال رهبر را دنبال کنید (Follow Suit).")
        Text("۳. تا زمانی که اسپیدز شکسته نشده (Spades Broken) نمی‌توانید با پیک شروع کنید!")
    }
}
