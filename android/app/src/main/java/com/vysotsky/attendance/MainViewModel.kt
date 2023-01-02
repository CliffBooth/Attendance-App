package com.vysotsky.attendance

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MainViewModel: ViewModel() {
    val debug = MutableLiveData(false)
}