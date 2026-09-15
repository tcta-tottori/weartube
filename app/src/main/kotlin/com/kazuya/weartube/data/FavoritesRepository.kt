package com.kazuya.weartube.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

private val Context.favoritesStore: DataStore<Preferences> by preferencesDataStore(name = "weartube_favorites")

/** design.md 6 の `favorites`。JSON 文字列 1 本で保存する（件数は個人利用の範囲なので十分）。 */
class FavoritesRepository(
    private val context: Context,
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val store get() = context.favoritesStore

    /** 追加が新しい順。 */
    val favorites: Flow<List<VideoItem>> = store.data.map { decode(it[Keys.FAVORITES]) }

    suspend fun isFavorite(videoId: String): Boolean = favorites.first().any { it.videoId == videoId }

    suspend fun add(item: VideoItem) {
        store.edit { prefs ->
            val current = decode(prefs[Keys.FAVORITES]).filterNot { it.videoId == item.videoId }
            prefs[Keys.FAVORITES] = json.encodeToString(listOf(item.copy(addedAt = System.currentTimeMillis())) + current)
        }
    }

    suspend fun remove(videoId: String) {
        store.edit { prefs ->
            prefs[Keys.FAVORITES] = json.encodeToString(decode(prefs[Keys.FAVORITES]).filterNot { it.videoId == videoId })
        }
    }

    /** 登録済みなら削除、未登録なら追加。追加したとき true。 */
    suspend fun toggle(item: VideoItem): Boolean {
        val added = !isFavorite(item.videoId)
        if (added) add(item) else remove(item.videoId)
        return added
    }

    /** URL 入力などでタイトルが空のまま登録した動画に、プレーヤーから取れたタイトルを補う。 */
    suspend fun updateTitle(
        videoId: String,
        title: String,
    ) {
        if (title.isBlank()) return
        store.edit { prefs ->
            val current = decode(prefs[Keys.FAVORITES])
            if (current.none { it.videoId == videoId && it.title != title }) return@edit
            prefs[Keys.FAVORITES] = json.encodeToString(current.map { if (it.videoId == videoId) it.copy(title = title) else it })
        }
    }

    private fun decode(raw: String?): List<VideoItem> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            json.decodeFromString<List<VideoItem>>(raw)
        } catch (e: SerializationException) {
            emptyList()
        }
    }

    private object Keys {
        val FAVORITES = stringPreferencesKey("favorites")
    }
}
