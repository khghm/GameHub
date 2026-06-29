package com.gamehub.shared.graphics.particles

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.gamehub.shared.graphics.DefaultTimeProvider
import com.gamehub.shared.graphics.DeviceTierDetector
import com.gamehub.shared.graphics.TimeProvider
import com.gamehub.shared.graphics.engine.GameLoop
import com.gamehub.shared.graphics.engine.GameLoopListener
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

enum class ParticleShape {
    Circle, Square
}

data class Particle(
    var x: Float = 0f,
    var y: Float = 0f,
    var vx: Float = 0f,
    var vy: Float = 0f,
    var life: Float = 1f,
    var maxLife: Float = 1f,
    var size: Float = 0f,
    var color: Color = Color.White,
    var rotation: Float = 0f,
    var isNormalized: Boolean = false,
    var shape: ParticleShape = ParticleShape.Circle
) {
    val isAlive: Boolean get() = life > 0f

    fun reset() {
        x = 0f
        y = 0f
        vx = 0f
        vy = 0f
        life = 1f
        maxLife = 1f
        size = 0f
        color = Color.White
        rotation = 0f
        isNormalized = false
        shape = ParticleShape.Circle
    }
}

class ParticlePool(private val maxSize: Int) {
    private val pool = mutableListOf<Particle>()
    private val active = mutableListOf<Particle>()

    init {
        repeat(maxSize) {
            pool.add(Particle())
        }
    }

    fun acquire(): Particle? {
        return if (pool.isNotEmpty()) {
            val particle = pool.removeAt(pool.size - 1)
            active.add(particle)
            particle
        } else {
            null
        }
    }

    fun release(particle: Particle) {
        if (active.remove(particle)) {
            particle.reset()
            pool.add(particle)
        }
    }

    fun getActiveParticles(): List<Particle> = active.toList()

    fun releaseAll() {
        active.forEach { it.reset() }
        pool.addAll(active)
        active.clear()
    }
}

interface ParticleBehavior {
    fun apply(particle: Particle, deltaSeconds: Float)
}

class GravityBehavior(private val gravity: Float = 400f) : ParticleBehavior {
    override fun apply(particle: Particle, deltaSeconds: Float) {
        particle.vy += gravity * deltaSeconds
    }
}

class WindBehavior(private val windX: Float = 0f, private val windY: Float = 0f) : ParticleBehavior {
    override fun apply(particle: Particle, deltaSeconds: Float) {
        particle.vx += windX * deltaSeconds
        particle.vy += windY * deltaSeconds
    }
}

class ParticleSystem(
    private val maxParticles: Int = DeviceTierDetector.getMaxParticleCount(),
    private val timeProvider: TimeProvider = DefaultTimeProvider()
) {
    private val pool = ParticlePool(maxParticles)
    private val random = Random(timeProvider.currentTimeMillis())
    private val behaviors = mutableListOf<ParticleBehavior>()

    fun addBehavior(behavior: ParticleBehavior) {
        behaviors.add(behavior)
    }

    fun removeBehavior(behavior: ParticleBehavior) {
        behaviors.remove(behavior)
    }

    fun clearBehaviors() {
        behaviors.clear()
    }

    fun emitBurst(
        centerX: Float,
        centerY: Float,
        count: Int,
        colors: List<Color>,
        minSpeed: Float = 100f,
        maxSpeed: Float = 300f,
        minLife: Float = 0.5f,
        maxLife: Float = 1.5f,
        minSize: Float = 4f,
        maxSize: Float = 12f,
        isNormalized: Boolean = false,
        shape: ParticleShape = ParticleShape.Circle
    ) {
        repeat(count) {
            val particle = pool.acquire() ?: return@repeat

            val angle = random.nextFloat() * Math.PI * 2
            val speed = minSpeed + random.nextFloat() * (maxSpeed - minSpeed)

            particle.x = centerX
            particle.y = centerY
            particle.vx = cos(angle).toFloat() * speed
            particle.vy = sin(angle).toFloat() * speed
            particle.maxLife = minLife + random.nextFloat() * (maxLife - minLife)
            particle.life = particle.maxLife
            particle.size = minSize + random.nextFloat() * (maxSize - minSize)
            particle.color = colors.random()
            particle.rotation = random.nextFloat() * 360f
            particle.isNormalized = isNormalized
            particle.shape = shape
        }
    }

    fun emitContinuous(
        centerX: Float,
        centerY: Float,
        ratePerSecond: Int,
        colors: List<Color>,
        minSpeed: Float = 100f,
        maxSpeed: Float = 300f,
        minLife: Float = 0.5f,
        maxLife: Float = 1.5f,
        minSize: Float = 4f,
        maxSize: Float = 12f,
        isNormalized: Boolean = false,
        shape: ParticleShape = ParticleShape.Circle
    ) {
        val count = ratePerSecond / 60 // ~60 FPS
        repeat(count) {
            emitBurst(
                centerX = centerX,
                centerY = centerY,
                count = 1,
                colors = colors,
                minSpeed = minSpeed,
                maxSpeed = maxSpeed,
                minLife = minLife,
                maxLife = maxLife,
                minSize = minSize,
                maxSize = maxSize,
                isNormalized = isNormalized,
                shape = shape
            )
        }
    }

    fun emitDirectional(
        centerX: Float,
        centerY: Float,
        count: Int,
        angleRadians: Float,
        spreadRadians: Float,
        colors: List<Color>,
        minSpeed: Float = 100f,
        maxSpeed: Float = 300f,
        minLife: Float = 0.5f,
        maxLife: Float = 1.5f,
        minSize: Float = 4f,
        maxSize: Float = 12f,
        isNormalized: Boolean = false,
        shape: ParticleShape = ParticleShape.Circle
    ) {
        repeat(count) {
            val particle = pool.acquire() ?: return@repeat

            val angle = angleRadians + (random.nextFloat() - 0.5f) * spreadRadians
            val speed = minSpeed + random.nextFloat() * (maxSpeed - minSpeed)

            particle.x = centerX
            particle.y = centerY
            particle.vx = cos(angle).toFloat() * speed
            particle.vy = sin(angle).toFloat() * speed
            particle.maxLife = minLife + random.nextFloat() * (maxLife - minLife)
            particle.life = particle.maxLife
            particle.size = minSize + random.nextFloat() * (maxSize - minSize)
            particle.color = colors.random()
            particle.rotation = random.nextFloat() * 360f
            particle.isNormalized = isNormalized
            particle.shape = shape
        }
    }

    /**
     * Emit particles from a rectangular area
     */
    fun emitRect(
        left: Float,
        top: Float,
        width: Float,
        height: Float,
        count: Int,
        colors: List<Color>,
        minSpeed: Float = 100f,
        maxSpeed: Float = 300f,
        minLife: Float = 0.5f,
        maxLife: Float = 1.5f,
        minSize: Float = 4f,
        maxSize: Float = 12f,
        isNormalized: Boolean = false,
        shape: ParticleShape = ParticleShape.Circle
    ) {
        repeat(count) {
            val particle = pool.acquire() ?: return@repeat
            val angle = random.nextFloat() * Math.PI.toFloat() * 2f
            val speed = minSpeed + random.nextFloat() * (maxSpeed - minSpeed)
            particle.x = left + random.nextFloat() * width
            particle.y = top + random.nextFloat() * height
            particle.vx = cos(angle) * speed
            particle.vy = sin(angle) * speed
            particle.maxLife = minLife + random.nextFloat() * (maxLife - minLife)
            particle.life = particle.maxLife
            particle.size = minSize + random.nextFloat() * (maxSize - minSize)
            particle.color = colors.random()
            particle.rotation = random.nextFloat() * 360f
            particle.isNormalized = isNormalized
            particle.shape = shape
        }
    }

    /**
     * Emit particles from a circle (or ring)
     */
    fun emitCircle(
        centerX: Float,
        centerY: Float,
        minRadius: Float,
        maxRadius: Float,
        count: Int,
        colors: List<Color>,
        minSpeed: Float = 100f,
        maxSpeed: Float = 300f,
        minLife: Float = 0.5f,
        maxLife: Float = 1.5f,
        minSize: Float = 4f,
        maxSize: Float = 12f,
        isNormalized: Boolean = false,
        shape: ParticleShape = ParticleShape.Circle
    ) {
        repeat(count) {
            val particle = pool.acquire() ?: return@repeat
            val angle = random.nextFloat() * Math.PI.toFloat() * 2f
            val radius = minRadius + random.nextFloat() * (maxRadius - minRadius)
            val speed = minSpeed + random.nextFloat() * (maxSpeed - minSpeed)
            particle.x = centerX + cos(angle) * radius
            particle.y = centerY + sin(angle) * radius
            particle.vx = cos(angle) * speed
            particle.vy = sin(angle) * speed
            particle.maxLife = minLife + random.nextFloat() * (maxLife - minLife)
            particle.life = particle.maxLife
            particle.size = minSize + random.nextFloat() * (maxSize - minSize)
            particle.color = colors.random()
            particle.rotation = random.nextFloat() * 360f
            particle.isNormalized = isNormalized
            particle.shape = shape
        }
    }

    /**
     * Update particles with delta time in milliseconds
     */
    fun update(deltaTimeMs: Long) {
        update(deltaTimeMs / 1000f)
    }

    /**
     * Update particles with delta time in seconds (compatible with GameLoop)
     */
    fun update(deltaSeconds: Float) {
        val particles = pool.getActiveParticles()

        for (particle in particles) {
            if (!particle.isAlive) {
                pool.release(particle)
                continue
            }

            particle.x += particle.vx * deltaSeconds
            particle.y += particle.vy * deltaSeconds
            
            behaviors.forEach { it.apply(particle, deltaSeconds) }
            
            particle.life -= deltaSeconds / particle.maxLife
            particle.rotation += 100f * deltaSeconds
        }
    }

    fun draw(scope: DrawScope) {
        val particles = pool.getActiveParticles()
        val canvasSize = scope.size
        for (particle in particles) {
            if (!particle.isAlive) continue

            val alpha = particle.life.coerceIn(0f, 1f)
            val color = particle.color.copy(alpha = alpha)

            val (x, y) = if (particle.isNormalized) {
                particle.x * canvasSize.width to particle.y * canvasSize.height
            } else {
                particle.x to particle.y
            }

            val radius = if (particle.isNormalized) {
                particle.size * (canvasSize.width / 400f) // Scale size for normalized coordinates
            } else {
                particle.size
            }

            when (particle.shape) {
                ParticleShape.Circle -> {
                    scope.drawCircle(
                        color = color,
                        radius = radius,
                        center = Offset(x, y)
                    )
                }
                ParticleShape.Square -> {
                    scope.drawRect(
                        color = color,
                        topLeft = Offset(x - radius, y - radius),
                        size = Size(radius * 2, radius * 2)
                    )
                }
            }

            // Glow effect for high tier
            if (DeviceTierDetector.isHighTier()) {
                scope.drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(color.copy(alpha = alpha * 0.5f), Color.Transparent),
                        center = Offset(x, y),
                        radius = radius * 2
                    ),
                    radius = radius * 2,
                    center = Offset(x, y)
                )
            }
        }
    }

    fun clear() {
        pool.releaseAll()
    }
}

@Composable
fun ParticleEffect(
    modifier: Modifier = Modifier,
    particleSystem: ParticleSystem,
    isActive: Boolean = true,
    timeProvider: TimeProvider = DefaultTimeProvider(),
    gameLoop: GameLoop? = null
) {
    var lastFrameTime by remember { mutableStateOf<Long?>(null) }

    // If gameLoop is provided, use it, otherwise use internal loop
    if (gameLoop != null) {
        DisposableEffect(particleSystem, gameLoop, isActive) {
            val listener = object : GameLoopListener {
                override fun onFixedUpdate(deltaTime: Float) {
                    if (isActive) {
                        particleSystem.update(deltaTime)
                    }
                }

                override fun onUpdate(deltaTime: Float, alpha: Float) {
                    // Rendering handled in Canvas
                }
            }
            gameLoop.addListener(listener)
            onDispose {
                gameLoop.removeListener(listener)
                particleSystem.clear()
            }
        }
    } else {
        LaunchedEffect(isActive) {
            if (!isActive) {
                particleSystem.clear()
                return@LaunchedEffect
            }

            while (true) {
                val currentTime = timeProvider.currentTimeMillis()
                val deltaTime = lastFrameTime?.let { currentTime - it } ?: 16L
                lastFrameTime = currentTime
                particleSystem.update(deltaTime)
                delay(16L)
            }
        }

        DisposableEffect(Unit) {
            onDispose {
                particleSystem.clear()
            }
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        particleSystem.draw(this)
    }
}
