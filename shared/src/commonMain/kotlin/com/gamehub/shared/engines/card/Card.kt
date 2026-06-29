package com.gamehub.shared.engines.card

import kotlinx.serialization.Serializable

@Serializable
data class Card(
    val color: CardColor,
    val value: CardValue
)

@Serializable
enum class CardColor { RED, BLUE, GREEN, YELLOW, WILD }

@Serializable
sealed class CardValue {
    @Serializable
    data class Number(val number: Int) : CardValue()

    @Serializable
    data object Skip : CardValue()

    @Serializable
    data object Reverse : CardValue()

    @Serializable
    data object DrawTwo : CardValue()

    @Serializable
    data object Wild : CardValue()

    @Serializable
    data object WildDrawFour : CardValue()
}