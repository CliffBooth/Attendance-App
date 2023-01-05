package com.vysotsky.attendance

import java.io.InputStream
import java.util.UUID

var PORT = 7000
var API_URL = "http://192.168.0.106:${PORT}"
var randomLocation = false
const val T = "myTag"
var debug = false
var polling = true
val englishQRRegex = Regex("^[A-Za-z]+:[A-Za-z]+:\\w+:((-?[\\w\\.]+---?[\\w\\.]+)|null)\$")

val BLUETOOTH_UUID = UUID.fromString("e26963a7-c58d-448d-a9ab-3b33bc4a9c0e")

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