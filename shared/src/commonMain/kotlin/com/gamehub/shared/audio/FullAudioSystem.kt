package com.gamehub.shared.audio

import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.*

// ==================== 1. Sound Effect Engine ====================
class SoundEffectEngine(
    private val audioPlayer: AudioPlayer,
    private val ioScope: CoroutineScope
) {
    private val loadedSounds = mutableMapOf<String, String>() // Name -> Path

    fun load(name: String, path: String) {
        loadedSounds[name] = path
        audioPlayer.loadSound(name, path)
    }

    fun play(name: String, options: SoundOptions = SoundOptions()): SoundHandle {
        val path = loadedSounds[name] ?: return NoopSoundHandle()
        return audioPlayer.playSound(name, options)
    }

    fun playSpatial(
        name: String,
        spatialOptions: SpatialSoundOptions,
        baseOptions: SoundOptions = SoundOptions()
    ): SoundHandle {
        val delta = spatialOptions.soundPosition - spatialOptions.listenerPosition
        val distance = delta.getDistance()
        val volumeFalloff = 1f - (distance / spatialOptions.maxDistance).coerceIn(0f, 1f)
        val rolloff = volumeFalloff.pow(spatialOptions.rolloff)
        val pan = (delta.x / spatialOptions.maxDistance).coerceIn(-1f, 1f)

        return play(name, baseOptions.copy(
            volume = baseOptions.volume * rolloff,
            pan = pan
        ))
    }

    suspend fun preloadAll(sounds: List<Pair<String, String>>) = withContext(ioScope.coroutineContext) {
        sounds.forEach { (name, path) -> load(name, path) }
    }
}

// ==================== 2. Background Music Player ====================
class MusicPlayer(
    private val audioPlayer: AudioPlayer,
    private val ioScope: CoroutineScope
) {
    private val _currentTrack = MutableStateFlow<String?>(null)
    val currentTrack: StateFlow<String?> = _currentTrack

    private val loadedTracks = mutableMapOf<String, String>()
    private var isCrossfading = false

    fun load(name: String, path: String) {
        loadedTracks[name] = path
        audioPlayer.loadMusic(name, path)
    }

    fun play(name: String, options: SoundOptions = SoundOptions(isLooping = true)) {
        if (name !in loadedTracks) return
        audioPlayer.stopMusic()
        _currentTrack.value = name
        audioPlayer.playMusic(name, options)
    }

    fun playCrossfade(
        newTrackName: String,
        durationMs: Long = 1000L,
        options: SoundOptions = SoundOptions(isLooping = true)
    ) = ioScope.launch {
        if (newTrackName !in loadedTracks || isCrossfading) return@launch
        isCrossfading = true

        if (_currentTrack.value != null) {
            for (i in 10 downTo 0) {
                audioPlayer.setChannelVolume(AudioChannel.MUSIC, i / 10f)
                delay(durationMs / 10)
            }
        }

        audioPlayer.stopMusic()
        _currentTrack.value = newTrackName
        audioPlayer.setChannelVolume(AudioChannel.MUSIC, 0f)
        audioPlayer.playMusic(newTrackName, options)

        for (i in 0..10) {
            audioPlayer.setChannelVolume(AudioChannel.MUSIC, i / 10f)
            delay(durationMs / 10)
        }

        isCrossfading = false
    }

    fun stop() = ioScope.launch {
        audioPlayer.stopMusic()
        _currentTrack.value = null
    }

    fun pause() = audioPlayer.pauseMusic()
    fun resume() = audioPlayer.resumeMusic()
}

// ==================== 3. Dynamic Audio Mixer ====================
class AudioMixer(
    private val audioPlayer: AudioPlayer
) {
    private val _masterVolume = MutableStateFlow(1f)
    val masterVolume: StateFlow<Float> = _masterVolume

    private val _channelVolumes = MutableStateFlow(
        AudioChannel.values().associateWith { 1f }
    )
    val channelVolumes: StateFlow<Map<AudioChannel, Float>> = _channelVolumes

    fun setMasterVolume(volume: Float) {
        val newVol = volume.coerceIn(0f, 1f)
        _masterVolume.value = newVol
        audioPlayer.setMasterVolume(newVol)
    }

    fun setChannelVolume(channel: AudioChannel, volume: Float) {
        val newVol = volume.coerceIn(0f, 1f)
        val newChannels = _channelVolumes.value.toMutableMap()
        newChannels[channel] = newVol
        _channelVolumes.value = newChannels
        audioPlayer.setChannelVolume(channel, newVol)
    }

    fun getEffectiveVolume(channel: AudioChannel): Float {
        return (_channelVolumes.value[channel] ?: 1f) * _masterVolume.value
    }
}

// ==================== 4. 2D Spatial Audio Manager ====================
class SpatialAudioManager(
    private val sfxEngine: SoundEffectEngine
) {
    var listenerPosition: Offset = Offset.Zero

    fun play(
        soundName: String,
        soundPosition: Offset,
        maxDistance: Float = 500f,
        rolloff: Float = 1.5f,
        baseOptions: SoundOptions = SoundOptions()
    ): SoundHandle {
        return sfxEngine.playSpatial(
            soundName,
            SpatialSoundOptions(soundPosition, listenerPosition, maxDistance, rolloff),
            baseOptions
        )
    }
}

// ==================== 5. Master Audio Manager (Facade) ====================
class GameAudioManager(
    val player: AudioPlayer = NoopAudioPlayer(),
    private val ioScope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) {
    val sfx = SoundEffectEngine(player, ioScope)
    val music = MusicPlayer(player, ioScope)
    val mixer = AudioMixer(player)
    val spatial = SpatialAudioManager(sfx)

    fun release() {
        player.release()
        ioScope.cancel()
    }
}
