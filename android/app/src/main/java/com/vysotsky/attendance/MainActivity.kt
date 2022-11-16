package com.vysotsky.attendance

import android.app.Activity
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.vysotsky.attendance.QRCode.QRCodeActivity
import com.vysotsky.attendance.camera.CameraActivity
import com.vysotsky.attendance.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //check if already logged in
        val sharedPreferences =
            getSharedPreferences(getString(R.string.preference_file_key), MODE_PRIVATE)
        if (sharedPreferences.contains(getString(R.string.saved_first_name)) &&
            sharedPreferences.contains(getString(R.string.saved_second_name))
        ) {
            val intent = Intent(this, QRCodeActivity::class.java)
            startActivity(intent)
            finish()
        }

        if (sharedPreferences.contains(getString(R.string.saved_email))) {
            val intent = Intent(this, CameraActivity::class.java)
            startActivity(intent)
            finish()
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val studentLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
                if (res.resultCode == Activity.RESULT_OK) {
                    Log.d(T, "MainActivity GOT RESULT!")
                    finish()
                }
            }

        val professorLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
                if (res.resultCode == Activity.RESULT_OK) {
                    finish()
                }
            }

        binding.studentButton.setOnClickListener {
            studentLauncher.launch(Intent(this, StudentLogInActivity::class.java))
        }

        binding.professorButton.setOnClickListener {
            professorLauncher.launch(Intent(this, ProfessorLogInActivity::class.java))
        }
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.ip, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_ip -> {
                startActivity(Intent(this, ChangeIPActivity::class.java))
                true
            }

            R.id.action_openOptionsDialog -> {
                OptionsDialog.show(supportFragmentManager, "options")
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    object OptionsDialog : DialogFragment() {
        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val checkedItems = arrayOf(debug, polling).toBooleanArray()
            return AlertDialog.Builder(this.requireContext())
                .setMultiChoiceItems(
                    arrayOf("debug", "student polling"),
                    checkedItems
                ) { _: DialogInterface, which: Int, isChecked: Boolean ->
                    Log.d(T, "which: $which")
                    when (which) {
                        0 -> debug = isChecked
                        1 -> polling = isChecked
                    }
                    Log.d(T, "debug = ${debug}, polling = ${polling}")
                }
                .setPositiveButton("OK") { _, _ -> }
                .create()
        }
    }


    override fun onPause() {
        super.onPause()
        Log.d(T, "onPause")
    }

    override fun onResume() {
        super.onResume()
        Log.d(T, "onResume")
    }

    override fun onStop() {
        super.onStop()
        Log.d(T, "onStop")
    }

    override fun onStart() {
        super.onStart()
        Log.d(T, "onStart")
        binding.serverAddressText.text = API_URL
    }

    override fun onRestart() {
        super.onRestart()
        Log.d(T, "onRestart")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(T, "Main Activity: onDestroy")
    }

}