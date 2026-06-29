package com.gamehub.shared.audio

interface AudioManager {
    fun playSound(soundId: String, volume: Float = 1f, loop: Boolean = false)
    fun stopSound(soundId: String)
    fun stopAllSounds()
    fun setMasterVolume(volume: Float)
    fun setSoundVolume(volume: Float)
    fun setMusicVolume(volume: Float)
}

class NoopAudioManager : AudioManager {
    override fun playSound(soundId: String, volume: Float, loop: Boolean) {}
    override fun stopSound(soundId: String) {}
    override fun stopAllSounds() {}
    override fun setMasterVolume(volume: Float) {}
    override fun setSoundVolume(volume: Float) {}
    override fun setMusicVolume(volume: Float) {}
}
