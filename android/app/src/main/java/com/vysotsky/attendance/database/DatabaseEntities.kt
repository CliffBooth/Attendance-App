package com.vysotsky.attendance.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.vysotsky.attendance.api.Student

@Entity
data class Class (
    val date: Long,
    val subjectName: String,
    val students: ArrayList<Student>,
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
)