package com.vysotsky.attendance.professor

import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.fragment.app.add
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import com.vysotsky.attendance.MenuActivity
import com.vysotsky.attendance.R
import com.vysotsky.attendance.TAG
import com.vysotsky.attendance.databinding.ActivityProfessorHomeBinding

/**
 * intermediate activity between MainActivity and ProfessorActivity
 * it goes like this: MainActivity -> ProfessorHomeActivity(startSessionFragment and AttendanceViewFragment) -> ProfessorActivity(where all the fragments are)
 */

class ProfessorHomeActivity : MenuActivity() {

    private lateinit var binding: ActivityProfessorHomeBinding

    private val viewModel: ProfessorHomeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityProfessorHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Log.d(TAG, "ProfessorHomeActivity onCreate()")

        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                setReorderingAllowed(true)
                add<StartSessionFragment>(R.id.fcv_professor_home)
            }
        }

        binding.bottomNavigation.selectedItemId = R.id.page_start_session

        binding.bottomNavigation.setOnItemSelectedListener  {
            when (it.itemId) {
                R.id.page_home -> {
                    supportFragmentManager.commit {
                        replace<ProfessorHomeFragment>(R.id.fcv_professor_home)
                    }
                    true
                }

                R.id.page_start_session -> {
                    supportFragmentManager.commit {
                        replace<StartSessionFragment>(R.id.fcv_professor_home)
                    }
                    true
                }
                else -> false
            }
        }
    }
}