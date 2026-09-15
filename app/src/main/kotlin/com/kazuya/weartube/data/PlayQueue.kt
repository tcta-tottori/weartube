package com.kazuya.weartube.data

/** プレーヤーに渡す再生元リストと開始位置。画面遷移の直前にセットし、PlayerViewModel が読む。 */
class PlayQueue {
    var items: List<VideoItem> = emptyList()
        private set
    var startIndex: Int = 0
        private set

    fun set(
        items: List<VideoItem>,
        startIndex: Int,
    ) {
        this.items = items
        this.startIndex = startIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0))
    }
}
