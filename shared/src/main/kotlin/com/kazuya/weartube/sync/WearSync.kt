package com.kazuya.weartube.sync

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataItem
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.kazuya.weartube.data.FavoritesRepository
import com.kazuya.weartube.data.SettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await

/**
 * スマホ ⇔ 時計の同期（Wearable Data Layer）。送る側・受ける側で同じクラスを使い、パスだけ変える。
 *
 * - 送信: 現在のお気に入り（とスマホならユーザー入力の API キー）を [outgoingPath] に置く
 * - 受信: [incomingPath] の最新項目を DataStore に書き込む。内容が同じなら DataStore は書き込まないので、
 *   受け取った内容をそのまま送り返してもループしない
 * - 時計が無い／Play 開発者サービスが無い端末では失敗するが、無視してよい
 */
class WearSync(
    context: Context,
    private val favorites: FavoritesRepository,
    private val settings: SettingsRepository,
    private val outgoingPath: String,
    private val incomingPath: String,
    /** スマホ側だけ true。ユーザーが入れた API キーを一緒に送る。 */
    private val sendsApiKey: Boolean,
) {
    private val appContext = context.applicationContext
    private val dataClient: DataClient get() = Wearable.getDataClient(appContext)

    /** 直近に適用した相手側の送信時刻。これより古い項目は捨てる。 */
    @Volatile
    private var lastAppliedSentAt = 0L

    /** 現在の内容を相手に送る。成功したら true。 */
    suspend fun publish(): Boolean {
        val sentAt = System.currentTimeMillis()
        val payload =
            SyncPayload(
                apiKey = if (sendsApiKey) settings.userApiKey.first() else null,
                favorites = favorites.favorites.first(),
                sentAtEpochMillis = sentAt,
            )
        val request =
            PutDataMapRequest.create(outgoingPath).apply {
                dataMap.putString(SyncPaths.KEY_JSON, payload.toJson())
                dataMap.putLong(SyncPaths.KEY_SENT_AT, sentAt)
            }
        return runCatching { dataClient.putDataItem(request.asPutDataRequest().setUrgent()).await() }
            .onSuccess { settings.setLastSyncAt(sentAt) }
            .onFailure { Log.w(TAG, "同期の送信に失敗（相手が未接続なら無視してよい）: ${it.message}") }
            .isSuccess
    }

    /** 相手側の最新項目を取り込む（起動時用）。取り込んだら true。 */
    suspend fun applyLatest(): Boolean {
        val uri = Uri.parse("wear://*$incomingPath")
        val buffer =
            runCatching { dataClient.getDataItems(uri).await() }
                .onFailure { Log.w(TAG, "同期データを読めません: ${it.message}") }
                .getOrNull() ?: return false
        val newest =
            try {
                buffer.map { it.freeze() }.maxByOrNull { DataMapItem.fromDataItem(it).dataMap.getLong(SyncPaths.KEY_SENT_AT) }
            } finally {
                buffer.release()
            }
        return newest?.let { apply(it) } ?: false
    }

    /** WearableListenerService の onDataChanged から呼ぶ。取り込んだ項目があれば true。 */
    suspend fun applyEvents(events: DataEventBuffer): Boolean {
        var applied = false
        events.forEach { event ->
            if (event.type == DataEvent.TYPE_CHANGED && event.dataItem.uri.path == incomingPath) {
                if (apply(event.dataItem)) applied = true
            }
        }
        return applied
    }

    private suspend fun apply(item: DataItem): Boolean {
        val json = DataMapItem.fromDataItem(item).dataMap.getString(SyncPaths.KEY_JSON) ?: return false
        val payload =
            runCatching { SyncPayload.parse(json) }
                .onFailure { Log.w(TAG, "同期データを解釈できません: ${it.message}") }
                .getOrNull() ?: return false
        if (payload.sentAtEpochMillis <= lastAppliedSentAt) return false
        lastAppliedSentAt = payload.sentAtEpochMillis
        favorites.replaceAll(payload.favorites)
        payload.apiKey?.let { key -> if (key.isBlank()) settings.clearUserApiKey() else settings.setUserApiKey(key) }
        settings.setLastSyncAt(System.currentTimeMillis())
        return true
    }

    /** 相手の端末が Bluetooth で接続されているか。 */
    suspend fun isPeerConnected(): Boolean =
        runCatching {
            Wearable
                .getNodeClient(appContext)
                .connectedNodes
                .await()
                .isNotEmpty()
        }.getOrDefault(false)

    private companion object {
        const val TAG = "WearSync"
    }
}
