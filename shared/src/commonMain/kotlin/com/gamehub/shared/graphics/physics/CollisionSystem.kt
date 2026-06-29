package com.gamehub.shared.graphics.physics

import com.gamehub.shared.graphics.math.*
import kotlin.math.*

sealed class CollisionShape {
    abstract val position: Vec2
}

data class CircleShape(
    override val position: Vec2,
    val radius: Float
) : CollisionShape()

data class RectShape(
    override val position: Vec2,
    val size: Size,
    val rotation: Float = 0f
) : CollisionShape() {
    val left: Float get() = position.x - size.width / 2
    val top: Float get() = position.y - size.height / 2
    val right: Float get() = position.x + size.width / 2
    val bottom: Float get() = position.y + size.height / 2
    val rect: Rect get() = Rect(left, top, right, bottom)

    fun getLocalVertices(): List<Vec2> {
        val halfW = size.width / 2
        val halfH = size.height / 2
        return listOf(
            Vec2(-halfW, -halfH),
            Vec2(halfW, -halfH),
            Vec2(halfW, halfH),
            Vec2(-halfW, halfH)
        )
    }

    fun getVertices(): List<Vec2> {
        val cos = cos(rotation)
        val sin = sin(rotation)
        return getLocalVertices().map { v ->
            val rx = v.x * cos - v.y * sin
            val ry = v.x * sin + v.y * cos
            Vec2(rx + position.x, ry + position.y)
        }
    }
}

data class LineShape(
    val start: Vec2,
    val end: Vec2
) : CollisionShape() {
    override val position: Vec2 get() = (start + end) / 2f
}

data class PolygonShape(
    val vertices: List<Vec2>,
    override val position: Vec2 = Vec2.Zero,
    val rotation: Float = 0f
) : CollisionShape() {
    val worldVertices: List<Vec2>
        get() {
            val cos = cos(rotation)
            val sin = sin(rotation)
            return vertices.map { v ->
                Vec2(
                    v.x * cos - v.y * sin,
                    v.x * sin + v.y * cos
                ) + position
            }
        }

    init {
        require(vertices.size >= 3) { "چندضلعی باید حداقل ۳ رأس داشته باشد" }
        for (i in vertices.indices) {
            val j = (i + 1) % vertices.size
            val edge = vertices[j] - vertices[i]
            require(edge.lengthSquared() > 1e-8f) { "ضلع صفر در چندضلعی مجاز نیست" }
        }
    }
}

data class CollisionResult(
    val hasCollided: Boolean,
    val normal: Vec2 = Vec2.Zero,
    val depth: Float = 0f,
    val contactPoints: List<Vec2> = emptyList()
)

object CollisionSystem {

    // ========== برخورد دایره-دایره ==========
    fun checkCollision(a: CircleShape, b: CircleShape): CollisionResult {
        val delta = b.position - a.position
        val distSq = delta.lengthSquared()
        val sumRadii = a.radius + b.radius
        if (distSq >= sumRadii * sumRadii) return CollisionResult(false)
        val dist = sqrt(distSq)
        val normal = if (dist > 0f) delta / dist else Vec2.Right
        val depth = sumRadii - dist
        val contact = a.position + normal * (a.radius - depth / 2f)
        return CollisionResult(true, normal, depth, listOf(contact))
    }

    // ========== برخورد چندضلعی-چندضلعی با GJK + EPA ==========
    fun checkCollision(a: PolygonShape, b: PolygonShape): CollisionResult {
        val gjkResult = gjkIntersection(a, b)
        if (!gjkResult.intersects) {
            return CollisionResult(false)
        }

        val epaResult = epaPenetration(a, b, gjkResult.simplex)
        if (epaResult == null) {
            return satCollision(a, b)
        }

        val contactPoints = findContactPoints(a, b, epaResult.normal)

        return CollisionResult(
            hasCollided = true,
            normal = epaResult.normal,
            depth = epaResult.depth,
            contactPoints = contactPoints
        )
    }

    // ========== برخورد چندضلعی-دایره ==========
    fun checkCollision(poly: PolygonShape, circle: CircleShape): CollisionResult {
        val circlePoly = approximateCircle(circle.position, circle.radius)
        return checkCollision(poly, circlePoly)
    }

    // ========== برخورد مستطیل-دایره ==========
    fun checkCollision(rect: RectShape, circle: CircleShape): CollisionResult {
        val polyRect = PolygonShape(rect.getLocalVertices(), rect.position, rect.rotation)
        return checkCollision(polyRect, circle)
    }

    // ========== برخورد مستطیل-مستطیل ==========
    fun checkCollision(a: RectShape, b: RectShape): CollisionResult {
        val polyA = PolygonShape(a.getLocalVertices(), a.position, a.rotation)
        val polyB = PolygonShape(b.getLocalVertices(), b.position, b.rotation)
        return checkCollision(polyA, polyB)
    }

    // ========== برخورد دایره-مستطیل ==========
    fun checkCollision(circle: CircleShape, rect: RectShape): CollisionResult {
        val polyRect = PolygonShape(rect.getLocalVertices(), rect.position, rect.rotation)
        return checkCollision(polyRect, circle)
    }

    // ========== برخورد چندضلعی-مستطیل ==========
    fun checkCollision(poly: PolygonShape, rect: RectShape): CollisionResult {
        val polyRect = PolygonShape(rect.getLocalVertices(), rect.position, rect.rotation)
        return checkCollision(poly, polyRect)
    }

    // ==================== GJK ====================
    private data class GJKResult(val intersects: Boolean, val simplex: MutableList<Vec2>)

    private fun gjkIntersection(a: PolygonShape, b: PolygonShape): GJKResult {
        val simplex = mutableListOf<Vec2>()
        var direction = a.position - b.position
        if (direction.lengthSquared() < 1e-8f) {
            direction = Vec2.Right
        }
        var support = getSupportPoint(a, b, direction)
        simplex.add(support)
        direction = -direction

        var maxIterations = 100
        while (maxIterations-- > 0) {
            support = getSupportPoint(a, b, direction)
            if (support dot direction <= 0f) {
                return GJKResult(false, simplex)
            }
            simplex.add(support)
            if (simplex.size > 3) {
                simplex.removeAt(0)
            }
            val nextDir = getNextDirection(simplex)
            if (nextDir == null) {
                return GJKResult(true, simplex)
            }
            direction = nextDir
        }
        return GJKResult(true, simplex)
    }

    private fun getSupportPoint(a: PolygonShape, b: PolygonShape, direction: Vec2): Vec2 {
        val aWorld = a.worldVertices
        val bWorld = b.worldVertices
        val maxA = aWorld.maxByOrNull { it dot direction } ?: aWorld[0]
        val maxB = bWorld.maxByOrNull { -(it dot direction) } ?: bWorld[0]
        return maxA - maxB
    }

    private fun getNextDirection(simplex: MutableList<Vec2>): Vec2? {
        when (simplex.size) {
            1 -> {
                return -simplex[0]
            }
            2 -> {
                val a = simplex[0]
                val b = simplex[1]
                val ab = b - a
                val ao = -a
                val perp = ab.perpendicular()
                val dir = if (perp dot ao > 0f) perp else -perp
                return dir
            }
            3 -> {
                val a = simplex[0]
                val b = simplex[1]
                val c = simplex[2]
                val ab = b - a
                val ac = c - a
                val ao = -a
                val abc = ab cross ac
                if (abc < 0f) {
                    simplex[1] = c
                    simplex[2] = b
                    return getNextDirection(simplex)
                }
                val acPerp = ac.perpendicular()
                val abPerp = ab.perpendicular()
                if (acPerp dot ao > 0f) {
                    simplex.removeAt(1)
                    return acPerp
                }
                if (abPerp dot ao > 0f) {
                    simplex.removeAt(2)
                    return abPerp
                }
                return null
            }
            else -> return null
        }
    }

    // ==================== EPA ====================
    private data class EPAResult(val normal: Vec2, val depth: Float)

    private fun epaPenetration(a: PolygonShape, b: PolygonShape, simplex: MutableList<Vec2>): EPAResult? {
        var polytope = simplex.toMutableList()
        var maxIterations = 100
        while (maxIterations-- > 0) {
            var edgeIndex = -1
            var minDist = Float.MAX_VALUE
            for (i in polytope.indices) {
                val j = (i + 1) % polytope.size
                val a_ = polytope[i]
                val b_ = polytope[j]
                val edge = b_ - a_
                val normal = edge.perpendicular().normalized()
                val dist = normal dot a_
                if (dist < minDist) {
                    minDist = dist
                    edgeIndex = i
                }
            }
            if (edgeIndex == -1) return null

            val i = edgeIndex
            val j = (i + 1) % polytope.size
            val edgeNormal = (polytope[j] - polytope[i]).perpendicular().normalized()
            val support = getSupportPoint(a, b, edgeNormal)
            val dist = support dot edgeNormal
            if (abs(dist - minDist) < 1e-6f) {
                return EPAResult(edgeNormal, dist)
            }
            polytope.add(i + 1, support)
            if (polytope.size > 100) break
        }
        return null
    }

    // ==================== SAT (Fallback) ====================
    private fun satCollision(a: PolygonShape, b: PolygonShape): CollisionResult {
        val axes = getAxes(a.worldVertices) + getAxes(b.worldVertices)
        var minDepth = Float.MAX_VALUE
        var minNormal = Vec2.Zero

        for (axis in axes) {
            val (aMin, aMax) = project(a.worldVertices, axis)
            val (bMin, bMax) = project(b.worldVertices, axis)
            val overlap = min(aMax, bMax) - max(aMin, bMin)
            if (overlap <= 0f) return CollisionResult(false)
            if (overlap < minDepth) {
                minDepth = overlap
                minNormal = axis
            }
        }

        val delta = b.position - a.position
        if ((delta dot minNormal) < 0f) {
            minNormal = -minNormal
        }

        val contactPoints = findContactPoints(a, b, minNormal)
        return CollisionResult(true, minNormal, minDepth, contactPoints)
    }

    // ==================== پیدا کردن نقاط تماس ====================
    private data class FaceSelection(
        val refPoly: PolygonShape,
        val incPoly: PolygonShape,
        val refFace: Int,
        val flip: Boolean
    )

    private fun findContactPoints(a: PolygonShape, b: PolygonShape, normal: Vec2): List<Vec2> {
        val refFaceA = findIncidentFace(a, normal)
        val refFaceB = findIncidentFace(b, -normal)
        val projA = projectFace(a.worldVertices, refFaceA, normal)
        val projB = projectFace(b.worldVertices, refFaceB, -normal)

        val selection = if (projA.second <= projB.second) {
            FaceSelection(a, b, refFaceA, false)
        } else {
            FaceSelection(b, a, refFaceB, true)
        }

        val refPoly = selection.refPoly
        val incPoly = selection.incPoly
        val refFace = selection.refFace
        val flip = selection.flip

        val refVerts = refPoly.worldVertices
        val incVerts = incPoly.worldVertices
        val v0 = refVerts[refFace]
        val v1 = refVerts[(refFace + 1) % refVerts.size]
        val refEdge = v1 - v0
        val refNormal = if (flip) -refEdge.perpendicular().normalized() else refEdge.perpendicular().normalized()

        val incFace = findIncidentFace(incPoly, if (flip) -normal else normal)
        var clippedVerts = listOf(
            incVerts[incFace],
            incVerts[(incFace + 1) % incVerts.size]
        )

        val sideNormal1 = refEdge.perpendicular()
        clippedVerts = clipSegmentToLine(clippedVerts, sideNormal1, sideNormal1 dot v0)
        if (clippedVerts.size < 2) return emptyList()
        val sideNormal2 = -sideNormal1
        clippedVerts = clipSegmentToLine(clippedVerts, sideNormal2, -(sideNormal2 dot v1))
        if (clippedVerts.size < 2) return emptyList()

        val refPlaneNormal = if (flip) -refNormal else refNormal
        val refPlaneDist = refPlaneNormal dot v0
        val contactPoints = mutableListOf<Vec2>()
        for (vert in clippedVerts) {
            if ((refPlaneNormal dot vert) - refPlaneDist <= 1e-6f) {
                contactPoints.add(vert)
            }
        }
        return contactPoints
    }

    // ==================== توابع کمکی ====================
    private fun getAxes(vertices: List<Vec2>): List<Vec2> {
        val axes = mutableListOf<Vec2>()
        for (i in vertices.indices) {
            val j = (i + 1) % vertices.size
            val edge = vertices[j] - vertices[i]
            val normal = edge.perpendicular().normalized()
            axes.add(normal)
        }
        return axes
    }

    private fun project(vertices: List<Vec2>, axis: Vec2): Pair<Float, Float> {
        var min = vertices[0] dot axis
        var max = min
        for (i in 1 until vertices.size) {
            val proj = vertices[i] dot axis
            if (proj < min) min = proj
            if (proj > max) max = proj
        }
        return min to max
    }

    private fun findIncidentFace(poly: PolygonShape, n: Vec2): Int {
        var maxDot = -Float.MAX_VALUE
        var face = 0
        val verts = poly.worldVertices
        for (i in verts.indices) {
            val edge = verts[(i + 1) % verts.size] - verts[i]
            val edgeNormal = edge.perpendicular().normalized()
            val dot = edgeNormal dot n
            if (dot > maxDot) {
                maxDot = dot
                face = i
            }
        }
        return face
    }

    private fun projectFace(verts: List<Vec2>, face: Int, n: Vec2): Pair<Float, Float> {
        val v0 = verts[face]
        val v1 = verts[(face + 1) % verts.size]
        return (v0 dot n) to (v1 dot n)
    }

    private fun clipSegmentToLine(verts: List<Vec2>, normal: Vec2, offset: Float): List<Vec2> {
        val clipped = mutableListOf<Vec2>()
        val d0 = (normal dot verts[0]) - offset
        val d1 = (normal dot verts[1]) - offset
        if (d0 <= 0f) clipped.add(verts[0])
        if (d1 <= 0f) clipped.add(verts[1])
        if (d0 * d1 < 0f) {
            val t = d0 / (d0 - d1)
            val v = verts[0] + (verts[1] - verts[0]) * t
            clipped.add(v)
        }
        return clipped
    }

    private fun approximateCircle(center: Vec2, radius: Float, segments: Int = 8): PolygonShape {
        val verts = mutableListOf<Vec2>()
        for (i in 0 until segments) {
            val angle = (2f * PI / segments * i).toFloat()
            verts.add(Vec2(center.x + radius * cos(angle), center.y + radius * sin(angle)))
        }
        return PolygonShape(verts, center, 0f)
    }

    // ========== CCD ==========
    fun circleCircleCCD(
        aStart: CircleShape, bStart: CircleShape,
        aDelta: Vec2, bDelta: Vec2,
        maxTime: Float = 1f
    ): Pair<Boolean, Float> {
        val relativeVel = aDelta - bDelta
        val deltaPosStart = bStart.position - aStart.position
        val sumRadii = aStart.radius + bStart.radius

        if (deltaPosStart.lengthSquared() < sumRadii * sumRadii) {
            return Pair(true, 0f)
        }

        val aCoeff = relativeVel.lengthSquared()
        if (aCoeff < 1e-8f) {
            return Pair(false, maxTime)
        }
        val bCoeff = 2f * (deltaPosStart dot relativeVel)
        val cCoeff = deltaPosStart.lengthSquared() - sumRadii * sumRadii
        var discriminant = bCoeff * bCoeff - 4f * aCoeff * cCoeff
        if (discriminant < 0f) return Pair(false, maxTime)
        discriminant = sqrt(discriminant)
        val t1 = (-bCoeff - discriminant) / (2f * aCoeff)
        val t2 = (-bCoeff + discriminant) / (2f * aCoeff)
        var t = maxTime
        if (t1 in 0f..maxTime) t = t1
        else if (t2 in 0f..maxTime) t = t2
        return Pair(t in 0f..maxTime, t)
    }

    fun circleRectCCD(
        circleStart: CircleShape, rect: RectShape,
        circleDelta: Vec2, maxTime: Float = 1f
    ): Pair<Boolean, Float> {
        if (rect.rotation != 0f) {
            val rectPoly = PolygonShape(rect.getLocalVertices(), rect.position, rect.rotation)
            return polygonCircleCCD(rectPoly, circleStart, Vec2.Zero, circleDelta, maxTime)
        }

        val r = circleStart.radius
        val extents = Vec2(rect.size.width / 2f, rect.size.height / 2f)
        val min = rect.position - extents
        val max = rect.position + extents
        val center = circleStart.position

        if (center.x >= min.x - r && center.x <= max.x + r &&
            center.y >= min.y - r && center.y <= max.y + r) {
            return Pair(true, 0f)
        }

        var tMin = 0f
        var tMax = maxTime
        val vx = circleDelta.x
        val vy = circleDelta.y

        if (abs(vx) < 1e-8f) {
            if (center.x < min.x - r || center.x > max.x + r) return Pair(false, maxTime)
        } else {
            val t1 = (min.x - r - center.x) / vx
            val t2 = (max.x + r - center.x) / vx
            val tEnter = minOf(t1, t2)
            val tExit = maxOf(t1, t2)
            tMin = maxOf(tMin, tEnter)
            tMax = minOf(tMax, tExit)
            if (tMin > tMax) return Pair(false, maxTime)
        }

        if (abs(vy) < 1e-8f) {
            if (center.y < min.y - r || center.y > max.y + r) return Pair(false, maxTime)
        } else {
            val t1 = (min.y - r - center.y) / vy
            val t2 = (max.y + r - center.y) / vy
            val tEnter = minOf(t1, t2)
            val tExit = maxOf(t1, t2)
            tMin = maxOf(tMin, tEnter)
            tMax = minOf(tMax, tExit)
            if (tMin > tMax) return Pair(false, maxTime)
        }
        return Pair(true, tMin)
    }

    fun polygonCircleCCD(
        poly: PolygonShape, circleStart: CircleShape,
        polyDelta: Vec2, circleDelta: Vec2,
        maxTime: Float = 1f
    ): Pair<Boolean, Float> {
        val steps = 8
        val dt = maxTime / steps
        var currentPolyPos = poly.position
        var currentCirclePos = circleStart.position
        for (i in 0 until steps) {
            val t = i * dt
            val polyPos = poly.position + polyDelta * t
            val circlePos = circleStart.position + circleDelta * t
            val polyAtT = PolygonShape(poly.vertices, polyPos, poly.rotation)
            val circleAtT = CircleShape(circlePos, circleStart.radius)
            if (checkCollision(polyAtT, circleAtT).hasCollided) {
                return Pair(true, t)
            }
        }
        return Pair(false, maxTime)
    }

    // ========== تست نقطه درون شکل ==========
    fun isPointInside(point: Vec2, shape: CollisionShape): Boolean {
        return when (shape) {
            is CircleShape -> (point - shape.position).length() <= shape.radius
            is RectShape -> {
                val local = point - shape.position
                val cos = cos(-shape.rotation)
                val sin = sin(-shape.rotation)
                val lx = local.x * cos - local.y * sin
                val ly = local.x * sin + local.y * cos
                abs(lx) <= shape.size.width / 2 && abs(ly) <= shape.size.height / 2
            }
            is PolygonShape -> isPointInPolygon(point, shape.worldVertices)
            is LineShape -> false
        }
    }

    private fun isPointInPolygon(point: Vec2, vertices: List<Vec2>): Boolean {
        var inside = false
        for (i in vertices.indices) {
            val j = (i + 1) % vertices.size
            val a = vertices[i]
            val b = vertices[j]
            if (((a.y > point.y) != (b.y > point.y)) &&
                (point.x < (b.x - a.x) * (point.y - a.y) / (b.y - a.y) + a.x)) {
                inside = !inside
            }
        }
        return inside
    }
}