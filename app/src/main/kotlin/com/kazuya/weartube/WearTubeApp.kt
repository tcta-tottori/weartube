package com.kazuya.weartube

import android.app.Application

/** DI は使わず、アプリ全体で共有するインスタンスを [AppContainer] にまとめて持つ（CLAUDE.md 技術方針）。 */
class WearTubeApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
