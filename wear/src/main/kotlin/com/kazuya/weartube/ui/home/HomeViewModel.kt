package com.kazuya.weartube.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kazuya.weartube.data.FavoritesRepository
import com.kazuya.weartube.data.PlayQueue
import com.kazuya.weartube.data.SettingsRepository
import com.kazuya.weartube.data.VideoIdParser
import com.kazuya.weartube.data.VideoItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val favorites: List<VideoItem> = emptyList(),
    val hasApiKey: Boolean = true,
)

/** ホーム画面で起きた 1 回きりの出来事（Toast 表示）。 */
enum class HomeMessage { FAVORITE_REMOVED, VIDEO_ID_INVALID }

class HomeViewModel(
    private val favorites: FavoritesRepository,
    settings: SettingsRepository,
    private val playQueue: PlayQueue,
) : ViewModel() {
    val uiState: StateFlow<HomeUiState> =
        combine(favorites.favorites, settings.apiKey) { list, key -> HomeUiState(favorites = list, hasApiKey = key.isSet) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), HomeUiState())

    private val mutableMessage = MutableStateFlow<HomeMessage?>(null)
    val message: StateFlow<HomeMessage?> = mutableMessage

    /** お気に入り一覧の [index] から再生を始める。⏮ / ⏭ はこの一覧内を移動する。 */
    fun playFavorite(index: Int) {
        playQueue.set(uiState.value.favorites, index)
    }

    /** URL または動画 ID の入力から単独再生。読み取れなければ false。 */
    fun playVideoInput(input: String): Boolean {
        val id = VideoIdParser.parse(input)
        if (id == null) {
            mutableMessage.value = HomeMessage.VIDEO_ID_INVALID
            return false
        }
        playQueue.set(listOf(VideoItem.fromId(id)), 0)
        return true
    }

    fun removeFavorite(item: VideoItem) {
        viewModelScope.launch {
            favorites.remove(item.videoId)
            mutableMessage.value = HomeMessage.FAVORITE_REMOVED
        }
    }

    fun consumeMessage() {
        mutableMessage.value = null
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
