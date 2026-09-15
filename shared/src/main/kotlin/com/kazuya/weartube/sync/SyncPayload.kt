package com.kazuya.weartube.sync

import com.kazuya.weartube.data.VideoItem
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Wearable Data Layer のパスとキー。スマホ → 時計と、時計 → スマホで別のパスを使う。 */
object SyncPaths {
    /** スマホからの設定とお気に入り。 */
    const val FROM_PHONE = "/weartube/from_phone"

    /** 時計で変更したお気に入り。 */
    const val FROM_WEAR = "/weartube/from_wear"
    const val KEY_JSON = "json"
    const val KEY_SENT_AT = "sentAt"
}

/**
 * 同期する内容。お気に入りは一覧を丸ごと送り、受け取った側は置き換える（後勝ち）。
 * [apiKey] はスマホ → 時計だけ。null なら触らない、空文字なら消す。
 */
@Serializable
data class SyncPayload(
    val apiKey: String? = null,
    val favorites: List<VideoItem> = emptyList(),
    /** 送信時刻（epoch ミリ秒）。古い更新で新しい内容を上書きしないために使う。 */
    val sentAtEpochMillis: Long,
) {
    fun toJson(): String = json.encodeToString(serializer(), this)

    companion object {
        private val json =
            Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
            }

        fun parse(text: String): SyncPayload = json.decodeFromString(serializer(), text)
    }
}
