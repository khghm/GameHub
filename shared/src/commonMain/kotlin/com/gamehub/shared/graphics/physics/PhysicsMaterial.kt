
package com.gamehub.shared.graphics.physics

data class PhysicsMaterial(
    val bounciness: Float = 0.7f,
    val friction: Float = 0.3f,
    val density: Float = 1f,
    val staticFriction: Float = 0.5f
) {
    companion object {
        val Default = PhysicsMaterial()
        val Rubber = PhysicsMaterial(bounciness = 0.9f, friction = 0.8f)
        val Ice = PhysicsMaterial(bounciness = 0.05f, friction = 0.02f)
        val Metal = PhysicsMaterial(bounciness = 0.2f, friction = 0.4f)
        val Wood = PhysicsMaterial(bounciness = 0.3f, friction = 0.6f)
    }
}
