package com.gamehub.server.bot

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class BotRotationScheduler(
    private val centralBotManager: CentralBotManager,
    private val botProfileRepository: BotProfileRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun start() {
        scope.launch {
            while (true) {
                delay(24 * 60 * 60 * 1000L) // هر ۲۴ ساعت یک بار
                runRotation()
            }
        }
    }

    private suspend fun runRotation() {
        val allBots = botProfileRepository.findAllActive()
        val now = System.currentTimeMillis()
        for (bot in allBots) {
            if (!bot.isTutorial) {
                bot.lastRotation?.let { lastRot ->
                    if (now - lastRot > 30 * 24 * 60 * 60 * 1000L) {
                        centralBotManager.rotateBotProfile(bot.botId.value)
                    }
                }
            }
        }
        centralBotManager.cleanupInactiveBots(30)
        println("🤖 Bot rotation completed")
    }
}