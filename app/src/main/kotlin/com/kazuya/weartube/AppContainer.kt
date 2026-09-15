package com.kazuya.weartube

import android.content.Context
import com.kazuya.weartube.audio.AudioOutputMonitor
import com.kazuya.weartube.data.FavoritesRepository
import com.kazuya.weartube.data.PlayQueue
import com.kazuya.weartube.data.SearchRepository
import com.kazuya.weartube.data.SettingsRepository
import com.kazuya.weartube.data.YouTubeApi
import com.kazuya.weartube.net.Connectivity

/** 手動 DI の置き場。各 ViewModel はここから必要なものを受け取る。 */
class AppContainer(
    context: Context,
) {
    private val appContext = context.applicationContext

    val settings = SettingsRepository(appContext)
    val favorites = FavoritesRepository(appContext)
    val connectivity = Connectivity(appContext)
    val audioOutput = AudioOutputMonitor(appContext)
    val search = SearchRepository(YouTubeApi.create(), settings)

    /** 画面をまたいで「再生元リスト」を渡す（design.md 4.2 の ⏮ / ⏭ はこのリスト内を移動する）。 */
    val playQueue = PlayQueue()
}
