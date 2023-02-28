package com.vysotsky.attendance.professor

import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams
import android.widget.TableRow
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.setPadding
import com.vysotsky.attendance.R
import com.vysotsky.attendance.TAG
import com.vysotsky.attendance.databinding.ActivityClassAttendanceBinding
import com.vysotsky.attendance.models.Session
import com.vysotsky.attendance.models.Student
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Date
import java.util.Locale


/**
 * Displays student's attendance by dates as a table
 */

class ClassAttendanceActivity : AppCompatActivity() {

    private lateinit var binding: ActivityClassAttendanceBinding
    private lateinit var sessions: List<Session>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sessions = intent.extras?.getSerializable(EXTRA_CLASSES_KEY) as List<Session>
        Log.d(TAG, "ClassAttendanceActivity onCreate() sessions.size = ${sessions.size}")
        binding = ActivityClassAttendanceBinding.inflate(layoutInflater)
        binding.tvSubjectName.text = sessions[0].subject_name
        setUpTable()
        setContentView(binding.root)
    }

    /**
     * dynamically creates table based on passed data
     */
    private fun setUpTable() {
        val dates = sessions.map { s -> s.date }
        val students = listOfDistinct(sessions.map {session -> session.students}.flatten()).sortedWith(
            compareBy {it: Student -> it.second_name }.thenBy { it.first_name }
        )
        val cellSize = resources.getDimensionPixelSize(R.dimen.table_cell_size)

        setUpFirstRow(dates)

        for (student in students) {
            val tr = TableRow(this)
            val tv = TextView(this)
            tv.layoutParams = TableRow.LayoutParams((cellSize * 1.5).toInt(), cellSize)
            tv.text = "${student.second_name} ${student.first_name}"
            tv.gravity = Gravity.CENTER
            tv.setBackgroundResource(R.drawable.table_cell_border)
            tv.setPadding(10)
            tr.addView(tv)
            for (date in dates) {
                //check if student was present on that date and change background color of a view accordingly
                val wasPresent = sessions.filter { s -> s.date == date }[0].students.contains(student)
                val view = View(this)
                view.layoutParams = TableRow.LayoutParams(cellSize, LayoutParams.MATCH_PARENT)
                if (wasPresent) {
                    view.setBackgroundResource(R.drawable.table_cell_present)
                } else {
                    view.setBackgroundResource(R.drawable.table_cell_absent)
                }
                tr.addView(view)
            }
            binding.tl.addView(tr)
        }
    }

    private fun listOfDistinct(l: List<Student>): List<Student> {
        val res = mutableListOf<Student>()
        for (st in l) {
            if (res.find {s -> st.first_name == s.first_name && st.second_name == s.second_name} == null) {
                res += st
            }
        }
        return res
    }

    private fun setUpFirstRow(dates: List<String>) {
        val tr = TableRow(this)
        var tv = TextView(this)

        val cellSize = resources.getDimensionPixelSize(R.dimen.table_cell_size)
        tv.layoutParams = TableRow.LayoutParams(LayoutParams.WRAP_CONTENT, /*cellSize*/)
        tv.text = "Students"
        tv.setTypeface(tv.typeface, Typeface.BOLD)
        tv.setBackgroundResource(R.drawable.table_cell_border)
        tv.setPadding(10)
        tr.addView(tv)
        for (dateStr in dates) {
            val instant = Instant.parse(dateStr)
            val date = Date.from(instant)
            val format = SimpleDateFormat("dd.MM.yy", Locale.getDefault())
            val formatted = format.format(date)
            tv = TextView(this)
            tv.setBackgroundResource(R.drawable.table_cell_border)
            tv.text = formatted
            tv.layoutParams = TableRow.LayoutParams(cellSize, LayoutParams.MATCH_PARENT)
            tv.setPadding(10)
            tr.addView(tv)
        }
        binding.tl.addView(tr)
    }

    companion object {
        const val EXTRA_CLASSES_KEY = "com.vysotsky.attendance.professor.ClassAttendanceActivity.classes"
    }
}