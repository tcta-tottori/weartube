package com.kazuya.weartube.playback

/**
 * 動画をどこで再生するか（design.md 12 章 STEP 9）。
 * UI は経路の実装を直接呼ばず、[PlaybackRouter] を通す。
 */
sealed interface PlaybackRoute {
    /** 時計のブラウザ（または Custom Tabs）に YouTube の URL を渡す。 */
    data object WatchBrowser : PlaybackRoute

    /** アプリ内の描画エンジンで公式 IFrame Player を動かす。検証は別アプリ（WearTube PoC）で行う。 */
    data object InAppGecko : PlaybackRoute

    /** スマホの YouTube アプリで開く（未実装）。 */
    data object Phone : PlaybackRoute
}

/** 経路が今この端末で使えるか。使えない場合は理由を持つ。 */
data class RouteStatus(
    val route: PlaybackRoute,
    val available: Boolean,
    val reason: String,
)
