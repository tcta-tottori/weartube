package com.kazuya.weartube.player

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/**
 * WebView + YouTube IFrame Player のラッパー。WebView の操作はこのクラスに閉じ込める（CLAUDE.md コード規約）。
 * WebView は Application Context で作り、ViewModel の寿命に合わせて破棄する。
 */
class PlayerController(
    context: Context,
) : PlayerBridge.Listener {
    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private val mutableState = MutableStateFlow(PlayerState())
    private var pendingVideoId: String? = null

    val state: StateFlow<PlayerState> = mutableState

    /**
     * WebView を生成できなかった理由。実機でしか再現しないため、エラー画面にそのまま出して原因を追う。
     * 生成できたときは null。
     */
    var unavailableReason: String? = null
        private set

    /** WebView を生成できない端末では null。呼び出し側は [isAvailable] を先に見る。 */
    val webView: WebView? = createWebView()

    val isAvailable: Boolean get() = webView != null

    /** 動画を読み込む。API 準備前なら保留し、onReady 後に流す。 */
    fun load(videoId: String) {
        mutableState.update {
            it.copy(status = PlaybackStatus.LOADING, title = "", currentSec = 0.0, durationSec = 0.0, errorCode = null)
        }
        if (state.value.apiReady) {
            evaluate("loadVideo('${escape(videoId)}')")
        } else {
            pendingVideoId = videoId
        }
    }

    fun play() = evaluate("playVideo()")

    fun pause() = evaluate("pauseVideo()")

    fun seekTo(seconds: Double) = evaluate("seekTo(${seconds.coerceAtLeast(0.0)})")

    fun stop() = evaluate("stopVideo()")

    fun destroy() {
        mainHandler.post {
            webView?.apply {
                (parent as? ViewGroup)?.removeView(this)
                removeJavascriptInterface(PlayerBridge.NAME)
                destroy()
            }
        }
    }

    // ---- PlayerBridge.Listener（JavaBridge スレッドから呼ばれる） ----

    override fun onApiReady() {
        mutableState.update { it.copy(apiReady = true) }
        pendingVideoId?.let { id ->
            pendingVideoId = null
            evaluate("loadVideo('${escape(id)}')")
        }
    }

    override fun onStateChange(
        state: Int,
        title: String,
        durationSec: Double,
    ) {
        mutableState.update {
            it.copy(
                status = YtState.toStatus(state, it.status),
                title = title.ifBlank { it.title },
                durationSec = if (durationSec > 0) durationSec else it.durationSec,
            )
        }
    }

    override fun onTime(
        currentSec: Double,
        durationSec: Double,
    ) {
        mutableState.update { it.copy(currentSec = currentSec, durationSec = if (durationSec > 0) durationSec else it.durationSec) }
    }

    override fun onError(code: Int) {
        Log.w(TAG, "IFrame Player がエラーを返しました: code=$code")
        mutableState.update { it.copy(errorCode = code, status = PlaybackStatus.IDLE) }
    }

    // ---- 内部 ----

    private fun evaluate(script: String) {
        val view = webView ?: return
        mainHandler.post { view.evaluateJavascript(script, null) }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun createWebView(): WebView? {
        // Wear OS には WebViewUpdateService が無く、WebView が使えても getCurrentWebViewPackage() が
        // null を返すことがある。事前判定はせず、実際に生成してみて失敗したときだけ「無い」とみなす。
        // Application Context では失敗してテーマ付き Context なら通る端末があるので、順に試す。
        val attempts =
            listOf<Pair<String, () -> Context>>(
                "app" to { appContext },
                "themed" to { ContextThemeWrapper(appContext, android.R.style.Theme_DeviceDefault) },
            )
        var view: WebView? = null
        val failures = mutableListOf<String>()
        for ((label, contextOf) in attempts) {
            view =
                try {
                    WebView(contextOf())
                } catch (e: RuntimeException) {
                    // MissingWebViewPackageException など。WebView が入っていない・無効化されている
                    failures += "$label=${e.javaClass.simpleName}: ${e.message?.take(REASON_LENGTH)}"
                    null
                } catch (e: LinkageError) {
                    failures += "$label=${e.javaClass.simpleName}: ${e.message?.take(REASON_LENGTH)}"
                    null
                }
            if (view != null) break
        }
        if (view == null) {
            val hasFeature = appContext.packageManager.hasSystemFeature(PackageManager.FEATURE_WEBVIEW)
            unavailableReason = "feature=$hasFeature " + failures.joinToString(" / ")
            Log.w(TAG, "WebView を生成できません: $unavailableReason")
            return null
        }
        view.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            // 自動再生に必要（design.md 5.3）
            mediaPlaybackRequiresUserGesture = false
            loadWithOverviewMode = true
            useWideViewPort = true
        }
        view.setBackgroundColor(Color.BLACK)
        // 操作は Compose 側のオーバーレイで行う。iframe へのタップを通さない
        view.setOnTouchListener { _, _ -> true }
        // フォーカスは Compose 側（リューズ＝音量）に残す
        view.isFocusable = false
        view.isFocusableInTouchMode = false
        view.isVerticalScrollBarEnabled = false
        view.isHorizontalScrollBarEnabled = false
        view.webViewClient =
            object : WebViewClient() {
                // iframe 内のリンク（YouTube 本体へ飛ぶもの）で画面遷移させない
                override fun shouldOverrideUrlLoading(
                    view: WebView,
                    request: WebResourceRequest,
                ): Boolean = request.isForMainFrame

                override fun onReceivedError(
                    view: WebView,
                    request: WebResourceRequest,
                    error: WebResourceError,
                ) {
                    Log.w(TAG, "読み込み失敗: ${request.url} ${error.description}")
                }
            }
        // 実機でしか再現しない不具合を adb logcat で追えるようにする
        view.webChromeClient =
            object : WebChromeClient() {
                override fun onConsoleMessage(message: ConsoleMessage): Boolean {
                    Log.d(TAG, "console: ${message.message()} (${message.sourceId()}:${message.lineNumber()})")
                    return true
                }
            }
        view.addJavascriptInterface(PlayerBridge(this), PlayerBridge.NAME)
        val html =
            appContext.assets
                .open(PLAYER_HTML)
                .bufferedReader()
                .use { it.readText() }
        // origin 検証を通すため YouTube を base URL にする（省略すると再生できない）
        view.loadDataWithBaseURL(BASE_URL, html, "text/html", "utf-8", null)
        return view
    }

    private fun escape(videoId: String): String = videoId.filter { it.isLetterOrDigit() || it == '-' || it == '_' }

    private companion object {
        const val TAG = "WearTubePlayer"

        /** エラー画面に出す理由の長さ上限（小さい画面に収めるため）。 */
        const val REASON_LENGTH = 120
        const val PLAYER_HTML = "player.html"
        const val BASE_URL = "https://www.youtube.com"
    }
}
