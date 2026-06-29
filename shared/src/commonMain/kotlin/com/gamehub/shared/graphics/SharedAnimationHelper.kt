@file:Suppress("unused", "DEPRECATION")
package com.gamehub.shared.graphics

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.gamehub.shared.graphics.animation.bounceClick as newBounceClick
import com.gamehub.shared.graphics.animation.pulse as newPulse
import com.gamehub.shared.graphics.animation.slideIn as newSlideIn
import com.gamehub.shared.graphics.animation.rotateIn as newRotateIn
import com.gamehub.shared.graphics.animation.scaleIn as newScaleIn
import com.gamehub.shared.graphics.theme.*
import com.gamehub.shared.graphics.particles.*
import kotlin.ReplaceWith

// Re-export theme classes
typealias GraphicsSpec = com.gamehub.shared.graphics.theme.GraphicsSpec
typealias DefaultGraphicsSpec = com.gamehub.shared.graphics.theme.DefaultGraphicsSpec

// Re-export animation classes
@Deprecated("Use com.gamehub.shared.graphics.animation.SlideDirection instead", ReplaceWith("com.gamehub.shared.graphics.animation.SlideDirection"))
typealias SlideDirection = com.gamehub.shared.graphics.animation.SlideDirection

@Deprecated("Use com.gamehub.shared.graphics.animation.bounceClick instead", ReplaceWith("bounceClick", "com.gamehub.shared.graphics.animation.bounceClick"))
fun Modifier.bounceClick(
    scaleFactor: Float = 0.95f,
    onClick: () -> Unit
): Modifier = newBounceClick(scaleFactor, onClick)

@Deprecated("Use com.gamehub.shared.graphics.animation.pulse instead", ReplaceWith("pulse", "com.gamehub.shared.graphics.animation.pulse"))
fun Modifier.pulse(
    targetScale: Float = 1.1f,
    durationMs: Int = 1000,
    enabled: Boolean = true
): Modifier = newPulse(targetScale, durationMs, enabled)

@Deprecated("Use com.gamehub.shared.graphics.animation.slideIn instead", ReplaceWith("slideIn", "com.gamehub.shared.graphics.animation.slideIn"))
fun Modifier.slideIn(
    direction: SlideDirection = SlideDirection.Bottom,
    durationMs: Int = 300,
    delayMs: Int = 0
): Modifier = newSlideIn(direction, durationMs, delayMs)

@Deprecated("Use com.gamehub.shared.graphics.animation.rotateIn instead", ReplaceWith("rotateIn", "com.gamehub.shared.graphics.animation.rotateIn"))
fun Modifier.rotateIn(
    durationMs: Int = 500,
    delayMs: Int = 0
): Modifier = newRotateIn(durationMs, delayMs)

@Deprecated("Use com.gamehub.shared.graphics.animation.scaleIn instead", ReplaceWith("scaleIn", "com.gamehub.shared.graphics.animation.scaleIn"))
fun Modifier.scaleIn(
    durationMs: Int = 400,
    delayMs: Int = 0
): Modifier = newScaleIn(durationMs, delayMs)

// Re-export particle system
typealias Particle = com.gamehub.shared.graphics.particles.Particle
typealias ParticlePool = com.gamehub.shared.graphics.particles.ParticlePool
typealias ParticleSystem = com.gamehub.shared.graphics.particles.ParticleSystem
typealias ParticleShape = com.gamehub.shared.graphics.particles.ParticleShape
typealias ParticleBehavior = com.gamehub.shared.graphics.particles.ParticleBehavior
typealias GravityBehavior = com.gamehub.shared.graphics.particles.GravityBehavior
typealias WindBehavior = com.gamehub.shared.graphics.particles.WindBehavior

@Deprecated("Use com.gamehub.shared.graphics.particles.ParticleEffect instead", ReplaceWith("ParticleEffect", "com.gamehub.shared.graphics.particles.ParticleEffect"))
@Composable
fun ParticleEffect(
    modifier: Modifier = Modifier,
    particleSystem: ParticleSystem,
    isActive: Boolean = true
) {
    com.gamehub.shared.graphics.particles.ParticleEffect(modifier, particleSystem, isActive)
}
