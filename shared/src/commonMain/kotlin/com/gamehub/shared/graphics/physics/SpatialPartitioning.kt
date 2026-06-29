package com.gamehub.shared.graphics.physics

import com.gamehub.shared.graphics.math.*

/**
 * درخت چهارتایی (QuadTree) برای تقسیم فضایی
 */
class QuadTree<T>(
    val bounds: Rect,
    val maxObjects: Int = 4,
    val maxLevels: Int = 5,
    val level: Int = 0
) {
    private val objects = mutableListOf<Pair<T, Rect>>()
    private var nodes: MutableList<QuadTree<T>>? = null
    private var hasSplit = false

    fun clear() {
        objects.clear()
        hasSplit = false
        nodes?.forEach { it.clear() }
        nodes = null
    }

    private fun split() {
        val subW = bounds.width / 2f
        val subH = bounds.height / 2f
        val x = bounds.left
        val y = bounds.top
        nodes = mutableListOf(
            QuadTree(Rect(x + subW, y, subW, subH), maxObjects, maxLevels, level + 1),
            QuadTree(Rect(x, y, subW, subH), maxObjects, maxLevels, level + 1),
            QuadTree(Rect(x, y + subH, subW, subH), maxObjects, maxLevels, level + 1),
            QuadTree(Rect(x + subW, y + subH, subW, subH), maxObjects, maxLevels, level + 1)
        )
        hasSplit = true
    }

    private fun getIndices(rect: Rect): List<Int> {
        val indices = mutableListOf<Int>()
        val midX = bounds.left + bounds.width / 2
        val midY = bounds.top + bounds.height / 2
        val top = rect.top < midY
        val bottom = rect.bottom > midY
        val left = rect.left < midX
        val right = rect.right > midX
        if (top && left) indices.add(1)
        if (top && right) indices.add(0)
        if (bottom && left) indices.add(2)
        if (bottom && right) indices.add(3)
        return indices
    }

    fun insert(obj: T, rect: Rect) {
        if (hasSplit) {
            getIndices(rect).forEach { nodes!![it].insert(obj, rect) }
            return
        }
        objects.add(obj to rect)
        if (objects.size > maxObjects && level < maxLevels) {
            if (!hasSplit) split()
            val toReinsert = objects.toList()
            objects.clear()
            toReinsert.forEach { (o, r) -> insert(o, r) }
        }
    }

    fun retrieve(obj: T, rect: Rect): List<T> {
        val possible = mutableListOf<T>()
        if (hasSplit) {
            getIndices(rect).forEach { index ->
                possible.addAll(nodes!![index].retrieve(obj, rect))
            }
        }
        objects.filter { it.first != obj }.mapTo(possible) { it.first }
        return possible
    }
}

/**
 * جدول درهم‌سازی مکانی (Spatial Hash Grid) – به‌عنوان جایگزین
 */
class SpatialHashGrid<T>(
    val cellSize: Float = 50f
) {
    private val grid = mutableMapOf<Long, MutableList<T>>()

    private fun key(x: Int, y: Int): Long =
        (x.toLong() shl 32) or (y.toLong() and 0xFFFFFFFFL)

    fun insert(obj: T, bounds: Rect) {
        val minX = (bounds.left / cellSize).toInt()
        val maxX = (bounds.right / cellSize).toInt()
        val minY = (bounds.top / cellSize).toInt()
        val maxY = (bounds.bottom / cellSize).toInt()
        for (x in minX..maxX) {
            for (y in minY..maxY) {
                grid.getOrPut(key(x, y)) { mutableListOf() }.add(obj)
            }
        }
    }

    fun retrieve(bounds: Rect): List<T> {
        val results = mutableSetOf<T>()
        val minX = (bounds.left / cellSize).toInt()
        val maxX = (bounds.right / cellSize).toInt()
        val minY = (bounds.top / cellSize).toInt()
        val maxY = (bounds.bottom / cellSize).toInt()
        for (x in minX..maxX) {
            for (y in minY..maxY) {
                grid[key(x, y)]?.let { results.addAll(it) }
            }
        }
        return results.toList()
    }

    fun clear() {
        grid.clear()
    }
}