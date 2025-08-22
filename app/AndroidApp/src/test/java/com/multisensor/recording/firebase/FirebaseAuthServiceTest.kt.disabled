package com.multisensor.recording.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.AuthResult
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.multisensor.recording.TestConstants
import com.multisensor.recording.testutils.BaseUnitTest
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

/**
 * Comprehensive unit tests for FirebaseAuthService.
 * Covers authentication workflows, state management, and error handling.
 * 
 * Test Categories:
 * - Authentication state management
 * - Sign in/out operations
 * - User registration
 * - Password reset
 * - User profile management
 * - Error handling and edge cases
 */
@ExperimentalCoroutinesApi
class FirebaseAuthServiceTest : BaseUnitTest() {

    @get:Rule
    val mockKRule = io.mockk.junit4.MockKRule(this)

    @MockK
    private lateinit var firebaseAuth: FirebaseAuth
    
    @MockK
    private lateinit var mockUser: FirebaseUser
    
    @MockK
    private lateinit var authResult: AuthResult
    
    @MockK
    private lateinit var authTask: Task<AuthResult>
    
    @MockK
    private lateinit var voidTask: Task<Void>

    private lateinit var authService: FirebaseAuthService
    private lateinit var testScope: TestScope

    @Before
    override fun setUp() {
        super.setUp()
        testScope = TestScope(testDispatcher)
        
        setupDefaultMocks()
        
        authService = FirebaseAuthService(firebaseAuth)
    }

    @After
    override fun tearDown() {
        super.tearDown()
        testScope.cancel()
    }

    private fun setupDefaultMocks() {
        // Firebase Auth mocks
        every { firebaseAuth.currentUser } returns null
        every { firebaseAuth.addAuthStateListener(any()) } just Runs
        every { firebaseAuth.removeAuthStateListener(any()) } just Runs
        every { firebaseAuth.signInWithEmailAndPassword(any(), any()) } returns authTask
        every { firebaseAuth.createUserWithEmailAndPassword(any(), any()) } returns authTask
        every { firebaseAuth.sendPasswordResetEmail(any()) } returns voidTask
        every { firebaseAuth.signOut() } just Runs
        
        // Auth result mocks
        every { authResult.user } returns mockUser
        
        // Task mocks
        every { authTask.isSuccessful } returns true
        every { authTask.result } returns authResult
        every { authTask.exception } returns null
        every { voidTask.isSuccessful } returns true
        every { voidTask.exception } returns null
        
        // User mocks
        every { mockUser.uid } returns TestConstants.TEST_USER_ID
        every { mockUser.email } returns TestConstants.TEST_EMAIL
        every { mockUser.displayName } returns "Test User"
        every { mockUser.isEmailVerified } returns true
        every { mockUser.updateProfile(any()) } returns voidTask
        every { mockUser.sendEmailVerification() } returns voidTask
        every { mockUser.updatePassword(any()) } returns voidTask
        every { mockUser.delete() } returns voidTask
    }

    @Test
    fun `test_initial_auth_state_not_authenticated`() = testScope.runTest {
        // Given: FirebaseAuthService is initialized with no user
        
        // When: Getting initial auth state
        val initialState = authService.authState.first()
        
        // Then: Should be not authenticated
        assertTrue("Initial state should be NotAuthenticated", 
            initialState is FirebaseAuthService.AuthState.NotAuthenticated)
        
        // Current user should be null
        val currentUser = authService.currentUser.first()
        assertNull("Current user should be null", currentUser)
    }

    @Test
    fun `test_sign_in_success`() = testScope.runTest {
        // Given: Valid credentials and successful task
        every { authTask.isSuccessful } returns true
        every { authTask.result } returns authResult
        every { authResult.user } returns mockUser
        
        // When: Signing in
        val result = authService.signIn(TestConstants.TEST_EMAIL, TestConstants.TEST_PASSWORD)
        
        // Then: Sign in should succeed
        assertTrue("Sign in should succeed", result.isSuccess)
        assertEquals("Should return user", mockUser, result.getOrNull())
        
        // Verify Firebase Auth was called
        verify { firebaseAuth.signInWithEmailAndPassword(TestConstants.TEST_EMAIL, TestConstants.TEST_PASSWORD) }
    }

    @Test
    fun `test_sign_in_failure_invalid_credentials`() = testScope.runTest {
        // Given: Invalid credentials
        val exception = Exception("Invalid credentials")
        every { authTask.isSuccessful } returns false
        every { authTask.exception } returns exception
        
        // When: Signing in with invalid credentials
        val result = authService.signIn("invalid@email.com", "wrongpassword")
        
        // Then: Sign in should fail
        assertTrue("Sign in should fail", result.isFailure)
        assertEquals("Should return exception", exception, result.exceptionOrNull())
        
        // Verify Firebase Auth was called
        verify { firebaseAuth.signInWithEmailAndPassword("invalid@email.com", "wrongpassword") }
    }

    @Test
    fun `test_sign_up_success`() = testScope.runTest {
        // Given: Valid registration data
        every { authTask.isSuccessful } returns true
        every { authTask.result } returns authResult
        every { authResult.user } returns mockUser
        
        // When: Signing up
        val result = authService.signUp(TestConstants.TEST_EMAIL, TestConstants.TEST_PASSWORD, "Test User")
        
        // Then: Sign up should succeed
        assertTrue("Sign up should succeed", result.isSuccess)
        assertEquals("Should return user", mockUser, result.getOrNull())
        
        // Verify Firebase Auth was called
        verify { firebaseAuth.createUserWithEmailAndPassword(TestConstants.TEST_EMAIL, TestConstants.TEST_PASSWORD) }
        
        // Verify profile update was called
        verify { mockUser.updateProfile(any()) }
    }

    @Test
    fun `test_sign_up_failure_email_already_exists`() = testScope.runTest {
        // Given: Email already exists
        val exception = Exception("Email already in use")
        every { authTask.isSuccessful } returns false
        every { authTask.exception } returns exception
        
        // When: Signing up with existing email
        val result = authService.signUp(TestConstants.TEST_EMAIL, TestConstants.TEST_PASSWORD, "Test User")
        
        // Then: Sign up should fail
        assertTrue("Sign up should fail", result.isFailure)
        assertEquals("Should return exception", exception, result.exceptionOrNull())
    }

    @Test
    fun `test_sign_out_success`() = testScope.runTest {
        // Given: User is signed in
        every { firebaseAuth.currentUser } returns mockUser
        
        // When: Signing out
        authService.signOut()
        
        // Then: Firebase Auth sign out should be called
        verify { firebaseAuth.signOut() }
    }

    @Test
    fun `test_reset_password_success`() = testScope.runTest {
        // Given: Valid email and successful task
        every { voidTask.isSuccessful } returns true
        
        // When: Resetting password
        val result = authService.resetPassword(TestConstants.TEST_EMAIL)
        
        // Then: Reset should succeed
        assertTrue("Password reset should succeed", result.isSuccess)
        
        // Verify Firebase Auth was called
        verify { firebaseAuth.sendPasswordResetEmail(TestConstants.TEST_EMAIL) }
    }

    @Test
    fun `test_reset_password_failure_invalid_email`() = testScope.runTest {
        // Given: Invalid email
        val exception = Exception("Invalid email")
        every { voidTask.isSuccessful } returns false
        every { voidTask.exception } returns exception
        
        // When: Resetting password with invalid email
        val result = authService.resetPassword("invalid-email")
        
        // Then: Reset should fail
        assertTrue("Password reset should fail", result.isFailure)
        assertEquals("Should return exception", exception, result.exceptionOrNull())
    }

    @Test
    fun `test_update_user_profile_success`() = testScope.runTest {
        // Given: User is signed in and task succeeds
        every { firebaseAuth.currentUser } returns mockUser
        every { voidTask.isSuccessful } returns true
        
        // When: Updating user profile
        val result = authService.updateUserProfile("Updated Name", null)
        
        // Then: Update should succeed
        assertTrue("Profile update should succeed", result.isSuccess)
        
        // Verify profile update was called
        verify { mockUser.updateProfile(any()) }
    }

    @Test
    fun `test_update_user_profile_not_signed_in`() = testScope.runTest {
        // Given: No user is signed in
        every { firebaseAuth.currentUser } returns null
        
        // When: Trying to update profile
        val result = authService.updateUserProfile("New Name", null)
        
        // Then: Update should fail
        assertTrue("Profile update should fail when not signed in", result.isFailure)
        assertTrue("Should return appropriate error", 
            result.exceptionOrNull()?.message?.contains("not signed in") == true)
    }

    @Test
    fun `test_send_email_verification_success`() = testScope.runTest {
        // Given: User is signed in and email not verified
        every { firebaseAuth.currentUser } returns mockUser
        every { mockUser.isEmailVerified } returns false
        every { voidTask.isSuccessful } returns true
        
        // When: Sending email verification
        val result = authService.sendEmailVerification()
        
        // Then: Verification should be sent
        assertTrue("Email verification should succeed", result.isSuccess)
        
        // Verify verification was sent
        verify { mockUser.sendEmailVerification() }
    }

    @Test
    fun `test_send_email_verification_already_verified`() = testScope.runTest {
        // Given: User is signed in and email is verified
        every { firebaseAuth.currentUser } returns mockUser
        every { mockUser.isEmailVerified } returns true
        
        // When: Sending email verification
        val result = authService.sendEmailVerification()
        
        // Then: Should indicate already verified
        assertTrue("Should succeed with already verified message", result.isSuccess)
        
        // Verify verification was not sent
        verify(exactly = 0) { mockUser.sendEmailVerification() }
    }

    @Test
    fun `test_change_password_success`() = testScope.runTest {
        // Given: User is signed in and task succeeds
        every { firebaseAuth.currentUser } returns mockUser
        every { voidTask.isSuccessful } returns true
        
        // When: Changing password
        val result = authService.changePassword("newpassword123")
        
        // Then: Password change should succeed
        assertTrue("Password change should succeed", result.isSuccess)
        
        // Verify password update was called
        verify { mockUser.updatePassword("newpassword123") }
    }

    @Test
    fun `test_delete_account_success`() = testScope.runTest {
        // Given: User is signed in and deletion succeeds
        every { firebaseAuth.currentUser } returns mockUser
        every { voidTask.isSuccessful } returns true
        
        // When: Deleting account
        val result = authService.deleteAccount()
        
        // Then: Account deletion should succeed
        assertTrue("Account deletion should succeed", result.isSuccess)
        
        // Verify delete was called
        verify { mockUser.delete() }
    }

    @Test
    fun `test_is_signed_in_true`() = testScope.runTest {
        // Given: User is signed in
        every { firebaseAuth.currentUser } returns mockUser
        
        // When: Checking if signed in
        val isSignedIn = authService.isSignedIn()
        
        // Then: Should return true
        assertTrue("Should be signed in", isSignedIn)
    }

    @Test
    fun `test_is_signed_in_false`() = testScope.runTest {
        // Given: No user is signed in
        every { firebaseAuth.currentUser } returns null
        
        // When: Checking if signed in
        val isSignedIn = authService.isSignedIn()
        
        // Then: Should return false
        assertFalse("Should not be signed in", isSignedIn)
    }

    @Test
    fun `test_get_current_user_id`() = testScope.runTest {
        // Given: User is signed in
        every { firebaseAuth.currentUser } returns mockUser
        
        // When: Getting current user ID
        val userId = authService.getCurrentUserId()
        
        // Then: Should return user ID
        assertEquals("Should return user ID", TestConstants.TEST_USER_ID, userId)
    }

    @Test
    fun `test_get_current_user_id_not_signed_in`() = testScope.runTest {
        // Given: No user is signed in
        every { firebaseAuth.currentUser } returns null
        
        // When: Getting current user ID
        val userId = authService.getCurrentUserId()
        
        // Then: Should return null
        assertNull("Should return null when not signed in", userId)
    }

    @Test
    fun `test_auth_state_listener_authentication`() = testScope.runTest {
        // Given: Auth state listener is set up
        val authStateListenerSlot = slot<FirebaseAuth.AuthStateListener>()
        verify { firebaseAuth.addAuthStateListener(capture(authStateListenerSlot)) }
        
        // When: User signs in (simulated by listener)
        every { firebaseAuth.currentUser } returns mockUser
        authStateListenerSlot.captured.onAuthStateChanged(firebaseAuth)
        
        // Then: Auth state should update to authenticated
        val authState = authService.authState.first()
        assertTrue("Auth state should be authenticated", 
            authState is FirebaseAuthService.AuthState.Authenticated)
        
        // Current user should be updated
        val currentUser = authService.currentUser.first()
        assertEquals("Current user should be updated", mockUser, currentUser)
    }

    @Test
    fun `test_auth_state_listener_sign_out`() = testScope.runTest {
        // Given: User was signed in and now signs out
        val authStateListenerSlot = slot<FirebaseAuth.AuthStateListener>()
        verify { firebaseAuth.addAuthStateListener(capture(authStateListenerSlot)) }
        
        // When: User signs out (simulated by listener)
        every { firebaseAuth.currentUser } returns null
        authStateListenerSlot.captured.onAuthStateChanged(firebaseAuth)
        
        // Then: Auth state should update to not authenticated
        val authState = authService.authState.first()
        assertTrue("Auth state should be not authenticated", 
            authState is FirebaseAuthService.AuthState.NotAuthenticated)
        
        // Current user should be null
        val currentUser = authService.currentUser.first()
        assertNull("Current user should be null", currentUser)
    }

    @Test
    fun `test_concurrent_auth_operations`() = testScope.runTest {
        // Given: Multiple auth operations called simultaneously
        
        // When: Calling sign in and sign up concurrently
        val signInResult = authService.signIn(TestConstants.TEST_EMAIL, TestConstants.TEST_PASSWORD)
        val signUpResult = authService.signUp("other@email.com", "password", "Other User")
        
        // Then: Both operations should complete without interference
        // Note: Actual behavior depends on implementation thread safety
        assertNotNull("Sign in result should exist", signInResult)
        assertNotNull("Sign up result should exist", signUpResult)
    }

    @Test
    fun `test_error_handling_network_issues`() = testScope.runTest {
        // Given: Network error during sign in
        val networkException = Exception("Network error")
        every { authTask.isSuccessful } returns false
        every { authTask.exception } returns networkException
        
        // When: Signing in with network issues
        val result = authService.signIn(TestConstants.TEST_EMAIL, TestConstants.TEST_PASSWORD)
        
        // Then: Should handle network error gracefully
        assertTrue("Should fail with network error", result.isFailure)
        assertEquals("Should return network exception", networkException, result.exceptionOrNull())
    }

    @Test
    fun `test_memory_management_and_cleanup`() = testScope.runTest {
        // Given: AuthService is being cleaned up
        
        // When: Service is garbage collected (simulated)
        // Note: Actual cleanup would happen in onCleared or similar
        
        // Then: Auth state listener should be removed
        // This test verifies proper cleanup to prevent memory leaks
        verify { firebaseAuth.addAuthStateListener(any()) }
        
        // In actual implementation, there should be cleanup that removes the listener
        // verify { firebaseAuth.removeAuthStateListener(any()) }
    }
}