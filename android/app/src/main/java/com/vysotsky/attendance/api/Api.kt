package com.vysotsky.attendance.api

import com.vysotsky.attendance.models.ProfessorData
import com.vysotsky.attendance.models.Session
import com.vysotsky.attendance.models.StudentData
import com.vysotsky.attendance.models.StudentLoginData
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path


interface Api {
    //professor
    @GET("api/professor_classes/{email}")
    suspend fun getProfessorSessions(@Path("email") email: String): Response<List<Session>>

    @PUT("api/professor_classes/{email}")
    suspend fun addSession(@Path("email") email: String, @Body session: Session): Response<Session>

    @POST("api/login_professor")
    suspend fun login(@Body professorData: ProfessorData): Response<Void>

    @POST("api/signup_professor")
    suspend fun signup(@Body professorData: ProfessorData): Response<Void>

    //student
    @GET("api/student_classes/{email}")
    suspend fun getStudentSessions(@Path("email") email: String): Response<List<Session>>

    @POST("api/login_student")
    suspend fun login(@Body studentData: StudentLoginData): Response<Void>

    @POST("api/signup_student")
    suspend fun signup(@Body studentData: StudentData): Response<Void>

}
