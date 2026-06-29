package com.gamehub.games.ludo

import kotlinx.serialization.Serializable

@Serializable
data class PieceInfo(
    val loc: String,
    val spot: Int,
    val color: String = ""
)