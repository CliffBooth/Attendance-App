package com.vysotsky.attendance

import android.app.Activity
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.vysotsky.attendance.databinding.ActivityMainBinding
import com.vysotsky.attendance.professor.ProfessorHomeActivity
import com.vysotsky.attendance.student.StudentActivity
import kotlin.math.log


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()

    private val APP_PACKAGE_DOT_COUNT = 2
//    private val DUAL_APP_ID_999 = "/999/" //will not work in dual space
    private val defaultUserId = "/0/"
    private val DOT = '.'


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //check if the app is cloned
        val dataDir = applicationInfo.dataDir
        val sourceDir = applicationInfo.sourceDir
        binding = ActivityMainBinding.inflate(layoutInflater)
        binding.infoText.text = "dataDir = $dataDir, sourceDir = $sourceDir, contains /0/ = ${dataDir.contains(defaultUserId)}"
        Log.d(TAG, "dataDir = $dataDir, sourceDir = $sourceDir")
        if (!dataDir.contains(defaultUserId) || dataDir.count { c -> c == DOT } != APP_PACKAGE_DOT_COUNT) {
            Toast.makeText(this,
                getString(R.string.you_can_t_open_a_clone_of_the_app), Toast.LENGTH_LONG).show()
            Log.d(TAG, "toast made, finishing...")
            finish()
            //android.os.Process.killProcess(android.os.Process.myPid())
        }
//        val deviceID = intent.extras?.getString("id") ?: Settings.Secure.getString(
//            applicationContext.contentResolver,
//            Settings.Secure.ANDROID_ID
//        )
//        Log.d(T, "MainActivity: deviceID = $deviceID")

        //check if already logged in
        val sharedPreferences =
            getSharedPreferences(getString(R.string.preference_file_key), MODE_PRIVATE)
        if (sharedPreferences.contains(getString(R.string.saved_first_name)) &&
            sharedPreferences.contains(getString(R.string.saved_second_name))
        ) {
            val intent = Intent(this, StudentActivity::class.java)
            startActivity(intent)
            finish()
        }

        if (sharedPreferences.contains(getString(R.string.saved_email))) {
            val intent = Intent(this, ProfessorHomeActivity::class.java)
            Log.d(TAG, "MainActivity: starting ProfessorHomeActivity")
            startActivity(intent)
            finish()
        }

        setContentView(binding.root)

        val studentLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
                if (res.resultCode == Activity.RESULT_OK) {
                    Log.d(TAG, "MainActivity GOT RESULT!")
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

        viewModel.debug.observe(this) {
            debug = it
            Log.d(TAG, "MainActivity observe: debug: $it")
            if (it) {
                binding.serverAddressText.text = API_URL
                binding.serverAddressText.visibility = View.VISIBLE
                binding.infoText.visibility = View.VISIBLE
            } else {
                binding.serverAddressText.visibility = View.GONE
                binding.infoText.visibility = View.GONE
            }
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
        private val viewModel: MainViewModel by activityViewModels()
        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val checkedItems = arrayOf(debug, polling).toBooleanArray()
            return AlertDialog.Builder(this.requireContext())
                .setMultiChoiceItems(
                    arrayOf("debug", "student polling"),
                    checkedItems
                ) { _: DialogInterface, which: Int, isChecked: Boolean ->
                    Log.d(TAG, "which: $which")
                    when (which) {
                        0 -> {
                            viewModel.debug.value = isChecked
                            Log.d(TAG, "onCreateDialog: debug=${viewModel.debug.value}")
                        }
                        1 -> polling = isChecked
                    }
                    Log.d(TAG, "debug = ${debug}, polling = ${polling}")
                }
                .setPositiveButton("OK") { _, _ -> }
                .create()
        }
    }


    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause")
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume")
    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG, "onStop")
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart")
    }

    override fun onRestart() {
        super.onRestart()
        Log.d(TAG, "onRestart")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Main Activity: onDestroy")
    }

}