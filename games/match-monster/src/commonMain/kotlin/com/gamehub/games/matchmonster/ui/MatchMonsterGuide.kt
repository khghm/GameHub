package com.gamehub.games.matchmonster.ui

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
fun MatchMonsterGuide() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "راهنمای بازی مچ مانستر (Match Monster)",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Text(
            text = """
                بازی مچ مانستر یک بازی پازل هم‌زمان دو نفره است.
                
                نحوه بازی:
                - انگشت خود را روی کاشی‌های هیولا بکشید تا مسیری از ۳ یا بیشتر کاشی هم‌نوع ایجاد کنید.
                - هرچه مسیر طولانی‌تر باشد، آسیب بیشتری به حریف وارد می‌کنید و آشغال بیشتری برای او ایجاد می‌کنید.
                
                کاشی‌های هیولا:
                🔥 آتش - 💧 آب - 🪨 زمین - 🌪️ باد - 🌑 تاریکی - ☀️ نور
                
                کاشی‌های ویژه:
                - 💣 بمب (۵ کاشی هم‌نوع): تمام کاشی‌های مجاور را نابود می‌کند.
                - ⚡ صاعقه (۶ کاشی هم‌نوع): یک سطر یا ستون کامل را پاک می‌کند.
                - 🌈 رنگین‌کمان (۷+ کاشی هم‌نوع): تمام کاشی‌های یک نوع را از تخته پاک می‌کند.
                
                هدف بازی: سلامتی حریف را به صفر برسانید!
            """.trimIndent(),
            fontSize = 16.sp,
            lineHeight = 28.sp,
            textAlign = TextAlign.Right,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
