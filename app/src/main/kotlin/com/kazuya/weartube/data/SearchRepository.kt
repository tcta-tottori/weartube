package com.kazuya.weartube.data

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException

/** 検索の失敗理由。UI は 1〜2 行の日本語に置き換える（CLAUDE.md エラー時の振る舞い）。 */
sealed class SearchError {
    /** APIキーが未設定。 */
    data object NoApiKey : SearchError()

    /** 1 日のクォータを使い切った。検索を止めてお気に入り再生のみに縮退する。 */
    data object QuotaExceeded : SearchError()

    /** キーが無効、または API が有効化されていない。 */
    data object InvalidKey : SearchError()

    data object Network : SearchError()

    data object Unknown : SearchError()
}

sealed class SearchResult {
    data class Success(
        val items: List<VideoItem>,
    ) : SearchResult()

    data class Failure(
        val error: SearchError,
    ) : SearchResult()
}

class SearchRepository(
    private val api: YouTubeApi,
    private val settings: SettingsRepository,
) {
    private val json = Json { ignoreUnknownKeys = true }

    /** クォータ超過を検出したら、アプリを起動し直すまで検索を止める。 */
    @Volatile
    var quotaExceeded: Boolean = false
        private set

    suspend fun search(query: String): SearchResult {
        val key = settings.currentApiKey()
        if (!key.isSet) return SearchResult.Failure(SearchError.NoApiKey)
        if (quotaExceeded) return SearchResult.Failure(SearchError.QuotaExceeded)
        return try {
            val response = api.search(query = query, apiKey = key.key)
            SearchResult.Success(response.items.mapNotNull { it.toVideoItem() })
        } catch (e: HttpException) {
            SearchResult.Failure(classify(e))
        } catch (e: IOException) {
            SearchResult.Failure(SearchError.Network)
        }
    }

    private fun classify(e: HttpException): SearchError {
        val body =
            e
                .response()
                ?.errorBody()
                ?.string()
                .orEmpty()
        val reasons =
            try {
                json
                    .decodeFromString<ApiErrorBody>(body)
                    .error.errors
                    .map { it.reason }
            } catch (ex: SerializationException) {
                emptyList()
            } catch (ex: IllegalArgumentException) {
                emptyList()
            }
        return when {
            reasons.any { it == "quotaExceeded" || it == "dailyLimitExceeded" || it == "rateLimitExceeded" } -> {
                quotaExceeded = true
                SearchError.QuotaExceeded
            }

            e.code() == 400 || e.code() == 403 -> {
                SearchError.InvalidKey
            }

            else -> {
                SearchError.Unknown
            }
        }
    }
}
