package com.kazuya.weartube.mobile.sync

import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.WearableListenerService
import com.kazuya.weartube.mobile.MobileApp
import kotlinx.coroutines.runBlocking

/** 時計で変更したお気に入りを受け取り、スマホ側の一覧を置き換える。 */
class PhoneDataListenerService : WearableListenerService() {
    override fun onDataChanged(dataEvents: DataEventBuffer) {
        val sync = (application as MobileApp).container.sync
        runBlocking { sync.applyEvents(dataEvents) }
    }
}
