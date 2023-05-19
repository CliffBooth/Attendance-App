package com.vysotsky.attendance.professor

import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.fragment.app.add
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import com.vysotsky.attendance.MenuActivity
import com.vysotsky.attendance.R
import com.vysotsky.attendance.TAG
import com.vysotsky.attendance.database.getDatabase
import com.vysotsky.attendance.databinding.ActivityProfessorHomeBinding

/**
 * intermediate activity between MainActivity and ProfessorActivity
 * it goes like this: MainActivity -> ProfessorHomeActivity(startSessionFragment and AttendanceViewFragment) -> ProfessorActivity(where all the fragments are)
 */

class ProfessorHomeActivity : MenuActivity() {

    private lateinit var binding: ActivityProfessorHomeBinding

    private val db by lazy {
        getDatabase(this)
    }

//    private val viewModel1 = ViewModelProvider.

    private val viewModel by viewModels<ProfessorHomeViewModel>(
        factoryProducer = {
            object: ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    Log.d(TAG, "ProfessorHomeActivity inside viewModel create()!!")
                    return ProfessorHomeViewModel(db.dao) as T
                }
            }
        }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel //create
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