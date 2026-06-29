package com.gamehub.games.hex.ui

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
fun HexGuide() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "راهنمای بازی هگس (Hex)",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Text(
            text = """
                بازی هگس یک بازی استراتژیک دو نفره است که روی صفحه ۱۱×۱۱ شش‌ضلعی انجام می‌شود.
                
                قوانین بازی:
                - بازیکن اول (قرمز) باید دو لبه افقی (چپ و راست) را به هم متصل کند.
                - بازیکن دوم (آبی) باید دو لبه عمودی (بالا و پایین) را به هم متصل کند.
                - بازیکنان به نوبت یک مهره روی خانه خالی قرار می‌دهند.
                - اولین بازیکن که لبه‌های خود را متصل کند برنده است.
                - در این بازی هیچ تساوی وجود ندارد.
            """.trimIndent(),
            fontSize = 16.sp,
            lineHeight = 28.sp,
            textAlign = TextAlign.Right,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
