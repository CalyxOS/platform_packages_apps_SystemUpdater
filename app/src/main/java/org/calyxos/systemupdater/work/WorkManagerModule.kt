package org.calyxos.systemupdater.work

import android.content.Context
import androidx.work.WorkManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object WorkManagerModule {

    /**
     * Provides an instance of UpdateEngine
     */
    @Singleton
    @Provides
    fun provideWorkManagerInstance(@ApplicationContext context: Context): WorkManager {
        return WorkManager.getInstance(context)
    }
}
