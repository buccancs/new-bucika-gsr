package com.multisensor.recording.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firebase Authentication service for multi-researcher access
 */
@Singleton
class FirebaseAuthService @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) {
    
    private val _authState = MutableStateFlow<AuthState>(AuthState.NotAuthenticated)
    val authState: Flow<AuthState> = _authState.asStateFlow()
    
    private val _currentUser = MutableStateFlow<FirebaseUser?>(null)
    val currentUser: Flow<FirebaseUser?> = _currentUser.asStateFlow()
    
    init {
        // Monitor authentication state changes
        firebaseAuth.addAuthStateListener { auth ->
            val user = auth.currentUser
            _currentUser.value = user
            _authState.value = if (user != null) {
                AuthState.Authenticated(user)
            } else {
                AuthState.NotAuthenticated
            }
        }
    }
    
    /**
     * Sign in with email and password
     */
    suspend fun signInWithEmailAndPassword(email: String, password: String): Result<FirebaseUser> {
        return try {
            _authState.value = AuthState.Loading
            val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val user = result.user
            if (user != null) {
                Result.success(user)
            } else {
                Result.failure(Exception("Sign in failed: No user returned"))
            }
        } catch (e: Exception) {
            _authState.value = AuthState.Error(e.message ?: "Sign in failed")
            Result.failure(e)
        }
    }
    
    /**
     * Create new researcher account
     */
    suspend fun createResearcherAccount(
        email: String, 
        password: String, 
        displayName: String,
        researcherType: ResearcherType = ResearcherType.RESEARCHER
    ): Result<FirebaseUser> {
        return try {
            _authState.value = AuthState.Loading
            val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user
            
            if (user != null) {
                // Update user profile with researcher information
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(displayName)
                    .build()
                
                user.updateProfile(profileUpdates).await()
                
                // Set custom claims for researcher type (this would typically be done on the backend)
                // For now, we'll store this in Firestore user profile
                
                Result.success(user)
            } else {
                Result.failure(Exception("Account creation failed: No user returned"))
            }
        } catch (e: Exception) {
            _authState.value = AuthState.Error(e.message ?: "Account creation failed")
            Result.failure(e)
        }
    }
    
    /**
     * Sign out current user
     */
    suspend fun signOut(): Result<Unit> {
        return try {
            firebaseAuth.signOut()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Send password reset email
     */
    suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            firebaseAuth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Update user profile
     */
    suspend fun updateUserProfile(displayName: String): Result<Unit> {
        return try {
            val user = firebaseAuth.currentUser
            if (user != null) {
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(displayName)
                    .build()
                
                user.updateProfile(profileUpdates).await()
                Result.success(Unit)
            } else {
                Result.failure(Exception("No user signed in"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get current user ID
     */
    fun getCurrentUserId(): String? {
        return firebaseAuth.currentUser?.uid
    }
    
    /**
     * Get current user email
     */
    fun getCurrentUserEmail(): String? {
        return firebaseAuth.currentUser?.email
    }
    
    /**
     * Get current user display name
     */
    fun getCurrentUserDisplayName(): String? {
        return firebaseAuth.currentUser?.displayName
    }
    
    /**
     * Check if user is signed in
     */
    fun isSignedIn(): Boolean {
        return firebaseAuth.currentUser != null
    }
}

/**
 * Authentication state sealed class
 */
sealed class AuthState {
    object NotAuthenticated : AuthState()
    object Loading : AuthState()
    data class Authenticated(val user: FirebaseUser) : AuthState()
    data class Error(val message: String) : AuthState()
}

/**
 * Researcher types for access control
 */
enum class ResearcherType(val displayName: String) {
    RESEARCHER("Researcher"),
    PRINCIPAL_INVESTIGATOR("Principal Investigator"),
    RESEARCH_ASSISTANT("Research Assistant"),
    STUDENT("Student Researcher"),
    ADMIN("System Administrator")
}

/**
 * Researcher profile data class
 */
data class ResearcherProfile(
    val uid: String,
    val email: String,
    val displayName: String,
    val researcherType: ResearcherType,
    val institution: String? = null,
    val department: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val lastActiveAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
)