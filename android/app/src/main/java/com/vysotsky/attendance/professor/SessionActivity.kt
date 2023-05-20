package com.vysotsky.attendance.professor

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.MenuItem
import android.widget.Toast
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
import com.vysotsky.attendance.databinding.ActivityProfessorBinding
import com.vysotsky.attendance.professor.attendeeList.AttendeesListFragment
import com.vysotsky.attendance.professor.camera.CameraFragment
import com.vysotsky.attendance.professor.proximity.ProfessorProximityFragment

/**
 * Activity, responsible for switching between professor's fragments
 */

class SessionActivity : MenuActivity() {
    private lateinit var binding: ActivityProfessorBinding
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var actionBarDrawerToggle: ActionBarDrawerToggle
    private val viewModel: SessionViewModel by viewModels()

    private lateinit var email: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sharedPreferences =
            getSharedPreferences(getString(R.string.preference_file_key), MODE_PRIVATE)
        email = sharedPreferences.getString(getString(R.string.saved_email), "ERROR!") ?: "error"
        Log.d(TAG, "SessionActivity: email: $email")
        binding = ActivityProfessorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel.isUsingGeodata = intent.extras?.getBoolean(GEOLOCATION_KEY) ?: false
        viewModel.subjectName = intent.extras?.getString(SUBJECT_NAME_KEY) ?: "error"

        Log.d(TAG, "Camera activity: using geodata = ${viewModel.isUsingGeodata}")

        //without it will add fragment with every screen rotation
        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                setReorderingAllowed(true)
                add<ProfessorProximityFragment>(R.id.fragment_container_view)
                //addToBackStack("starSessionFragment")
                Log.d(TAG, "Camera activity after add()")
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
                R.id.nav_scan_qr_code -> {
                    supportFragmentManager.commit {
                        replace<CameraFragment>(R.id.fragment_container_view)
                    }
                    true
                }

                R.id.nav_prof_wifi -> {
                    supportFragmentManager.commit {
                        replace<ProfessorProximityFragment>(R.id.fragment_container_view)
                    }
                    true
                }

                R.id.nav_attendees_list -> {
                    supportFragmentManager.commit {
                        replace<AttendeesListFragment>(R.id.fragment_container_view)
                    }
                    true
                }

                R.id.nav_stop_session -> {
                    supportFragmentManager.commit {
                        replace<StopSessionFragment>(R.id.fragment_container_view)
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

        subscribe()

    }

    private fun subscribe() {
        viewModel.isSessionTerminated.observe(this) {
            if (it) {
                Toast.makeText(this, "Session has been terminated!", Toast.LENGTH_LONG).show()
                val intent = Intent(this, ProfessorHomeActivity::class.java)
                startActivity(intent)
                finish()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.runningPolling = true
        viewModel.runPolling(email) //TODO: how to stop polling when closing the
    }

    override fun onStop() {
        super.onStop()
        viewModel.runningPolling = false
    }



    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        Log.d(TAG, "ProfessorActivity: item selected: $item")
        return if (actionBarDrawerToggle.onOptionsItemSelected(item)) {
            true
        } else {
            return super.onOptionsItemSelected(item)
        }
    }

    private var doubleBackToExitPressedOnce = false
    override fun onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed()
            return
        }
        this.doubleBackToExitPressedOnce = true
        Toast.makeText(this, getString(R.string.double_back), Toast.LENGTH_SHORT).show()
        Handler(Looper.getMainLooper()).postDelayed({ doubleBackToExitPressedOnce = false }, 1000)
    }

    companion object {
        const val GEOLOCATION_KEY = "geolocation_key"
        const val SUBJECT_NAME_KEY = "SessionActivity.subject_name"
    }
}