package com.gamehub.shared.graphics.engine

import kotlin.time.TimeSource
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Game Loop Listener - receives callbacks from the game loop
 */
interface GameLoopListener {
    /**
     * Called for fixed timestep updates (physics, game logic)
     * @param deltaTime Fixed time step in seconds
     */
    fun onFixedUpdate(deltaTime: Float)

    /**
     * Called for variable timestep updates (rendering)
     * @param deltaTime Time since last frame in seconds
     * @param alpha Interpolation factor between previous and current state
     */
    fun onUpdate(deltaTime: Float, alpha: Float)
}

/**
 * Game Loop - handles timing, fixed timestep, and interpolation
 *
 * Features:
 * - Fixed timestep for consistent physics
 * - Variable timestep for rendering
 * - Interpolation between physics states
 * - Pause/Resume support
 * - Time scaling (slow motion)
 */
class GameLoop(
    /**
     * Fixed time step in seconds (default: 1/60 = 60 FPS physics)
     */
    var fixedTimestep: Float = 1f / 60f,

    /**
     * Maximum accumulated time to prevent spiral of death
     */
    var maxAccumulatedTime: Float = 0.25f,

    /**
     * Time scale factor (1.0 = normal speed, 0.5 = half speed, etc.)
     */
    var timeScale: Float = 1f
) {
    private val timeSource = TimeSource.Monotonic
    @Volatile
    private var isRunning: Boolean = false
    @Volatile
    private var isPaused: Boolean = false
    private var lastMark: TimeSource.Monotonic.ValueTimeMark? = null
    private var accumulatedTime: Float = 0f
    private var frameCount: Int = 0
    private var fpsUpdateTime: Float = 0f
    @Volatile
    private var currentFps: Int = 0

    // Listeners - thread-safe copy-on-write list
    private val listeners = CopyOnWriteArrayList<GameLoopListener>()

    /**
     * Current FPS (updated every second)
     */
    val fps: Int
        get() = currentFps

    /**
     * Is the game loop running?
     */
    val running: Boolean
        get() = isRunning

    /**
     * Is the game loop paused?
     */
    val paused: Boolean
        get() = isPaused

    /**
     * Add a listener
     */
    fun addListener(listener: GameLoopListener) {
        listeners.add(listener)
    }

    /**
     * Remove a listener
     */
    fun removeListener(listener: GameLoopListener) {
        listeners.remove(listener)
    }

    /**
     * Start the game loop
     */
    fun start() {
        if (isRunning) return
        isRunning = true
        isPaused = false
        lastMark = timeSource.markNow()
        accumulatedTime = 0f
        frameCount = 0
        fpsUpdateTime = 0f
    }

    /**
     * Stop the game loop
     */
    fun stop() {
        isRunning = false
    }

    /**
     * Pause the game loop
     */
    fun pause() {
        isPaused = true
    }

    /**
     * Resume the game loop
     */
    fun resume() {
        isPaused = false
        lastMark = timeSource.markNow() // Reset last time to avoid large delta after pause
    }

    /**
     * Call this from your rendering framework's frame callback (e.g., Compose's draw loop or Canvas animation)
     */
    fun tick() {
        if (!isRunning) return

        val currentMark = timeSource.markNow()
        val previousMark = lastMark ?: currentMark
        lastMark = currentMark
        var frameTime = (currentMark - previousMark).inWholeNanoseconds / 1e9f // Convert to seconds

        // Apply time scale
        frameTime *= timeScale

        // Clamp frame time to prevent spiral of death
        if (frameTime > maxAccumulatedTime) {
            frameTime = maxAccumulatedTime
        }

        // Calculate FPS
        fpsUpdateTime += frameTime
        frameCount++
        if (fpsUpdateTime >= 1f) {
            currentFps = frameCount
            frameCount = 0
            fpsUpdateTime -= 1f
        }

        if (!isPaused) {
            accumulatedTime += frameTime

            // Fixed timestep updates
            while (accumulatedTime >= fixedTimestep) {
                listeners.forEach { it.onFixedUpdate(fixedTimestep) }
                accumulatedTime -= fixedTimestep
            }
        }

        // Calculate interpolation factor
        val alpha = accumulatedTime / fixedTimestep

        // Variable timestep update (rendering)
        listeners.forEach { it.onUpdate(frameTime, alpha) }
    }
}
