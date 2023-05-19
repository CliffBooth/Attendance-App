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

data class ManualStudentData(
    val email: String,
    val student: Student
)

data class PredefinedClass(
    val updatedAt: Long,
    val subjectName: String,
    val method: Method,
    val students: List<Student>,
) {
    data class Method(val name: String)
    fun asDatabaseModel() = com.vysotsky.attendance.database.PredefinedClassDB(
        subjectName = this.subjectName,
        students = ArrayList(this.students),
        method = this.method.name,
        updatedAt = this.updatedAt
    )
}

data class PredefinedClassToSend(
    val studentList: List<Student>,
    val method: String,
    val subjectName: String,
)

