package com.gamehub.games.tictactoe.ui

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
fun TicTacToeGuide() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "راهنمای بازی دوز (Tic Tac Toe)",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Text(
            text = """
                بازی دوز یک بازی دو نفره است که روی یک صفحه ۳×۳ انجام می‌شود.
                
                قوانین بازی:
                • بازیکن اول علامت X و بازیکن دوم علامت O را می‌گذارد.
                • بازیکنان به نوبت یک خانه خالی را انتخاب می‌کنند.
                • اولین بازیکنی که سه علامت خود را در یک ردیف (افقی، عمودی یا مورب) قرار دهد، برنده است.
                • اگر تمام خانه‌ها پر شوند و هیچ برنده‌ای نباشد، بازی مساوی می‌شود.
                
                نکته: سعی کنید همزمان با ایجاد فرصت‌های برد برای خود، جلوی برد حریف را بگیرید!
            """.trimIndent(),
            fontSize = 16.sp,
            lineHeight = 28.sp,
            textAlign = TextAlign.Right,
            modifier = Modifier.fillMaxWidth()
        )
    }
}