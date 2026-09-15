package com.kazuya.weartube.sync

import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.WearableListenerService
import com.kazuya.weartube.WearTubeApp
import kotlinx.coroutines.runBlocking

/** スマホから同期された API キーとお気に入りを受け取り、時計側の DataStore を置き換える。 */
class WearDataListenerService : WearableListenerService() {
    override fun onDataChanged(dataEvents: DataEventBuffer) {
        val sync = (application as WearTubeApp).container.sync
        // コールバックはバックグラウンドスレッド。短い DataStore 書き込みなので同期的に待つ
        runBlocking { sync.applyEvents(dataEvents) }
    }
}
