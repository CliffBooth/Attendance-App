package com.vysotsky.attendance.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [Class::class],
    version = 2,
    exportSchema = false,
)
@TypeConverters(TypeConverter::class)
abstract class ClassDatabase : RoomDatabase() {
    abstract val dao: ClassDao
}

private lateinit var INSTANCE: ClassDatabase

fun getDatabase(context: Context): ClassDatabase {
    synchronized(ClassDatabase::class.java) {
        if (!::INSTANCE.isInitialized) {
            INSTANCE = Room.databaseBuilder(
                context.applicationContext,
                ClassDatabase::class.java,
                "class.db"
            )
                .fallbackToDestructiveMigration()
                .build()
        }
    }
    return INSTANCE
}