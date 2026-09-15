package com.kazuya.weartube.ui.player

import android.app.Application
import android.webkit.WebView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kazuya.weartube.AppContainer
import com.kazuya.weartube.audio.AudioFocusHelper
import com.kazuya.weartube.audio.AudioOutput
import com.kazuya.weartube.audio.VolumeController
import com.kazuya.weartube.data.VideoItem
import com.kazuya.weartube.player.PlaybackStatus
import com.kazuya.weartube.player.PlayerController
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 再生できない理由。UI は 1〜2 行の日本語にする。 */
enum class PlayerError { OFFLINE, NO_WEBVIEW, UNPLAYABLE, LOAD_FAILED }

data class PlayerUiState(
    val item: VideoItem? = null,
    /** 一覧のタイトル。URL 入力で開いた動画はプレーヤーから取れたタイトルで補う。 */
    val title: String = "",
    val status: PlaybackStatus = PlaybackStatus.IDLE,
    val currentSec: Double = 0.0,
    val durationSec: Double = 0.0,
    val hasPrev: Boolean = false,
    val hasNext: Boolean = false,
    val isFavorite: Boolean = false,
    val output: AudioOutput = AudioOutput.SPEAKER,
    val error: PlayerError? = null,
    /** エラーの内訳（WebView を生成できなかった理由など）。実機で原因を追うためエラー画面に小さく出す。 */
    val errorDetail: String? = null,
    /** BT 未接続かつメディア音量 0 のとき、再生開始から数秒だけ true。 */
    val volumeWarning: Boolean = false,
) {
    val isLive: Boolean get() = item?.isLive == true
    val isPlaying: Boolean get() = status == PlaybackStatus.PLAYING || status == PlaybackStatus.BUFFERING
    val isLoading: Boolean get() = status == PlaybackStatus.LOADING || status == PlaybackStatus.BUFFERING
    val progress: Float get() = if (durationSec > 0) (currentSec / durationSec).toFloat().coerceIn(0f, 1f) else 0f
}

class PlayerViewModel(
    app: Application,
    private val container: AppContainer,
) : ViewModel() {
    private val controller = PlayerController(app)
    private val focus = AudioFocusHelper(app)
    val volume = VolumeController(app)

    /** Compose の AndroidView に渡す。WebView が無い端末では null。 */
    val webView: WebView? get() = controller.webView

    private val queue: List<VideoItem> = container.playQueue.items
    private val index = MutableStateFlow(container.playQueue.startIndex)
    private val loadError = MutableStateFlow<PlayerError?>(null)
    private val volumeWarning = MutableStateFlow(false)

    val uiState: StateFlow<PlayerUiState> =
        combine(
            combine(controller.state, index, loadError) { player, i, err -> Triple(player, i, err) },
            container.favorites.favorites,
            container.audioOutput.output,
            volumeWarning,
        ) { (player, i, err), favorites, output, warning ->
            val item = queue.getOrNull(i)
            PlayerUiState(
                item = item,
                title = item?.title?.ifBlank { player.title }.orEmpty(),
                status = player.status,
                currentSec = player.currentSec,
                durationSec = player.durationSec,
                hasPrev = i > 0,
                hasNext = i < queue.size - 1,
                isFavorite = item != null && favorites.any { it.videoId == item.videoId },
                output = output,
                error = err ?: player.errorCode?.let { toError(it) },
                errorDetail = if (err == PlayerError.NO_WEBVIEW) controller.unavailableReason else null,
                volumeWarning = warning,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), PlayerUiState())

    init {
        load(index.value)
        observePlayer()
    }

    fun togglePlay() {
        when (uiState.value.status) {
            PlaybackStatus.PLAYING, PlaybackStatus.BUFFERING -> {
                controller.pause()
            }

            PlaybackStatus.ENDED -> {
                requestFocus()
                controller.seekTo(0.0)
                controller.play()
            }

            else -> {
                requestFocus()
                controller.play()
            }
        }
    }

    fun next() {
        if (uiState.value.hasNext) load(index.value + 1)
    }

    fun prev() {
        if (uiState.value.hasPrev) load(index.value - 1)
    }

    /** シークバーの位置（0〜1）へ移動。ライブ配信では呼ばれない。 */
    fun seekToFraction(fraction: Float) {
        val duration = uiState.value.durationSec
        if (duration > 0) controller.seekTo(duration * fraction.coerceIn(0f, 1f))
    }

    fun retry() = load(index.value)

    fun toggleFavorite() {
        val state = uiState.value
        val item = state.item ?: return
        viewModelScope.launch {
            container.favorites.toggle(item.copy(title = state.title.ifBlank { item.title }))
        }
    }

    override fun onCleared() {
        controller.stop()
        controller.destroy()
        focus.abandon()
    }

    private fun load(i: Int) {
        index.value = i
        val item = queue.getOrNull(i) ?: return
        when {
            !controller.isAvailable -> {
                loadError.value = PlayerError.NO_WEBVIEW
            }

            !container.connectivity.isOnline() -> {
                loadError.value = PlayerError.OFFLINE
            }

            else -> {
                loadError.value = null
                warnIfSilent()
                requestFocus()
                controller.load(item.videoId)
            }
        }
    }

    private fun requestFocus() {
        focus.request(onLoss = controller::pause)
    }

    /** design.md 2.2: BT 未接続かつ音量 0 なら再生前に 1 行で警告する。 */
    private fun warnIfSilent() {
        if (!container.audioOutput.isSilentOnSpeaker()) return
        viewModelScope.launch {
            volumeWarning.value = true
            delay(WARNING_MILLIS)
            volumeWarning.value = false
        }
    }

    private fun observePlayer() {
        // 再生終了で次の動画へ（リスト末尾ならそのまま）
        viewModelScope.launch {
            controller.state
                .map { it.status }
                .distinctUntilChanged()
                .collect { status -> if (status == PlaybackStatus.ENDED) next() }
        }
        // URL 入力で開いた動画のタイトルをお気に入りに補う
        viewModelScope.launch {
            controller.state
                .map { it.title }
                .distinctUntilChanged()
                .collect { title ->
                    val item = queue.getOrNull(index.value) ?: return@collect
                    if (item.title.isBlank() && title.isNotBlank()) container.favorites.updateTitle(item.videoId, title)
                }
        }
    }

    private fun toError(code: Int): PlayerError =
        when (code) {
            API_LOAD_FAILED -> PlayerError.LOAD_FAILED
            else -> PlayerError.UNPLAYABLE
        }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
        const val WARNING_MILLIS = 3_000L

        /** player.html が IFrame API スクリプトを読めなかったときのコード。 */
        const val API_LOAD_FAILED = -1
    }
}
