package com.vysotsky.attendance.professor

import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.launchActivity
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.contrib.DrawerActions
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.filters.LargeTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.util.TreeIterables

import com.vysotsky.attendance.R
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher
import androidx.test.espresso.ViewInteraction.*
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.clearText
import androidx.test.espresso.matcher.ViewMatchers.withText

@RunWith(JUnit4::class)
@LargeTest
class SessionActivityTest {
    private lateinit var scenario: ActivityScenario<SessionActivity>

    @Before
    fun setup() {
        scenario = launchActivity()
        scenario.moveToState(Lifecycle.State.RESUMED)
    }

    @Test
    fun testNavigation() {
        navigateTo(R.id.nav_scan_qr_code)
        onView(withId(R.id.viewFinder)).check(matches(isDisplayed()))

        navigateTo(R.id.nav_prof_wifi)
        onView(withId(R.id.scan_button)).check(matches(isDisplayed()))

        navigateTo(R.id.nav_attendees_list)
        onView(withId(R.id.fab)).check(matches(isDisplayed()))

        navigateTo(R.id.nav_stop_session)
        onView(withId(R.id.stop_session_button)).check(matches(isDisplayed()))
    }

    @Test
    fun testAttendeesList() {
        navigateTo(R.id.nav_attendees_list)

        onView(withId(R.id.list)).check(matches(withViewCount(withId(R.id.counter_text), 0)))
        fillName("name1", "name1")
        onView(withText(R.string.add)).perform(click())
        onView(withId(R.id.list)).check(matches(withViewCount(withId(R.id.counter_text), 1)))
        fillName("name1", "name1")
        onView(withText(R.string.add)).perform(click())
        onView(withId(R.id.list)).check(matches(withViewCount(withId(R.id.counter_text), 1)))
        fillName("name1", "")
        onView(withText(R.string.add)).perform(click())
        onView(withId(R.id.list)).check(matches(withViewCount(withId(R.id.counter_text), 1)))
        fillName("", "name1")
        onView(withText(R.string.add)).perform(click())
        onView(withId(R.id.list)).check(matches(withViewCount(withId(R.id.counter_text), 1)))
        fillName("name2", "name2")
        onView(withText(R.string.add)).perform(click())
        onView(withId(R.id.list)).check(matches(withViewCount(withId(R.id.counter_text), 2)))
        fillName("name3", "name3")
        onView(withText(R.string.cancel)).perform(click())
        onView(withId(R.id.list)).check(matches(withViewCount(withId(R.id.counter_text), 2)))
    }

    private fun navigateTo(id: Int) {
        onView(withId(R.id.drawer_layout)).perform(DrawerActions.open())
        onView(withId(id)).perform(click())
    }

    fun withViewCount(viewMatcher: Matcher<View>, expectedCount: Int): Matcher<View?> {
        return object : TypeSafeMatcher<View?>() {
            private var actualCount = -1
            override fun describeTo(description: Description) {
                when {
                    actualCount >= 0 -> description.also {
                        it.appendText("Expected items count: $expectedCount, but got: $actualCount")
                    }
                }
            }

            override fun matchesSafely(root: View?): Boolean {
                actualCount = TreeIterables.breadthFirstViewTraversal(root).count {
                    viewMatcher.matches(it)
                }
                return expectedCount == actualCount
            }
        }
    }

    fun fillName(firstName: String, secondName: String) {
        ViewActions.closeSoftKeyboard()
        Thread.sleep(250)
        onView(withId(R.id.fab)).perform(click())
        onView(withId(R.id.first_name_edit_text1)).perform(
            clearText(),
            ViewActions.typeText(firstName),
            ViewActions.closeSoftKeyboard()
        )
        onView(withId(R.id.second_name_edit_text1)).perform(
            clearText(),
            ViewActions.typeText(secondName),
            ViewActions.closeSoftKeyboard()
        )
    }
}