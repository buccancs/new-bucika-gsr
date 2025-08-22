package com.bucikancs.newbucikagsr

/**
 * A simple calculator class with basic arithmetic operations
 */
class Calculator {
    
    /**
     * Adds two numbers
     */
    fun add(a: Double, b: Double): Double = a + b
    
    /**
     * Subtracts second number from first
     */
    fun subtract(a: Double, b: Double): Double = a - b
    
    /**
     * Multiplies two numbers
     */
    fun multiply(a: Double, b: Double): Double = a * b
    
    /**
     * Divides first number by second
     * @throws IllegalArgumentException if divisor is zero
     */
    fun divide(a: Double, b: Double): Double {
        if (b == 0.0) {
            throw IllegalArgumentException("Cannot divide by zero")
        }
        return a / b
    }
    
    /**
     * Calculates power of a number
     */
    fun power(base: Double, exponent: Double): Double {
        return Math.pow(base, exponent)
    }
    
    /**
     * Calculates square root
     * @throws IllegalArgumentException if number is negative
     */
    fun squareRoot(number: Double): Double {
        if (number < 0) {
            throw IllegalArgumentException("Cannot calculate square root of negative number")
        }
        return Math.sqrt(number)
    }
}