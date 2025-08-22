package com.bucikancs.newbucikagsr

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.assertThrows

class UserRepositoryTest {

    private lateinit var repository: UserRepository
    private lateinit var testUser1: User
    private lateinit var testUser2: User
    private lateinit var testUser3: User

    @BeforeEach
    fun setUp() {
        repository = UserRepository()
        testUser1 = User("1", "John Doe", "john@example.com", 25)
        testUser2 = User("2", "Jane Smith", "jane@example.com", 17)
        testUser3 = User("3", "Bob Johnson", "bob@example.com", 30)
    }

    @Test
    fun `test addUser success`() {
        assertTrue(repository.addUser(testUser1))
        assertEquals(testUser1, repository.getUserById("1"))
    }

    @Test
    fun `test addUser duplicate returns false`() {
        repository.addUser(testUser1)
        assertFalse(repository.addUser(testUser1))
    }

    @Test
    fun `test getUserById existing user`() {
        repository.addUser(testUser1)
        assertEquals(testUser1, repository.getUserById("1"))
    }

    @Test
    fun `test getUserById non-existing user returns null`() {
        assertNull(repository.getUserById("999"))
    }

    @Test
    fun `test updateUser existing user`() {
        repository.addUser(testUser1)
        val updatedUser = User("1", "John Updated", "john.updated@example.com", 26)
        assertTrue(repository.updateUser(updatedUser))
        assertEquals(updatedUser, repository.getUserById("1"))
    }

    @Test
    fun `test updateUser non-existing user returns false`() {
        val nonExistingUser = User("999", "Non Existing", "non@example.com", 25)
        assertFalse(repository.updateUser(nonExistingUser))
    }

    @Test
    fun `test removeUser existing user`() {
        repository.addUser(testUser1)
        assertTrue(repository.removeUser("1"))
        assertNull(repository.getUserById("1"))
    }

    @Test
    fun `test removeUser non-existing user returns false`() {
        assertFalse(repository.removeUser("999"))
    }

    @Test
    fun `test getAllUsers empty repository`() {
        assertTrue(repository.getAllUsers().isEmpty())
    }

    @Test
    fun `test getAllUsers with users`() {
        repository.addUser(testUser1)
        repository.addUser(testUser2)
        
        val allUsers = repository.getAllUsers()
        assertEquals(2, allUsers.size)
        assertTrue(allUsers.contains(testUser1))
        assertTrue(allUsers.contains(testUser2))
    }

    @Test
    fun `test getUsersByAgeRange valid range`() {
        repository.addUser(testUser1) // age 25
        repository.addUser(testUser2) // age 17
        repository.addUser(testUser3) // age 30
        
        val usersInRange = repository.getUsersByAgeRange(20, 28)
        assertEquals(1, usersInRange.size)
        assertTrue(usersInRange.contains(testUser1))
    }

    @Test
    fun `test getUsersByAgeRange no users in range`() {
        repository.addUser(testUser1) // age 25
        
        val usersInRange = repository.getUsersByAgeRange(50, 60)
        assertTrue(usersInRange.isEmpty())
    }

    @Test
    fun `test getUsersByAgeRange invalid range throws exception`() {
        assertThrows<IllegalArgumentException> {
            repository.getUsersByAgeRange(30, 20)
        }
    }

    @Test
    fun `test getUsersByAgeRange exact match`() {
        repository.addUser(testUser1) // age 25
        
        val usersInRange = repository.getUsersByAgeRange(25, 25)
        assertEquals(1, usersInRange.size)
        assertTrue(usersInRange.contains(testUser1))
    }

    @Test
    fun `test getUserCount empty repository`() {
        assertEquals(0, repository.getUserCount())
    }

    @Test
    fun `test getUserCount with users`() {
        repository.addUser(testUser1)
        repository.addUser(testUser2)
        assertEquals(2, repository.getUserCount())
    }

    @Test
    fun `test isEmpty when empty`() {
        assertTrue(repository.isEmpty())
    }

    @Test
    fun `test isEmpty when not empty`() {
        repository.addUser(testUser1)
        assertFalse(repository.isEmpty())
    }

    @Test
    fun `test clear repository`() {
        repository.addUser(testUser1)
        repository.addUser(testUser2)
        assertEquals(2, repository.getUserCount())
        
        repository.clear()
        assertEquals(0, repository.getUserCount())
        assertTrue(repository.isEmpty())
    }

    @Test
    fun `test getAdultUsers only adults`() {
        repository.addUser(testUser1) // age 25 - adult
        repository.addUser(testUser3) // age 30 - adult
        
        val adults = repository.getAdultUsers()
        assertEquals(2, adults.size)
        assertTrue(adults.contains(testUser1))
        assertTrue(adults.contains(testUser3))
    }

    @Test
    fun `test getAdultUsers mixed ages`() {
        repository.addUser(testUser1) // age 25 - adult
        repository.addUser(testUser2) // age 17 - minor
        repository.addUser(testUser3) // age 30 - adult
        
        val adults = repository.getAdultUsers()
        assertEquals(2, adults.size)
        assertTrue(adults.contains(testUser1))
        assertTrue(adults.contains(testUser3))
        assertFalse(adults.contains(testUser2))
    }

    @Test
    fun `test getAdultUsers no adults`() {
        repository.addUser(testUser2) // age 17 - minor
        
        val adults = repository.getAdultUsers()
        assertTrue(adults.isEmpty())
    }

    @Test
    fun `test getAdultUsers empty repository`() {
        val adults = repository.getAdultUsers()
        assertTrue(adults.isEmpty())
    }
}