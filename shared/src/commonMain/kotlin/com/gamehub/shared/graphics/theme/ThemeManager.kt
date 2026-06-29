package com.gamehub.shared.graphics.theme

import androidx.compose.runtime.*

object ThemeManager {
    private val _currentSpec = mutableStateOf<GraphicsSpec>(DefaultGraphicsSpec())
    val currentSpec: State<GraphicsSpec> = _currentSpec

    fun setTheme(spec: GraphicsSpec) {
        _currentSpec.value = spec
    }

    fun resetToDefault() {
        _currentSpec.value = DefaultGraphicsSpec()
    }
}

@Composable
fun rememberGraphicsSpec(): GraphicsSpec {
    return ThemeManager.currentSpec.value
}
