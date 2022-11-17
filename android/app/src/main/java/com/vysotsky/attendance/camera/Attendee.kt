package com.vysotsky.attendance.camera

data class Attendee(
    val firstName: String,
    val secondName: String,
    /*val geoLocation: GeoLocation?,*/
    var status: Status
)

data class GeoLocation(val longitude: Double, val latitude: Double)

enum class Status {
    OK, NO_DATA, OUT_OF_RANGE
}