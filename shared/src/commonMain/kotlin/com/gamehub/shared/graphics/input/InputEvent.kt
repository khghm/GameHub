
package com.gamehub.shared.graphics.input

import androidx.compose.ui.geometry.Offset

// ==================== Cross-platform Time Helper ====================
internal expect fun getTimeMillis(): Long

// ==================== Input Event Types ====================
sealed class InputEvent {
    abstract val timestamp: Long // Milliseconds since epoch
}

data class TapEvent(
    val position: Offset,
    val isDoubleTap: Boolean = false,
    val isLongPress: Boolean = false,
    override val timestamp: Long = getTimeMillis()
) : InputEvent()

data class DragEvent(
    val type: DragEventType,
    val position: Offset,
    val dragAmount: Offset = Offset.Zero,
    override val timestamp: Long = getTimeMillis()
) : InputEvent()

enum class DragEventType { START, DRAG, END, CANCEL }

data class ActionEvent(
    val action: InputAction,
    val isPressed: Boolean, // true = pressed, false = released
    val position: Offset? = null,
    override val timestamp: Long = getTimeMillis()
) : InputEvent()

data class TextInputEvent(
    val text: String,
    override val timestamp: Long = getTimeMillis()
) : InputEvent()
