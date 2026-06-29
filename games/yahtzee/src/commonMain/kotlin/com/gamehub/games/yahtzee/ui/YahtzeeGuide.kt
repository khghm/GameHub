package com.gamehub.games.yahtzee.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun YahtzeeGuide() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "راهنمای بازی Yahtzee",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        GuideSection(
            title = "هدف بازی",
            content = "در این بازی باید با ۵ تاس و در حداکثر ۳ پرتاب در هر نوبت، بیشترین امتیاز را کسب کنید!"
        )

        GuideSection(
            title = "نحوه بازی",
            content = """
                1. در هر نوبت، ۵ تاس را پرتاب می‌کنید (پرتاب اول).
                2. می‌توانید هر تعداد تاس را نگه دارید و بقیه را دوباره پرتاب کنید.
                3. این کار را یک بار دیگر هم تکرار کنید (پرتاب سوم).
                4. در نهایت یک دسته‌بندی از کارت امتیاز را برای ثبت امتیاز انتخاب کنید!
            """.trimIndent()
        )

        GuideSection(
            title = "بخش بالا",
            content = "در این بخش، امتیاز هر عدد را به صورت مجموع آن عدد را در تعداد تاس‌هایی که آن عدد را دارند محاسبه می‌کنید. اگر مجموع بخش بالا ≥ ۶۳ شود، ۳۵ امتیاز جایزه دریافت می‌کنید!"
        )

        GuideSection(
            title = "بخش پایین",
            content = """
                سه‌تایی: حداقل ۳ تاس یکسان → مجموع همه تاس‌ها
                چهارتایی: حداقل ۴ تاس یکسان → مجموع همه تاس‌ها
                فول هاوس: سه‌تایی + یک جفت (یا پنج تاس یکسان) → ۲۵ امتیاز
                ست کوچک: ۴ عدد پشت سر هم → ۳۰ امتیاز
                ست بزرگ: ۵ عدد پشت سر هم → ۴۰ امتیاز
                یاتزی: ۵ تاس یکسان → ۵۰ امتیاز (و هر یاتزی بعدی ۱۰۰ امتیاز جایزه می‌دهد!)
                شانس: مجموع همه تاس‌ها (بدون هیچ شرطی!)
            """.trimIndent()
        )
    }
}

@Composable
fun GuideSection(title: String, content: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
