package com.gamehub.shared.graphics.animation

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import kotlin.math.cos
import kotlin.math.sin

/**
 * 2D Bone for skeletal animation
 */
class Bone(
    val name: String,
    var position: Offset = Offset.Zero,
    var rotation: Float = 0f,
    var scale: Float = 1f,
    var length: Float = 50f,
    val parent: Bone? = null
) {
    val children = mutableListOf<Bone>()

    init {
        parent?.children?.add(this)
    }

    /**
     * Get world transform of this bone
     */
    val worldPosition: Offset
        get() = parent?.let { p ->
            val parentWorld = p.worldPosition
            val angleRad = Math.toRadians(p.worldRotation.toDouble()).toFloat()
            val rotatedOffset = Offset(
                position.x * cos(angleRad) - position.y * sin(angleRad),
                position.x * sin(angleRad) + position.y * cos(angleRad)
            )
            parentWorld + rotatedOffset * p.worldScale
        } ?: position

    val worldRotation: Float
        get() = parent?.let { it.worldRotation + rotation } ?: rotation

    val worldScale: Float
        get() = parent?.let { it.worldScale * scale } ?: scale

    /**
     * Render the bone (for debugging/visualization)
     */
    fun render(drawScope: DrawScope) {
        drawScope.withTransform({
            translate(left = worldPosition.x, top = worldPosition.y)
            rotate(degrees = worldRotation)
            scale(scaleX = worldScale, scaleY = worldScale)
        }) {
            // Draw bone as line
            drawLine(
                color = androidx.compose.ui.graphics.Color.Red,
                start = Offset.Zero,
                end = Offset(length, 0f),
                strokeWidth = 4f
            )
            // Draw joint
            drawCircle(
                color = androidx.compose.ui.graphics.Color.Blue,
                radius = 6f,
                center = Offset.Zero
            )
        }

        // Render children
        children.forEach { it.render(drawScope) }
    }
}

/**
 * Keyframe for bone animation
 */
data class BoneKeyframe(
    val timeMs: Long,
    val position: Offset,
    val rotation: Float,
    val scale: Float
)

/**
 * 2D Skeletal Animation Clip
 */
data class BoneAnimationClip(
    val name: String,
    val boneKeyframes: Map<String, List<BoneKeyframe>>, // bone name -> keyframes
    val durationMs: Long
)

/**
 * 2D Skeletal Animation Player
 */
class BoneAnimationPlayer(
    val skeleton: Bone
) {
    private var currentClip: BoneAnimationClip? = null
    private var elapsedMs: Long = 0L
    var isLooping: Boolean = true
    var playbackSpeed: Float = 1f

    /**
     * Play an animation clip
     */
    fun play(clip: BoneAnimationClip, loop: Boolean = true) {
        currentClip = clip
        isLooping = loop
        elapsedMs = 0L
        applyPose(0L)
    }

    /**
     * Update animation (call every frame)
     */
    fun update(deltaMs: Long) {
        val clip = currentClip ?: return
        elapsedMs += (deltaMs * playbackSpeed).toLong()

        if (elapsedMs >= clip.durationMs) {
            if (isLooping) {
                elapsedMs %= clip.durationMs
            } else {
                elapsedMs = clip.durationMs
            }
        }

        applyPose(elapsedMs)
    }

    private fun applyPose(timeMs: Long) {
        val clip = currentClip ?: return
        clip.boneKeyframes.forEach { (boneName, keyframes) ->
            val bone = findBone(skeleton, boneName) ?: return@forEach
            // Interpolate between keyframes
            val (prev, next) = getSurroundingKeyframes(keyframes, timeMs)
            bone.position = lerp(prev.position, next.position, getT(timeMs, prev, next))
            bone.rotation = lerp(prev.rotation, next.rotation, getT(timeMs, prev, next))
            bone.scale = lerp(prev.scale, next.scale, getT(timeMs, prev, next))
        }
    }

    private fun getSurroundingKeyframes(
        keyframes: List<BoneKeyframe>,
        timeMs: Long
    ): Pair<BoneKeyframe, BoneKeyframe> {
        var prev = keyframes.first()
        var next = keyframes.last()
        for (i in keyframes.indices) {
            if (keyframes[i].timeMs > timeMs) {
                next = keyframes[i]
                prev = if (i > 0) keyframes[i - 1] else keyframes.first()
                break
            }
        }
        return prev to next
    }

    private fun getT(timeMs: Long, prev: BoneKeyframe, next: BoneKeyframe): Float {
        if (prev.timeMs == next.timeMs) return 0f
        return ((timeMs - prev.timeMs).toFloat() / (next.timeMs - prev.timeMs).toFloat()).coerceIn(0f, 1f)
    }

    private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

    private fun lerp(a: Offset, b: Offset, t: Float): Offset = Offset(
        lerp(a.x, b.x, t),
        lerp(a.y, b.y, t)
    )

    private fun findBone(root: Bone, name: String): Bone? {
        if (root.name == name) return root
        root.children.forEach { child ->
            val found = findBone(child, name)
            if (found != null) return found
        }
        return null
    }
}
