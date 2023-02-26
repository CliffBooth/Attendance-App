package com.vysotsky.attendance.models

data class Session (
    val date: String,
    val students: List<Student>,
    val subject_name: String
)