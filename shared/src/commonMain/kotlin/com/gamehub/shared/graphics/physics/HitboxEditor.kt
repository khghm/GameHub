package com.gamehub.shared.graphics.physics

import com.gamehub.shared.graphics.math.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*

/**
 * Serializable Hitbox Data
 */
@Serializable
sealed class HitboxData {
    abstract val name: String
}

@Serializable
data class CircleHitboxData(
    override val name: String,
    val centerX: Float,
    val centerY: Float,
    val radius: Float
) : HitboxData() {
    fun toShape(): CircleShape = CircleShape(Vec2(centerX, centerY), radius)
}

@Serializable
data class RectHitboxData(
    override val name: String,
    val centerX: Float,
    val centerY: Float,
    val width: Float,
    val height: Float
) : HitboxData() {
    fun toShape(): RectShape = RectShape(Vec2(centerX, centerY), Size(width, height))
}

@Serializable
data class PolygonHitboxData(
    override val name: String,
    val vertices: List<VertexData>
) : HitboxData() {
    fun toShape(): PolygonShape = PolygonShape(vertices.map { Vec2(it.x, it.y) })
}

@Serializable
data class VertexData(
    val x: Float,
    val y: Float
)

/**
 * Sprite Hitbox Container (for animation frames)
 */
@Serializable
data class SpriteHitboxSet(
    val spriteName: String,
    val hitboxes: List<HitboxData>
)

/**
 * Hitbox Editor Helper
 */
object HitboxEditor {
    val json = Json {
        prettyPrint = true
        encodeDefaults = true
        isLenient = true
    }

    /**
     * Serialize a single hitbox
     */
    fun serializeHitbox(hitbox: HitboxData): String = json.encodeToString(hitbox)

    /**
     * Deserialize a single hitbox
     */
    fun deserializeHitbox(jsonStr: String): HitboxData = json.decodeFromString(jsonStr)

    /**
     * Serialize a hitbox set
     */
    fun serializeHitboxSet(hitboxSet: SpriteHitboxSet): String = json.encodeToString(hitboxSet)

    /**
     * Deserialize a hitbox set
     */
    fun deserializeHitboxSet(jsonStr: String): SpriteHitboxSet = json.decodeFromString(jsonStr)

    /**
     * Create a hitbox data from CollisionShape
     */
    fun createHitboxData(shape: CollisionShape, name: String): HitboxData = when (shape) {
        is CircleShape -> CircleHitboxData(name, shape.position.x, shape.position.y, shape.radius)
        is RectShape -> RectHitboxData(name, shape.position.x, shape.position.y, shape.size.width, shape.size.height)
        is PolygonShape -> PolygonHitboxData(name, shape.vertices.map { VertexData(it.x, it.y) })
        is LineShape -> RectHitboxData(name, shape.position.x, shape.position.y, 10f, 10f) // Fallback
    }
}
