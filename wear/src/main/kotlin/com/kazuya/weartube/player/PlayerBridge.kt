package com.kazuya.weartube.player

import android.webkit.JavascriptInterface

/**
 * player.html から呼ばれる JS → Kotlin の受け口（design.md 5.3）。
 * 呼び出しは WebView の JavaBridge スレッドで来るので、ここでは状態の更新だけを行う。
 */
class PlayerBridge(
    private val listener: Listener,
) {
    interface Listener {
        fun onApiReady()

        fun onStateChange(
            state: Int,
            title: String,
            durationSec: Double,
        )

        fun onTime(
            currentSec: Double,
            durationSec: Double,
        )

        fun onError(code: Int)
    }

    @JavascriptInterface
    fun onApiReady() = listener.onApiReady()

    @JavascriptInterface
    fun onStateChange(
        state: Int,
        title: String?,
        durationSec: Double,
    ) = listener.onStateChange(state, title.orEmpty(), durationSec)

    @JavascriptInterface
    fun onTime(
        currentSec: Double,
        durationSec: Double,
    ) = listener.onTime(currentSec, durationSec)

    @JavascriptInterface
    fun onError(code: Int) = listener.onError(code)

    companion object {
        /** window.Android として公開する。player.html 側の参照名と一致させる。 */
        const val NAME = "Android"
    }
}
