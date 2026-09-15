package com.kazuya.weartube.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager

/** 再生開始時に AUDIOFOCUS_GAIN を取り、喪失で一時停止する（design.md 2.2）。 */
class AudioFocusHelper(
    context: Context,
) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var request: AudioFocusRequest? = null

    /** 取得できたら true。[onLoss] は一時的な喪失を含めて呼ぶ（自動再開はしない）。 */
    fun request(onLoss: () -> Unit): Boolean {
        abandon()
        val req =
            AudioFocusRequest
                .Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(
                    AudioAttributes
                        .Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                        .build(),
                ).setOnAudioFocusChangeListener { change ->
                    if (change == AudioManager.AUDIOFOCUS_LOSS ||
                        change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT ||
                        change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK
                    ) {
                        onLoss()
                    }
                }.build()
        request = req
        return audioManager.requestAudioFocus(req) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    fun abandon() {
        request?.let(audioManager::abandonAudioFocusRequest)
        request = null
    }
}
