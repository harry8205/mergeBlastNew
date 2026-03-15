package com.mergeblast

import android.app.Application
import androidx.room.Room
import com.mergeblast.data.repository.GameRepository
import com.mergeblast.data.repository.MergeBlastDatabase
import com.mergeblast.utils.SoundManager

class MergeBlastApp : Application() {

    lateinit var database: MergeBlastDatabase
        private set

    lateinit var repository: GameRepository
        private set

    lateinit var soundManager: SoundManager
        private set

    override fun onCreate() {
        super.onCreate()
        INSTANCE = this

        database = Room.databaseBuilder(
            applicationContext,
            MergeBlastDatabase::class.java,
            MergeBlastDatabase.DATABASE_NAME
        ).fallbackToDestructiveMigration().build()

        repository    = GameRepository(applicationContext, database)
        soundManager  = SoundManager(applicationContext)
    }

    companion object {
        lateinit var INSTANCE: MergeBlastApp
            private set
    }
}