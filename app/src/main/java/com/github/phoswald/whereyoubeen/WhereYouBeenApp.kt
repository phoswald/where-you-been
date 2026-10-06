package com.github.phoswald.whereyoubeen

import android.app.Application

class WhereYouBeenApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
