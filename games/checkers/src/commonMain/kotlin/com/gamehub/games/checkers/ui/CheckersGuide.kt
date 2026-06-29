package com.gamehub.games.checkers.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CheckersGuide() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "راهنمای بازی چکرز",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Text(
            text = """
                بازی چکرز یک بازی دو نفره است که روی تخته 8x8 انجام می‌شود.
                
                قوانین بازی:
                - مهره‌ها فقط روی خانه‌های تیره حرکت می‌کنند.
                - مهره‌های سرباز (●) فقط جلو حرکت می‌کنند.
                - وقتی یک مهره به آخرین ردیف زمین حریف برسد، به وزیر (♔) تبدیل می‌شود.
                - وزیرها می‌توانند در هر جهتی حرکت کنند.
                - اگر امکان گرفتن مهره حریف وجود داشته باشد، باید انجام شود.
                - می‌توان با پرش از روی مهره حریف، آن را گرفت.
                - اگر پس از یک گرفتن، امکان گرفتن دیگری وجود داشته باشد، باید ادامه داد.
                
                هدف بازی:
                - گرفتن تمام مهره‌های حریف یا مسدود کردن تمام حرکات آن.
            """.trimIndent(),
            fontSize = 16.sp,
            lineHeight = 28.sp,
            textAlign = TextAlign.Right,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
