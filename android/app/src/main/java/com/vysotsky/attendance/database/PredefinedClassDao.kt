package com.vysotsky.attendance.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.runInterruptible

@Dao
interface PredefinedClassDao {
    @Query("SELECT * FROM PredefinedClassDB")
    suspend fun getAll(): List<PredefinedClassDB>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(value: PredefinedClassDB)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(values: List<PredefinedClassDB>)

    @Query("Delete from PredefinedClassDB")
    suspend fun clear()
}