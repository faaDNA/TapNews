package com.example.tapnews.ui.news

import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import com.example.tapnews.R
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class NewsFragmentTest {
    @Test
    fun recyclerView_isDisplayed() {
        // Launch NewsFragment in isolation with explicit theme
        launchFragmentInContainer<NewsFragment>(themeResId = R.style.Theme_TapNews)
        // Check if RecyclerView is displayed
        onView(withId(R.id.newsRecyclerView)).check(matches(isDisplayed()))
    }
}
