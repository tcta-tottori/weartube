package com.kazuya.weartube

import android.content.Context
import com.kazuya.weartube.audio.AudioOutputMonitor
import com.kazuya.weartube.data.FavoritesRepository
import com.kazuya.weartube.data.PlayQueue
import com.kazuya.weartube.data.SearchRepository
import com.kazuya.weartube.data.SettingsRepository
import com.kazuya.weartube.data.YouTubeApi
import com.kazuya.weartube.net.Connectivity
import com.kazuya.weartube.sync.SyncPaths
import com.kazuya.weartube.sync.WearSync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

/** 手動 DI の置き場。各 ViewModel はここから必要なものを受け取る。 */
class AppContainer(
    context: Context,
) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val settings = SettingsRepository(appContext, BuildConfig.YOUTUBE_API_KEY)
    val favorites = FavoritesRepository(appContext)
    val connectivity = Connectivity(appContext)
    val audioOutput = AudioOutputMonitor(appContext)
    val search = SearchRepository(YouTubeApi.create(), settings)

    /** 画面をまたいで「再生元リスト」を渡す（design.md 4.2 の ⏮ / ⏭ はこのリスト内を移動する）。 */
    val playQueue = PlayQueue()

    /** スマホからの設定・お気に入りを受け、時計で変えたお気に入りをスマホへ返す。 */
    val sync =
        WearSync(
            context = appContext,
            favorites = favorites,
            settings = settings,
            outgoingPath = SyncPaths.FROM_WEAR,
            incomingPath = SyncPaths.FROM_PHONE,
            sendsApiKey = false,
        )

    init {
        startSync()
    }

    /** 起動時にスマホの最新内容を取り込み、以後は時計側のお気に入りが変わるたびにスマホへ送る。 */
    @OptIn(FlowPreview::class)
    private fun startSync() {
        scope.launch {
            sync.applyLatest()
            favorites.favorites
                .distinctUntilChanged()
                // 起動直後の初期値は送らない（スマホから取り込んだ直後に送り返さない）
                .drop(1)
                .debounce(PUBLISH_DEBOUNCE_MILLIS)
                .collect { sync.publish() }
        }
    }

    private companion object {
        const val PUBLISH_DEBOUNCE_MILLIS = 500L
    }
}
