package com.bucikancs.newbucikagsr

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.assertThrows

class CalculatorTest {

    private lateinit var calculator: Calculator

    @BeforeEach
    fun setUp() {
        calculator = Calculator()
    }

    @Test
    fun `test add positive numbers`() {
        val result = calculator.add(5.0, 3.0)
        assertEquals(8.0, result, 0.0001)
    }

    @Test
    fun `test add negative numbers`() {
        val result = calculator.add(-5.0, -3.0)
        assertEquals(-8.0, result, 0.0001)
    }

    @Test
    fun `test add zero`() {
        val result = calculator.add(5.0, 0.0)
        assertEquals(5.0, result, 0.0001)
    }

    @Test
    fun `test subtract positive numbers`() {
        val result = calculator.subtract(10.0, 4.0)
        assertEquals(6.0, result, 0.0001)
    }

    @Test
    fun `test subtract negative numbers`() {
        val result = calculator.subtract(-10.0, -4.0)
        assertEquals(-6.0, result, 0.0001)
    }

    @Test
    fun `test subtract zero`() {
        val result = calculator.subtract(5.0, 0.0)
        assertEquals(5.0, result, 0.0001)
    }

    @Test
    fun `test multiply positive numbers`() {
        val result = calculator.multiply(4.0, 3.0)
        assertEquals(12.0, result, 0.0001)
    }

    @Test
    fun `test multiply negative numbers`() {
        val result = calculator.multiply(-4.0, -3.0)
        assertEquals(12.0, result, 0.0001)
    }

    @Test
    fun `test multiply by zero`() {
        val result = calculator.multiply(5.0, 0.0)
        assertEquals(0.0, result, 0.0001)
    }

    @Test
    fun `test divide positive numbers`() {
        val result = calculator.divide(15.0, 3.0)
        assertEquals(5.0, result, 0.0001)
    }

    @Test
    fun `test divide negative numbers`() {
        val result = calculator.divide(-15.0, -3.0)
        assertEquals(5.0, result, 0.0001)
    }

    @Test
    fun `test divide by zero throws exception`() {
        assertThrows<IllegalArgumentException> {
            calculator.divide(10.0, 0.0)
        }
    }

    @Test
    fun `test power positive numbers`() {
        val result = calculator.power(2.0, 3.0)
        assertEquals(8.0, result, 0.0001)
    }

    @Test
    fun `test power zero exponent`() {
        val result = calculator.power(5.0, 0.0)
        assertEquals(1.0, result, 0.0001)
    }

    @Test
    fun `test power negative exponent`() {
        val result = calculator.power(2.0, -2.0)
        assertEquals(0.25, result, 0.0001)
    }

    @Test
    fun `test square root positive number`() {
        val result = calculator.squareRoot(25.0)
        assertEquals(5.0, result, 0.0001)
    }

    @Test
    fun `test square root zero`() {
        val result = calculator.squareRoot(0.0)
        assertEquals(0.0, result, 0.0001)
    }

    @Test
    fun `test square root negative number throws exception`() {
        assertThrows<IllegalArgumentException> {
            calculator.squareRoot(-1.0)
        }
    }
}