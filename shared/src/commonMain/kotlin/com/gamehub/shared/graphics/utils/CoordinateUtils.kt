package com.gamehub.shared.graphics.utils

import androidx.compose.ui.geometry.Offset
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

object CoordinateUtils {
    data class HexCoord(val q: Int, val r: Int) {
        val s: Int = -q - r
    }
    
    data class OffsetCoord(val col: Int, val row: Int)
    
    fun hexToPixel(coord: HexCoord, size: Float): Offset {
        val x = size * (3.0 / 2 * coord.q)
        val y = size * (sqrt(3.0) / 2 * coord.q + sqrt(3.0) * coord.r)
        return Offset(x.toFloat(), y.toFloat())
    }
    
    fun pixelToHex(point: Offset, size: Float): HexCoord {
        val q = (2.0 / 3 * point.x) / size
        val r = (-1.0 / 3 * point.x + sqrt(3.0) / 3 * point.y) / size
        return hexRound(q, r)
    }
    
    fun hexRound(q: Double, r: Double): HexCoord {
        val s = -q - r
        var rq = q.roundToInt()
        var rr = r.roundToInt()
        var rs = s.roundToInt()
        
        val qDiff = kotlin.math.abs(rq - q)
        val rDiff = kotlin.math.abs(rr - r)
        val sDiff = kotlin.math.abs(rs - s)
        
        if (qDiff > rDiff && qDiff > sDiff) {
            rq = -rr - rs
        } else if (rDiff > sDiff) {
            rr = -rq - rs
        }
        
        return HexCoord(rq, rr)
    }
    
    fun hexNeighbors(coord: HexCoord): List<HexCoord> {
        val directions = listOf(
            HexCoord(1, 0), HexCoord(1, -1), HexCoord(0, -1),
            HexCoord(-1, 0), HexCoord(-1, 1), HexCoord(0, 1)
        )
        return directions.map { HexCoord(coord.q + it.q, coord.r + it.r) }
    }
    
    fun hexDistance(a: HexCoord, b: HexCoord): Int {
        return (kotlin.math.abs(a.q - b.q) + kotlin.math.abs(a.r - b.r) + kotlin.math.abs(a.s - b.s)) / 2
    }
    
    data class IsometricCoord(val x: Int, val y: Int)
    
    fun isometricToPixel(coord: IsometricCoord, tileWidth: Float, tileHeight: Float): Offset {
        val x = (coord.x - coord.y) * (tileWidth / 2f)
        val y = (coord.x + coord.y) * (tileHeight / 2f)
        return Offset(x, y)
    }
    
    fun pixelToIsometric(point: Offset, tileWidth: Float, tileHeight: Float): IsometricCoord {
        val x = (point.x / (tileWidth / 2f) + point.y / (tileHeight / 2f)) / 2f
        val y = (point.y / (tileHeight / 2f) - point.x / (tileWidth / 2f)) / 2f
        return IsometricCoord(x.roundToInt(), y.roundToInt())
    }
}
