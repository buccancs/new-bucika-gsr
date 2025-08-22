package com.bucikancs.newbucikagsr

/**
 * A simple repository class for managing users
 */
class UserRepository {
    
    private val users = mutableMapOf<String, User>()
    
    /**
     * Adds a user to the repository
     * @return true if user was added, false if user already exists
     */
    fun addUser(user: User): Boolean {
        return if (users.containsKey(user.id)) {
            false
        } else {
            users[user.id] = user
            true
        }
    }
    
    /**
     * Gets a user by ID
     * @return User if found, null otherwise
     */
    fun getUserById(id: String): User? {
        return users[id]
    }
    
    /**
     * Updates a user
     * @return true if user was updated, false if user not found
     */
    fun updateUser(user: User): Boolean {
        return if (users.containsKey(user.id)) {
            users[user.id] = user
            true
        } else {
            false
        }
    }
    
    /**
     * Removes a user by ID
     * @return true if user was removed, false if user not found
     */
    fun removeUser(id: String): Boolean {
        return users.remove(id) != null
    }
    
    /**
     * Gets all users
     */
    fun getAllUsers(): List<User> {
        return users.values.toList()
    }
    
    /**
     * Gets users by age range
     */
    fun getUsersByAgeRange(minAge: Int, maxAge: Int): List<User> {
        if (minAge > maxAge) {
            throw IllegalArgumentException("Minimum age cannot be greater than maximum age")
        }
        return users.values.filter { it.age in minAge..maxAge }
    }
    
    /**
     * Counts total users
     */
    fun getUserCount(): Int = users.size
    
    /**
     * Checks if repository is empty
     */
    fun isEmpty(): Boolean = users.isEmpty()
    
    /**
     * Clears all users
     */
    fun clear() {
        users.clear()
    }
    
    /**
     * Gets adult users only
     */
    fun getAdultUsers(): List<User> {
        return users.values.filter { it.isAdult() }
    }
}