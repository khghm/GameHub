package com.gamehub.shared.graphics.layout

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.gamehub.shared.graphics.utils.CoordinateUtils
import kotlin.math.max

@Composable
fun GameGrid(
    rows: Int,
    columns: Int,
    modifier: Modifier = Modifier,
    spacing: Dp = 4.dp,
    content: @Composable (row: Int, column: Int) -> Unit
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(spacing)
    ) {
        for (row in 0 until rows) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing)
            ) {
                for (column in 0 until columns) {
                    content(row, column)
                }
            }
        }
    }
}

@Composable
fun CenteredBox(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
        content = content
    )
}

@Composable
fun AspectRatioBox(
    aspectRatio: Float,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    BoxWithConstraints(modifier = modifier) {
        val width = maxWidth
        val height = (width.value * aspectRatio).dp
        
        Box(
            modifier = Modifier.size(width, height),
            contentAlignment = Alignment.Center,
            content = content
        )
    }
}

@Composable
fun HexGrid(
    radius: Int,
    hexSize: Float,
    modifier: Modifier = Modifier,
    content: @Composable (q: Int, r: Int) -> Unit
) {
    val hexCoords = mutableListOf<CoordinateUtils.HexCoord>()
    for (q in -radius..radius) {
        for (r in -radius..radius) {
            if (kotlin.math.abs(q + r) <= radius) {
                hexCoords.add(CoordinateUtils.HexCoord(q, r))
            }
        }
    }
    
    Layout(
        content = {
            hexCoords.forEach { coord ->
                content(coord.q, coord.r)
            }
        },
        modifier = modifier
    ) { measurables, constraints ->
        val placeables = measurables.map { it.measure(constraints) }
        
        var maxWidth = 0
        var maxHeight = 0
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        
        hexCoords.zip(placeables).forEach { (coord, placeable) ->
            val pixel = CoordinateUtils.hexToPixel(coord, hexSize)
            val x = pixel.x
            val y = pixel.y
            maxWidth = max(maxWidth, x.toInt() + placeable.width)
            maxHeight = max(maxHeight, y.toInt() + placeable.height)
            minX = kotlin.math.min(minX, x)
            minY = kotlin.math.min(minY, y)
        }
        
        layout(maxWidth, maxHeight) {
            hexCoords.zip(placeables).forEach { (coord, placeable) ->
                val pixel = CoordinateUtils.hexToPixel(coord, hexSize)
                val x = (pixel.x - minX).toInt()
                val y = (pixel.y - minY).toInt()
                placeable.place(x, y)
            }
        }
    }
}

@Composable
fun IsometricGrid(
    width: Int,
    height: Int,
    tileWidth: Float,
    tileHeight: Float,
    modifier: Modifier = Modifier,
    content: @Composable (x: Int, y: Int) -> Unit
) {
    val coords = mutableListOf<CoordinateUtils.IsometricCoord>()
    for (x in 0 until width) {
        for (y in 0 until height) {
            coords.add(CoordinateUtils.IsometricCoord(x, y))
        }
    }
    
    Layout(
        content = {
            coords.forEach { coord ->
                content(coord.x, coord.y)
            }
        },
        modifier = modifier
    ) { measurables, constraints ->
        val placeables = measurables.map { it.measure(constraints) }
        
        var maxWidth = 0
        var maxHeight = 0
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        
        coords.zip(placeables).forEach { (coord, placeable) ->
            val pixel = CoordinateUtils.isometricToPixel(coord, tileWidth, tileHeight)
            val x = pixel.x
            val y = pixel.y
            maxWidth = max(maxWidth, x.toInt() + placeable.width)
            maxHeight = max(maxHeight, y.toInt() + placeable.height)
            minX = kotlin.math.min(minX, x)
            minY = kotlin.math.min(minY, y)
        }
        
        layout(maxWidth, maxHeight) {
            coords.zip(placeables).forEach { (coord, placeable) ->
                val pixel = CoordinateUtils.isometricToPixel(coord, tileWidth, tileHeight)
                val x = (pixel.x - minX).toInt()
                val y = (pixel.y - minY).toInt()
                placeable.place(x, y)
            }
        }
    }
}

