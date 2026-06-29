package com.gamehub.shared.haptics

interface HapticFeedbackManager {
    fun performHapticFeedback(type: HapticType)
}

enum class HapticType {
    Click,
    HeavyClick,
    DoubleClick,
    Success,
    Error,
    LongPress
}

class NoopHapticFeedbackManager : HapticFeedbackManager {
    override fun performHapticFeedback(type: HapticType) {}
}
