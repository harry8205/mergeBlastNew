package com.mergeblast

import android.app.Application
import com.mergeblast.di.AppContainer

class MergeBlastApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        INSTANCE = this

        container = AppContainer(applicationContext)
    }

    companion object {
        lateinit var INSTANCE: MergeBlastApp
            private set
    }
}
