package com.vysotsky.attendance.professor

import android.content.SharedPreferences
import android.os.Bundle
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
import com.vysotsky.attendance.T
import com.vysotsky.attendance.databinding.ActivityProfessorBinding
import com.vysotsky.attendance.professor.attendeeList.AttendeesListFragment
import com.vysotsky.attendance.professor.bluetooth.ProfessorBluetoothFragment
import com.vysotsky.attendance.professor.camera.CameraFragment
import com.vysotsky.attendance.professor.proximity.ProfessorProximityFragment

/**
 * Activity, responsible for switching between professor's fragments
 */

class ProfessorActivity : MenuActivity() {
    private lateinit var binding: ActivityProfessorBinding
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var actionBarDrawerToggle: ActionBarDrawerToggle
    private val viewModel: ProfessorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sharedPreferences =
            getSharedPreferences(getString(R.string.preference_file_key), MODE_PRIVATE)
        val email = sharedPreferences.getString(getString(R.string.saved_email), "ERROR!");
        Log.d(T, "Camera activity: email: $email")
        binding = ActivityProfessorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel.isUsingGeodata = intent.extras?.getBoolean(getString(R.string.geolocation_bundle_key)) ?: false
        Log.d(T, "Camera activity: using geodata = ${viewModel.isUsingGeodata}")

        //check what will happen if not do that
        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                setReorderingAllowed(true)
                add<ProfessorBluetoothFragment>(R.id.fragment_container_view)
                //addToBackStack("starSessionFragment")
                Log.d(T, "Camera activity after add()")
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

                R.id.nav_bluetooth -> {
                    supportFragmentManager.commit {
                        replace<ProfessorBluetoothFragment>(R.id.fragment_container_view)
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
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        Log.d(T, "ProfessorActivity: item selected: $item")
        return if (actionBarDrawerToggle.onOptionsItemSelected(item)) {
            true
        } else {
            return super.onOptionsItemSelected(item)
        }
    }
}