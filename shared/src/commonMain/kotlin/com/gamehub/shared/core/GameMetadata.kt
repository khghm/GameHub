package com.gamehub.shared.core

data class GameMetadata(
    val id: String,
    val name: String,
    val minPlayers: Int,
    val maxPlayers: Int,
    val description: String = ""
)