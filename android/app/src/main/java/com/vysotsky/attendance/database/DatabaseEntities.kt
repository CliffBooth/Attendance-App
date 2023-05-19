package com.vysotsky.attendance.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.vysotsky.attendance.api.PredefinedClassToSend
import com.vysotsky.attendance.api.Student

@Entity
data class Class (
    val date: Long,
    val subjectName: String,
    val students: ArrayList<Student>,
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
)

@Entity
data class PredefinedClassDB(
    val subjectName: String,
    val students: ArrayList<Student>,
    val updatedAt: Long,
    @ColumnInfo(defaultValue = "any")
    val method: String = "any",
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
) {
    fun asNetworkModel() = PredefinedClassToSend(
        studentList = this.students,
        method = this.method,
        subjectName = this.subjectName
    )
}