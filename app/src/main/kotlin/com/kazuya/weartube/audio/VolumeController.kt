package com.kazuya.weartube.audio

import android.content.Context
import android.media.AudioManager

/** 回転リューズをメディア音量に割り当てる（design.md 4.2）。 */
class VolumeController(
    context: Context,
) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var accumulated = 0f

    /** リューズのスクロール量（px）を溜め、一定量ごとに 1 段階上げ下げする。 */
    fun onRotary(deltaPixels: Float) {
        accumulated += deltaPixels
        // 時計回り（正の値）で上げる
        while (accumulated >= STEP_PIXELS) {
            adjust(AudioManager.ADJUST_RAISE)
            accumulated -= STEP_PIXELS
        }
        while (accumulated <= -STEP_PIXELS) {
            adjust(AudioManager.ADJUST_LOWER)
            accumulated += STEP_PIXELS
        }
    }

    private fun adjust(direction: Int) {
        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, AudioManager.FLAG_SHOW_UI)
    }

    private companion object {
        /** リューズ 1 ノッチはおおむね 30〜60px。1 ノッチで 1 段階変わるようにする。 */
        const val STEP_PIXELS = 30f
    }
}
