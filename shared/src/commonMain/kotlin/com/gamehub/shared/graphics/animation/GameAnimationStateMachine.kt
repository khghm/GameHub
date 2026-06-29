package com.gamehub.shared.graphics.animation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

/**
 * Game-specific animation states for Backgammon
 */
enum class BackgammonAnimationState {
    IDLE,
    SELECTED,
    MOVING,
    CAPTURING,
    BEARING_OFF,
    FINISHED
}

/**
 * Game-specific events that trigger state transitions
 */
enum class BackgammonAnimationEvent {
    SELECT_CHECKER,
    SELECT_DESTINATION,
    CAPTURE_OPPONENT,
    BEAR_OFF,
    RESET,
    FINISH_GAME
}

/**
 * Pre-built Animation State Machine for Backgammon
 */
fun createBackgammonStateMachine(
    coroutineScope: CoroutineScope,
    onStateChanged: suspend (BackgammonAnimationState) -> Unit = {}
): AnimationStateMachine<BackgammonAnimationState, BackgammonAnimationEvent> {
    val transitions = listOf(
        StateTransition(
            from = BackgammonAnimationState.IDLE,
            event = BackgammonAnimationEvent.SELECT_CHECKER,
            to = BackgammonAnimationState.SELECTED,
            onEnter = { onStateChanged(BackgammonAnimationState.SELECTED) }
        ),
        StateTransition(
            from = BackgammonAnimationState.SELECTED,
            event = BackgammonAnimationEvent.SELECT_DESTINATION,
            to = BackgammonAnimationState.MOVING,
            onEnter = { onStateChanged(BackgammonAnimationState.MOVING) }
        ),
        StateTransition(
            from = BackgammonAnimationState.MOVING,
            event = BackgammonAnimationEvent.CAPTURE_OPPONENT,
            to = BackgammonAnimationState.CAPTURING,
            onEnter = { onStateChanged(BackgammonAnimationState.CAPTURING) }
        ),
        StateTransition(
            from = BackgammonAnimationState.MOVING,
            event = BackgammonAnimationEvent.BEAR_OFF,
            to = BackgammonAnimationState.BEARING_OFF,
            onEnter = { onStateChanged(BackgammonAnimationState.BEARING_OFF) }
        ),
        StateTransition(
            from = BackgammonAnimationState.CAPTURING,
            event = BackgammonAnimationEvent.RESET,
            to = BackgammonAnimationState.IDLE,
            onEnter = { onStateChanged(BackgammonAnimationState.IDLE) }
        ),
        StateTransition(
            from = BackgammonAnimationState.BEARING_OFF,
            event = BackgammonAnimationEvent.FINISH_GAME,
            to = BackgammonAnimationState.FINISHED,
            onEnter = { onStateChanged(BackgammonAnimationState.FINISHED) }
        ),
        StateTransition(
            from = BackgammonAnimationState.SELECTED,
            event = BackgammonAnimationEvent.RESET,
            to = BackgammonAnimationState.IDLE,
            onEnter = { onStateChanged(BackgammonAnimationState.IDLE) }
        )
    )

    return AnimationStateMachine(
        initialState = BackgammonAnimationState.IDLE,
        transitions = transitions,
        coroutineScope = coroutineScope
    )
}
