package com.vysotsky.attendance.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface PredefinedClassDao {
    @Query("SELECT * FROM PredefinedClassDB")
    suspend fun getAll(): List<PredefinedClassDB>

    @Upsert
    suspend fun insert(value: PredefinedClassDB)

    @Upsert
    suspend fun insertAll(values: List<PredefinedClassDB>)
}