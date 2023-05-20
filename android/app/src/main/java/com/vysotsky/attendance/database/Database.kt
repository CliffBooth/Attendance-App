package com.vysotsky.attendance.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [Class::class, PredefinedClassDB::class],
    version = 5,
    exportSchema = false,
)
@TypeConverters(TypeConverter::class)
abstract class AttendanceDatabase : RoomDatabase() {
    abstract val classDao: ClassDao
    abstract val predefinedClassDao: PredefinedClassDao
}

private lateinit var INSTANCE: AttendanceDatabase

fun getDatabase(context: Context): AttendanceDatabase {
    synchronized(AttendanceDatabase::class.java) {
        if (!::INSTANCE.isInitialized) {
            INSTANCE = Room.databaseBuilder(
                context.applicationContext,
                AttendanceDatabase::class.java,
                "class.db"
            )
                .fallbackToDestructiveMigration()
                .build()
        }
    }
    return INSTANCE
}