package com.vysotsky.attendance.api

import com.vysotsky.attendance.API_URL
import okhttp3.OkHttpClient
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

/**
    when logged in, store token in sharedPreferences

 * if when trying to make request we don't have a token in sharedPreferences, then open login screen.
if we have token, but it returns "invalid token" also close everything and open login screen.
 *
 */