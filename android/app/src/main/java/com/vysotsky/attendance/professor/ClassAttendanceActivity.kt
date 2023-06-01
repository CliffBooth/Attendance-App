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
import com.vysotsky.attendance.api.*
import com.vysotsky.attendance.database.PredefinedClassDB
import com.vysotsky.attendance.database.getDatabase
import com.vysotsky.attendance.util.compareByName
import com.vysotsky.attendance.util.formatDate
import com.vysotsky.attendance.util.listOfDistinct
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


/**
 * Displays student's attendance by dates as a table
 */

//students should be distinct only by names!!
class ClassAttendanceActivity : AppCompatActivity() {

    private lateinit var binding: ActivityClassAttendanceBinding
    private lateinit var sessions: List<Session>
    private lateinit var predefined: List<PredefinedClassDB>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sessions = intent.extras?.getSerializable(EXTRA_CLASSES_KEY) as List<Session>
        runBlocking {
            predefined = getDatabase(this@ClassAttendanceActivity).predefinedClassDao.getAll()
                .filter { it.subjectName == sessions[0].subjectName }
        }
        Log.d(TAG, "ClassAttendanceActivity: onCreate() predefined = $predefined")
        Log.d(TAG, "ClassAttendanceActivity onCreate() sessions.size = ${sessions.size}")
        binding = ActivityClassAttendanceBinding.inflate(layoutInflater)
        binding.tvSubjectName.text = sessions[0].subjectName
        setUpTable()
        setContentView(binding.root)
    }

    /**
     * dynamically creates table based on passed data
     */
    private fun setUpTable() {
        val dates = sessions.map { s -> s.date }
        val students = listOfDistinct(sessions.map {session -> session.students}.flatten() + predefined.map{p -> p.students}.flatten()).sortedWith(
            compareBy {it: Student -> it.secondName }.thenBy { it.firstName }
        )
        val cellSize = resources.getDimensionPixelSize(R.dimen.table_cell_size)

        setUpFirstRow(dates)

        for (student in students) {
            val tr = TableRow(this)
            val tv = TextView(this)
            tv.layoutParams = TableRow.LayoutParams((cellSize * 1.5).toInt(), cellSize)
            tv.text = "${student.secondName} ${student.firstName}"
            tv.gravity = Gravity.CENTER
            tv.setBackgroundResource(R.drawable.table_cell_border)
            tv.setPadding(10)
            tr.addView(tv)
            for (date in dates) {
                //check if student was present on that date and change background color of a view accordingly
                val allClassesThisDate = sessions.filter { s -> s.date == date }
//                Log.d(TAG, "allClasses = ${allClassesThisDate}")
                val classThisDate = allClassesThisDate[0]
//                Log.d(TAG, "class = ${classThisDate}")
                val wasPresent = classThisDate.students.find {s -> compareByName(s, student)} != null
//                Log.d(TAG, "$date) $student wasPresent = $wasPresent")
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

    private fun setUpFirstRow(dates: List<Long>) {
        val tr = TableRow(this)
        var tv = TextView(this)

        val cellSize = resources.getDimensionPixelSize(R.dimen.table_cell_size)
        tv.layoutParams = TableRow.LayoutParams(LayoutParams.WRAP_CONTENT, /*cellSize*/)
        tv.text = getString(R.string.students)
        tv.setTypeface(tv.typeface, Typeface.BOLD)
        tv.setBackgroundResource(R.drawable.table_cell_border)
        tv.setPadding(10)
        tr.addView(tv)
        for (unixTime in dates) {
            val formatted = formatDate(unixTime)
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