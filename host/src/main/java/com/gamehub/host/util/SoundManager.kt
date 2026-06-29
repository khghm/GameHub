package com.gamehub.host.util

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.gamehub.host.R

object SoundManager {
    private var soundPool: SoundPool? = null
    private val soundMap = mutableMapOf<String, Int>()

    fun init(context: Context) {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        soundPool = SoundPool.Builder()
            .setMaxStreams(4)
            .setAudioAttributes(attrs)
            .build()

        soundMap["dice"] = soundPool!!.load(context, R.raw.dice_roll, 1)
        soundMap["move"] = soundPool!!.load(context, R.raw.piece_move, 1)
        soundMap["capture"] = soundPool!!.load(context, R.raw.capture, 1)
        soundMap["finish"] = soundPool!!.load(context, R.raw.finish, 1)
        soundMap["win"] = soundPool!!.load(context, R.raw.win, 1)
    }

    fun play(soundName: String) {
        soundMap[soundName]?.let {
            soundPool?.play(it, 1f, 1f, 1, 0, 1f)
        }
    }

    fun release() {
        soundPool?.release()
        soundPool = null
    }
}