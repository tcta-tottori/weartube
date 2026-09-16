package com.kazuya.weartube.playback

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log

/**
 * この端末でどの再生経路が使えるかを調べる（design.md 12 章 STEP 2）。
 * 判定結果は画面にも Logcat（タグ [TAG]）にも出す。実機でしか分からないため。
 */
data class PlaybackCapabilities(
    /** Android System WebView が使えるか。Pixel Watch では false。 */
    val hasWebViewFeature: Boolean,
    /** https の URL を開けるアプリ（ブラウザ相当）があるか。 */
    val hasBrowserHandler: Boolean,
    /** Custom Tabs に対応したアプリがあるか。 */
    val hasCustomTabsProvider: Boolean,
    /** 上記に該当したアプリのパッケージ名。 */
    val handlerPackages: List<String>,
    val abis: List<String>,
    val apiLevel: Int,
    val screenWidthPx: Int,
    val screenHeightPx: Int,
    val density: Float,
) {
    /** CSS px に換算した画面の短辺。YouTube の player は 200x200 以上を要求する。 */
    val screenMinCssPx: Int
        get() = (minOf(screenWidthPx, screenHeightPx) / density).toInt()

    fun log() {
        Log.i(TAG, "FEATURE_WEBVIEW=$hasWebViewFeature")
        Log.i(TAG, "WATCH_BROWSER_HANDLER=$hasBrowserHandler")
        Log.i(TAG, "CUSTOM_TABS_PROVIDER=$hasCustomTabsProvider")
        Log.i(TAG, "HANDLER_PACKAGES=${handlerPackages.joinToString(",").ifEmpty { "(none)" }}")
        Log.i(TAG, "ABIS=${abis.joinToString(",")}")
        Log.i(TAG, "API_LEVEL=$apiLevel RELEASE=${Build.VERSION.RELEASE} MODEL=${Build.MODEL}")
        Log.i(TAG, "SCREEN_PX=${screenWidthPx}x$screenHeightPx DENSITY=$density SCREEN_MIN_CSS_PX=$screenMinCssPx")
    }

    companion object {
        const val TAG = "WearTubePoC"

        /** 検証に使う URL の雛形。動画 ID を差し込む。 */
        const val WATCH_URL = "https://www.youtube.com/watch?v=%s"

        private const val CUSTOM_TABS_ACTION = "android.support.customtabs.action.CustomTabsService"

        fun detect(context: Context): PlaybackCapabilities {
            val pm = context.packageManager
            val viewIntent = Intent(Intent.ACTION_VIEW, Uri.parse(WATCH_URL.format("dQw4w9WgXcQ")))
            val browsers = pm.queryIntentActivities(viewIntent, 0).map { it.activityInfo.packageName }
            val customTabs = pm.queryIntentServices(Intent(CUSTOM_TABS_ACTION), 0).map { it.serviceInfo.packageName }
            val metrics = context.resources.displayMetrics
            return PlaybackCapabilities(
                hasWebViewFeature = pm.hasSystemFeature(PackageManager.FEATURE_WEBVIEW),
                hasBrowserHandler = browsers.isNotEmpty(),
                hasCustomTabsProvider = customTabs.isNotEmpty(),
                handlerPackages = (browsers + customTabs).distinct(),
                abis = Build.SUPPORTED_ABIS.toList(),
                apiLevel = Build.VERSION.SDK_INT,
                screenWidthPx = metrics.widthPixels,
                screenHeightPx = metrics.heightPixels,
                density = metrics.density,
            ).also { it.log() }
        }
    }
}
