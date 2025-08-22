package com.multisensor.recording

import org.junit.Assert.*
import org.junit.Test

/**
 * Simple working test to verify test infrastructure is functioning.
 * This demonstrates that the test setup can run successfully.
 */
class BasicTestExample {

    @Test
    fun test_basic_functionality() {
        // Given: Basic test setup
        val testValue = "Hello Testing"
        
        // When: Performing basic assertion
        val result = testValue.isNotEmpty()
        
        // Then: Test should pass
        assertTrue("Test value should not be empty", result)
        assertEquals("String should match", "Hello Testing", testValue)
    }

    @Test
    fun test_mathematical_operations() {
        // Given: Basic numbers
        val a = 5
        val b = 10
        
        // When: Performing calculations
        val sum = a + b
        val product = a * b
        
        // Then: Results should be correct
        assertEquals("Sum should be correct", 15, sum)
        assertEquals("Product should be correct", 50, product)
    }

    @Test
    fun test_collections() {
        // Given: A list of test data
        val testList = listOf("A", "B", "C")
        
        // When: Testing list operations
        val size = testList.size
        val firstItem = testList.firstOrNull()
        
        // Then: Operations should work correctly
        assertEquals("List size should be 3", 3, size)
        assertEquals("First item should be A", "A", firstItem)
        assertTrue("List should contain B", testList.contains("B"))
    }

    @Test
    fun test_string_operations() {
        // Given: Test strings
        val input = "Android Testing"
        
        // When: Performing string operations
        val lowercase = input.lowercase()
        val words = input.split(" ")
        
        // Then: String operations should work
        assertEquals("Lowercase should work", "android testing", lowercase)
        assertEquals("Split should work", 2, words.size)
        assertEquals("First word should be Android", "Android", words[0])
    }

    @Test
    fun test_null_safety() {
        // Given: Nullable values
        val nullValue: String? = null
        val nonNullValue: String? = "Not null"
        
        // When: Testing null safety
        val isNullEmpty = nullValue.isNullOrEmpty()
        val isNonNullEmpty = nonNullValue.isNullOrEmpty()
        
        // Then: Null safety should work
        assertTrue("Null value should be empty", isNullEmpty)
        assertFalse("Non-null value should not be empty", isNonNullEmpty)
    }

    @Test
    fun test_exception_handling() {
        // Given: A scenario that might throw an exception
        
        // When: Testing exception handling
        var exceptionThrown = false
        try {
            val result = 10 / 0  // This will throw ArithmeticException
        } catch (e: ArithmeticException) {
            exceptionThrown = true
        }
        
        // Then: Exception should be caught
        assertTrue("Exception should be thrown and caught", exceptionThrown)
    }

    @Test
    fun test_data_class_functionality() {
        // Given: A simple data class
        data class TestData(val id: Int, val name: String)
        
        // When: Creating instances
        val data1 = TestData(1, "Test")
        val data2 = TestData(1, "Test")
        val data3 = TestData(2, "Other")
        
        // Then: Data class functionality should work
        assertEquals("Equal objects should be equal", data1, data2)
        assertNotEquals("Different objects should not be equal", data1, data3)
        assertEquals("ID should be accessible", 1, data1.id)
        assertEquals("Name should be accessible", "Test", data1.name)
    }

    @Test
    fun test_lambda_expressions() {
        // Given: A list of numbers
        val numbers = listOf(1, 2, 3, 4, 5)
        
        // When: Using lambda expressions
        val doubled = numbers.map { it * 2 }
        val evens = numbers.filter { it % 2 == 0 }
        val sum = numbers.reduce { acc, n -> acc + n }
        
        // Then: Lambda operations should work
        assertEquals("Doubled list should be correct", listOf(2, 4, 6, 8, 10), doubled)
        assertEquals("Even numbers should be correct", listOf(2, 4), evens)
        assertEquals("Sum should be correct", 15, sum)
    }

    @Test
    fun test_when_expression() {
        // Given: Test values
        val testValues = listOf(1, 2, 3, 4, 5)
        
        // When: Using when expressions
        val results = testValues.map { value ->
            when {
                value < 3 -> "Small"
                value == 3 -> "Medium"
                else -> "Large"
            }
        }
        
        // Then: When expressions should work correctly
        assertEquals("Results should be correct", 
            listOf("Small", "Small", "Medium", "Large", "Large"), results)
    }

    @Test
    fun test_extension_functions() {
        // Given: Extension function
        fun String.isValidEmail(): Boolean {
            return this.contains("@") && this.contains(".")
        }
        
        // When: Using extension function
        val validEmail = "test@example.com".isValidEmail()
        val invalidEmail = "invalid-email".isValidEmail()
        
        // Then: Extension function should work
        assertTrue("Valid email should return true", validEmail)
        assertFalse("Invalid email should return false", invalidEmail)
    }
}