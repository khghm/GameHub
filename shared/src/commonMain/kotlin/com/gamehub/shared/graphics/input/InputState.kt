
package com.gamehub.shared.graphics.input

import androidx.compose.ui.geometry.Offset

// ==================== Input State ====================
data class InputState(
    val pressedActions: Set<InputAction> = emptySet(),
    val mousePosition: Offset = Offset.Zero,
    val mouseDelta: Offset = Offset.Zero,
    val lastTapPosition: Offset? = null,
    val isDragging: Boolean = false,
    val dragStartPosition: Offset? = null,
    val dragPosition: Offset? = null
) {
    fun isActionPressed(action: InputAction): Boolean = pressedActions.contains(action)

    fun isActionJustPressed(action: InputAction, history: List<InputEvent>): Boolean {
        // Check if action was pressed in the latest event
        return history.lastOrNull()?.let { event ->
            event is ActionEvent && event.action == action && event.isPressed
        } ?: false
    }

    fun isActionJustReleased(action: InputAction, history: List<InputEvent>): Boolean {
        // Check if action was released in the latest event
        return history.lastOrNull()?.let { event ->
            event is ActionEvent && event.action == action && !event.isPressed
        } ?: false
    }
}
