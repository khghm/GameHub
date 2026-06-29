
package com.gamehub.shared.graphics.rendering

import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToInt

// ==================== Render Commands ====================
sealed class RenderCommand(open val zIndex: Float) {
    abstract val batchKey: Any // Key for grouping similar commands
}

data class RectCommand(
    val color: Color,
    val topLeft: Offset,
    val size: Size,
    val style: DrawStyle = Fill,
    val alpha: Float = 1.0f,
    val colorFilter: ColorFilter? = null,
    val blendMode: BlendMode = BlendMode.SrcOver,
    override val zIndex: Float = 0f
) : RenderCommand(zIndex) {
    override val batchKey: Any
        get() = Triple(color::class, style, alpha)
}

data class ImageCommand(
    val image: ImageBitmap,
    val topLeft: Offset,
    val size: Size? = null,
    val alpha: Float = 1.0f,
    val style: DrawStyle = Fill,
    val colorFilter: ColorFilter? = null,
    val blendMode: BlendMode = BlendMode.SrcOver,
    val filterQuality: FilterQuality = FilterQuality.Low,
    override val zIndex: Float = 0f
) : RenderCommand(zIndex) {
    override val batchKey: Any
        get() = Pair(image, alpha)
}

data class CircleCommand(
    val color: Color,
    val center: Offset,
    val radius: Float,
    val style: DrawStyle = Fill,
    val alpha: Float = 1.0f,
    val colorFilter: ColorFilter? = null,
    val blendMode: BlendMode = BlendMode.SrcOver,
    override val zIndex: Float = 0f
) : RenderCommand(zIndex) {
    override val batchKey: Any
        get() = Triple(color::class, style, alpha)
}

data class LineCommand(
    val color: Color,
    val start: Offset,
    val end: Offset,
    val strokeWidth: Float = 1f,
    val cap: StrokeCap = StrokeCap.Butt,
    val pathEffect: PathEffect? = null,
    val alpha: Float = 1.0f,
    val colorFilter: ColorFilter? = null,
    val blendMode: BlendMode = BlendMode.SrcOver,
    override val zIndex: Float = 0f
) : RenderCommand(zIndex) {
    override val batchKey: Any
        get() = Pair(color::class, strokeWidth)
}

data class PathCommand(
    val color: Color,
    val path: Path,
    val style: DrawStyle = Fill,
    val alpha: Float = 1.0f,
    val colorFilter: ColorFilter? = null,
    val blendMode: BlendMode = BlendMode.SrcOver,
    override val zIndex: Float = 0f
) : RenderCommand(zIndex) {
    override val batchKey: Any
        get() = Triple(color::class, style, alpha)
}

data class TextCommand(
    val text: String,
    val position: Offset,
    val color: Color = Color.Black,
    val alpha: Float = 1.0f,
    override val zIndex: Float = 0f
) : RenderCommand(zIndex) {
    override val batchKey: Any
        get() = color::class
}

// ==================== Batch Renderer ====================
class BatchRenderer {
    private val commands = mutableListOf<RenderCommand>()
    var isAutoClear: Boolean = true // Clear commands after rendering by default

    // ========== Drawing Methods ==========
    fun drawRect(
        color: Color,
        topLeft: Offset = Offset.Zero,
        size: Size = Size(100f, 100f),
        style: DrawStyle = Fill,
        alpha: Float = 1.0f,
        colorFilter: ColorFilter? = null,
        blendMode: BlendMode = BlendMode.SrcOver,
        zIndex: Float = 0f
    ) {
        commands.add(RectCommand(color, topLeft, size, style, alpha, colorFilter, blendMode, zIndex))
    }

    fun drawImage(
        image: ImageBitmap,
        topLeft: Offset = Offset.Zero,
        size: Size? = null,
        alpha: Float = 1.0f,
        style: DrawStyle = Fill,
        colorFilter: ColorFilter? = null,
        blendMode: BlendMode = BlendMode.SrcOver,
        filterQuality: FilterQuality = FilterQuality.Low,
        zIndex: Float = 0f
    ) {
        commands.add(ImageCommand(image, topLeft, size, alpha, style, colorFilter, blendMode, filterQuality, zIndex))
    }

    fun drawCircle(
        color: Color,
        center: Offset,
        radius: Float,
        style: DrawStyle = Fill,
        alpha: Float = 1.0f,
        colorFilter: ColorFilter? = null,
        blendMode: BlendMode = BlendMode.SrcOver,
        zIndex: Float = 0f
    ) {
        commands.add(CircleCommand(color, center, radius, style, alpha, colorFilter, blendMode, zIndex))
    }

    fun drawLine(
        color: Color,
        start: Offset,
        end: Offset,
        strokeWidth: Float = 1f,
        cap: StrokeCap = StrokeCap.Butt,
        pathEffect: PathEffect? = null,
        alpha: Float = 1.0f,
        colorFilter: ColorFilter? = null,
        blendMode: BlendMode = BlendMode.SrcOver,
        zIndex: Float = 0f
    ) {
        commands.add(LineCommand(color, start, end, strokeWidth, cap, pathEffect, alpha, colorFilter, blendMode, zIndex))
    }

    fun drawPath(
        color: Color,
        path: Path,
        style: DrawStyle = Fill,
        alpha: Float = 1.0f,
        colorFilter: ColorFilter? = null,
        blendMode: BlendMode = BlendMode.SrcOver,
        zIndex: Float = 0f
    ) {
        commands.add(PathCommand(color, path, style, alpha, colorFilter, blendMode, zIndex))
    }

    fun drawText(
        text: String,
        position: Offset,
        color: Color = Color.Black,
        alpha: Float = 1.0f,
        zIndex: Float = 0f
    ) {
        commands.add(TextCommand(text, position, color, alpha, zIndex))
    }

    // ========== Control Methods ==========
    fun clear() {
        commands.clear()
    }

    fun getCommandCount(): Int = commands.size

    // ========== Rendering ==========
    fun render(drawScope: DrawScope) {
        if (commands.isEmpty()) return

        // First sort all commands by z-index
        commands.sortBy { it.zIndex }

        // Then group by batch key (for similar commands)
        commands.groupBy { it.batchKey }.forEach { (_, group) ->
            renderGroup(drawScope, group)
        }

        // Clear commands if auto-clear is enabled
        if (isAutoClear) {
            clear()
        }
    }

    private fun renderGroup(drawScope: DrawScope, group: List<RenderCommand>) {
        group.forEach { command ->
            renderSingleCommand(drawScope, command)
        }
    }

    private fun renderSingleCommand(drawScope: DrawScope, command: RenderCommand) {
        when (command) {
            is RectCommand -> drawScope.drawRect(
                color = command.color,
                topLeft = command.topLeft,
                size = command.size,
                style = command.style,
                alpha = command.alpha,
                colorFilter = command.colorFilter,
                blendMode = command.blendMode
            )

            is ImageCommand -> {
                if (command.size != null) {
                    // Use the version with IntOffset/IntSize that supports filterQuality
                    drawScope.drawImage(
                        image = command.image,
                        srcOffset = IntOffset.Zero,
                        srcSize = IntSize(command.image.width, command.image.height),
                        dstOffset = IntOffset(command.topLeft.x.roundToInt(), command.topLeft.y.roundToInt()),
                        dstSize = IntSize(command.size.width.roundToInt(), command.size.height.roundToInt()),
                        alpha = command.alpha,
                        style = command.style,
                        colorFilter = command.colorFilter,
                        blendMode = command.blendMode,
                        filterQuality = command.filterQuality
                    )
                } else {
                    // Use the simple version with Offset
                    drawScope.drawImage(
                        image = command.image,
                        topLeft = command.topLeft,
                        alpha = command.alpha,
                        style = command.style,
                        colorFilter = command.colorFilter,
                        blendMode = command.blendMode
                    )
                }
            }

            is CircleCommand -> drawScope.drawCircle(
                color = command.color,
                center = command.center,
                radius = command.radius,
                style = command.style,
                alpha = command.alpha,
                colorFilter = command.colorFilter,
                blendMode = command.blendMode
            )

            is LineCommand -> drawScope.drawLine(
                color = command.color,
                start = command.start,
                end = command.end,
                strokeWidth = command.strokeWidth,
                cap = command.cap,
                pathEffect = command.pathEffect,
                alpha = command.alpha,
                colorFilter = command.colorFilter,
                blendMode = command.blendMode
            )

            is PathCommand -> drawScope.drawPath(
                path = command.path,
                color = command.color,
                style = command.style,
                alpha = command.alpha,
                colorFilter = command.colorFilter,
                blendMode = command.blendMode
            )

            is TextCommand -> {
                // TODO: Implement proper text rendering with Compose TextMeasurer
                // For now, this is a placeholder
                drawScope.drawCircle(
                    color = command.color,
                    center = command.position,
                    radius = 10f,
                    alpha = command.alpha
                )
            }
        }
    }
}
