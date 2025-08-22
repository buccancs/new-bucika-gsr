package com.multisensor.recording.testbase

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.mockk.MockKAnnotations
import io.mockk.clearAllMocks
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule

@OptIn(ExperimentalCoroutinesApi::class)
abstract class BaseUnitTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    protected open val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()

    @Before
    open fun setUp() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(testDispatcher)
        setupLogger()
    }

    @After
    open fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
        unmockkAll()
    }

    private fun setupLogger() {
    }
}
