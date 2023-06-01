package com.vysotsky.attendance.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.vysotsky.attendance.api.Student
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class ClassDaoTest {
    private lateinit var database: AttendanceDatabase
    private lateinit var dao: ClassDao

//    @get:Rule
//    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AttendanceDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = database.classDao
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertClassTest() = runTest {
        val students = ArrayList(
            listOf(
                Student("123", "student", "one"),
                Student(null, "student", "two"),
            )
        )
        val class1 = Class(
            currentTime,
            "name1",
            students,
            id = 1
        )
        var allItems = dao.getAll()
        assert(!allItems.contains(class1))
        dao.insertClass(class1)
        allItems = dao.getAll()
        assert(allItems.contains(class1))
    }

    @Test
    fun insertAllTest() = runTest {
        val students = ArrayList(
            listOf(
                Student("123", "student", "one"),
                Student(null, "student", "two"),
            )
        )
        val classes = listOf(
            Class(
                currentTime,
                "name1",
                students,
                id = 1
            ),
            Class(
                currentTime,
                "name2",
                students,
                id = 2
            ),
        )
        var allItems = dao.getAll()
        assert(!allItems.any { classes.contains(it) })
        dao.insertAll(classes)
        allItems = dao.getAll()
        assert(allItems == classes)
    }

    @Test
    fun clearTest() = runTest {
        val students = ArrayList(
            listOf(
                Student("123", "student", "one"),
                Student(null, "student", "two"),
            )
        )
        val classes = listOf(
            Class(
                currentTime,
                "name1",
                students,
                id = 1
            ),
            Class(
                currentTime,
                "name2",
                students,
                id = 2
            ),
        )
        dao.insertAll(classes)
        var allItems = dao.getAll()
        assert(allItems.isNotEmpty())
        dao.clear()
        allItems = dao.getAll()
        assert(allItems.isEmpty())
    }
}