package com.gamehub.shared.registry

import com.gamehub.shared.core.GameModule

object GameRegistry {
    private val modules = mutableListOf<GameModule<*, *, *>>()

    fun register(module: GameModule<*, *, *>) {
        modules.add(module)
    }

    fun getAll(): List<GameModule<*, *, *>> = modules.toList()

    fun getById(id: String): GameModule<*, *, *>? = modules.find { it.metadata.id == id }
}