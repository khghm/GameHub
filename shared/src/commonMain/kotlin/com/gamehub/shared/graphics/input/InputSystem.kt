
package com.gamehub.shared.graphics.input

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.key.Key
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// ==================== Touch Zone for Mapping ====================
data class TouchZone(
    val id: String,
    val rect: Rect,
    val action: InputAction,
    val isHoldable: Boolean = true // true = fires while held, false = fires once on tap
)

// ==================== Input System ====================
class InputSystem {
    // State
    private val _state = MutableStateFlow(InputState())
    val state: StateFlow<InputState> = _state.asStateFlow()

    // Event history buffer
    private val _history = mutableListOf<InputEvent>()
    val history: List<InputEvent> get() = _history.toList()
    var maxHistorySize: Int = 100

    // Bindings
    private val keyBindings = mutableMapOf<Key, InputAction>()
    private val touchZones = mutableListOf<TouchZone>()

    // Listeners
    private val eventListeners = mutableListOf<(InputEvent) -> Unit>()

    // ==================== Binding Methods ====================
    fun bindKey(key: Key, action: InputAction) {
        keyBindings[key] = action
    }

    fun unbindKey(key: Key) {
        keyBindings.remove(key)
    }

    fun addTouchZone(zone: TouchZone) {
        touchZones.add(zone)
    }

    fun removeTouchZone(id: String) {
        touchZones.removeIf { it.id == id }
    }

    fun clearBindings() {
        keyBindings.clear()
        touchZones.clear()
    }

    // ==================== Listener Methods ====================
    fun addEventListener(listener: (InputEvent) -> Unit) {
        eventListeners.add(listener)
    }

    fun removeEventListener(listener: (InputEvent) -> Unit) {
        eventListeners.remove(listener)
    }

    // ==================== Input Processing Methods ====================
    fun onKeyEvent(key: Key, isPressed: Boolean) {
        val action = keyBindings[key] ?: return
        val event = ActionEvent(action = action, isPressed = isPressed)
        processEvent(event)
    }

    fun onTap(position: Offset, isDoubleTap: Boolean = false, isLongPress: Boolean = false) {
        val event = TapEvent(position = position, isDoubleTap = isDoubleTap, isLongPress = isLongPress)
        processEvent(event)

        // Check touch zones for tap
        touchZones.forEach { zone ->
            if (zone.rect.contains(position)) {
                if (!zone.isHoldable) {
                    // Fire once for non-holdable zones
                    val actionEvent = ActionEvent(action = zone.action, isPressed = true, position = position)
                    processEvent(actionEvent)
                    // Immediately release for non-holdable
                    val releaseEvent = ActionEvent(action = zone.action, isPressed = false, position = position)
                    processEvent(releaseEvent)
                }
            }
        }
    }

    fun onPress(position: Offset) {
        // Check touch zones for press
        touchZones.forEach { zone ->
            if (zone.rect.contains(position) && zone.isHoldable) {
                val event = ActionEvent(action = zone.action, isPressed = true, position = position)
                processEvent(event)
            }
        }

        // Update state
        _state.value = _state.value.copy(
            lastTapPosition = position
        )
    }

    fun onRelease(position: Offset) {
        // Check touch zones for release
        touchZones.forEach { zone ->
            if (zone.isHoldable) {
                val event = ActionEvent(action = zone.action, isPressed = false, position = position)
                processEvent(event)
            }
        }
    }

    fun onDragStart(position: Offset) {
        val event = DragEvent(type = DragEventType.START, position = position)
        processEvent(event)

        _state.value = _state.value.copy(
            isDragging = true,
            dragStartPosition = position,
            dragPosition = position
        )
    }

    fun onDrag(position: Offset, dragAmount: Offset) {
        val event = DragEvent(type = DragEventType.DRAG, position = position, dragAmount = dragAmount)
        processEvent(event)

        _state.value = _state.value.copy(
            dragPosition = position,
            mouseDelta = dragAmount
        )
    }

    fun onDragEnd(position: Offset) {
        val event = DragEvent(type = DragEventType.END, position = position)
        processEvent(event)

        _state.value = _state.value.copy(
            isDragging = false,
            dragStartPosition = null,
            dragPosition = null
        )
    }

    fun onDragCancel(position: Offset) {
        val event = DragEvent(type = DragEventType.CANCEL, position = position)
        processEvent(event)

        _state.value = _state.value.copy(
            isDragging = false,
            dragStartPosition = null,
            dragPosition = null
        )
    }

    fun onTextInput(text: String) {
        val event = TextInputEvent(text = text)
        processEvent(event)
    }

    fun updateMousePosition(position: Offset) {
        _state.value = _state.value.copy(mousePosition = position)
    }

    // ==================== Internal Methods ====================
    private fun processEvent(event: InputEvent) {
        // Add to history
        _history.add(event)
        if (_history.size > maxHistorySize) {
            _history.removeFirst()
        }

        // Update state based on event
        when (event) {
            is ActionEvent -> {
                val currentActions = _state.value.pressedActions.toMutableSet()
                if (event.isPressed) {
                    currentActions.add(event.action)
                } else {
                    currentActions.remove(event.action)
                }
                _state.value = _state.value.copy(pressedActions = currentActions)
            }
            is TapEvent -> {
                // Already handled in state update
            }
            is DragEvent -> {
                // Handled in specific drag methods
            }
            is TextInputEvent -> {
                // No state update needed for text input
            }
        }

        // Notify listeners
        eventListeners.forEach { listener ->
            listener(event)
        }
    }

    // ==================== Helper Methods ====================
    fun isActionPressed(action: InputAction): Boolean = _state.value.isActionPressed(action)

    fun clearHistory() {
        _history.clear()
    }
}
