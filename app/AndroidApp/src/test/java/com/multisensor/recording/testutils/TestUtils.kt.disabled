package com.multisensor.recording.testutils

import io.mockk.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * Base test utilities providing common mock setups and test configurations.
 * Ensures consistent testing patterns across all test classes.
 */
object TestUtils {
    
    /**
     * Creates a mock that returns successful results by default
     */
    inline fun <reified T : Any> createSuccessfulMock(): T {
        return mockk<T>(relaxed = true) {
            // Configure common successful behaviors
            when (T::class) {
                // Add specific mock configurations as needed
            }
        }
    }
    
    /**
     * Creates a mock that returns failure results
     */
    inline fun <reified T : Any> createFailingMock(exception: Exception = RuntimeException("Test failure")): T {
        return mockk<T> {
            // Configure to throw exceptions for most calls
            every { any<Any>() } throws exception
        }
    }
    
    /**
     * Creates a Flow mock for testing reactive components
     */
    fun <T> createFlowMock(vararg values: T): Flow<T> {
        return flowOf(*values)
    }
    
    /**
     * Extension function for easier coroutine testing
     */
    fun createTestDispatcher(): TestDispatcher = UnconfinedTestDispatcher()
    
    /**
     * Verifies that a mock was called with specific parameters
     */
    inline fun <reified T : Any> T.verifyCalledWith(
        block: T.() -> Unit
    ) {
        verify { this@verifyCalledWith.block() }
    }
    
    /**
     * Verifies that a mock was never called
     */
    inline fun <reified T : Any> T.verifyNeverCalled() {
        verify(exactly = 0) { this@verifyNeverCalled }
    }
    
    /**
     * Creates a spy that can partially mock real objects
     */
    inline fun <reified T : Any> createSpyOf(obj: T): T = spyk(obj)
}

/**
 * JUnit rule for automatic MockK initialization and cleanup
 */
class MockKTestRule : TestRule {
    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                MockKAnnotations.init(this)
                try {
                    base.evaluate()
                } finally {
                    clearAllMocks()
                    unmockkAll()
                }
            }
        }
    }
}

/**
 * Base test class with common setup for all unit tests
 */
abstract class BaseUnitTest {
    
    protected val testDispatcher = TestUtils.createTestDispatcher()
    
    /**
     * Override this to provide test-specific setup
     */
    open fun setUp() {
        // Common setup for all tests
    }
    
    /**
     * Override this to provide test-specific cleanup
     */
    open fun tearDown() {
        // Common cleanup for all tests
        clearAllMocks()
    }
}

/**
 * Utility for testing ViewModels with coroutines
 */
object ViewModelTestUtils {
    
    /**
     * Creates a test scope for ViewModel testing
     */
    fun createTestScope(dispatcher: TestDispatcher = TestUtils.createTestDispatcher()) =
        kotlinx.coroutines.test.TestScope(dispatcher)
    
    /**
     * Extension for testing StateFlow emissions
     */
    suspend fun <T> kotlinx.coroutines.flow.StateFlow<T>.test(
        block: suspend (T) -> Unit
    ) {
        kotlinx.coroutines.flow.collect { value ->
            block(value)
        }
    }
}