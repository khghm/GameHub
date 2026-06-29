package com.gamehub.shared.graphics.physics

import com.gamehub.shared.graphics.math.*
import kotlin.math.*

/**
 * نقطه تماس – شامل اطلاعاتی برای حل برخورد
 */
data class ContactPoint(
    val position: Vec2,
    var normalImpulse: Float = 0f,      // ضربه نرمال (برای warm starting)
    var tangentImpulse: Float = 0f,     // ضربه مماسی (اصطکاک)
    val penetration: Float = 0f,        // عمق نفوذ (مثبت)
    val rA: Vec2 = Vec2.Zero,
    val rB: Vec2 = Vec2.Zero,
    val normalMass: Float = 0f,
    val tangentMass: Float = 0f,
    val velocityBias: Float = 0f
)

/**
 * منیفلد تماس – شامل چند نقطه تماس بین دو جسم
 */
data class ContactManifold(
    val bodyA: PhysicsBody,
    val bodyB: PhysicsBody,
    val normal: Vec2,
    val points: List<ContactPoint>,
    val friction: Float,
    val restitution: Float,
    val isSensor: Boolean = false
)

/**
 * تولیدکننده منیفلد تماس برای جفت‌های مختلف اشکال
 * (ارتقاء یافته برای استفاده از CollisionSystem با GJK)
 */
object ContactGenerator {
    fun generateManifold(
        bodyA: PhysicsBody,
        shapeA: CollisionShape,
        bodyB: PhysicsBody,
        shapeB: CollisionShape
    ): ContactManifold? {
        val result = when {
            shapeA is CircleShape && shapeB is CircleShape ->
                CollisionSystem.checkCollision(shapeA, shapeB)
            shapeA is RectShape && shapeB is RectShape ->
                CollisionSystem.checkCollision(shapeA, shapeB)
            shapeA is CircleShape && shapeB is RectShape ->
                CollisionSystem.checkCollision(shapeA, shapeB)
            shapeA is RectShape && shapeB is CircleShape ->
                CollisionSystem.checkCollision(shapeA, shapeB)
            shapeA is PolygonShape && shapeB is PolygonShape ->
                CollisionSystem.checkCollision(shapeA, shapeB)
            shapeA is CircleShape && shapeB is PolygonShape ->
                CollisionSystem.checkCollision(shapeB, shapeA)
            shapeA is PolygonShape && shapeB is CircleShape ->
                CollisionSystem.checkCollision(shapeA, shapeB)
            else -> null
        }

        if (result == null || !result.hasCollided) return null

        val contactPoints = result.contactPoints.map { point ->
            ContactPoint(
                position = point,
                penetration = result.depth
            )
        }

        val friction = min(bodyA.friction, bodyB.friction)
        val restitution = min(bodyA.bounciness, bodyB.bounciness)

        return ContactManifold(
            bodyA, bodyB, result.normal, contactPoints,
            friction, restitution
        )
    }
}