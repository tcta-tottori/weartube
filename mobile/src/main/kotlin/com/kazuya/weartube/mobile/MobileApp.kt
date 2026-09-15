package com.kazuya.weartube.mobile

import android.app.Application

/** 手動 DI。共有インスタンスは [MobileContainer] にまとめる。 */
class MobileApp : Application() {
    lateinit var container: MobileContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = MobileContainer(this)
    }
}
