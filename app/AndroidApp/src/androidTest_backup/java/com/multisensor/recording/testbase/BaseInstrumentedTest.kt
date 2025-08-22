package com.multisensor.recording.testbase

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
abstract class BaseInstrumentedTest {

    @get:Rule
    open var hiltRule = HiltAndroidRule(this)

    protected lateinit var context: Context

    @Before
    open fun setUp() {
        hiltRule.inject()
        context = ApplicationProvider.getApplicationContext()
    }
}
