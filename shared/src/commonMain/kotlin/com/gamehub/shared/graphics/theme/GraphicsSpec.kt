package com.gamehub.shared.graphics.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

abstract class GraphicsSpec {
    abstract val primaryColor: Color
    abstract val secondaryColor: Color
    abstract val accentColor: Color
    abstract val backgroundColor: Color
    abstract val surfaceColor: Color
    abstract val surfaceVariantColor: Color
    abstract val textColor: Color
    abstract val textSecondaryColor: Color
    abstract val errorColor: Color
    
    abstract val shadowElevation: Dp
    abstract val cornerRadius: Dp
    abstract val borderWidth: Dp
    
    abstract val animationDurationMs: Int
    abstract val particleEnabled: Boolean
    abstract val effectEnabled: Boolean
}

class DefaultGraphicsSpec : GraphicsSpec() {
    override val primaryColor: Color = Color(0xFF6200EE)
    override val secondaryColor: Color = Color(0xFF03DAC6)
    override val accentColor: Color = Color(0xFFFF9800)
    override val backgroundColor: Color = Color(0xFF121212)
    override val surfaceColor: Color = Color(0xFF1E1E1E)
    override val surfaceVariantColor: Color = Color(0xFF2D2D2D)
    override val textColor: Color = Color(0xFFFFFFFF)
    override val textSecondaryColor: Color = Color(0xFFB0B0B0)
    override val errorColor: Color = Color(0xFFB00020)
    override val shadowElevation: Dp = 8.dp
    override val cornerRadius: Dp = 12.dp
    override val borderWidth: Dp = 2.dp
    override val animationDurationMs: Int = 300
    override val particleEnabled: Boolean = true
    override val effectEnabled: Boolean = true
}
