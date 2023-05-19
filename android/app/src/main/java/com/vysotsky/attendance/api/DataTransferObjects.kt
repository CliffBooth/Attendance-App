package com.vysotsky.attendance.api

import java.io.Serializable

data class AuthResult(
    val token: String,
)

data class ProfessorAuthData(
    val email: String,
    val password: String
)

data class ProfessorData(
    val email: String,
)

data class Session(
    val date: Long,
    val students: List<Student>,
    val subjectName: String
) : Serializable

data class Student(
    val phoneId: String?,
    val firstName: String,
    val secondName: String
) : Serializable

data class StudentClass(
    val date: String,
    val subjectName: String
) : Serializable

data class StudentLoginData(
    val phoneId: String
)