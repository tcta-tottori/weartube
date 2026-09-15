package com.kazuya.weartube.data

import kotlinx.serialization.Serializable

/** 一覧・お気に入り・プレーヤーで共通に使う動画1件。DataStore には JSON で保存する（design.md 6）。 */
@Serializable
data class VideoItem(
    val videoId: String,
    val title: String,
    val channelTitle: String = "",
    val thumbnailUrl: String = "",
    /** Data API の liveBroadcastContent が live のとき true。シークバーと時間表示を出さない。 */
    val isLive: Boolean = false,
    val addedAt: Long = 0L,
) {
    companion object {
        /** 動画 ID だけが分かっているとき（URL 入力）。タイトルは再生開始後にプレーヤーから取る。 */
        fun fromId(videoId: String): VideoItem = VideoItem(videoId = videoId, title = "")
    }
}
