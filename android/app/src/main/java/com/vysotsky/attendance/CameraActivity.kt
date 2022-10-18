package com.vysotsky.attendance

import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.add
import androidx.fragment.app.commit
import com.vysotsky.attendance.databinding.ActivityCameraBinding

//rename to something like ProfessorActivity
class CameraActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCameraBinding
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sharedPreferences = getSharedPreferences(getString(R.string.preference_file_key), MODE_PRIVATE)
        val email = sharedPreferences.getString(getString(R.string.saved_email), "ERROR!");
        Log.d(T, "Camera activity: email: $email")
        binding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //check what will happen if not do that
        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                setReorderingAllowed(true)
                add<StartSessionFragment>(R.id.fragment_container_view)
                addToBackStack("starSessionFragment")
                Log.d(T, "Camera activity after add()")
            }
        }


    }

}