package com.bucikancs.newbucikagsr

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.assertThrows

class UserTest {

    @Test
    fun `test create valid user`() {
        val user = User("1", "John Doe", "john@example.com", 25)
        
        assertEquals("1", user.id)
        assertEquals("John Doe", user.name)
        assertEquals("john@example.com", user.email)
        assertEquals(25, user.age)
    }

    @Test
    fun `test create user with blank id throws exception`() {
        assertThrows<IllegalArgumentException> {
            User("", "John Doe", "john@example.com", 25)
        }
    }

    @Test
    fun `test create user with blank name throws exception`() {
        assertThrows<IllegalArgumentException> {
            User("1", "", "john@example.com", 25)
        }
    }

    @Test
    fun `test create user with invalid email throws exception`() {
        assertThrows<IllegalArgumentException> {
            User("1", "John Doe", "invalid-email", 25)
        }
    }

    @Test
    fun `test create user with invalid email no at symbol throws exception`() {
        assertThrows<IllegalArgumentException> {
            User("1", "John Doe", "johnexample.com", 25)
        }
    }

    @Test
    fun `test create user with invalid email no dot throws exception`() {
        assertThrows<IllegalArgumentException> {
            User("1", "John Doe", "john@examplecom", 25)
        }
    }

    @Test
    fun `test create user with short email throws exception`() {
        assertThrows<IllegalArgumentException> {
            User("1", "John Doe", "j@e.c", 25)
        }
    }

    @Test
    fun `test create user with negative age throws exception`() {
        assertThrows<IllegalArgumentException> {
            User("1", "John Doe", "john@example.com", -1)
        }
    }

    @Test
    fun `test create user with age over 150 throws exception`() {
        assertThrows<IllegalArgumentException> {
            User("1", "John Doe", "john@example.com", 151)
        }
    }

    @Test
    fun `test create user with age 0`() {
        val user = User("1", "Baby Doe", "baby@example.com", 0)
        assertEquals(0, user.age)
    }

    @Test
    fun `test create user with age 150`() {
        val user = User("1", "Elder Doe", "elder@example.com", 150)
        assertEquals(150, user.age)
    }

    @Test
    fun `test isAdult returns true for adult`() {
        val user = User("1", "John Doe", "john@example.com", 25)
        assertTrue(user.isAdult())
    }

    @Test
    fun `test isAdult returns false for minor`() {
        val user = User("1", "Johnny Doe", "johnny@example.com", 17)
        assertFalse(user.isAdult())
    }

    @Test
    fun `test isAdult returns true for exactly 18`() {
        val user = User("1", "Teen Doe", "teen@example.com", 18)
        assertTrue(user.isAdult())
    }

    @Test
    fun `test getInitials with two names`() {
        val user = User("1", "John Doe", "john@example.com", 25)
        assertEquals("JD", user.getInitials())
    }

    @Test
    fun `test getInitials with single name`() {
        val user = User("1", "John", "john@example.com", 25)
        assertEquals("J", user.getInitials())
    }

    @Test
    fun `test getInitials with three names`() {
        val user = User("1", "John Michael Doe", "john@example.com", 25)
        assertEquals("JM", user.getInitials())
    }

    @Test
    fun `test getInitials with extra spaces`() {
        val user = User("1", "  John   Doe  ", "john@example.com", 25)
        assertEquals("JD", user.getInitials())
    }

    @Test
    fun `test getDisplayName with multiple names`() {
        val user = User("1", "John Doe", "john@example.com", 25)
        assertEquals("John", user.getDisplayName())
    }

    @Test
    fun `test getDisplayName with single name`() {
        val user = User("1", "John", "john@example.com", 25)
        assertEquals("John", user.getDisplayName())
    }

    @Test
    fun `test getDisplayName with extra spaces`() {
        val user = User("1", "  John  Doe  ", "john@example.com", 25)
        assertEquals("John", user.getDisplayName())
    }

    @Test
    fun `test data class equality`() {
        val user1 = User("1", "John Doe", "john@example.com", 25)
        val user2 = User("1", "John Doe", "john@example.com", 25)
        assertEquals(user1, user2)
    }

    @Test
    fun `test data class inequality`() {
        val user1 = User("1", "John Doe", "john@example.com", 25)
        val user2 = User("2", "John Doe", "john@example.com", 25)
        assertNotEquals(user1, user2)
    }
}