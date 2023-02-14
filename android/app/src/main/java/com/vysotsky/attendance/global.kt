package com.vysotsky.attendance

import okhttp3.OkHttpClient
import java.io.InputStream
import java.util.UUID
import kotlin.random.Random

var PORT = 7000
var API_URL = "http://192.168.0.106:${PORT}"
var randomLocation = false
const val T = "myTag"
var debug = false
var polling = true
val englishQRRegex = Regex("^[A-Za-z]+:[A-Za-z]+:\\w+:((-?[\\w\\.]+---?[\\w\\.]+)|null)\$")
val httpClient = OkHttpClient()

val BLUETOOTH_UUID = UUID.fromString("e26963a7-c58d-448d-a9ab-3b33bc4a9c0e")

//for connections api
val SERVICE_ID = "com.vysotsky.attendance.SERVICE_ID"
//val userName = "attendance" //name might need to be random
fun getName(): String {
    var result = ""
    repeat(5) {
        result += Random.nextInt(10)
    }
    return result
}

fun getResponse(inputStream: InputStream): String {
    val br = inputStream.bufferedReader()
    val sb = StringBuilder()
    var s = br.readLine()
    while (s != null) {
        sb.append(s)
        s = br.readLine()
    }
    return sb.toString()
}