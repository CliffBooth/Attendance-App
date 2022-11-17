package com.vysotsky.attendance.camera

data class Attendee(val firstName: String, val secondName: String, val geoLocation: GeoLocation?) {
    constructor(firstName: String, secondName: String) : this(firstName, secondName, null)
}

data class GeoLocation(val longitude: Double, val latitude: Double)