package com.gamehub.shared.graphics.physics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.gamehub.shared.graphics.math.*

@Composable
fun PhysicsDebugDraw(
    engine: SimplePhysicsEngine,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        // Draw bodies
        engine.bodies.forEach { body ->
            val shape = engine.updateShapePosition(body)
            val color = if (body.isStatic) Color.Gray else if (body.isSleeping) Color.Blue else Color.Red
            when (shape) {
                is CircleShape -> {
                    drawCircle(
                        color = color,
                        radius = shape.radius,
                        center = shape.position.toComposeOffset(),
                        style = Stroke(width = 2.dp.toPx())
                    )
                    // Draw orientation line
                    val orientation = shape.position + Vec2(0f, -shape.radius).rotate(body.angle)
                    drawLine(
                        color = Color.Yellow,
                        start = shape.position.toComposeOffset(),
                        end = orientation.toComposeOffset(),
                        strokeWidth = 2.dp.toPx()
                    )
                }
                is RectShape -> {
                    drawRect(
                        color = color,
                        topLeft = Offset(shape.left, shape.top),
                        size = shape.size.toComposeSize(),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
                is PolygonShape -> {
                    for (i in shape.vertices.indices) {
                        val next = (i + 1) % shape.vertices.size
                        drawLine(
                            color = color,
                            start = shape.vertices[i].toComposeOffset(),
                            end = shape.vertices[next].toComposeOffset(),
                            strokeWidth = 2.dp.toPx()
                        )
                    }
                }
                is LineShape -> {
                    drawLine(
                        color = color,
                        start = shape.start.toComposeOffset(),
                        end = shape.end.toComposeOffset(),
                        strokeWidth = 2.dp.toPx()
                    )
                }
            }
        }

        // Draw joints
        engine.joints.forEach { joint ->
            when (joint) {
                is DistanceJoint -> {
                    val aWorld = joint.getWorldAnchorA()
                    val bWorld = joint.getWorldAnchorB()
                    drawLine(
                        color = Color.Green,
                        start = aWorld.toComposeOffset(),
                        end = bWorld.toComposeOffset(),
                        strokeWidth = 2.dp.toPx()
                    )
                    drawCircle(
                        color = Color.Green,
                        radius = 3.dp.toPx(),
                        center = aWorld.toComposeOffset()
                    )
                    drawCircle(
                        color = Color.Green,
                        radius = 3.dp.toPx(),
                        center = bWorld.toComposeOffset()
                    )
                }
                is RevoluteJoint -> {
                    val aWorld = joint.getWorldAnchorA()
                    drawCircle(
                        color = Color.Cyan,
                        radius = 5.dp.toPx(),
                        center = aWorld.toComposeOffset(),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }
        }
    }
}
