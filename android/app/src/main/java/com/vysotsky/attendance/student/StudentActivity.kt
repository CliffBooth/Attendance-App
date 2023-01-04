package com.vysotsky.attendance.student

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.add
import androidx.fragment.app.commit
import com.vysotsky.attendance.MenuActivity
import com.vysotsky.attendance.R
import com.vysotsky.attendance.T
import com.vysotsky.attendance.databinding.ActivityStudentBinding
import com.vysotsky.attendance.student.bluetooth.StudentBluetoothFragment

/**
 * Activity, responsible for switching between student's fragments
 */

class StudentActivity : MenuActivity() {

    private lateinit var binding: ActivityStudentBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityStudentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //check what will happen if not do that
        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                setReorderingAllowed(true)
                add<StudentBluetoothFragment>(R.id.fragment_container_view)
                //addToBackStack("starSessionFragment")
                Log.d(T, "StudentActivity after add()")
            }
        }

//        setSupportActionBar(binding.toolbar)

    }

}