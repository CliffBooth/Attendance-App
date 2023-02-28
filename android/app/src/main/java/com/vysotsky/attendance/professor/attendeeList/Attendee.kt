package com.vysotsky.attendance.professor.attendeeList

//import com.vysotsky.attendance.util.Student

data class Attendee(
    val firstName: String,
    val secondName: String,
    val id: String?, //android_id
    /*val geoLocation: GeoLocation?,*/
    var status: Status = Status.OK
) {
//    constructor(student: Student, status: Status) :
//            this(student.firstName, student.secondName, status)
}

data class GeoLocation(val longitude: Double, val latitude: Double)

enum class Status {
    OK, NO_DATA, OUT_OF_RANGE
}