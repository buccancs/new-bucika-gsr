package com.topdon.commons.base.entity

import android.content.Context
import android.content.ContextWrapper
import android.database.DatabaseErrorHandler
import android.database.sqlite.SQLiteDatabase
import androidx.annotation.NonNull
import java.io.File

class DatabaseContext(base: Context, @NonNull private val dbDir: File) : ContextWrapper(base) {
    
    init {
        require(dbDir != null) { "dbDir is null" }
    }
    
    override fun getDatabasePath(name: String): File {
        if (!dbDir.exists()) {
            dbDir.mkdirs()
        }
        return File(dbDir, name)
    }
    
    override fun openOrCreateDatabase(
        name: String,
        mode: Int,
        factory: SQLiteDatabase.CursorFactory?,
        errorHandler: DatabaseErrorHandler?
    ): SQLiteDatabase {
        return SQLiteDatabase.openOrCreateDatabase(getDatabasePath(name), factory)
    }
    
    override fun openOrCreateDatabase(
        name: String,
        mode: Int,
        factory: SQLiteDatabase.CursorFactory?
    ): SQLiteDatabase {
        return super.openOrCreateDatabase(getDatabasePath(name).name, mode, factory)
    }
}
