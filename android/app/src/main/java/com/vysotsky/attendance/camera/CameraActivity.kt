package com.vysotsky.attendance.camera

import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.add
import androidx.fragment.app.commit
import com.vysotsky.attendance.MenuActivity
import com.vysotsky.attendance.R
import com.vysotsky.attendance.T
import com.vysotsky.attendance.databinding.ActivityCameraBinding

//rename to something like ProfessorActivity
class CameraActivity : MenuActivity() {
    private lateinit var binding: ActivityCameraBinding
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var actionBarDrawerToggle: ActionBarDrawerToggle

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sharedPreferences =
            getSharedPreferences(getString(R.string.preference_file_key), MODE_PRIVATE)
        val email = sharedPreferences.getString(getString(R.string.saved_email), "ERROR!");
        Log.d(T, "Camera activity: email: $email")
        binding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //check what will happen if not do that
        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                setReorderingAllowed(true)
                add<StartSessionFragment>(R.id.fragment_container_view)
                //addToBackStack("starSessionFragment")
                Log.d(T, "Camera activity after add()")
            }
        }

//        drawerLayout = binding.drawerLayout
//        actionBarDrawerToggle = ActionBarDrawerToggle(this, drawerLayout, R.string.nav_open, R.string.nav_close )
//        drawerLayout.addDrawerListener(actionBarDrawerToggle)
//        actionBarDrawerToggle.syncState()
//        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }


//    override fun onOptionsItemSelected(item: MenuItem): Boolean {
//        Log.d(T, "Camera Activity: item selected: ${item}")
//        return if (actionBarDrawerToggle.onOptionsItemSelected(item)) {
//            true
//        } else {
//            return when (item.itemId) {
//                R.id.nav_scan_qr_code -> {
//                    supportFragmentManager.commit {
//                        add<CameraFragment>(R.id.fragment_container_view)
//                    }
//                    true
//                }
//                R.id.nav_attendees_list -> {
//                    supportFragmentManager.commit {
//                        add<AttendeesListFragment>(R.id.fragment_container_view)
//                    }
//                    true
//                }
//                R.id.nav_stop_session -> {
//                    supportFragmentManager.commit {
//                        add<StartSessionFragment>(R.id.fragment_container_view)
//                    }
//                    true
//                }
//                else -> return super.onOptionsItemSelected(item)
//            }
//        }
//    }
}