package com.vysotsky.attendance.student

import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.MenuItem
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.add
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import com.vysotsky.attendance.MenuActivity
import com.vysotsky.attendance.R
import com.vysotsky.attendance.TAG
import com.vysotsky.attendance.databinding.ActivityStudentBinding
import com.vysotsky.attendance.student.QRCode.QRCodeFragment
import com.vysotsky.attendance.student.camera.StudentCameraFragment
import com.vysotsky.attendance.student.home.StudentHomeFragment
import com.vysotsky.attendance.student.proximity.StudentProximityFragment

/**
 * Activity, responsible for switching between student's fragments
 */
class StudentActivity : MenuActivity() {

    private lateinit var binding: ActivityStudentBinding
    private val viewModel: StudentViewModel by viewModels()
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var actionBarDrawerToggle: ActionBarDrawerToggle

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sharedPreferences =
            getSharedPreferences(getString(R.string.preference_file_key), MODE_PRIVATE)
        viewModel.firstName =
            sharedPreferences.getString(getString(R.string.saved_first_name), null).toString()
        viewModel.secondName =
            sharedPreferences.getString(getString(R.string.saved_second_name), null).toString()
        viewModel.deviceID = getId()

        binding = ActivityStudentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Log.d(
            TAG,
            "StudentActivity: first name = ${viewModel.firstName} second name = ${viewModel.secondName} id = ${viewModel.deviceID}"
        )
        //if not do that it will add fragment with every screen rotation
        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                setReorderingAllowed(true)
                replace<StudentHomeFragment>(R.id.fragment_container_view)
                //addToBackStack("starSessionFragment")
                Log.d(TAG, "StudentActivity after add()")
            }
        }

        drawerLayout = binding.drawerLayout
        actionBarDrawerToggle =
            ActionBarDrawerToggle(this, drawerLayout, R.string.nav_open, R.string.nav_close)
        drawerLayout.addDrawerListener(actionBarDrawerToggle)
        actionBarDrawerToggle.syncState()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        val navView = binding.navView
        navView.setNavigationItemSelectedListener {
            drawerLayout.close()
            when (it.itemId) {
                R.id.student_home -> {
                    supportFragmentManager.commit {
                        replace<StudentHomeFragment>(R.id.fragment_container_view)
                    }
                    true
                }

                R.id.nav_display_qr_code -> {
                    supportFragmentManager.commit {
                        replace<QRCodeFragment>(R.id.fragment_container_view)
                    }
                    true
                }

                R.id.nav_wifi_student -> {
                    supportFragmentManager.commit {
                        replace<StudentProximityFragment>(R.id.fragment_container_view)
                    }
                    true
                }

                R.id.nav_student_camera -> {
                    supportFragmentManager.commit {
                        replace<StudentCameraFragment>(R.id.fragment_container_view)
                    }
                    true
                }

                else -> false
            }
        }
        onBackPressedDispatcher.addCallback(this) {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START)
            } else {
                finish()
            }
        }
//        setSupportActionBar(binding.toolbar)

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        Log.d(TAG, "StudentActivity: item selected: $item")
        return if (actionBarDrawerToggle.onOptionsItemSelected(item)) {
            true
        } else {
            return super.onOptionsItemSelected(item)
        }
    }

    /**
     * returns a unique id for the device
     */
    private fun getId(): String {
        return intent.extras?.getString("id") ?: Settings.Secure.getString(
            applicationContext.contentResolver,
            Settings.Secure.ANDROID_ID
        )
    }

}