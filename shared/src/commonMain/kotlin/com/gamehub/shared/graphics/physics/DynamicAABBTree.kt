package com.gamehub.shared.graphics.physics

import com.gamehub.shared.graphics.math.Rect
import com.gamehub.shared.graphics.math.Vec2
import kotlin.math.max
import kotlin.math.min

/**
 * درخت AABB پویا برای Broad-Phase (با قابلیت به‌روزرسانی افزایشی)
 * (با اصلاحات برای عملکرد بهتر)
 */
class DynamicAABBTree<T> {
    private class Node<T>(
        val data: T? = null,
        var aabb: Rect = Rect.Zero,
        var parent: Node<T>? = null,
        var left: Node<T>? = null,
        var right: Node<T>? = null,
        var height: Int = 0
    ) {
        val isLeaf: Boolean get() = left == null
    }

    private var root: Node<T>? = null
    private val dataToNode = mutableMapOf<T, Node<T>>()
    private val fatAABBMargin = 2f

    fun insert(data: T, aabb: Rect) {
        val fatAABB = expandAABB(aabb)
        val node = Node(data, fatAABB)
        dataToNode[data] = node
        insertLeaf(node)
    }

    fun remove(data: T) {
        val node = dataToNode.remove(data) ?: return
        removeLeaf(node)
    }

    fun update(data: T, newAABB: Rect): Boolean {
        val node = dataToNode[data] ?: return false
        val fatAABB = expandAABB(newAABB)
        if (node.aabb.contains(fatAABB)) {
            return false
        }
        removeLeaf(node)
        node.aabb = fatAABB
        insertLeaf(node)
        return true
    }

    fun retrieve(aabb: Rect): List<T> {
        val result = mutableListOf<T>()
        val stack = mutableListOf<Node<T>>()
        root?.let { stack.add(it) }
        while (stack.isNotEmpty()) {
            val node = stack.removeLast()
            if (node.aabb.intersects(aabb)) {
                if (node.isLeaf) {
                    node.data?.let { result.add(it) }
                } else {
                    node.left?.let { stack.add(it) }
                    node.right?.let { stack.add(it) }
                }
            }
        }
        return result
    }

    fun clear() {
        root = null
        dataToNode.clear()
    }

    private fun expandAABB(aabb: Rect): Rect {
        return Rect(
            aabb.left - fatAABBMargin,
            aabb.top - fatAABBMargin,
            aabb.right + fatAABBMargin,
            aabb.bottom + fatAABBMargin
        )
    }

    private fun insertLeaf(leaf: Node<T>) {
        if (root == null) {
            root = leaf
            return
        }

        var sibling = root!!
        while (!sibling.isLeaf) {
            val left = sibling.left!!
            val right = sibling.right!!
            val costLeft = 2f * combinedAABB(left.aabb, leaf.aabb).area + left.aabb.area
            val costRight = 2f * combinedAABB(right.aabb, leaf.aabb).area + right.aabb.area
            sibling = if (costLeft < costRight) left else right
        }

        val oldParent = sibling.parent
        val newParent = Node<T>(
            parent = oldParent,
            left = sibling,
            right = leaf,
            aabb = combinedAABB(sibling.aabb, leaf.aabb)
        )
        sibling.parent = newParent
        leaf.parent = newParent

        if (oldParent != null) {
            if (oldParent.left === sibling) {
                oldParent.left = newParent
            } else {
                oldParent.right = newParent
            }
        } else {
            root = newParent
        }

        var n = newParent.parent
        while (n != null) {
            n = balance(n)
            n.height = 1 + max(n.left?.height ?: 0, n.right?.height ?: 0)
            n.aabb = combinedAABB(n.left!!.aabb, n.right!!.aabb)
            n = n.parent
        }
    }

    private fun removeLeaf(leaf: Node<T>) {
        if (leaf === root) {
            root = null
            return
        }

        val parent = leaf.parent!!
        val grandparent = parent.parent
        val sibling = if (parent.left === leaf) parent.right!! else parent.left!!

        if (grandparent != null) {
            if (grandparent.left === parent) {
                grandparent.left = sibling
            } else {
                grandparent.right = sibling
            }
            sibling.parent = grandparent
            var n = grandparent
            while (n != null) {
                n = balance(n)
                n.height = 1 + max(n.left?.height ?: 0, n.right?.height ?: 0)
                n.aabb = combinedAABB(n.left!!.aabb, n.right!!.aabb)
                n = n.parent
            }
        } else {
            root = sibling
            sibling.parent = null
        }
    }

    private fun balance(node: Node<T>): Node<T> {
        val left = node.left ?: return node
        val right = node.right ?: return node
        val balanceVal = right.height - left.height

        if (balanceVal > 1) {
            val rightLeft = right.left
            val rightRight = right.right
            if (rightLeft != null && rightRight != null && rightLeft.height > rightRight.height) {
                right.left = rightLeft.right
                rightLeft.right?.parent = right
                rightLeft.right = right
                right.parent = rightLeft
                val oldRightParent = right.parent?.parent
                rightLeft.parent = oldRightParent
                if (oldRightParent != null) {
                    if (oldRightParent.left === right) oldRightParent.left = rightLeft else oldRightParent.right = rightLeft
                } else {
                    root = rightLeft
                }
                right.height = 1 + max(right.left?.height ?: 0, rightRight.height)
                right.aabb = combinedAABB(right.left?.aabb ?: rightRight.aabb, rightRight.aabb)
                rightLeft.height = 1 + max(rightLeft.left?.height ?: 0, right.height)
                rightLeft.aabb = combinedAABB(rightLeft.left?.aabb ?: right.aabb, right.aabb)
            }
            node.right = right.left
            right.left?.parent = node
            right.left = node
            val oldParent = node.parent
            node.parent = right
            right.parent = oldParent
            if (oldParent != null) {
                if (oldParent.left === node) oldParent.left = right else oldParent.right = right
            } else {
                root = right
            }
            node.height = 1 + max(left.height, (node.right?.height ?: 0))
            node.aabb = combinedAABB(left.aabb, (node.right?.aabb ?: left.aabb))
            right.height = 1 + max(node.height, (right.right?.height ?: 0))
            right.aabb = combinedAABB(node.aabb, (right.right?.aabb ?: node.aabb))
            return right
        }

        if (balanceVal < -1) {
            val leftLeft = left.left
            val leftRight = left.right
            if (leftRight != null && leftLeft != null && leftRight.height > leftLeft.height) {
                left.right = leftRight.left
                leftRight.left?.parent = left
                leftRight.left = left
                left.parent = leftRight
                val oldLeftParent = left.parent?.parent
                leftRight.parent = oldLeftParent
                if (oldLeftParent != null) {
                    if (oldLeftParent.left === left) oldLeftParent.left = leftRight else oldLeftParent.right = leftRight
                } else {
                    root = leftRight
                }
                left.height = 1 + max(leftLeft.height, (left.right?.height ?: 0))
                left.aabb = combinedAABB(leftLeft.aabb, (left.right?.aabb ?: leftLeft.aabb))
                leftRight.height = 1 + max((leftRight.right?.height ?: 0), left.height)
                leftRight.aabb = combinedAABB((leftRight.right?.aabb ?: left.aabb), left.aabb)
            }
            node.left = left.right
            left.right?.parent = node
            left.right = node
            val oldParent = node.parent
            node.parent = left
            left.parent = oldParent
            if (oldParent != null) {
                if (oldParent.left === node) oldParent.left = left else oldParent.right = left
            } else {
                root = left
            }
            node.height = 1 + max(right.height, (node.left?.height ?: 0))
            node.aabb = combinedAABB(right.aabb, (node.left?.aabb ?: right.aabb))
            left.height = 1 + max((left.left?.height ?: 0), node.height)
            left.aabb = combinedAABB((left.left?.aabb ?: node.aabb), node.aabb)
            return left
        }
        return node
    }

    private fun combinedAABB(a: Rect, b: Rect): Rect {
        return Rect(
            min(a.left, b.left),
            min(a.top, b.top),
            max(a.right, b.right),
            max(a.bottom, b.bottom)
        )
    }
}