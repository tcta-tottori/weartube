package com.kazuya.weartube.player

/** IFrame Player の状態を Kotlin 側で持ち直したもの。 */
enum class PlaybackStatus { IDLE, LOADING, BUFFERING, PLAYING, PAUSED, ENDED }

data class PlayerState(
    /** IFrame API の onReady が来たか。来る前の loadVideo は保留する。 */
    val apiReady: Boolean = false,
    val status: PlaybackStatus = PlaybackStatus.IDLE,
    /** プレーヤーから取れた動画タイトル。URL 入力で開いた動画の表示とお気に入り補完に使う。 */
    val title: String = "",
    val currentSec: Double = 0.0,
    val durationSec: Double = 0.0,
    /** IFrame API の onError コード。-1 は API スクリプト自体の読み込み失敗。 */
    val errorCode: Int? = null,
) {
    val progress: Float
        get() = if (durationSec > 0) (currentSec / durationSec).toFloat().coerceIn(0f, 1f) else 0f
}

/** IFrame API の onStateChange の値。 */
object YtState {
    const val UNSTARTED = -1
    const val ENDED = 0
    const val PLAYING = 1
    const val PAUSED = 2
    const val BUFFERING = 3
    const val CUED = 5

    fun toStatus(
        code: Int,
        previous: PlaybackStatus,
    ): PlaybackStatus =
        when (code) {
            ENDED -> PlaybackStatus.ENDED
            PLAYING -> PlaybackStatus.PLAYING
            PAUSED -> PlaybackStatus.PAUSED
            BUFFERING -> PlaybackStatus.BUFFERING
            CUED -> PlaybackStatus.PAUSED
            UNSTARTED -> if (previous == PlaybackStatus.IDLE) PlaybackStatus.IDLE else PlaybackStatus.LOADING
            else -> previous
        }
}
