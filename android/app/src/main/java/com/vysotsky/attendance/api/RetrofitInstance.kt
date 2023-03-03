package com.vysotsky.attendance.api

import com.vysotsky.attendance.API_URL
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {
    var api: Api =
        Retrofit.Builder()
            .baseUrl(API_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(Api::class.java)

    fun changeURL(newURL: String) {
        api = Retrofit.Builder()
            .baseUrl(newURL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(Api::class.java)
    }
}