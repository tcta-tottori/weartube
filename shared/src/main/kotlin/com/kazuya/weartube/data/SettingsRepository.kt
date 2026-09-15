package com.kazuya.weartube.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore(name = "weartube_settings")

/** API キーの出所。設定画面の表示に使う。 */
enum class ApiKeySource { NONE, BUILD_CONFIG, USER }

data class ApiKeyState(
    val key: String,
    val source: ApiKeySource,
) {
    val isSet: Boolean get() = key.isNotBlank()
}

/**
 * design.md 6 の `youtube_api_key`。設定画面（またはスマホから同期）で入れたキーがあればそれを、
 * 無ければビルド時に注入したキー [buildTimeApiKey] を使う。
 */
class SettingsRepository(
    private val context: Context,
    private val buildTimeApiKey: String,
) {
    private val store get() = context.settingsStore

    val apiKey: Flow<ApiKeyState> =
        store.data.map { prefs ->
            val user = prefs[Keys.API_KEY].orEmpty()
            when {
                user.isNotBlank() -> ApiKeyState(user, ApiKeySource.USER)
                buildTimeApiKey.isNotBlank() -> ApiKeyState(buildTimeApiKey, ApiKeySource.BUILD_CONFIG)
                else -> ApiKeyState("", ApiKeySource.NONE)
            }
        }

    suspend fun currentApiKey(): ApiKeyState = apiKey.first()

    suspend fun setUserApiKey(key: String) {
        store.edit { it[Keys.API_KEY] = key.trim() }
    }

    suspend fun clearUserApiKey() {
        store.edit { it.remove(Keys.API_KEY) }
    }

    /** 設定画面で入れたキーそのもの（ビルド時キーは含まない）。スマホ → 時計の同期に使う。 */
    val userApiKey: Flow<String> = store.data.map { it[Keys.API_KEY].orEmpty() }

    /** スマホ ⇔ 時計で最後に同期した時刻（epoch ミリ秒）。未同期なら null。 */
    val lastSyncAt: Flow<Long?> = store.data.map { it[Keys.LAST_SYNC_AT] }

    suspend fun setLastSyncAt(epochMillis: Long) {
        store.edit { it[Keys.LAST_SYNC_AT] = epochMillis }
    }

    private object Keys {
        val API_KEY = stringPreferencesKey("youtube_api_key")
        val LAST_SYNC_AT = longPreferencesKey("last_sync_at")
    }
}
