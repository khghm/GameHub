package com.gamehub.shared.graphics.input

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput

fun Modifier.tapHandler(
    onTap: (Offset) -> Unit = {},
    onDoubleTap: (Offset) -> Unit = {},
    onLongPress: (Offset) -> Unit = {},
    onPress: (Offset) -> Unit = {}
): Modifier = composed {
    this.pointerInput(Unit) {
        detectTapGestures(
            onTap = onTap,
            onDoubleTap = onDoubleTap,
            onLongPress = onLongPress,
            onPress = { offset ->
                onPress(offset)
                tryAwaitRelease()
            }
        )
    }
}

fun Modifier.dragHandler(
    onDragStart: (Offset) -> Unit = {},
    onDragEnd: () -> Unit = {},
    onDragCancel: () -> Unit = {},
    onDrag: (change: PointerInputChange, dragAmount: Offset) -> Unit
): Modifier = composed {
    this.pointerInput(Unit) {
        detectDragGestures(
            onDragStart = onDragStart,
            onDragEnd = onDragEnd,
            onDragCancel = onDragCancel,
            onDrag = onDrag
        )
    }
}
