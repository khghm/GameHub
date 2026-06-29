package com.gamehub.shared.graphics.math

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect as ComposeRect
import androidx.compose.ui.geometry.Size as ComposeSize

// ==================== Conversion Extensions ====================

// Vec2 <-> Offset
fun Vec2.toComposeOffset(): Offset = Offset(x, y)
fun Offset.toVec2(): Vec2 = Vec2(x, y)

// Size <-> ComposeSize
fun Size.toComposeSize(): ComposeSize = ComposeSize(width, height)
fun ComposeSize.toGameSize(): Size = Size(width, height)

// Rect <-> ComposeRect
fun Rect.toComposeRect(): ComposeRect = ComposeRect(left, top, right, bottom)
fun ComposeRect.toGameRect(): Rect = Rect(left, top, right, bottom)
