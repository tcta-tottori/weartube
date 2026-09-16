package com.kazuya.weartube.playback

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log

/**
 * 端末の状況から使える再生経路を決める（design.md 12 章 STEP 9）。
 * 今は検証用。本番の再生画面はまだこの経路を使っていない。
 */
class PlaybackRouter(
    private val capabilities: PlaybackCapabilities,
) {
    /** 経路ごとの可否。画面にそのまま並べる。 */
    fun statuses(): List<RouteStatus> =
        listOf(
            RouteStatus(
                route = PlaybackRoute.WatchBrowser,
                available = capabilities.hasBrowserHandler || capabilities.hasCustomTabsProvider,
                reason =
                    if (capabilities.hasBrowserHandler || capabilities.hasCustomTabsProvider) {
                        capabilities.handlerPackages.joinToString(", ")
                    } else {
                        "URL を開けるアプリがありません"
                    },
            ),
            RouteStatus(
                route = PlaybackRoute.InAppGecko,
                available = false,
                reason = "別アプリ WearTube PoC で検証",
            ),
            RouteStatus(
                route = PlaybackRoute.Phone,
                available = false,
                reason = "未実装",
            ),
        )

    /** 時計のブラウザに URL を渡す。開けたら true。 */
    fun openInWatchBrowser(
        context: Context,
        videoId: String,
    ): Boolean {
        val intent =
            Intent(Intent.ACTION_VIEW, Uri.parse(PlaybackCapabilities.WATCH_URL.format(videoId)))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(intent)
            Log.i(PlaybackCapabilities.TAG, "WATCH_BROWSER_LAUNCH=ok videoId=$videoId")
            true
        } catch (e: android.content.ActivityNotFoundException) {
            Log.w(PlaybackCapabilities.TAG, "WATCH_BROWSER_LAUNCH=failed ${e.message}")
            false
        }
    }
}
