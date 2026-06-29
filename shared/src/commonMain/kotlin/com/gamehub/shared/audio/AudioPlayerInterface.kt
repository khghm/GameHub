package com.gamehub.shared.audio

import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.*

// ==================== Audio Channel Enum ====================
enum class AudioChannel {
    SFX,
    MUSIC,
    UI,
    VOICE
}

// ==================== Sound Options ====================
data class SoundOptions(
    val volume: Float = 1f,
    val pitch: Float = 1f,
    val pan: Float = 0f, // -1 = left, 0 = center, 1 = right
    val isLooping: Boolean = false
)

data class SpatialSoundOptions(
    val soundPosition: Offset,
    val listenerPosition: Offset,
    val maxDistance: Float = 500f,
    val rolloff: Float = 1.5f
)

// ==================== Sound Handle ====================
interface SoundHandle {
    fun stop()
    fun pause()
    fun resume()
    fun setVolume(volume: Float)
    fun setPitch(pitch: Float)
    fun setPan(pan: Float)
}

// ==================== Common Audio Player Interface ====================
interface AudioPlayer {
    fun loadSound(name: String, filePath: String)
    fun playSound(name: String, options: SoundOptions): SoundHandle
    fun loadMusic(name: String, filePath: String)
    fun playMusic(name: String, options: SoundOptions)
    fun stopMusic()
    fun pauseMusic()
    fun resumeMusic()
    fun setMasterVolume(volume: Float)
    fun setChannelVolume(channel: AudioChannel, volume: Float)
    fun release()
}

// ==================== No-op Implementation (fallback) ====================
class NoopAudioPlayer : AudioPlayer {
    override fun loadSound(name: String, filePath: String) {}
    override fun playSound(name: String, options: SoundOptions): SoundHandle = NoopSoundHandle()
    override fun loadMusic(name: String, filePath: String) {}
    override fun playMusic(name: String, options: SoundOptions) {}
    override fun stopMusic() {}
    override fun pauseMusic() {}
    override fun resumeMusic() {}
    override fun setMasterVolume(volume: Float) {}
    override fun setChannelVolume(channel: AudioChannel, volume: Float) {}
    override fun release() {}
}

class NoopSoundHandle : SoundHandle {
    override fun stop() {}
    override fun pause() {}
    override fun resume() {}
    override fun setVolume(volume: Float) {}
    override fun setPitch(pitch: Float) {}
    override fun setPan(pan: Float) {}
}
