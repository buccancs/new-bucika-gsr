package com.bucikancs.newbucikagsr

/**
 * A simple user data class with validation
 */
data class User(
    val id: String,
    val name: String,
    val email: String,
    val age: Int
) {
    
    init {
        validateUser()
    }
    
    private fun validateUser() {
        if (id.isBlank()) {
            throw IllegalArgumentException("User ID cannot be blank")
        }
        if (name.isBlank()) {
            throw IllegalArgumentException("User name cannot be blank")
        }
        if (!isValidEmail(email)) {
            throw IllegalArgumentException("Invalid email format")
        }
        if (age < 0 || age > 150) {
            throw IllegalArgumentException("Age must be between 0 and 150")
        }
    }
    
    private fun isValidEmail(email: String): Boolean {
        return email.contains("@") && email.contains(".") && email.length > 5
    }
    
    /**
     * Checks if user is an adult (18 or older)
     */
    fun isAdult(): Boolean = age >= 18
    
    /**
     * Gets user's initials
     */
    fun getInitials(): String {
        val parts = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        return if (parts.size >= 2) {
            "${parts[0].first().uppercase()}${parts[1].first().uppercase()}"
        } else if (parts.isNotEmpty()) {
            parts[0].first().uppercase()
        } else {
            ""
        }
    }
    
    /**
     * Gets display name (first name only)
     */
    fun getDisplayName(): String {
        val parts = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        return if (parts.isNotEmpty()) parts[0] else ""
    }
}