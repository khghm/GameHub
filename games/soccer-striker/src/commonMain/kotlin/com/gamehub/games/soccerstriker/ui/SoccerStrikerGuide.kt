
package com.gamehub.games.soccerstriker.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SoccerStrikerGuide() {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("راهنمای فوتبال انگشتی", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        Text("• هر تیم ۵ مهره دارد", style = MaterialTheme.typography.bodyLarge)
        Text("• صفحه را بکشید تا مهره را حرکت دهید", style = MaterialTheme.typography.bodyLarge)
        Text("• توپ را به دروازه حریف بزنید", style = MaterialTheme.typography.bodyLarge)
        Text("• اولین تیم که به ۳ گل برسد برنده است!", style = MaterialTheme.typography.bodyLarge)
    }
}
