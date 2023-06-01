package com.vysotsky.attendance.professor

import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.launchActivity
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.vysotsky.attendance.R
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
@LargeTest
class ProfessorHomeActivityTest {
//    @get:Rule
//    val activityRule = ActivityScenarioRule(ProfessorHomeActivity::class.java)

    private lateinit var scenario: ActivityScenario<ProfessorHomeActivity>

    @Before
    fun setup() {
        scenario = launchActivity()
        scenario.moveToState(Lifecycle.State.RESUMED)
    }

    @Test
    fun testNavigation() {
        onView(withId(R.id.page_home)).perform(click())
        onView(withId(R.id.textView)).check(matches(isDisplayed()))
        onView(withId(R.id.page_start_session)).perform(click())
        onView(withId(R.id.subject_name)).check(matches(isDisplayed()))
    }

    @Test
    fun testEmptySubjectName() {
        closeSoftKeyboard()
        onView(withId(R.id.page_start_session)).perform(click())
        onView(withId(R.id.start_button)).perform(click())
        onView(withId(R.id.subject_name)).perform(typeText("class"), closeSoftKeyboard())
        Intents.init()
        onView(withId(R.id.start_button)).perform(click())
        intended(hasComponent(SessionActivity::class.java.name))
        Intents.release()
    }
}