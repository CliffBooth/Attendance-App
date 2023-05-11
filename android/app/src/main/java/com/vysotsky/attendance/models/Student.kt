package com.vysotsky.attendance.models

import java.io.Serializable

data class Student(
    val phoneId: String?,
    val firstName: String,
    val secondName: String
) : Serializable