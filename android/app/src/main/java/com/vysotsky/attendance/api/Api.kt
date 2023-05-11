package com.vysotsky.attendance.api

import com.vysotsky.attendance.models.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path


interface Api {
    //professor
    @GET("api/professor_classes/{email}")
    suspend fun getProfessorSessions(@Path("email") email: String, @Header("Authorization") token: String): Response<List<Session>>

    @POST("api/professor_classes/{email}")
    suspend fun addSession(@Path("email") email: String, @Body session: Session, @Header("Authorization") token: String): Response<Session>

    @POST("api/login_professor")
    suspend fun login(@Body professorData: ProfessorAuthData): Response<AuthResult>

    @POST("api/signup_professor")
    suspend fun signup(@Body professorData: ProfessorAuthData): Response<AuthResult>

    @POST("/current-students")
    suspend fun getCurrentStudentsList(@Body professorData: ProfessorData): Response<CurrentStudentListResponse>

    @POST("/end")
    suspend fun endSession(@Body professorData: ProfessorData): Response<List<Student>>

    //student
    @GET("api/student_classes/{email}")
    suspend fun getStudentSessions(@Path("email") email: String): Response<List<StudentClass>>

    @POST("api/login_student")
    suspend fun login(@Body studentData: StudentLoginData): Response<Student>

    @POST("api/signup_student")
    suspend fun signup(@Body studentData: Student): Response<Void>

    @POST("/student-qr-code")
    suspend fun sendQRCode(@Body body: SendQrCodeBody): Response<Void>

}

data class SendQrCodeBody(val qrCode: String, val data: String)

data class CurrentStudentListResponse(val subjectName: String, val students: List<Student>)
