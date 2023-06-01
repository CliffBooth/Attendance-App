package com.vysotsky.attendance

import com.vysotsky.attendance.api.Student
import org.junit.jupiter.api.Assertions.*
import com.vysotsky.attendance.util.formatDate
import com.vysotsky.attendance.util.listOfDistinct
import org.junit.Test

class UtilTest {
    @Test
    fun `unix timestamp to string`() {
        val timestampToString = mapOf(
            0L to "01.01.70",
            -100L to "01.01.70",
            1684162800000 to "15.05.23",
            1709220788000 to "29.02.24",
            1709220788000 to "29.02.24",
            1646148788000 to "01.03.22",
        )
        timestampToString.forEach {
            val string = formatDate(it.key)
            assertEquals(it.value, string)
        }
    }

    @Test
    fun listOfDistinctTest() {
        val inputOutput = mapOf(
            listOf(
                Student(null, "student", "one"),
                Student("123", "student", "one"),
            )
                    to
                    listOf(
                        Student(null, "student", "one")
                    ),
            listOf(
                Student(null, "student", "two"),
                Student("123", "student", "one"),
            )
                    to
                    listOf(
                        Student(null, "student", "one"),
                        Student(null, "student", "two")
                    ),
            listOf(
                Student("123", "namea", "second"),
                Student("123", "nameb", "second"),
            )
                    to
                    listOf(
                        Student(null, "namea", "second"),
                        Student(null, "nameb", "second"),
                    ),
            listOf<Student>() to listOf(),

            )

        inputOutput.forEach {
            val output = listOfDistinct(it.key)
            assertEquals(it.value, output)
        }
    }


}