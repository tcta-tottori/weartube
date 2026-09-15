package com.kazuya.weartube.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kazuya.weartube.data.FavoritesRepository
import com.kazuya.weartube.data.PlayQueue
import com.kazuya.weartube.data.SearchError
import com.kazuya.weartube.data.SearchRepository
import com.kazuya.weartube.data.SearchResult
import com.kazuya.weartube.data.VideoItem
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val isSearching: Boolean = false,
    val results: List<VideoItem> = emptyList(),
    val favoriteIds: Set<String> = emptySet(),
    val error: SearchError? = null,
    /** まだ 1 度も検索していない（音声入力の待ち）。 */
    val idle: Boolean = true,
)

enum class SearchMessage { FAVORITE_ADDED, FAVORITE_REMOVED, VOICE_UNAVAILABLE }

class SearchViewModel(
    private val search: SearchRepository,
    private val favorites: FavoritesRepository,
    private val playQueue: PlayQueue,
) : ViewModel() {
    private val local = MutableStateFlow(SearchUiState())
    private var job: Job? = null

    val uiState: StateFlow<SearchUiState> =
        combine(local, favorites.favorites.map { list -> list.map { it.videoId }.toSet() }) { s, ids -> s.copy(favoriteIds = ids) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), SearchUiState())

    /** まだ検索していないか。画面を開いた直後の音声入力起動の判定に使う（StateFlow の初期値に依存しない）。 */
    val isIdle: Boolean get() = local.value.idle

    private val mutableMessage = MutableStateFlow<SearchMessage?>(null)
    val message: StateFlow<SearchMessage?> = mutableMessage

    /** 同じ語の連打はクォータの無駄なので、検索中は無視する。 */
    fun search(query: String) {
        val q = query.trim()
        if (q.isEmpty() || local.value.isSearching) return
        job?.cancel()
        local.value = local.value.copy(query = q, isSearching = true, error = null, idle = false)
        job =
            viewModelScope.launch {
                when (val result = search.search(q)) {
                    is SearchResult.Success -> {
                        local.value = local.value.copy(isSearching = false, results = result.items)
                    }

                    is SearchResult.Failure -> {
                        local.value =
                            local.value.copy(isSearching = false, results = emptyList(), error = result.error)
                    }
                }
            }
    }

    fun play(index: Int) {
        playQueue.set(local.value.results, index)
    }

    fun toggleFavorite(item: VideoItem) {
        viewModelScope.launch {
            val added = favorites.toggle(item)
            mutableMessage.value = if (added) SearchMessage.FAVORITE_ADDED else SearchMessage.FAVORITE_REMOVED
        }
    }

    fun voiceUnavailable() {
        mutableMessage.value = SearchMessage.VOICE_UNAVAILABLE
    }

    fun consumeMessage() {
        mutableMessage.value = null
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
