package com.gamehub.shared.graphics.physics

import com.gamehub.shared.graphics.math.*
import kotlin.math.*

data class RaycastResult(
    val body: PhysicsBody,
    val point: Vec2,
    val normal: Vec2,
    val fraction: Float
)

object RaycastSystem {
    fun raycast(
        engine: SimplePhysicsEngine,
        start: Vec2,
        end: Vec2,
        layerMask: Int = 0xFFFF
    ): RaycastResult? {
        var closest: RaycastResult? = null
        var minFrac = 1f
        engine.bodies.forEach { body ->
            if ((body.layer and layerMask) == 0) return@forEach
            val shape = engine.updateShapePosition(body)
            val hit = raycastShape(shape, start, end)
            if (hit != null && hit.fraction < minFrac) {
                minFrac = hit.fraction
                closest = RaycastResult(body, hit.point, hit.normal, hit.fraction)
            }
        }
        return closest
    }

    fun raycastAll(
        engine: SimplePhysicsEngine,
        start: Vec2,
        end: Vec2,
        layerMask: Int = 0xFFFF
    ): List<RaycastResult> {
        val results = mutableListOf<RaycastResult>()
        engine.bodies.forEach { body ->
            if ((body.layer and layerMask) == 0) return@forEach
            val shape = engine.updateShapePosition(body)
            val hit = raycastShape(shape, start, end)
            if (hit != null) {
                results.add(RaycastResult(body, hit.point, hit.normal, hit.fraction))
            }
        }
        return results.sortedBy { it.fraction }
    }

    private fun raycastShape(shape: CollisionShape, start: Vec2, end: Vec2): RaycastHit? {
        return when (shape) {
            is CircleShape -> raycastCircle(shape, start, end)
            is RectShape -> raycastRect(shape, start, end)
            is PolygonShape -> raycastPolygon(shape, start, end)
            is LineShape -> raycastLine(shape, start, end)
        }
    }

    private fun raycastCircle(circle: CircleShape, start: Vec2, end: Vec2): RaycastHit? {
        val s = start - circle.position
        val d = end - start
        val a = d dot d
        if (a == 0f) return null
        val b = 2f * (s dot d)
        val c = (s dot s) - circle.radius * circle.radius
        var disc = b * b - 4f * a * c
        if (disc < 0f) return null
        disc = sqrt(disc)
        val t1 = (-b - disc) / (2f * a)
        val t2 = (-b + disc) / (2f * a)
        val t = if (t1 in 0f..1f) t1 else if (t2 in 0f..1f) t2 else return null
        val point = start + d * t
        val normal = (point - circle.position).normalized()
        return RaycastHit(point, normal, t)
    }

    private fun raycastRect(rect: RectShape, start: Vec2, end: Vec2): RaycastHit? {
        // تبدیل به فضای محلی مستطیل (بدون چرخش)
        val invRot = -rect.rotation
        val cosR = cos(invRot)
        val sinR = sin(invRot)
        val localStart = Vec2(
            (start.x - rect.position.x) * cosR - (start.y - rect.position.y) * sinR,
            (start.x - rect.position.x) * sinR + (start.y - rect.position.y) * cosR
        )
        val localEnd = Vec2(
            (end.x - rect.position.x) * cosR - (end.y - rect.position.y) * sinR,
            (end.x - rect.position.x) * sinR + (end.y - rect.position.y) * cosR
        )
        val halfW = rect.size.width / 2f
        val halfH = rect.size.height / 2f
        val min = Vec2(-halfW, -halfH)
        val max = Vec2(halfW, halfH)

        // برخورد با AABB محلی
        var tMin = 0f
        var tMax = 1f
        val d = localEnd - localStart
        var normalLocal = Vec2.Zero

        for (i in 0..1) {
            val p = if (i == 0) d.x else d.y
            val q = if (i == 0) localStart.x - min.x else localStart.y - min.y
            val size = if (i == 0) rect.size.width else rect.size.height
            if (abs(p) < 1e-8f) {
                if (q < 0f || q > size) return null
            } else {
                val t1 = -q / p
                val t2 = (size - q) / p
                val tNear = kotlin.math.min(t1, t2)
                val tFar = kotlin.math.max(t1, t2)
                tMin = kotlin.math.max(tMin, tNear)
                tMax = kotlin.math.min(tMax, tFar)
                if (tMin > tMax) return null
            }
        }

        val t = tMin
        val localPoint = localStart + d * t
        // نرمال محلی
        val dx = localPoint.x
        val dy = localPoint.y
        if (abs(dx - (-halfW)) < 1e-8f) normalLocal = Vec2.Left
        else if (abs(dx - halfW) < 1e-8f) normalLocal = Vec2.Right
        else if (abs(dy - (-halfH)) < 1e-8f) normalLocal = Vec2.Up
        else if (abs(dy - halfH) < 1e-8f) normalLocal = Vec2.Down
        else normalLocal = Vec2.Zero // نباید رخ دهد

        // تبدیل نقطه و نرمال به فضای جهان
        val cosW = cos(rect.rotation)
        val sinW = sin(rect.rotation)
        val worldPoint = Vec2(
            localPoint.x * cosW - localPoint.y * sinW,
            localPoint.x * sinW + localPoint.y * cosW
        ) + rect.position
        val worldNormal = Vec2(
            normalLocal.x * cosW - normalLocal.y * sinW,
            normalLocal.x * sinW + normalLocal.y * cosW
        ).normalized()

        return RaycastHit(worldPoint, worldNormal, t)
    }

    private fun raycastPolygon(poly: PolygonShape, start: Vec2, end: Vec2): RaycastHit? {
        var best: RaycastHit? = null
        var bestFrac = 1f
        val polyVertices = poly.worldVertices
        for (i in polyVertices.indices) {
            val j = (i + 1) % polyVertices.size
            val hit = raycastLineSegment(start, end, polyVertices[i], polyVertices[j])
            if (hit != null && hit.fraction < bestFrac) {
                bestFrac = hit.fraction
                best = hit
            }
        }
        return best
    }

    private fun raycastLine(line: LineShape, start: Vec2, end: Vec2): RaycastHit? {
        return raycastLineSegment(start, end, line.start, line.end)
    }

    private fun raycastLineSegment(
        rayStart: Vec2,
        rayEnd: Vec2,
        segA: Vec2,
        segB: Vec2
    ): RaycastHit? {
        val d1 = rayEnd - rayStart
        val d2 = segB - segA
        val cross = d1 cross d2
        if (abs(cross) < 1e-8f) return null
        val s = ((segA - rayStart) cross d2) / cross
        val t = ((segA - rayStart) cross d1) / cross
        if (s < 0f || s > 1f || t < 0f || t > 1f) return null
        val point = rayStart + d1 * s
        val normal = Vec2(-d2.y, d2.x).normalized()
        return RaycastHit(point, normal, s)
    }

    private data class RaycastHit(val point: Vec2, val normal: Vec2, val fraction: Float)
}