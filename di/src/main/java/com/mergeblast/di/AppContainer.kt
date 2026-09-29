package com.mergeblast.di

import android.content.Context
import androidx.room.Room
import com.mergeblast.data.repository.GameRepository
import com.mergeblast.data.repository.MergeBlastDatabase
import com.mergeblast.utils.SoundManager

class AppContainer(context: Context) {
    val database: MergeBlastDatabase = Room.databaseBuilder(
        context.applicationContext,
        MergeBlastDatabase::class.java,
        MergeBlastDatabase.DATABASE_NAME
    ).addMigrations(MergeBlastDatabase.MIGRATION_1_2).build()

    val repository = GameRepository(context.applicationContext, database)
    val soundManager = SoundManager(context.applicationContext)
}
