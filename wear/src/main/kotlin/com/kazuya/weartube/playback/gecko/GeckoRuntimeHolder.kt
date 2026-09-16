package com.kazuya.weartube.playback.gecko

import android.content.Context
import android.util.Log
import com.kazuya.weartube.playback.PlaybackCapabilities
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoRuntimeSettings

/**
 * GeckoRuntime はプロセスに 1 つだけ。作り直すと子プロセスが増えて時計では確実に足りなくなる。
 * consoleOutput を有効にしてあるので、ラッパーページの console.log が Logcat に出る。
 */
object GeckoRuntimeHolder {
    @Volatile
    private var runtime: GeckoRuntime? = null

    /** 生成できなかった理由。画面に出して原因を追う。 */
    @Volatile
    var failure: String? = null
        private set

    fun getOrNull(context: Context): GeckoRuntime? {
        runtime?.let { return it }
        synchronized(this) {
            runtime?.let { return it }
            return try {
                val settings =
                    GeckoRuntimeSettings
                        .Builder()
                        .consoleOutput(true)
                        .javaScriptEnabled(true)
                        .build()
                GeckoRuntime.create(context.applicationContext, settings).also {
                    runtime = it
                    failure = null
                    Log.i(PlaybackCapabilities.TAG, "GECKO_RUNTIME=created")
                }
            } catch (e: RuntimeException) {
                failure = "${e.javaClass.simpleName}: ${e.message}"
                Log.w(PlaybackCapabilities.TAG, "GECKO_RUNTIME=failed $failure")
                null
            } catch (e: LinkageError) {
                failure = "${e.javaClass.simpleName}: ${e.message}"
                Log.w(PlaybackCapabilities.TAG, "GECKO_RUNTIME=failed $failure")
                null
            }
        }
    }
}
