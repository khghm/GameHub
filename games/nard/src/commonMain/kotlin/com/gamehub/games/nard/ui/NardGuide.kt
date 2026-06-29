package com.gamehub.games.nard.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun NardGuide() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            "راهنمای بازی تخته نرد شرقی (Nard)",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))

        SectionTitle("🎯 هدف بازی")
        Text(
            "هدف بازی، خارج کردن تمام ۱۵ مهره خود از تخته زودتر از حریف است.",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(12.dp))

        SectionTitle("🎲 تاس‌ها")
        Text(
            "• دو تاس شش‌وجهی دارید\n" +
            "• اگر دو تاس یکسان بیاید (دابل)، چهار حرکت با آن عدد انجام می‌دهید\n" +
            "• تاس دوبرابر (Doubling Cube) برای دوبرابر کردن امتیاز استفاده می‌شود",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(12.dp))

        SectionTitle("🏁 چیدمان اولیه")
        Text(
            "• مهره‌های سفید روی نقاط ۲۴ (۲ تا)، ۱۳ (۵ تا)، ۸ (۳ تا) و ۶ (۵ تا) قرار می‌گیرند\n" +
            "• مهره‌های سیاه روی نقاط ۱ (۲ تا)، ۱۲ (۵ تا)، ۱۷ (۳ تا) و ۱۹ (۵ تا) قرار می‌گیرند\n" +
            "• سفیدها از ۲۴ به سمت ۱ حرکت می‌کنند، سیاه‌ها برعکس",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(12.dp))

        SectionTitle("⚔️ قوانین حرکت")
        Text(
            "۱. در ابتدای نوبت می‌توانید تاس دوبرابر را پیشنهاد دهید\n" +
            "۲. تاس‌ها را پرتاب کنید\n" +
            "۳. اگر مهره‌ای در بار دارید، ابتدا باید آن را وارد کنید\n" +
            "۴. مهره‌ها را طبق اعداد تاس حرکت دهید\n" +
            "۵. اگر روی نقطه‌ای با یک مهره حریف فرود بیایید، آن مهره خورده و به بار می‌رود\n" +
            "۶. روی نقطه‌ای با دو یا چند مهره حریف نمی‌توانید فرود بیایید\n" +
            "۷. وقتی تمام مهره‌ها در خانه خودتان بودند، می‌توانید خارج کردن را آغاز کنید",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(12.dp))

        SectionTitle("🏠 خارج کردن (Bearing Off)")
        Text(
            "• فقط وقتی می‌توانید خارج کنید که تمام مهره‌هایتان در خانه خودتان باشند\n" +
            "• مهره‌ای از نقطه‌ای که عدد تاس دقیقاً نشان می‌دهد، خارج می‌شود\n" +
            "• اگر در آن نقطه مهره‌ای نباشد، از بالاترین نقطه‌ای که مهره دارد خارج کنید",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(12.dp))

        SectionTitle("🏆 امتیازدهی")
        Text(
            "• برد معمولی: ۱ امتیاز (اگر حریف حداقل یک مهره خارج کرده باشد)\n" +
            "• گام‌برگ (Gammon): ۲ امتیاز (اگر حریف هیچ مهره‌ای خارج نکرده باشد)\n" +
            "• بک‌گامون (Backgammon): ۳ امتیاز (اگر حریف مهره‌ای در بار یا در خانه شما داشته باشد)\n" +
            "• امتیاز نهایی = امتیاز پایه × ضریب تاس دوبرابر",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(12.dp))

        SectionTitle("💡 نکات مهم")
        Text(
            "• همیشه سعی کنید نقاطتان را مسدود کنید تا حریف نتواند حرکت کند\n" +
            "• حواست به مهره‌های تکی باشد چون خورده شدنشان می‌تواند بازی را تغییر دهد\n" +
            "• از تاس دوبرابر در موقعیت مناسب استفاده کنید",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )
    Spacer(modifier = Modifier.height(4.dp))
}
