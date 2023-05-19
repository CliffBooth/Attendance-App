package com.vysotsky.attendance.student.home

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import com.vysotsky.attendance.databinding.ActivityStudentAttendanceBinding
import com.vysotsky.attendance.api.StudentClass
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Date
import java.util.Locale

class StudentAttendanceActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStudentAttendanceBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val classes = intent.extras?.getSerializable(EXTRA_CLASSES_KEY) as List<StudentClass>
        binding = ActivityStudentAttendanceBinding.inflate(layoutInflater)

        //adapter
        binding.title.text = classes[0].subjectName
        val dates = classes.map{cl -> formatDate(cl.date)}
        setUpRecyclerView(dates)
        setContentView(binding.root)
    }

    //data = list of strings to display inside items
    private fun setUpRecyclerView(data: List<String>) = binding.rv.apply {
        setHasFixedSize(true)
        adapter = ClassesAdapter()
        (adapter as ClassesAdapter).classes = data
        layoutManager = LinearLayoutManager(this@StudentAttendanceActivity)
    }

    private fun formatDate(dateStr: String): String {
        val instant = Instant.parse(dateStr)
        val date = Date.from(instant)
        val format = SimpleDateFormat("dd.MM.yy", Locale.getDefault())
        return format.format(date)
    }

    companion object {
        const val EXTRA_CLASSES_KEY = "com.vysotsky.attendance.student.StudentAttendanceActivity.classes"
    }
}