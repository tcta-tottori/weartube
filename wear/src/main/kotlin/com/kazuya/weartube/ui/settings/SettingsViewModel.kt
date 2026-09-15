package com.kazuya.weartube.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kazuya.weartube.data.ApiKeySource
import com.kazuya.weartube.data.FavoritesRepository
import com.kazuya.weartube.data.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val apiKeySource: ApiKeySource = ApiKeySource.NONE,
    /** 表示用にキーの末尾 4 文字だけ。 */
    val apiKeyTail: String = "",
    val favoriteCount: Int = 0,
    /** スマホと最後に同期した時刻（epoch ミリ秒）。未同期なら null。 */
    val lastSyncAt: Long? = null,
)

class SettingsViewModel(
    private val settings: SettingsRepository,
    favorites: FavoritesRepository,
) : ViewModel() {
    val uiState: StateFlow<SettingsUiState> =
        combine(settings.apiKey, favorites.favorites, settings.lastSyncAt) { key, favs, syncAt ->
            SettingsUiState(apiKeySource = key.source, apiKeyTail = key.key.takeLast(4), favoriteCount = favs.size, lastSyncAt = syncAt)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), SettingsUiState())

    fun setApiKey(key: String) {
        viewModelScope.launch { settings.setUserApiKey(key) }
    }

    fun clearApiKey() {
        viewModelScope.launch { settings.clearUserApiKey() }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
