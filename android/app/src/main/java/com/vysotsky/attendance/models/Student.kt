package com.vysotsky.attendance.models

import java.io.Serializable

data class Student(
    val email: String?,
    val first_name: String,
    val second_name: String
) : Serializable