package com.gamehub.games.ludo.ui

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
fun LudoGuide() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "راهنمای بازی Ludo (منچ)",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Text(
            text = """
                بازی Ludo یک بازی تخته‌ای کلاسیک است که می‌تواند ۲ تا ۴ بازیکن داشته باشد.
                
                قوانین بازی:
                • هدف رسیدن به خانه ۵۱ است.
                • برای شروع باید ۶ بیاورید.
                • با هر ۶ می‌توانید دوباره تاس بیندازید.
                • اگر روی خانه حریف بروید، حریف به خانه اول برمی‌گردد.
                • هر بازیکن یک مهره دارد که روی تخته حرکت می‌کند.
            """.trimIndent(),
            fontSize = 16.sp,
            lineHeight = 28.sp,
            textAlign = TextAlign.Right,
            modifier = Modifier.fillMaxWidth()
        )
    }
}