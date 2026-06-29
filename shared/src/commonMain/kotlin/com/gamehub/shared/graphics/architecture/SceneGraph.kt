package com.gamehub.shared.graphics.architecture

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import kotlin.math.*

/**
 * 2D Transform Component for Scene Nodes
 */
data class Transform2D(
    var position: Offset = Offset.Zero,
    var rotation: Float = 0f,
    var scale: Offset = Offset(1f, 1f)
) {
    // Local Matrix (identity by default)
    val localMatrix: FloatArray
        get() = calculateMatrix(position, rotation, scale)

    // Helper to calculate 2D transform matrix
    private fun calculateMatrix(
        pos: Offset, rot: Float, scl: Offset
    ): FloatArray {
        val cos = cos(rot.toRadians())
        val sin = sin(rot.toRadians())
        return floatArrayOf(
            scl.x * cos, -scl.y * sin, 0f,
            scl.x * sin, scl.y * cos, 0f,
            pos.x, pos.y, 1f
        )
    }

    private fun Float.toRadians(): Float = this * PI.toFloat() / 180f
}

/**
 * Scene Graph Node
 */
open class SceneNode(
    val name: String,
    val transform: Transform2D = Transform2D()
) {
    val children = mutableListOf<SceneNode>()
    var parent: SceneNode? = null

    // Cached World Matrix
    var worldMatrix: FloatArray = FloatArray(9)
        private set

    // Computed World Transform
    val worldPosition: Offset
        get() = Offset(worldMatrix[6], worldMatrix[7])

    /**
     * Add Child
     */
    fun addChild(child: SceneNode) {
        child.parent?.removeChild(child)
        children.add(child)
        child.parent = this
    }

    /**
     * Remove Child
     */
    fun removeChild(child: SceneNode) {
        if (children.remove(child)) {
            child.parent = null
        }
    }

    /**
     * Update World Transforms (traverse from root)
     */
    fun updateWorldTransform(parentMatrix: FloatArray = FloatArray(9).also { it[0] = 1f; it[4] = 1f; it[8] = 1f }) {
        worldMatrix = multiplyMatrices(parentMatrix, transform.localMatrix)
        children.forEach { it.updateWorldTransform(worldMatrix) }
    }

    /**
     * Multiply two 3x3 matrices for 2D
     */
    private fun multiplyMatrices(a: FloatArray, b: FloatArray): FloatArray {
        return FloatArray(9).apply {
            this[0] = a[0] * b[0] + a[1] * b[3] + a[2] * b[6]
            this[1] = a[0] * b[1] + a[1] * b[4] + a[2] * b[7]
            this[2] = a[0] * b[2] + a[1] * b[5] + a[2] * b[8]
            this[3] = a[3] * b[0] + a[4] * b[3] + a[5] * b[6]
            this[4] = a[3] * b[1] + a[4] * b[4] + a[5] * b[7]
            this[5] = a[3] * b[2] + a[4] * b[5] + a[5] * b[8]
            this[6] = a[6] * b[0] + a[7] * b[3] + a[8] * b[6]
            this[7] = a[6] * b[1] + a[7] * b[4] + a[8] * b[7]
            this[8] = a[6] * b[2] + a[7] * b[5] + a[8] * b[8]
        }
    }

    /**
     * Draw this node and children
     */
    open fun draw(drawScope: DrawScope) {
        drawScope.withTransform({
            translate(left = transform.position.x, top = transform.position.y)
            rotate(degrees = transform.rotation)
            scale(scaleX = transform.scale.x, scaleY = transform.scale.y)
        }) {
            onDraw(this)
            children.forEach { it.draw(this) }
        }
    }

    /**
     * Override to draw node-specific content
     */
    protected open fun onDraw(drawScope: DrawScope) {}
}

/**
 * Root Scene Node
 */
class SceneGraph(name: String = "Root") : SceneNode(name)
