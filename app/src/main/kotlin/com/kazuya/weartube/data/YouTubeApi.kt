package com.kazuya.weartube.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

/** YouTube Data API v3 の search.list だけを使う（1 回 100 units）。 */
interface YouTubeApi {
    @GET("youtube/v3/search")
    suspend fun search(
        @Query("q") query: String,
        @Query("key") apiKey: String,
        @Query("part") part: String = "snippet",
        @Query("type") type: String = "video",
        // IFrame で再生できない動画を最初から除く
        @Query("videoEmbeddable") embeddable: String = "true",
        @Query("videoSyndicated") syndicated: String = "true",
        @Query("maxResults") maxResults: Int = 15,
        @Query("safeSearch") safeSearch: String = "none",
        @Query("regionCode") regionCode: String = "JP",
        @Query("relevanceLanguage") relevanceLanguage: String = "ja",
    ): SearchResponse

    companion object {
        private const val BASE_URL = "https://www.googleapis.com/"

        fun create(): YouTubeApi {
            val json = Json { ignoreUnknownKeys = true }
            val client =
                OkHttpClient
                    .Builder()
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(20, TimeUnit.SECONDS)
                    .build()
            return Retrofit
                .Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()
                .create(YouTubeApi::class.java)
        }
    }
}

@Serializable
data class SearchResponse(
    val items: List<SearchItem> = emptyList(),
)

@Serializable
data class SearchItem(
    val id: SearchId = SearchId(),
    val snippet: Snippet = Snippet(),
) {
    fun toVideoItem(): VideoItem? {
        val videoId = id.videoId ?: return null
        return VideoItem(
            videoId = videoId,
            title = snippet.title,
            channelTitle = snippet.channelTitle,
            thumbnailUrl =
                snippet.thumbnails.default?.url ?: snippet.thumbnails.medium
                    ?.url
                    .orEmpty(),
            isLive = snippet.liveBroadcastContent == "live",
        )
    }
}

@Serializable
data class SearchId(
    val kind: String = "",
    val videoId: String? = null,
)

@Serializable
data class Snippet(
    val title: String = "",
    val channelTitle: String = "",
    val thumbnails: Thumbnails = Thumbnails(),
    val liveBroadcastContent: String = "none",
)

@Serializable
data class Thumbnails(
    val default: Thumbnail? = null,
    val medium: Thumbnail? = null,
)

@Serializable
data class Thumbnail(
    val url: String = "",
)

/** Data API のエラー本文（HTTP 4xx のとき）。quotaExceeded の判定に使う。 */
@Serializable
data class ApiErrorBody(
    val error: ApiError = ApiError(),
)

@Serializable
data class ApiError(
    val code: Int = 0,
    val message: String = "",
    val errors: List<ApiErrorDetail> = emptyList(),
)

@Serializable
data class ApiErrorDetail(
    val reason: String = "",
)
