package com.gamehub.server.bot

import kotlin.random.Random

object BotProfileGenerator {
    private val firstNames = listOf(
        "علی", "محمد", "رضا", "سارا", "نرگس", "زهرا", "حسین", "حمید", "مریم", "پریسا",
        "Alex", "Sam", "Chris", "Jordan", "Taylor", "Morgan", "Casey", "Riley", "Jessie", "Dana"
    )

    private val lastNames = listOf(
        "کریمی", "محمدی", "احمدی", "رضایی", "حسینی", "نوروزی", "صالحی", "کاظمی", "موسوی", "اکبری",
        "Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Miller", "Davis", "Rodriguez", "Martinez"
    )

    private val avatars = listOf(
        "avatar_bot_1", "avatar_bot_2", "avatar_bot_3", "avatar_bot_4", "avatar_bot_5"
    )

    fun generateUsername(): String {
        val firstName = firstNames.random()
        val lastName = lastNames.random()
        val number = Random.nextInt(1, 1000)
        return "$firstName$number"
    }

    fun generateAvatarId(): String = avatars.random()

    fun generateTutorialUsername(gameId: String): String = "ربات آموزشی $gameId"
    fun generateTutorialAvatarId(): String = "avatar_tutorial"
}