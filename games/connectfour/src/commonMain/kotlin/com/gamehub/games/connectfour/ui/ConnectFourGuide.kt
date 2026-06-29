package com.gamehub.games.connectfour.ui

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
fun ConnectFourGuide() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "راهنمای بازی چهارخطی",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Text(
            text = """
                بازی چهارخطی یک بازی دو نفره روی صفحه ۶×۷ است.
                
                قوانین بازی:
                • بازیکنان به نوبت یکی از ۷ ستون را انتخاب می‌کنند.
                • مهره در پایین‌ترین خانه خالی ستون قرار می‌گیرد.
                • اولین بازیکنی که ۴ مهره خود را به صورت افقی، عمودی یا مورب ردیف کند، برنده است.
                • اگر صفحه پر شود و برنده‌ای نباشد، بازی مساوی می‌شود.
                
                نکته راهبردی:
                سعی کنید همزمان با ساختن ردیف‌های خود، جلوی ردیف‌های حریف را بگیرید.
                کنترل مرکز صفحه مزیت بزرگتری به شما می‌دهد!
            """.trimIndent(),
            fontSize = 16.sp,
            lineHeight = 28.sp,
            textAlign = TextAlign.Right,
            modifier = Modifier.fillMaxWidth()
        )
    }
}