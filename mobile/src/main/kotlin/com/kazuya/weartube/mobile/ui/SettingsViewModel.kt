package com.kazuya.weartube.mobile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.kazuya.weartube.data.ApiKeySource
import com.kazuya.weartube.data.VideoIdParser
import com.kazuya.weartube.data.VideoItem
import com.kazuya.weartube.mobile.MobileContainer
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val apiKeySource: ApiKeySource = ApiKeySource.NONE,
    val apiKeyTail: String = "",
    val favorites: List<VideoItem> = emptyList(),
    val watchConnected: Boolean = false,
    val lastSyncAt: Long? = null,
    val busy: Boolean = false,
)

enum class Message { API_KEY_SAVED, SYNC_DONE, SYNC_FAILED, FAVORITE_ADDED, FAVORITE_EXISTS, FAVORITE_REMOVED, VIDEO_ID_INVALID }

class SettingsViewModel(
    private val container: MobileContainer,
) : ViewModel() {
    private val watchConnected = MutableStateFlow(false)
    private val busy = MutableStateFlow(false)
    private val mutableMessage = MutableStateFlow<Message?>(null)
    val message: StateFlow<Message?> = mutableMessage

    val uiState: StateFlow<SettingsUiState> =
        combine(
            container.settings.apiKey,
            container.favorites.favorites,
            container.settings.lastSyncAt,
            watchConnected,
            busy,
        ) { key, favs, syncAt, connected, isBusy ->
            SettingsUiState(
                apiKeySource = key.source,
                apiKeyTail = key.key.takeLast(4),
                favorites = favs,
                watchConnected = connected,
                lastSyncAt = syncAt,
                busy = isBusy,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), SettingsUiState())

    init {
        // 時計の接続状態を定期的に見る
        viewModelScope.launch {
            while (true) {
                watchConnected.value = container.sync.isPeerConnected()
                delay(CONNECTION_POLL_MILLIS)
            }
        }
    }

    fun saveApiKey(key: String) {
        val trimmed = key.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            container.settings.setUserApiKey(trimmed)
            mutableMessage.value = Message.API_KEY_SAVED
        }
    }

    fun clearApiKey() {
        viewModelScope.launch { container.settings.clearUserApiKey() }
    }

    fun syncNow() {
        viewModelScope.launch {
            busy.value = true
            val ok = container.sync.publish()
            busy.value = false
            mutableMessage.value = if (ok) Message.SYNC_DONE else Message.SYNC_FAILED
        }
    }

    /** URL / 動画 ID / 共有テキストからお気に入りに追加する。タイトルは Data API で補う（キーが無ければ ID のみ）。 */
    fun addFavoriteFromText(text: String) {
        val id = VideoIdParser.parse(text)
        if (id == null) {
            mutableMessage.value = Message.VIDEO_ID_INVALID
            return
        }
        viewModelScope.launch {
            if (container.favorites.isFavorite(id)) {
                mutableMessage.value = Message.FAVORITE_EXISTS
                return@launch
            }
            busy.value = true
            container.favorites.add(container.search.details(id))
            busy.value = false
            mutableMessage.value = Message.FAVORITE_ADDED
        }
    }

    fun removeFavorite(item: VideoItem) {
        viewModelScope.launch {
            container.favorites.remove(item.videoId)
            mutableMessage.value = Message.FAVORITE_REMOVED
        }
    }

    fun consumeMessage() {
        mutableMessage.value = null
    }

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L
        private const val CONNECTION_POLL_MILLIS = 10_000L

        fun factory(container: MobileContainer): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { SettingsViewModel(container) }
            }
    }
}
