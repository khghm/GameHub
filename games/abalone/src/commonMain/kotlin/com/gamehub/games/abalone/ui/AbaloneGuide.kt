package com.gamehub.games.abalone.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun AbaloneGuide() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "راهنمای بازی ابلون",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            "هدف بازی:",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            "اولین بازیکنی که ۶ مهره از حریف را از تخته خارج کند، برنده است."
        )

        Text(
            "قوانین حرکت:",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            "- در هر نوبت می‌توان ۱ تا ۳ مهره از مهره‌های خود را حرکت داد.\n" +
                    "- مهره‌ها فقط می‌توانند در یک خط مستقیم حرکت کنند.\n" +
                    "- می‌توان مهره‌های حریف را فقط در صورت برتری عددی (۲ به ۱، ۳ به ۱، ۳ به ۲) هل داد."
        )
    }
}
