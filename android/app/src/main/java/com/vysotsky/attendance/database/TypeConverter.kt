package com.vysotsky.attendance.database

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.vysotsky.attendance.api.Student

class TypeConverter {
    @TypeConverter
    fun toJson(value: ArrayList<Student>): String {
        return Gson().toJson(value)
    }

    @TypeConverter
    fun fromJson(value: String): ArrayList<Student> {
        return try {
            Gson().fromJson(value, object: TypeToken<ArrayList<Student>>() {}.type)
        } catch (e: Exception) {
            arrayListOf()
        }
    }
}