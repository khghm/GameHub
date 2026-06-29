package com.gamehub.shared.graphics.text

import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

object GameTypography {
    fun title(
        color: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.White,
        fontSize: TextUnit = 32.sp
    ): TextStyle {
        return TextStyle(
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            color = color,
            letterSpacing = 2.sp
        )
    }
    
    fun subtitle(
        color: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.White,
        fontSize: TextUnit = 20.sp
    ): TextStyle {
        return TextStyle(
            fontSize = fontSize,
            fontWeight = FontWeight.Medium,
            color = color,
            letterSpacing = 1.sp
        )
    }
    
    fun body(
        color: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.White,
        fontSize: TextUnit = 14.sp
    ): TextStyle {
        return TextStyle(
            fontSize = fontSize,
            fontWeight = FontWeight.Normal,
            color = color
        )
    }
    
    fun neonTitle(
        glowColor: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Cyan,
        fontSize: TextUnit = 36.sp
    ): TextStyle {
        return TextStyle(
            fontSize = fontSize,
            fontWeight = FontWeight.Black,
            color = androidx.compose.ui.graphics.Color.White
        )
    }
    
    fun gradientTitle(
        fontSize: TextUnit = 32.sp
    ): TextStyle {
        return TextStyle(
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )
    }
}
