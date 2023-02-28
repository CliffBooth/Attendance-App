package com.vysotsky.attendance.models

import java.io.Serializable

data class Session (
    val date: String,
    val students: List<Student>,
    val subject_name: String
): Serializable