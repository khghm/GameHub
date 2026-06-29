package com.gamehub.shared.graphics.physics

import com.gamehub.shared.graphics.math.*
import kotlin.math.*

/**
 * کلاس پایه برای تمام مفاصل
 */
abstract class Joint(
    val bodyA: PhysicsBody,
    val bodyB: PhysicsBody,
    var collideConnected: Boolean = false
) {
    abstract fun initVelocityConstraint(dt: Float)
    abstract fun solveVelocityConstraint(dt: Float)
    abstract fun solvePositionConstraint(dt: Float)
    fun apply(dt: Float) {
        initVelocityConstraint(dt)
        solveVelocityConstraint(dt)
        solvePositionConstraint(dt)
    }

    protected fun getWorldPoint(body: PhysicsBody, localPoint: Vec2): Vec2 {
        val c = cos(body.angle)
        val s = sin(body.angle)
        return Vec2(
            localPoint.x * c - localPoint.y * s,
            localPoint.x * s + localPoint.y * c
        ) + body.position
    }
}

/**
 * مفصل فاصله‌گذار – دو نقطه را در فاصله ثابت نگه می‌دارد
 * با پشتیبانی از سفتی (stiffness) و میرایی (damping)
 */
class DistanceJoint(
    bodyA: PhysicsBody,
    bodyB: PhysicsBody,
    val localAnchorA: Vec2 = Vec2.Zero,
    val localAnchorB: Vec2 = Vec2.Zero,
    var targetDistance: Float = 0f,
    var stiffness: Float = 1000f,   // N/m (واحد فرضی)
    var damping: Float = 5f          // Ns/m
) : Joint(bodyA, bodyB) {
    private var bias: Float = 0f
    private var effectiveMass: Float = 0f
    private var impulse: Float = 0f
    private var worldAnchorA: Vec2 = Vec2.Zero
    private var worldAnchorB: Vec2 = Vec2.Zero
    private var normal: Vec2 = Vec2.Zero
    private var massA: Float = 0f
    private var massB: Float = 0f
    private var invIA: Float = 0f
    private var invIB: Float = 0f
    private var rA: Vec2 = Vec2.Zero
    private var rB: Vec2 = Vec2.Zero

    fun getWorldAnchorA(): Vec2 = getWorldPoint(bodyA, localAnchorA)
    fun getWorldAnchorB(): Vec2 = getWorldPoint(bodyB, localAnchorB)

    init {
        if (targetDistance <= 0f) {
            targetDistance = (getWorldAnchorA() - getWorldAnchorB()).length()
        }
    }

    override fun initVelocityConstraint(dt: Float) {
        worldAnchorA = getWorldPoint(bodyA, localAnchorA)
        worldAnchorB = getWorldPoint(bodyB, localAnchorB)
        normal = worldAnchorB - worldAnchorA
        val len = normal.length()
        normal = if (len > 1e-8f) normal / len else Vec2.Right

        rA = worldAnchorA - bodyA.position
        rB = worldAnchorB - bodyB.position
        massA = if (bodyA.isStatic) 0f else 1f / bodyA.mass
        massB = if (bodyB.isStatic) 0f else 1f / bodyB.mass
        invIA = if (bodyA.isStatic) 0f else 1f / bodyA.momentOfInertia
        invIB = if (bodyB.isStatic) 0f else 1f / bodyB.momentOfInertia

        val crA = rA cross normal
        val crB = rB cross normal
        effectiveMass = massA + massB + invIA * crA * crA + invIB * crB * crB
        if (effectiveMass > 0f) effectiveMass = 1f / effectiveMass

        // بایاس با استفاده از stiffness و damping (روش جبران خطا)
        val currentDist = len
        val error = currentDist - targetDistance
        val softness = 1f / (stiffness * dt + damping)
        bias = -softness * error

        // warm starting
        val p = normal * impulse
        if (!bodyA.isStatic) {
            bodyA.velocity -= p * massA
            bodyA.angularVelocity -= (rA cross p) * invIA
        }
        if (!bodyB.isStatic) {
            bodyB.velocity += p * massB
            bodyB.angularVelocity += (rB cross p) * invIB
        }
    }

    override fun solveVelocityConstraint(dt: Float) {
        val vA = bodyA.velocity + Vec2(-bodyA.angularVelocity * rA.y, bodyA.angularVelocity * rA.x)
        val vB = if (bodyB.isStatic) Vec2.Zero else bodyB.velocity + Vec2(-bodyB.angularVelocity * rB.y, bodyB.angularVelocity * rB.x)
        val relV = vB - vA
        val vn = relV dot normal
        val lambda = -effectiveMass * (vn + bias)
        impulse += lambda
        val p = normal * lambda
        if (!bodyA.isStatic) {
            bodyA.velocity -= p * massA
            bodyA.angularVelocity -= (rA cross p) * invIA
        }
        if (!bodyB.isStatic) {
            bodyB.velocity += p * massB
            bodyB.angularVelocity += (rB cross p) * invIB
        }
    }

    override fun solvePositionConstraint(dt: Float) {
        // به‌روزرسانی موقعیت با استفاده از روش جبران خطا (قبلاً در bias لحاظ شده)
        // نیازی به اصلاح مستقیم موقعیت نیست (چون softness اجازه می‌دهد)
    }
}

/**
 * مفصل چرخشی (Revolute) – اجازه چرخش حول یک نقطه را می‌دهد
 * با محدودیت زاویه و موتور
 */
class RevoluteJoint(
    bodyA: PhysicsBody,
    bodyB: PhysicsBody,
    val localAnchorA: Vec2 = Vec2.Zero,
    val localAnchorB: Vec2 = Vec2.Zero,
    var enableLimit: Boolean = false,
    var lowerAngle: Float = -Float.MAX_VALUE,
    var upperAngle: Float = Float.MAX_VALUE,
    var enableMotor: Boolean = false,
    var motorSpeed: Float = 0f,
    var maxMotorTorque: Float = Float.MAX_VALUE
) : Joint(bodyA, bodyB) {
    private var effectiveMass: Mat2x2 = Mat2x2()
    private var impulse: Vec2 = Vec2.Zero
    private var motorImpulse: Float = 0f
    private var rA: Vec2 = Vec2.Zero
    private var rB: Vec2 = Vec2.Zero
    private var invMA: Float = 0f
    private var invMB: Float = 0f
    private var invIA: Float = 0f
    private var invIB: Float = 0f
    private var worldAnchorA: Vec2 = Vec2.Zero
    private var worldAnchorB: Vec2 = Vec2.Zero
    private var bias: Vec2 = Vec2.Zero
    private var initialAngleOffset: Float = 0f

    fun getWorldAnchorA(): Vec2 = getWorldPoint(bodyA, localAnchorA)
    fun getWorldAnchorB(): Vec2 = getWorldPoint(bodyB, localAnchorB)

    init {
        initialAngleOffset = bodyB.angle - bodyA.angle
    }

    override fun initVelocityConstraint(dt: Float) {
        worldAnchorA = getWorldPoint(bodyA, localAnchorA)
        worldAnchorB = getWorldPoint(bodyB, localAnchorB)
        rA = worldAnchorA - bodyA.position
        rB = worldAnchorB - bodyB.position
        invMA = if (bodyA.isStatic) 0f else 1f / bodyA.mass
        invMB = if (bodyB.isStatic) 0f else 1f / bodyB.mass
        invIA = if (bodyA.isStatic) 0f else 1f / bodyA.momentOfInertia
        invIB = if (bodyB.isStatic) 0f else 1f / bodyB.momentOfInertia

        val rA_skew = Vec2(-rA.y, rA.x)
        val rB_skew = Vec2(-rB.y, rB.x)

        val k11 = invMA + invMB
        val k12 = -rA.y * invIA - rB.y * invIB
        val k21 = rA.x * invIA + rB.x * invIB
        val k22 = rA.x * rA.x * invIA + rA.y * rA.y * invIA + rB.x * rB.x * invIB + rB.y * rB.y * invIB
        effectiveMass = Mat2x2(k11, k12, k21, k22).inverse()

        val c = worldAnchorB - worldAnchorA
        val biasFactor = 0.2f
        bias = -c * (biasFactor / dt)

        // warm starting
        val p = impulse
        if (!bodyA.isStatic) {
            bodyA.velocity -= p * invMA
            bodyA.angularVelocity -= (rA cross p) * invIA
        }
        if (!bodyB.isStatic) {
            bodyB.velocity += p * invMB
            bodyB.angularVelocity += (rB cross p) * invIB
        }
        if (enableMotor) {
            if (!bodyA.isStatic) bodyA.angularVelocity -= motorImpulse * invIA
            if (!bodyB.isStatic) bodyB.angularVelocity += motorImpulse * invIB
        }
    }

    override fun solveVelocityConstraint(dt: Float) {
        // موتور
        if (enableMotor) {
            val w = bodyB.angularVelocity - bodyA.angularVelocity
            val cDot = w - motorSpeed
            val motorMass = invIA + invIB
            var lambda = -cDot / if (motorMass > 0f) motorMass else 1f
            val oldMotorImpulse = motorImpulse
            motorImpulse = (oldMotorImpulse + lambda).coerceIn(-maxMotorTorque * dt, maxMotorTorque * dt)
            lambda = motorImpulse - oldMotorImpulse
            if (!bodyA.isStatic) bodyA.angularVelocity -= lambda * invIA
            if (!bodyB.isStatic) bodyB.angularVelocity += lambda * invIB
        }

        // محدودیت نقطه
        val vA = bodyA.velocity + Vec2(-bodyA.angularVelocity * rA.y, bodyA.angularVelocity * rA.x)
        val vB = if (bodyB.isStatic) Vec2.Zero else bodyB.velocity + Vec2(-bodyB.angularVelocity * rB.y, bodyB.angularVelocity * rB.x)
        var cDot = vB - vA
        cDot += bias
        val lambda = effectiveMass * (-cDot)
        impulse += lambda
        if (!bodyA.isStatic) {
            bodyA.velocity -= lambda * invMA
            bodyA.angularVelocity -= (rA cross lambda) * invIA
        }
        if (!bodyB.isStatic) {
            bodyB.velocity += lambda * invMB
            bodyB.angularVelocity += (rB cross lambda) * invIB
        }

        // محدودیت زاویه (با بایاس سرعت برای پایداری)
        if (enableLimit) {
            val angle = bodyB.angle - bodyA.angle - initialAngleOffset
            if (angle < lowerAngle || angle > upperAngle) {
                val target = angle.coerceIn(lowerAngle, upperAngle)
                val error = angle - target
                val biasAng = -0.2f / dt * error
                val w = bodyB.angularVelocity - bodyA.angularVelocity
                val massAng = invIA + invIB
                val lambdaAng = if (massAng > 0f) (biasAng - w) / massAng else 0f
                if (!bodyA.isStatic) bodyA.angularVelocity -= lambdaAng * invIA
                if (!bodyB.isStatic) bodyB.angularVelocity += lambdaAng * invIB
            }
        }
    }

    override fun solvePositionConstraint(dt: Float) {
        // اصلاح موقعیت نقطه
        worldAnchorA = getWorldPoint(bodyA, localAnchorA)
        worldAnchorB = getWorldPoint(bodyB, localAnchorB)
        val c = worldAnchorB - worldAnchorA
        if (c.lengthSquared() < 1e-8f) return

        val rA_ = worldAnchorA - bodyA.position
        val rB_ = worldAnchorB - bodyB.position
        val k11 = invMA + invMB
        val k12 = -rA_.y * invIA - rB_.y * invIB
        val k21 = rA_.x * invIA + rB_.x * invIB
        val k22 = rA_.x * rA_.x * invIA + rA_.y * rA_.y * invIA + rB_.x * rB_.x * invIB + rB_.y * rB_.y * invIB
        val k = Mat2x2(k11, k12, k21, k22).inverse()
        val impulse_ = k * (-c)
        if (!bodyA.isStatic) {
            bodyA.position -= impulse_ * invMA
            bodyA.angle -= (rA_ cross impulse_) * invIA
        }
        if (!bodyB.isStatic) {
            bodyB.position += impulse_ * invMB
            bodyB.angle += (rB_ cross impulse_) * invIB
        }

        // اصلاح محدودیت زاویه (با روش مستقیم)
        if (enableLimit) {
            val angle = bodyB.angle - bodyA.angle - initialAngleOffset
            if (angle < lowerAngle || angle > upperAngle) {
                val target = angle.coerceIn(lowerAngle, upperAngle)
                val error = angle - target
                val massAng = invIA + invIB
                val lambdaAng = if (massAng > 0f) -error / massAng else 0f
                if (!bodyA.isStatic) bodyA.angle -= lambdaAng * invIA
                if (!bodyB.isStatic) bodyB.angle += lambdaAng * invIB
                // به‌روزرسانی offset برای جلوگیری از رانش
                initialAngleOffset += lambdaAng
            }
        }
    }
}

// ماتریس ۲×۲ برای محاسبات مفاصل
data class Mat2x2(val a: Float = 1f, val b: Float = 0f, val c: Float = 0f, val d: Float = 1f) {
    operator fun times(v: Vec2): Vec2 = Vec2(a * v.x + b * v.y, c * v.x + d * v.y)
    fun inverse(): Mat2x2 {
        val det = a * d - b * c
        if (abs(det) < 1e-8f) return this
        val invDet = 1f / det
        return Mat2x2(d * invDet, -b * invDet, -c * invDet, a * invDet)
    }
}