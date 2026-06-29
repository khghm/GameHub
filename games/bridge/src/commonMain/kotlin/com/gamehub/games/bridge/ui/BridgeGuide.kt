package com.gamehub.games.bridge.ui

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
fun BridgeGuide() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "راهنمای بازی بریج (Bridge)",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Text(
            text = """
                بازی بریج یک بازی چهار نفره است که در دو تیم دو نفره انجام می‌شود.
                
                مراحل بازی:
                ۱. مرحله مزایده (Bidding):
                   • بازیکنان به نوبت پیشنهاد می‌دهند که چند تریک با چه سوت ( یا بدون سوت) برنده می‌شوند.
                   • می‌توانید "پاس" بدهید، پیشنهاد جدید بدهید، مضاعف (Double) یا بازمضاعف (Redouble) کنید.
                   • پس از سه پاس متوالی، مزایده بسته شده و آخرین پیشنهاد به عنوان قرارداد تعیین می‌شود.
                
                ۲. مرحله بازی (Play):
                   • بازیکن سمت چپ اعلام‌کننده اولین کارت را بازی می‌کند (ورق اول).
                   • دست پارتنر اعلام‌کننده (دست ساختگی) رو به بالا نمایش داده می‌شود و توسط اعلام‌کننده بازی می‌شود.
                   • اگر سوت (Trump) تعیین شده باشد، کارت‌های آن سوت از سایر کارت‌ها قوی‌تر هستند.
                   • در صورتی که سوت شروع شده (Lead Suit) را داشته باشید، باید همان سوت را بازی کنید.
                   • برنده هر تریک، تریک بعدی را شروع می‌کند.
                
                هدف بازی:
                تیمی که قرارداد را بسته‌اند، باید حداقل تعداد تریک‌های مورد نیاز (سطح + ۶) را ببردند. تیم حریف باید جلوی آنها را بگیرد.
            """.trimIndent(),
            fontSize = 16.sp,
            lineHeight = 28.sp,
            textAlign = TextAlign.Right,
            modifier = Modifier.fillMaxWidth()
        )
    }
}