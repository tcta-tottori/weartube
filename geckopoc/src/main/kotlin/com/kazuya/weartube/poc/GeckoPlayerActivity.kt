package com.kazuya.weartube.poc

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.TextView
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoView

/**
 * GeckoView で YouTube 公式 player を出すだけの画面（design.md 12 章 STEP 3）。
 * UI は作り込まない。公式 player をそのまま全画面に出し、隠す・覆う・切り取るはしない。
 */
class GeckoPlayerActivity : Activity() {
    private var session: GeckoSession? = null
    private var wrapperServer: WrapperServer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val videoId = intent.getStringExtra(EXTRA_VIDEO_ID).orEmpty().ifBlank { DEFAULT_VIDEO_ID }
        val mode =
            runCatching { GeckoLoadMode.valueOf(intent.getStringExtra(EXTRA_MODE).orEmpty()) }
                .getOrDefault(GeckoLoadMode.LOCAL_WRAPPER)

        val runtime = GeckoRuntimeHolder.getOrNull(this)
        if (runtime == null) {
            setContentView(TextView(this).apply { text = getString(R.string.gecko_failed, GeckoRuntimeHolder.failure.orEmpty()) })
            return
        }

        val view = GeckoView(this)
        setContentView(view)

        val newSession = GeckoSession()
        newSession.progressDelegate = progressDelegate
        newSession.contentDelegate = contentDelegate
        newSession.open(runtime)
        view.setSession(newSession)
        session = newSession

        load(newSession, mode, videoId)
    }

    private fun load(
        session: GeckoSession,
        mode: GeckoLoadMode,
        videoId: String,
    ) {
        when (mode) {
            GeckoLoadMode.DIRECT_EMBED -> {
                val url = EMBED_URL.format(videoId)
                Log.i(GeckoRuntimeHolder.TAG, "GECKO_LOAD mode=DIRECT_EMBED url=$url referrer=$YOUTUBE_ORIGIN")
                session.load(GeckoSession.Loader().uri(url).referrer(YOUTUBE_ORIGIN))
            }

            GeckoLoadMode.LOCAL_WRAPPER -> {
                val html = assets.open(WRAPPER_HTML).bufferedReader().use { it.readText() }
                val server = WrapperServer(html)
                wrapperServer = server
                val url = server.urlFor(videoId)
                Log.i(GeckoRuntimeHolder.TAG, "GECKO_LOAD mode=LOCAL_WRAPPER url=$url")
                session.load(GeckoSession.Loader().uri(url))
            }
        }
    }

    private val progressDelegate =
        object : GeckoSession.ProgressDelegate {
            override fun onPageStart(
                session: GeckoSession,
                url: String,
            ) {
                Log.i(GeckoRuntimeHolder.TAG, "GECKO_PAGE_START $url")
            }

            override fun onPageStop(
                session: GeckoSession,
                success: Boolean,
            ) {
                Log.i(GeckoRuntimeHolder.TAG, "GECKO_PAGE_STOP success=$success")
            }
        }

    private val contentDelegate =
        object : GeckoSession.ContentDelegate {
            override fun onCrash(session: GeckoSession) {
                Log.w(GeckoRuntimeHolder.TAG, "GECKO_CONTENT=crash")
            }

            override fun onKill(session: GeckoSession) {
                Log.w(GeckoRuntimeHolder.TAG, "GECKO_CONTENT=killed")
            }

            override fun onTitleChange(
                session: GeckoSession,
                title: String?,
            ) {
                Log.i(GeckoRuntimeHolder.TAG, "GECKO_TITLE=$title")
            }
        }

    override fun onDestroy() {
        super.onDestroy()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        session?.close()
        session = null
        wrapperServer?.close()
        wrapperServer = null
        Log.i(GeckoRuntimeHolder.TAG, "GECKO_SESSION=closed")
    }

    companion object {
        const val EXTRA_VIDEO_ID = "videoId"
        const val EXTRA_MODE = "mode"

        /** 埋め込みが許可されている確認用の動画（Blender の Big Buck Bunny）。 */
        const val DEFAULT_VIDEO_ID = "aqz-KE-bpKQ"

        private const val YOUTUBE_ORIGIN = "https://www.youtube.com/"
        private const val EMBED_URL = "https://www.youtube.com/embed/%s?playsinline=1"
        private const val WRAPPER_HTML = "iframe_wrapper.html"
    }
}
