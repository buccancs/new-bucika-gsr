package com.multisensor.recording.persistence

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PersistenceModule {

    @Provides
    @Singleton
    fun provideSessionStateDatabase(@ApplicationContext context: Context): SessionStateDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            SessionStateDatabase::class.java,
            "session_state_database"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideSessionStateDao(database: SessionStateDatabase): SessionStateDao {
        return database.sessionStateDao()
    }

    @Provides
    fun provideShimmerDeviceStateDao(database: SessionStateDatabase): ShimmerDeviceStateDao {
        return database.shimmerDeviceStateDao()
    }

    @Provides
    @Singleton
    fun provideShimmerDeviceStateRepository(@ApplicationContext context: Context): ShimmerDeviceStateRepository {
        return ShimmerDeviceStateRepository(context)
    }
}
