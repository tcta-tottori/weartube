package com.kazuya.weartube.playback

/**
 * 動画をどこで再生するか（design.md 12 章 STEP 9）。
 * UI は経路の実装を直接呼ばず、[PlaybackRouter] を通す。
 */
sealed interface PlaybackRoute {
    /** 時計のブラウザ（または Custom Tabs）に YouTube の URL を渡す。 */
    data object WatchBrowser : PlaybackRoute

    /** アプリ内の GeckoView で公式 IFrame Player を動かす。 */
    data object InAppGecko : PlaybackRoute

    /** スマホの YouTube アプリで開く（未実装）。 */
    data object Phone : PlaybackRoute
}

/** GeckoView で何を読み込むか。referer と origin の効き方を比べるため分ける（STEP 5）。 */
enum class GeckoLoadMode {
    /** YouTube の埋め込みページを直接開く。referer は https://www.youtube.com/ を付ける。 */
    DIRECT_EMBED,

    /** 端末内の HTTP サーバーが返すラッパーページから IFrame Player API を使う。 */
    LOCAL_WRAPPER,
}

/** 経路が今この端末で使えるか。使えない場合は理由を持つ。 */
data class RouteStatus(
    val route: PlaybackRoute,
    val available: Boolean,
    val reason: String,
)
