
package com.gamehub.shared.graphics.input

// ==================== Input Actions ====================
enum class InputAction {
    // Movement
    MOVE_LEFT,
    MOVE_RIGHT,
    MOVE_UP,
    MOVE_DOWN,

    // Actions
    JUMP,
    ATTACK,
    INTERACT,
    USE,

    // UI
    PAUSE,
    CONFIRM,
    CANCEL,

    // Camera
    CAMERA_ZOOM_IN,
    CAMERA_ZOOM_OUT,
    CAMERA_PAN_LEFT,
    CAMERA_PAN_RIGHT,
    CAMERA_PAN_UP,
    CAMERA_PAN_DOWN
}
