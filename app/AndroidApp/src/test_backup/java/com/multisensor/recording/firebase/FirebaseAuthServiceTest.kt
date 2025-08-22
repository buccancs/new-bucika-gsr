package com.multisensor.recording.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.AuthResult
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.junit4.MockKRule
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for Firebase Authentication Service
 */
class FirebaseAuthServiceTest {

    @get:Rule
    val mockkRule = MockKRule(this)

    @MockK
    private lateinit var firebaseAuth: FirebaseAuth

    @MockK
    private lateinit var mockUser: FirebaseUser

    @MockK
    private lateinit var mockAuthResult: AuthResult

    @MockK
    private lateinit var mockTask: Task<AuthResult>

    private lateinit var authService: FirebaseAuthService

    @Before
    fun setup() {
        authService = FirebaseAuthService(firebaseAuth)
    }

    @Test
    fun `signInWithEmailAndPassword should return success when authentication succeeds`() = runTest {
        // Given
        val email = "researcher@institution.edu"
        val password = "password123"
        
        every { mockAuthResult.user } returns mockUser
        every { mockUser.uid } returns "test-uid"
        every { mockUser.email } returns email
        
        every { firebaseAuth.signInWithEmailAndPassword(email, password) } returns mockTask
        every { mockTask.result } returns mockAuthResult
        mockkStatic(Tasks::class)
        every { Tasks.await(mockTask) } returns mockAuthResult

        // When
        val result = authService.signInWithEmailAndPassword(email, password)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(mockUser, result.getOrNull())
        verify { firebaseAuth.signInWithEmailAndPassword(email, password) }
    }

    @Test
    fun `signInWithEmailAndPassword should return failure when authentication fails`() = runTest {
        // Given
        val email = "researcher@institution.edu"
        val password = "wrongpassword"
        val exception = Exception("Authentication failed")
        
        every { firebaseAuth.signInWithEmailAndPassword(email, password) } returns mockTask
        mockkStatic(Tasks::class)
        every { Tasks.await(mockTask) } throws exception

        // When
        val result = authService.signInWithEmailAndPassword(email, password)

        // Then
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }

    @Test
    fun `createResearcherAccount should create user and update profile`() = runTest {
        // Given
        val email = "newresearcher@institution.edu"
        val password = "password123"
        val displayName = "Dr. Jane Researcher"
        val researcherType = ResearcherType.RESEARCHER
        
        val mockUpdateTask: Task<Void> = mockk()
        
        every { mockAuthResult.user } returns mockUser
        every { mockUser.uid } returns "new-uid"
        every { mockUser.updateProfile(any()) } returns mockUpdateTask
        
        every { firebaseAuth.createUserWithEmailAndPassword(email, password) } returns mockTask
        every { mockTask.result } returns mockAuthResult
        
        mockkStatic(Tasks::class)
        every { Tasks.await(mockTask) } returns mockAuthResult
        every { Tasks.await(mockUpdateTask) } returns mockk()

        // When
        val result = authService.createResearcherAccount(email, password, displayName, researcherType)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(mockUser, result.getOrNull())
        verify { firebaseAuth.createUserWithEmailAndPassword(email, password) }
        verify { mockUser.updateProfile(any()) }
    }

    @Test
    fun `signOut should call firebaseAuth signOut`() = runTest {
        // Given
        every { firebaseAuth.signOut() } just Runs

        // When
        val result = authService.signOut()

        // Then
        assertTrue(result.isSuccess)
        verify { firebaseAuth.signOut() }
    }

    @Test
    fun `getCurrentUserId should return current user uid`() {
        // Given
        val expectedUid = "test-uid"
        every { firebaseAuth.currentUser } returns mockUser
        every { mockUser.uid } returns expectedUid

        // When
        val actualUid = authService.getCurrentUserId()

        // Then
        assertEquals(expectedUid, actualUid)
    }

    @Test
    fun `getCurrentUserId should return null when no user signed in`() {
        // Given
        every { firebaseAuth.currentUser } returns null

        // When
        val actualUid = authService.getCurrentUserId()

        // Then
        assertNull(actualUid)
    }

    @Test
    fun `isSignedIn should return true when user is signed in`() {
        // Given
        every { firebaseAuth.currentUser } returns mockUser

        // When
        val isSignedIn = authService.isSignedIn()

        // Then
        assertTrue(isSignedIn)
    }

    @Test
    fun `isSignedIn should return false when no user signed in`() {
        // Given
        every { firebaseAuth.currentUser } returns null

        // When
        val isSignedIn = authService.isSignedIn()

        // Then
        assertFalse(isSignedIn)
    }

    @Test
    fun `sendPasswordResetEmail should return success when email is sent`() = runTest {
        // Given
        val email = "researcher@institution.edu"
        val mockResetTask: Task<Void> = mockk()
        
        every { firebaseAuth.sendPasswordResetEmail(email) } returns mockResetTask
        mockkStatic(Tasks::class)
        every { Tasks.await(mockResetTask) } returns mockk()

        // When
        val result = authService.sendPasswordResetEmail(email)

        // Then
        assertTrue(result.isSuccess)
        verify { firebaseAuth.sendPasswordResetEmail(email) }
    }

    @Test
    fun `researcher types should have correct display names`() {
        // Test that all researcher types have appropriate display names
        assertEquals("Researcher", ResearcherType.RESEARCHER.displayName)
        assertEquals("Principal Investigator", ResearcherType.PRINCIPAL_INVESTIGATOR.displayName)
        assertEquals("Research Assistant", ResearcherType.RESEARCH_ASSISTANT.displayName)
        assertEquals("Student Researcher", ResearcherType.STUDENT.displayName)
        assertEquals("System Administrator", ResearcherType.ADMIN.displayName)
    }

    @Test
    fun `researcher profile should contain all required fields`() {
        // Given
        val profile = ResearcherProfile(
            uid = "test-uid",
            email = "researcher@institution.edu",
            displayName = "Dr. Test Researcher",
            researcherType = ResearcherType.RESEARCHER,
            institution = "Test University",
            department = "Computer Science"
        )

        // Then
        assertEquals("test-uid", profile.uid)
        assertEquals("researcher@institution.edu", profile.email)
        assertEquals("Dr. Test Researcher", profile.displayName)
        assertEquals(ResearcherType.RESEARCHER, profile.researcherType)
        assertEquals("Test University", profile.institution)
        assertEquals("Computer Science", profile.department)
        assertTrue(profile.isActive)
        assertTrue(profile.createdAt > 0)
        assertTrue(profile.lastActiveAt > 0)
    }
}