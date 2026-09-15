package com.kazuya.weartube.mobile

import android.content.Context
import com.kazuya.weartube.data.FavoritesRepository
import com.kazuya.weartube.data.SearchRepository
import com.kazuya.weartube.data.SettingsRepository
import com.kazuya.weartube.data.YouTubeApi
import com.kazuya.weartube.sync.SyncPaths
import com.kazuya.weartube.sync.WearSync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

class MobileContainer(
    context: Context,
) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val settings = SettingsRepository(appContext, BuildConfig.YOUTUBE_API_KEY)
    val favorites = FavoritesRepository(appContext)
    val search = SearchRepository(YouTubeApi.create(), settings)

    /** スマホ → 時計に API キーとお気に入りを送り、時計で変えたお気に入りを受け取る。 */
    val sync =
        WearSync(
            context = appContext,
            favorites = favorites,
            settings = settings,
            outgoingPath = SyncPaths.FROM_PHONE,
            incomingPath = SyncPaths.FROM_WEAR,
            sendsApiKey = true,
        )

    init {
        startSync()
    }

    /** 起動時に時計側の最新お気に入りを取り込み、以後は変更のたびに時計へ送る。 */
    @OptIn(FlowPreview::class)
    private fun startSync() {
        scope.launch {
            sync.applyLatest()
            combine(favorites.favorites, settings.userApiKey) { favs, key -> favs to key }
                .distinctUntilChanged()
                .drop(1)
                .debounce(PUBLISH_DEBOUNCE_MILLIS)
                .collect { sync.publish() }
        }
    }

    private companion object {
        const val PUBLISH_DEBOUNCE_MILLIS = 500L
    }
}
