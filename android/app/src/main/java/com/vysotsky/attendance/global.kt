package com.vysotsky.attendance

import java.io.InputStream

var PORT = 7000
var API_URL = "http://192.168.0.106:${PORT}"
var randomLocation = false
const val T = "myTag"
var debug = false
var polling = true

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