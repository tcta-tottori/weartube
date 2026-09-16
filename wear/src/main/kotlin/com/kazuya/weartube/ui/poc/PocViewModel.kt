package com.kazuya.weartube.ui.poc

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kazuya.weartube.data.FavoritesRepository
import com.kazuya.weartube.playback.PlaybackCapabilities
import com.kazuya.weartube.playback.PlaybackRouter
import com.kazuya.weartube.playback.RouteStatus
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** 再生方式の検証画面（design.md 12 章）。判定は端末を見るだけなので副作用は起動時の 1 回。 */
class PocViewModel(
    app: Application,
    favorites: FavoritesRepository,
) : ViewModel() {
    val capabilities: PlaybackCapabilities = PlaybackCapabilities.detect(app)

    private val router = PlaybackRouter(capabilities)

    val statuses: List<RouteStatus> = router.statuses()

    /** お気に入りの先頭を検証に使う。無ければ埋め込みが許可された確認用の動画。 */
    val videoId: StateFlow<String> =
        favorites.favorites
            .map { list -> list.firstOrNull()?.videoId ?: DEFAULT_VIDEO_ID }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), DEFAULT_VIDEO_ID)

    /** 時計のブラウザに URL を渡す。開けたら true。 */
    fun openInWatchBrowser(context: Context): Boolean = router.openInWatchBrowser(context, videoId.value)

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L

        /** 埋め込みが許可されている確認用の動画（Blender の Big Buck Bunny）。 */
        const val DEFAULT_VIDEO_ID = "aqz-KE-bpKQ"
    }
}
