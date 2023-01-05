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
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.add
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import com.vysotsky.attendance.MenuActivity
import com.vysotsky.attendance.R
import com.vysotsky.attendance.T
import com.vysotsky.attendance.databinding.ActivityStudentBinding
import com.vysotsky.attendance.student.QRCode.QRCodeFragment
import com.vysotsky.attendance.student.bluetooth.StudentBluetoothFragment

/**
 * Activity, responsible for switching between student's fragments
 */
//TODO add global viewModel and save
class StudentActivity : MenuActivity() {

    private lateinit var binding: ActivityStudentBinding
    private val viewModel: StudentViewModel by viewModels()
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var actionBarDrawerToggle: ActionBarDrawerToggle

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityStudentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val sharedPreferences =
            getSharedPreferences(getString(R.string.preference_file_key), MODE_PRIVATE)
        viewModel.firstName =
            sharedPreferences.getString(getString(R.string.saved_first_name), null).toString()
        viewModel.secondName =
            sharedPreferences.getString(getString(R.string.saved_second_name), null).toString()
        viewModel.deviceID = intent.extras?.getString("id") ?: Settings.Secure.getString(
            applicationContext.contentResolver,
            Settings.Secure.ANDROID_ID
        )
        Log.d(
            T,
            "StudentActivity: first name = ${viewModel.firstName} second name = ${viewModel.secondName} id = ${viewModel.deviceID}"
        )
        //check what will happen if not do that
        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                setReorderingAllowed(true)
                add<StudentBluetoothFragment>(R.id.fragment_container_view)
                //addToBackStack("starSessionFragment")
                Log.d(T, "StudentActivity after add()")
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
                R.id.nav_display_qr_code -> {
                    supportFragmentManager.commit {
                        replace<QRCodeFragment>(R.id.fragment_container_view)
                    }
                    true
                }

                R.id.nav_bluetooth_student -> {
                    supportFragmentManager.commit {
                        replace<StudentBluetoothFragment>(R.id.fragment_container_view)
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
        Log.d(T, "StudentActivity: item selected: $item")
        return if (actionBarDrawerToggle.onOptionsItemSelected(item)) {
            true
        } else {
            return super.onOptionsItemSelected(item)
        }
    }

}