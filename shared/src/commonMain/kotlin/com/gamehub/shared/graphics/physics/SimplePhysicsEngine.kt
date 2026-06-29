package com.gamehub.shared.graphics.physics

import com.gamehub.shared.graphics.math.*
import kotlin.math.*

/**
 * بدنه فیزیکی
 */
class PhysicsBody(
    var position: Vec2 = Vec2.Zero,
    var velocity: Vec2 = Vec2.Zero,
    var mass: Float = 1f,
    var drag: Float = 0.01f,
    var bounciness: Float = 0.7f,
    var friction: Float = 0.3f,
    var material: PhysicsMaterial = PhysicsMaterial.Default,
    var shape: CollisionShape,
    var layer: Int = 0x1,
    var mask: Int = 0xFFFF,
    var isSensor: Boolean = false   // اگر true باشد، فقط تشخیص برخورد می‌دهد
) {
    var isStatic: Boolean = false
    var gravityScale: Float = 1f
    var angle: Float = 0f
    var angularVelocity: Float = 0f
    var torque: Float = 0f
    var momentOfInertia: Float = calculateMomentOfInertia()
        private set
    var previousPosition: Vec2 = position
    var previousAngle: Float = angle

    // CCD
    var useCCD: Boolean = false

    // خواب
    var isSleeping: Boolean = false
    private var sleepTimer: Float = 0f
    val sleepThreshold: Float = 0.1f
    val linearSleepThreshold: Float = 0.01f
    val angularSleepThreshold: Float = 0.01f

    fun wakeUp() {
        isSleeping = false
        sleepTimer = 0f
    }

    fun applyForce(force: Vec2) {
        if (!isStatic && !isSleeping) {
            velocity += force / mass
            wakeUp()
        }
    }

    fun applyTorque(t: Float) {
        if (!isStatic && !isSleeping) {
            torque += t
            wakeUp()
        }
    }

    fun applyForceAtPoint(force: Vec2, point: Vec2) {
        if (!isStatic && !isSleeping) {
            velocity += force / mass
            val r = point - position
            torque += (r cross force)
            wakeUp()
        }
    }

    fun applyImpulse(impulse: Vec2, point: Vec2) {
        if (isStatic) return
        velocity += impulse / mass
        val r = point - position
        angularVelocity += (r cross impulse) / momentOfInertia
        wakeUp()
    }

    internal fun updateSleep(deltaTime: Float) {
        if (isStatic) {
            isSleeping = true
            return
        }
        val isMovingSlow = velocity.lengthSquared() < linearSleepThreshold * linearSleepThreshold
        val isRotatingSlow = abs(angularVelocity) < angularSleepThreshold
        if (isMovingSlow && isRotatingSlow) {
            sleepTimer += deltaTime
            if (sleepTimer >= sleepThreshold) {
                isSleeping = true
                velocity = Vec2.Zero
                angularVelocity = 0f
                torque = 0f
            }
        } else {
            sleepTimer = 0f
        }
    }

    private fun calculateMomentOfInertia(): Float {
        if (isStatic || mass <= 0f) return Float.MAX_VALUE
        return when (val s = shape) {
            is CircleShape -> 0.5f * mass * s.radius * s.radius
            is RectShape -> (mass * (s.size.width * s.size.width + s.size.height * s.size.height)) / 12f
            is PolygonShape -> {
                val maxDistSq = s.vertices.maxOf { it.lengthSquared() }
                0.5f * mass * maxDistSq
            }
            is LineShape -> {
                val len = (s.end - s.start).length()
                (mass * len * len) / 12f
            }
        }
    }
}

/**
 * رابط موتور فیزیک
 */
interface PhysicsEngine {
    fun addBody(body: PhysicsBody)
    fun removeBody(body: PhysicsBody)
    fun addJoint(joint: Joint)
    fun removeJoint(joint: Joint)
    fun update(deltaTime: Float)
}

/**
 * موتور فیزیک ساده با قابلیت‌های پیشرفته
 */
class SimplePhysicsEngine(
    var gravity: Vec2 = Vec2(0f, 980f),
    var subSteps: Int = 8,
    var velocityIterations: Int = 8,
    var positionIterations: Int = 3,
    private var worldBounds: Rect = Rect(-100000f, -100000f, 200000f, 200000f)
) : PhysicsEngine {
    val bodies = mutableListOf<PhysicsBody>()
    val joints = mutableListOf<Joint>()

    // استفاده از DynamicAABBTree به جای QuadTree
    private val aabbTree = DynamicAABBTree<PhysicsBody>()

    private val contactManifolds = mutableListOf<ContactManifold>()
    private val previousManifolds = mutableSetOf<Pair<PhysicsBody, PhysicsBody>>()
    private val contactListeners = mutableListOf<PhysicsContactListener>()
    private val bodyListeners = mutableListOf<PhysicsBodyListener>()

    fun addContactListener(listener: PhysicsContactListener) { contactListeners.add(listener) }
    fun removeContactListener(listener: PhysicsContactListener) { contactListeners.remove(listener) }
    fun addBodyListener(listener: PhysicsBodyListener) { bodyListeners.add(listener) }
    fun removeBodyListener(listener: PhysicsBodyListener) { bodyListeners.remove(listener) }

    override fun addBody(body: PhysicsBody) {
        bodies.add(body)
        val bounds = getBoundsForBody(body)
        aabbTree.insert(body, bounds)
        bodyListeners.forEach { it.onBodyAdded(body) }
    }

    override fun removeBody(body: PhysicsBody) {
        bodies.remove(body)
        aabbTree.remove(body)
        bodyListeners.forEach { it.onBodyRemoved(body) }
    }

    override fun addJoint(joint: Joint) { joints.add(joint) }
    override fun removeJoint(joint: Joint) { joints.remove(joint) }

    private fun getBoundsForBody(body: PhysicsBody): Rect {
        return when (val shape = body.shape) {
            is CircleShape -> {
                val r = shape.radius + 0.5f
                Rect.fromCenter(body.position, Size(r * 2, r * 2))
            }
            is RectShape -> {
                val w = shape.size.width + 1f
                val h = shape.size.height + 1f
                Rect.fromCenter(body.position, Size(w, h))
            }
            is PolygonShape -> {
                val verts = shape.copy(position = body.position, rotation = body.angle).worldVertices
                val minX = verts.minOf { it.x } - 0.5f
                val maxX = verts.maxOf { it.x } + 0.5f
                val minY = verts.minOf { it.y } - 0.5f
                val maxY = verts.maxOf { it.y } + 0.5f
                Rect(minX, minY, maxX, maxY)
            }
            is LineShape -> {
                val x = min(shape.start.x, shape.end.x) - 0.5f
                val y = min(shape.start.y, shape.end.y) - 0.5f
                val w = max(shape.start.x, shape.end.x) + 0.5f
                val h = max(shape.start.y, shape.end.y) + 0.5f
                Rect(x, y, w, h).translate(body.position - shape.position)
            }
        }
    }

    override fun update(deltaTime: Float) {
        val dt = deltaTime / subSteps.toFloat()

        for (step in 0 until subSteps) {
            // ذخیره وضعیت قبلی
            bodies.forEach { body ->
                body.previousPosition = body.position
                body.previousAngle = body.angle
            }

            // اعمال نیروها و به‌روزرسانی سرعت
            bodies.filterNot { it.isStatic || it.isSleeping }.forEach { body ->
                body.velocity += gravity * body.gravityScale * dt
                body.velocity -= body.velocity * body.drag
                body.angularVelocity += (body.torque / body.momentOfInertia) * dt
                body.angularVelocity -= body.angularVelocity * body.drag
                body.torque = 0f
            }

            // === Broad Phase با DynamicAABBTree ===
            // به‌روزرسانی موقعیت اجسام در درخت
            bodies.forEach { body ->
                val newBounds = getBoundsForBody(body)
                aabbTree.update(body, newBounds)
            }

            // === تولید منیفلد ===
            contactManifolds.clear()
            val handledPairs = mutableSetOf<Pair<PhysicsBody, PhysicsBody>>()
            val currentManifolds = mutableSetOf<Pair<PhysicsBody, PhysicsBody>>()
            val currentSensorPairs = mutableSetOf<Pair<PhysicsBody, PhysicsBody>>()

            for (i in bodies.indices) {
                val a = bodies[i]
                val aBounds = getBoundsForBody(a)
                val possible = aabbTree.retrieve(aBounds)
                for (b in possible) {
                    if (a === b) continue
                    if ((a.layer and b.mask) == 0 || (b.layer and a.mask) == 0) continue
                    if (a.isSleeping && b.isSleeping && a.isStatic && b.isStatic) continue
                    val pair = if (System.identityHashCode(a) < System.identityHashCode(b)) a to b else b to a
                    if (handledPairs.contains(pair)) continue
                    handledPairs.add(pair)

                    val aShape = updateShapePosition(a)
                    val bShape = updateShapePosition(b)
                    val manifold = ContactGenerator.generateManifold(a, aShape, b, bShape)
                    manifold?.let {
                        val hasJointBetween = joints.any { j ->
                            (j.bodyA == a && j.bodyB == b || j.bodyA == b && j.bodyB == a) && !j.collideConnected
                        }
                        if (!hasJointBetween) {
                            if (a.isSensor || b.isSensor) {
                                currentSensorPairs.add(pair)
                            } else {
                                contactManifolds.add(it)
                                currentManifolds.add(pair)
                                a.wakeUp()
                                b.wakeUp()
                            }
                        }
                    }
                }
            }

            // پردازش رویدادهای سنسور
            val previousSensorPairs = previousManifolds.filter { (a, b) -> a.isSensor || b.isSensor }.toSet()
            contactListeners.forEach { listener ->
                currentSensorPairs.filter { !previousSensorPairs.contains(it) }.forEach { (sensor, other) ->
                    val actualSensor = if (sensor.isSensor) sensor else other
                    val actualOther = if (sensor.isSensor) other else sensor
                    listener.onSensorEnter(actualSensor, actualOther)
                }
                previousSensorPairs.filter { !currentSensorPairs.contains(it) }.forEach { (sensor, other) ->
                    val actualSensor = if (sensor.isSensor) sensor else other
                    val actualOther = if (sensor.isSensor) other else sensor
                    listener.onSensorExit(actualSensor, actualOther)
                }
            }

            // پردازش رویدادهای برخورد
            val previousContactPairs = previousManifolds.filter { (a, b) -> !a.isSensor && !b.isSensor }.toSet()
            contactListeners.forEach { listener ->
                currentManifolds.filter { !previousContactPairs.contains(it) }.forEach { pair ->
                    val manifold = contactManifolds.firstOrNull { m ->
                        (m.bodyA == pair.first && m.bodyB == pair.second) ||
                                (m.bodyA == pair.second && m.bodyB == pair.first)
                    }
                    manifold?.let { listener.onContactEnter(it) }
                }
                currentManifolds.filter { previousContactPairs.contains(it) }.forEach { pair ->
                    val manifold = contactManifolds.firstOrNull { m ->
                        (m.bodyA == pair.first && m.bodyB == pair.second) ||
                                (m.bodyA == pair.second && m.bodyB == pair.first)
                    }
                    manifold?.let { listener.onContactStay(it) }
                }
                previousContactPairs.filter { !currentManifolds.contains(it) }.forEach { pair ->
                    listener.onContactExit(pair.first, pair.second)
                }
            }
            previousManifolds.clear()
            previousManifolds.addAll(currentManifolds)
            previousManifolds.addAll(currentSensorPairs)

            // === حل محدودیت‌ها ===
            contactManifolds.forEach { manifold ->
                initializeContacts(manifold, dt)
            }

            for (iter in 0 until velocityIterations) {
                contactManifolds.forEach { manifold ->
                    solveVelocityConstraints(manifold)
                }
                joints.forEach { joint ->
                    joint.initVelocityConstraint(dt)
                    joint.solveVelocityConstraint(dt)
                }
            }

            // انتگرال‌گیری موقعیت
            bodies.filterNot { it.isStatic || it.isSleeping }.forEach { body ->
                body.position = body.previousPosition + body.velocity * dt
                body.angle = body.previousAngle + body.angularVelocity * dt
            }

            // === CCD پیشرفته ===
            bodies.filter { it.useCCD && !it.isStatic && !it.isSleeping }.forEach { a ->
                var earliestTOI = 1f
                var collidedWith: PhysicsBody? = null
                val aShape = a.shape
                val aDelta = a.position - a.previousPosition

                bodies.forEach { b ->
                    if (a === b) return@forEach
                    if ((a.layer and b.mask) == 0 || (b.layer and a.mask) == 0) return@forEach
                    val bDelta = if (b.isStatic) Vec2.Zero else b.position - b.previousPosition
                    val bShape = b.shape

                    // برای دایره-دایره
                    if (aShape is CircleShape && bShape is CircleShape) {
                        val aStart = CircleShape(a.previousPosition, aShape.radius)
                        val bStart = CircleShape(if (b.isStatic) b.position else b.previousPosition, bShape.radius)
                        val (collided, toi) = CollisionSystem.circleCircleCCD(aStart, bStart, aDelta, bDelta, 1f)
                        if (collided && toi < earliestTOI) {
                            earliestTOI = toi
                            collidedWith = b
                        }
                    }
                    // برای دایره-مستطیل
                    else if (aShape is CircleShape && bShape is RectShape) {
                        val aStart = CircleShape(a.previousPosition, aShape.radius)
                        val bStart = RectShape(if (b.isStatic) b.position else b.previousPosition, bShape.size, bShape.rotation)
                        val (collided, toi) = CollisionSystem.circleRectCCD(aStart, bStart, aDelta, 1f)
                        if (collided && toi < earliestTOI) {
                            earliestTOI = toi
                            collidedWith = b
                        }
                    }
                    // برای چندضلعی-دایره
                    else if (aShape is PolygonShape && bShape is CircleShape) {
                        val aStart = PolygonShape(aShape.vertices, a.previousPosition, a.angle)
                        val bStart = CircleShape(if (b.isStatic) b.position else b.previousPosition, bShape.radius)
                        val (collided, toi) = CollisionSystem.polygonCircleCCD(aStart, bStart, aDelta, bDelta, 1f)
                        if (collided && toi < earliestTOI) {
                            earliestTOI = toi
                            collidedWith = b
                        }
                    }
                    // برای دایره-چندضلعی
                    else if (aShape is CircleShape && bShape is PolygonShape) {
                        val aStart = CircleShape(a.previousPosition, aShape.radius)
                        val bStart = PolygonShape(bShape.vertices, if (b.isStatic) b.position else b.previousPosition, b.angle)
                        val (collided, toi) = CollisionSystem.polygonCircleCCD(bStart, aStart, bDelta, aDelta, 1f)
                        if (collided && toi < earliestTOI) {
                            earliestTOI = toi
                            collidedWith = b
                        }
                    }
                }

                collidedWith?.let { cw ->
                    if (earliestTOI < 1f) {
                        a.position = a.previousPosition + aDelta * earliestTOI
                        if (!cw.isStatic) {
                            val bDelta = cw.position - cw.previousPosition
                            cw.position = cw.previousPosition + bDelta * earliestTOI
                        }
                        a.angle = a.previousAngle + (a.angle - a.previousAngle) * earliestTOI
                        if (!cw.isStatic) {
                            cw.angle = cw.previousAngle + (cw.angle - cw.previousAngle) * earliestTOI
                        }
                    }
                }
            }

            // حل محدودیت‌های موقعیت
            for (iter in 0 until positionIterations) {
                contactManifolds.forEach { manifold ->
                    solvePositionConstraints(manifold)
                }
                joints.forEach { joint ->
                    joint.solvePositionConstraint(dt)
                }
            }

            // به‌روزرسانی خواب
            bodies.forEach { it.updateSleep(dt) }

            // اعمال محدودیت مرزهای جهان
            bodies.filterNot { it.isStatic }.forEach { body ->
                val r = when (val s = body.shape) {
                    is CircleShape -> s.radius
                    else -> 0f
                }
                if (body.position.x - r < worldBounds.left) {
                    body.position.x = worldBounds.left + r
                    body.velocity.x = abs(body.velocity.x) * 0.2f
                }
                if (body.position.x + r > worldBounds.right) {
                    body.position.x = worldBounds.right - r
                    body.velocity.x = -abs(body.velocity.x) * 0.2f
                }
                if (body.position.y - r < worldBounds.top) {
                    body.position.y = worldBounds.top + r
                    body.velocity.y = abs(body.velocity.y) * 0.2f
                }
                if (body.position.y + r > worldBounds.bottom) {
                    body.position.y = worldBounds.bottom - r
                    body.velocity.y = -abs(body.velocity.y) * 0.2f
                }
            }
        }
    }

    // ===== توابع کمکی =====

    private fun initializeContacts(manifold: ContactManifold, dt: Float) {
        val updatedPoints = manifold.points.map { point ->
            val rA = point.position - manifold.bodyA.position
            val rB = point.position - manifold.bodyB.position
            val invMA = if (manifold.bodyA.isStatic) 0f else 1f / manifold.bodyA.mass
            val invMB = if (manifold.bodyB.isStatic) 0f else 1f / manifold.bodyB.mass
            val invIA = if (manifold.bodyA.isStatic) 0f else 1f / manifold.bodyA.momentOfInertia
            val invIB = if (manifold.bodyB.isStatic) 0f else 1f / manifold.bodyB.momentOfInertia
            val rACrossN = rA cross manifold.normal
            val rBCrossN = rB cross manifold.normal
            val normalMass = 1f / (invMA + invMB + invIA * rACrossN * rACrossN + invIB * rBCrossN * rBCrossN)

            val tangent = Vec2(-manifold.normal.y, manifold.normal.x)
            val rACrossT = rA cross tangent
            val rBCrossT = rB cross tangent
            val tangentMass = 1f / (invMA + invMB + invIA * rACrossT * rACrossT + invIB * rBCrossT * rBCrossT)

            // محاسبه سرعت نسبی و بایاس بازگشت (فقط در صورت نزدیک شدن)
            val vA = manifold.bodyA.velocity + Vec2(-manifold.bodyA.angularVelocity * rA.y, manifold.bodyA.angularVelocity * rA.x)
            val vB = if (manifold.bodyB.isStatic) Vec2.Zero else manifold.bodyB.velocity + Vec2(-manifold.bodyB.angularVelocity * rB.y, manifold.bodyB.angularVelocity * rB.x)
            val relV = vA - vB
            val vn = relV dot manifold.normal
            val velocityBias = if (vn < 0f) -manifold.restitution * vn else 0f

            // warm starting
            val normalImpulse = point.normalImpulse
            val tangentImpulse = point.tangentImpulse
            val normalP = manifold.normal * normalImpulse
            val tangentP = tangent * tangentImpulse
            manifold.bodyA.applyImpulse(-normalP - tangentP, point.position)
            manifold.bodyB.applyImpulse(normalP + tangentP, point.position)

            point.copy(
                rA = rA,
                rB = rB,
                normalMass = normalMass,
                tangentMass = tangentMass,
                velocityBias = velocityBias
            )
        }
        val index = contactManifolds.indexOf(manifold)
        if (index != -1) {
            contactManifolds[index] = manifold.copy(points = updatedPoints)
        }
    }

    private fun solveVelocityConstraints(manifold: ContactManifold) {
        val invMA = if (manifold.bodyA.isStatic) 0f else 1f / manifold.bodyA.mass
        val invMB = if (manifold.bodyB.isStatic) 0f else 1f / manifold.bodyB.mass
        val invIA = if (manifold.bodyA.isStatic) 0f else 1f / manifold.bodyA.momentOfInertia
        val invIB = if (manifold.bodyB.isStatic) 0f else 1f / manifold.bodyB.momentOfInertia
        val tangent = Vec2(-manifold.normal.y, manifold.normal.x)

        val updatedPoints = manifold.points.map { point ->
            val vA = manifold.bodyA.velocity + Vec2(-manifold.bodyA.angularVelocity * point.rA.y, manifold.bodyA.angularVelocity * point.rA.x)
            val vB = if (manifold.bodyB.isStatic) Vec2.Zero else manifold.bodyB.velocity + Vec2(-manifold.bodyB.angularVelocity * point.rB.y, manifold.bodyB.angularVelocity * point.rB.x)
            val relV = vA - vB

            // ضربه نرمال
            val vn = relV dot manifold.normal
            var lambda = -point.normalMass * (vn + point.velocityBias)
            val oldNormalImpulse = point.normalImpulse
            val newNormalImpulse = (oldNormalImpulse + lambda).coerceAtLeast(0f)
            lambda = newNormalImpulse - oldNormalImpulse
            val normalP = manifold.normal * lambda
            manifold.bodyA.applyImpulse(-normalP, point.position)
            manifold.bodyB.applyImpulse(normalP, point.position)

            // ضربه اصطکاک
            val vt = relV dot tangent
            val frictionCoeff = manifold.friction
            var lambdaT = -point.tangentMass * vt
            val oldTangentImpulse = point.tangentImpulse
            val maxFriction = frictionCoeff * newNormalImpulse
            val newTangentImpulse = (oldTangentImpulse + lambdaT).coerceIn(-maxFriction, maxFriction)
            lambdaT = newTangentImpulse - oldTangentImpulse
            val tangentP = tangent * lambdaT
            manifold.bodyA.applyImpulse(-tangentP, point.position)
            manifold.bodyB.applyImpulse(tangentP, point.position)

            point.copy(
                normalImpulse = newNormalImpulse,
                tangentImpulse = newTangentImpulse
            )
        }
        val index = contactManifolds.indexOf(manifold)
        if (index != -1) {
            contactManifolds[index] = manifold.copy(points = updatedPoints)
        }
    }

    private fun solvePositionConstraints(manifold: ContactManifold) {
        manifold.points.forEach { point ->
            val invMA = if (manifold.bodyA.isStatic) 0f else 1f / manifold.bodyA.mass
            val invMB = if (manifold.bodyB.isStatic) 0f else 1f / manifold.bodyB.mass
            val invIA = if (manifold.bodyA.isStatic) 0f else 1f / manifold.bodyA.momentOfInertia
            val invIB = if (manifold.bodyB.isStatic) 0f else 1f / manifold.bodyB.momentOfInertia
            val rA = point.position - manifold.bodyA.position
            val rB = point.position - manifold.bodyB.position
            val rACrossN = rA cross manifold.normal
            val rBCrossN = rB cross manifold.normal
            val K = invMA + invMB + invIA * rACrossN * rACrossN + invIB * rBCrossN * rBCrossN

            // استفاده از عمق نفوذ ذخیره‌شده (بدون بازتولید منیفلد)
            val penetration = point.penetration
            if (penetration > 0.0001f) {
                val lambda = penetration * 0.9f / if (K > 0.0001f) K else 1f
                val p = manifold.normal * lambda
                if (!manifold.bodyA.isStatic) {
                    manifold.bodyA.position -= p * invMA
                    manifold.bodyA.angle -= (rA cross p) * invIA
                }
                if (!manifold.bodyB.isStatic) {
                    manifold.bodyB.position += p * invMB
                    manifold.bodyB.angle += (rB cross p) * invIB
                }
            }
        }
    }

    fun updateShapePosition(body: PhysicsBody): CollisionShape {
        return when (val shape = body.shape) {
            is CircleShape -> shape.copy(position = body.position)
            is RectShape -> shape.copy(position = body.position, rotation = body.angle)
            is PolygonShape -> {
                shape.copy(
                    position = body.position,
                    rotation = body.angle
                )
            }
            is LineShape -> {
                val localCenter = shape.position
                val rotatedStart = (shape.start - localCenter).rotate(body.angle) + localCenter + body.position
                val rotatedEnd = (shape.end - localCenter).rotate(body.angle) + localCenter + body.position
                shape.copy(start = rotatedStart, end = rotatedEnd)
            }
        }
    }
}