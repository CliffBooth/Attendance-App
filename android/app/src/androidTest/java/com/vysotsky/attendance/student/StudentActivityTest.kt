package com.vysotsky.attendance.student

import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.launchActivity
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.contrib.DrawerActions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.filters.LargeTest
import com.vysotsky.attendance.R
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
@LargeTest
class StudentActivityTest {
    private lateinit var scenario: ActivityScenario<StudentActivity>

    @Before
    fun setup() {
        scenario = launchActivity()
        scenario.moveToState(Lifecycle.State.RESUMED)
    }

    @Test
    fun navigationTest() {
        navigateTo(R.id.student_home)
        Espresso.onView(ViewMatchers.withId(R.id.name))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))

        navigateTo(R.id.nav_display_qr_code)
        Espresso.onView(ViewMatchers.withId(R.id.qr_code_image))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))

        navigateTo(R.id.nav_wifi_student)
        Espresso.onView(ViewMatchers.withId(R.id.devices_list))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))

        navigateTo(R.id.nav_student_camera)
        Espresso.onView(ViewMatchers.withId(R.id.viewFinder))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }

    private fun navigateTo(id: Int) {
        Espresso.onView(ViewMatchers.withId(R.id.drawer_layout)).perform(DrawerActions.open())
        Espresso.onView(ViewMatchers.withId(id)).perform(ViewActions.click())
    }
}