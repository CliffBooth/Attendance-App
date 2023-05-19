package com.vysotsky.attendance.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

//TODO: delete everything when loggin out?
@Dao
interface ClassDao {
    @Upsert
    suspend fun insertClass(cl: Class)

    @Upsert
    suspend fun insertAll(values: List<Class>)

    @Query("Select * from class")
    suspend fun getAll(): List<Class>

    @Query("Delete from class")
    suspend fun clear()
}