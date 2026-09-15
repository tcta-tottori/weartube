package com.kazuya.weartube.audio

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** 現在の音声出力先。切替 UI は置かず、接続状態に追従してアイコン表示だけ行う（design.md 2.2）。 */
enum class AudioOutput { SPEAKER, BLUETOOTH }

class AudioOutputMonitor(
    context: Context,
) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val state = MutableStateFlow(detect())

    val output: StateFlow<AudioOutput> = state

    init {
        audioManager.registerAudioDeviceCallback(
            object : AudioDeviceCallback() {
                override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) {
                    state.value = detect()
                }

                override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) {
                    state.value = detect()
                }
            },
            Handler(Looper.getMainLooper()),
        )
    }

    /** BT 未接続かつメディア音量 0 なら true（再生前の 1 行警告に使う）。 */
    fun isSilentOnSpeaker(): Boolean = state.value == AudioOutput.SPEAKER && audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) == 0

    private fun detect(): AudioOutput {
        val bluetooth =
            audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).any {
                it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP || it.type == AudioDeviceInfo.TYPE_BLE_HEADSET
            }
        return if (bluetooth) AudioOutput.BLUETOOTH else AudioOutput.SPEAKER
    }
}
