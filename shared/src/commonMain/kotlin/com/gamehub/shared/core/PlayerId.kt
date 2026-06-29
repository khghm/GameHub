package com.gamehub.shared.core

import kotlinx.serialization.Serializable

@Serializable
data class PlayerId(val value: String) {
    override fun toString(): String = value

    companion object {
        // برای راحتی در ساخت
        fun of(value: String): PlayerId = PlayerId(value)
    }
}