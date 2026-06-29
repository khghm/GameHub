package com.gamehub.games.uno.ui

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
fun UnoGuide() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "راهنمای بازی اونو (Uno)",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Text(
            text = """
                بازی اونو یک بازی کارتی گروهی است که هدف آن خلاص شدن از تمام کارت‌های دست است.
                
                قوانین بازی:
                • هر بازیکن ۷ کارت دریافت می‌کند.
                • بازیکنان به نوبت باید کارتی هم‌رنگ یا هم‌شماره با کارت روی زمین بیندازند.
                • اگر کارت مناسبی ندارید، باید یک کارت از دسته بکشید.
                
                کارت‌های ویژه:
                ⊘ (Skip): نوبت بازیکن بعدی را از دست می‌دهد.
                ↺ (Reverse): جهت بازی را برعکس می‌کند.
                +2 (Draw Two): بازیکن بعدی ۲ کارت می‌کشد و نوبتش می‌پرد.
                W (Wild): می‌توانید رنگ بازی را تغییر دهید.
                +4 (Wild Draw Four): رنگ را تغییر دهید و بازیکن بعدی ۴ کارت بکشد.
                
                نکته مهم: وقتی فقط یک کارت دارید، باید سریع بگویید "اونو"!
            """.trimIndent(),
            fontSize = 16.sp,
            lineHeight = 28.sp,
            textAlign = TextAlign.Right,
            modifier = Modifier.fillMaxWidth()
        )
    }
}