
package com.gamehub.shared.graphics.input

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset

// ==================== Compose Integration ====================
fun Modifier.inputSystemHandler(inputSystem: InputSystem): Modifier = composed {
    this.tapHandler(
        onTap = { position ->
            inputSystem.onTap(position)
        },
        onDoubleTap = { position ->
            inputSystem.onTap(position, isDoubleTap = true)
        },
        onLongPress = { position ->
            inputSystem.onTap(position, isLongPress = true)
        },
        onPress = { position ->
            inputSystem.onPress(position)
        }
    ).dragHandler(
        onDragStart = { position ->
            inputSystem.onDragStart(position)
        },
        onDragEnd = {
            // We don't have position here, use last known position
        },
        onDragCancel = {
            // We don't have position here
        },
        onDrag = { change, dragAmount ->
            inputSystem.onDrag(change.position, dragAmount)
        }
    )
}

// ==================== Quick Setup Helper ====================
@Composable
fun rememberInputSystem(): InputSystem {
    return remember { InputSystem() }
}
