package com.gamehub.games.battleship.ui

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
fun BattleshipGuide() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "راهنمای بازی نبردناو (Battleship)",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Text(
            text = """
                بازی نبردناو یک بازی استراتژیک دو نفره است که روی شبکه‌های ۱۰×۱۰ انجام می‌شود.
                
                مراحل بازی:
                ۱. فاز جانمایی: هر بازیکن ۶ کشتی خود را در شبکه‌ی خود قرار می‌دهد (می‌توانید از دکمه تصادفی استفاده کنید).
                ۲. فاز نبرد: بازیکنان به‌نوبت در شبکه‌ی حریف شلیک می‌کنند. هر بار که به کشتی حریف ضربه بزنید، نوبت دوباره شماست!
                
                کشتی‌ها:
                - ناو هواپیمابر (۵ خانه)
                - رزم‌ناو (۴ خانه)
                - ناوشکن (۳ خانه)
                - زیردریایی (۳ خانه)
                - ناوچه (۲ خانه)
                - قایق گشت (۲ خانه)
                
                قوانین جانمایی:
                - کشتی‌ها نمی‌توانند هم‌پوشانی داشته باشند.
                - کشتی‌ها نمی‌توانند در مجاورت یکدیگر (حتی مورب) قرار گیرند.
                - کشتی‌ها باید کاملاً در شبکه قرار بگیرند.
                
                هدف بازی: غرق کردن تمام کشتی‌های حریف!
            """.trimIndent(),
            fontSize = 16.sp,
            lineHeight = 28.sp,
            textAlign = TextAlign.Right,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
