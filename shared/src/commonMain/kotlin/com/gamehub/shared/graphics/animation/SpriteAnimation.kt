package com.gamehub.shared.graphics.animation

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Sprite Animation State
 */
enum class AnimationPlaybackState {
    IDLE,
    PLAYING,
    PAUSED,
    FINISHED
}

/**
 * Sprite Animation Frame
 */
data class SpriteFrame(
    val textureRegion: Rect,
    val durationMs: Long = 100L,
    val customEvent: String? = null
)

/**
 * Sprite Animation - plays sprite sheets
 */
class SpriteAnimation(
    val name: String,
    val frames: List<SpriteFrame>,
    val isLooping: Boolean = true,
    val spriteSheet: ImageBitmap
) {
    private val _state = MutableStateFlow(AnimationPlaybackState.IDLE)
    val state: StateFlow<AnimationPlaybackState> = _state.asStateFlow()

    private val _currentFrameIndex = MutableStateFlow(0)
    val currentFrameIndex: StateFlow<Int> = _currentFrameIndex.asStateFlow()

    private val _lastEvent = MutableStateFlow<String?>(null)
    val lastEvent: StateFlow<String?> = _lastEvent.asStateFlow()

    var frameRateScale: Float = 1f

    val currentFrame: SpriteFrame get() = frames[_currentFrameIndex.value]

    /**
     * Play the animation from start or current position
     */
    suspend fun play(fromStart: Boolean = true) {
        if (fromStart) {
            _currentFrameIndex.value = 0
        }
        _state.value = AnimationPlaybackState.PLAYING

        while (_state.value == AnimationPlaybackState.PLAYING) {
            val frame = frames[_currentFrameIndex.value]
            frame.customEvent?.let { _lastEvent.value = it }

            // Wait for frame duration (adjusted by scale)
            delay((frame.durationMs / frameRateScale).toLong())

            if (_state.value != AnimationPlaybackState.PLAYING) break

            if (_currentFrameIndex.value < frames.size - 1) {
                _currentFrameIndex.value++
            } else {
                if (isLooping) {
                    _currentFrameIndex.value = 0
                } else {
                    _state.value = AnimationPlaybackState.FINISHED
                }
            }
        }
    }

    /**
     * Pause the animation
     */
    fun pause() {
        _state.value = AnimationPlaybackState.PAUSED
    }

    /**
     * Resume the animation
     */
    fun resume() {
        _state.value = AnimationPlaybackState.PLAYING
    }

    /**
     * Stop the animation and reset to first frame
     */
    fun stop() {
        _state.value = AnimationPlaybackState.IDLE
        _currentFrameIndex.value = 0
    }

    /**
     * Draw current frame
     */
    fun draw(drawScope: DrawScope, position: Offset) {
        drawScope.drawImage(
            image = spriteSheet,
            srcOffset = IntOffset(
                currentFrame.textureRegion.left.toInt(),
                currentFrame.textureRegion.top.toInt()
            ),
            srcSize = IntSize(
                currentFrame.textureRegion.width.toInt(),
                currentFrame.textureRegion.height.toInt()
            ),
            dstOffset = IntOffset(
                position.x.toInt(),
                position.y.toInt()
            ),
            dstSize = IntSize(
                currentFrame.textureRegion.width.toInt(),
                currentFrame.textureRegion.height.toInt()
            )
        )
    }
}