package com.vysotsky.attendance.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.SmallTest
import com.vysotsky.attendance.api.Student
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
@SmallTest
class PredefinedClassDaoTest {
    private lateinit var database: AttendanceDatabase
    private lateinit var dao: PredefinedClassDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AttendanceDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = database.predefinedClassDao
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
        val class1 = PredefinedClassDB(
            students = students,
            updatedAt = System.currentTimeMillis(),
            subjectName = "name1",
        )
        var allItems = dao.getAll()
        assert(!allItems.contains(class1))
        dao.insert(class1)
        allItems = dao.getAll()
        assert(allItems.contains(class1))
        dao.insert(class1)
        allItems = dao.getAll()
        assert(allItems.contains(class1) && allItems.size == 1)
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
            PredefinedClassDB(
                students = students,
                updatedAt = System.currentTimeMillis(),
                subjectName = "name1",
            ),
            PredefinedClassDB(
                students = students,
                updatedAt = System.currentTimeMillis(),
                subjectName = "name2",
            )
        )
        var allItems = dao.getAll()
        assert(!allItems.any { classes.contains(it) })
        dao.insertAll(classes)
        allItems = dao.getAll()
        assert(allItems == classes)
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
            PredefinedClassDB(
                students = students,
                updatedAt = System.currentTimeMillis(),
                subjectName = "name1",
            ),
            PredefinedClassDB(
                students = students,
                updatedAt = System.currentTimeMillis(),
                subjectName = "name2",
            )
        )
        dao.insertAll(classes)
        var allItems = dao.getAll()
        assert(allItems.isNotEmpty())
        dao.clear()
        allItems = dao.getAll()
        assert(allItems.isEmpty())
    }
}