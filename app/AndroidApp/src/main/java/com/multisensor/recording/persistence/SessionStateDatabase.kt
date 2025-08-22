package com.multisensor.recording.persistence

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        SessionState::class,
        ShimmerDeviceState::class,
        ShimmerConnectionHistory::class
    ],
    version = 2,
    exportSchema = false
)
abstract class SessionStateDatabase : RoomDatabase() {

    abstract fun sessionStateDao(): SessionStateDao
    abstract fun shimmerDeviceStateDao(): ShimmerDeviceStateDao

    companion object {
        @Volatile
        private var INSTANCE: SessionStateDatabase? = null

        private const val DATABASE_NAME = "session_state_database"

        fun getDatabase(context: Context): SessionStateDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SessionStateDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
