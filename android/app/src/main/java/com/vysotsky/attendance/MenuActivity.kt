package com.vysotsky.attendance

import android.content.Intent
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.vysotsky.attendance.database.getDatabase
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

open class MenuActivity : AppCompatActivity() {

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_exit -> {
                getSharedPreferences(getString(R.string.preference_file_key), MODE_PRIVATE)
                    .edit()
                    .clear()
                    .apply()
                runBlocking {
                    val db = getDatabase(this@MenuActivity)
                    launch { db.classDao.clear() }
                    launch { db.predefinedClassDao.clear() }
                }
                startActivity(Intent(this, MainActivity::class.java))
                finish()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }
}