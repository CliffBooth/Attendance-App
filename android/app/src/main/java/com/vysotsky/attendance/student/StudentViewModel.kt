package com.vysotsky.attendance.student

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import com.vysotsky.attendance.R

class StudentViewModel: ViewModel() {
    lateinit var firstName: String
    lateinit var secondName: String
    lateinit var deviceID: String
}