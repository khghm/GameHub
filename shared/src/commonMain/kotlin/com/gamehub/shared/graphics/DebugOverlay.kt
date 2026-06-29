package com.gamehub.shared.graphics

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun DebugOverlay(
    modifier: Modifier = Modifier,
    timeProvider: TimeProvider = DefaultTimeProvider()
) {
    var isVisible by remember { mutableStateOf(false) }
    var tapCount by remember { mutableStateOf(0) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(tapCount) {
        if (tapCount >= 3) {
            isVisible = !isVisible
            tapCount = 0
            alpha.animateTo(
                if (isVisible) 1f else 0f,
                animationSpec = spring()
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            tapCount = 0
        }
    }

    Box(
        modifier = modifier.pointerInput(Unit) {
            detectTapGestures(
                onTap = {
                    tapCount += 1
                    if (tapCount < 3) {
                        // Reset tap count after 1 second of inactivity
                        // For simplicity, we'll just count taps quickly
                        // In real use, add a timer to reset tapCount
                    }
                }
            )
        }
    ) {
        if (isVisible || alpha.value > 0.01f) {
            DebugInfoPanel(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
                    .background(Color.Black.copy(alpha = 0.7f * alpha.value)),
                timeProvider = timeProvider
            )
        }
    }
}

@Composable
private fun DebugInfoPanel(
    modifier: Modifier = Modifier,
    timeProvider: TimeProvider
) {
    var fps by remember { mutableStateOf(0) }
    var frameTimeMs by remember { mutableStateOf(0f) }
    val frameTimes = remember { mutableStateListOf<Long>() }
    var lastFrameTimeNanos by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(Unit) {
        while (true) {
            val now = timeProvider.currentTimeNanos()
            lastFrameTimeNanos?.let { last ->
                val deltaNs = now - last
                frameTimes.add(deltaNs)
                if (frameTimes.size > 60) frameTimes.removeAt(0)

                val averageDeltaNs = frameTimes.average().toLong()
                fps = (1_000_000_000.0 / averageDeltaNs).roundToInt()
                frameTimeMs = averageDeltaNs / 1_000_000f
                DeviceTierDetector.updateFps(fps)
            }
            lastFrameTimeNanos = now
            delay(16L)
        }
    }

    Column(modifier = modifier.padding(8.dp)) {
        Text(text = "FPS: $fps", color = Color.White)
        Text(text = "Frame Time: ${String.format("%.2f", frameTimeMs)} ms", color = Color.White)
        Text(text = "Device Tier: ${DeviceTierDetector.getCurrentTier()}", color = Color.White)
    }
}
