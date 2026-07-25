package com.dehong.duelofSuits.ui.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import androidx.compose.runtime.staticCompositionLocalOf
import com.dehong.duelofSuits.R

class SoundManager(context: Context) {
    private val soundPool = SoundPool.Builder()
        .setMaxStreams(6)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val cardSoundId = soundPool.load(context, R.raw.card_play, 1)

    fun playCardSound() {
        soundPool.play(cardSoundId, 0.7f, 0.7f, 1, 0, 1.0f)
    }

    fun release() {
        soundPool.release()
    }
}

val LocalSoundManager = staticCompositionLocalOf<SoundManager?> { null }
