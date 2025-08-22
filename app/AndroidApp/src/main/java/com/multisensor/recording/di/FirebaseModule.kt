package com.multisensor.recording.di

import android.content.Context
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.multisensor.recording.firebase.FirebaseAnalyticsService
import com.multisensor.recording.firebase.FirebaseAuthService
import com.multisensor.recording.firebase.FirebaseFirestoreService
import com.multisensor.recording.firebase.FirebaseStorageService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for Firebase dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    @Provides
    @Singleton
    fun provideFirebaseAnalytics(@ApplicationContext context: Context): FirebaseAnalytics {
        return FirebaseAnalytics.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth {
        return FirebaseAuth.getInstance()
    }

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore {
        return FirebaseFirestore.getInstance()
    }

    @Provides
    @Singleton
    fun provideFirebaseStorage(): FirebaseStorage {
        return FirebaseStorage.getInstance()
    }

    @Provides
    @Singleton
    fun provideFirebaseAnalyticsService(
        firebaseAnalytics: FirebaseAnalytics
    ): FirebaseAnalyticsService {
        return FirebaseAnalyticsService(firebaseAnalytics)
    }

    @Provides
    @Singleton
    fun provideFirebaseAuthService(
        firebaseAuth: FirebaseAuth
    ): FirebaseAuthService {
        return FirebaseAuthService(firebaseAuth)
    }

    @Provides
    @Singleton
    fun provideFirebaseFirestoreService(
        firestore: FirebaseFirestore,
        authService: FirebaseAuthService
    ): FirebaseFirestoreService {
        return FirebaseFirestoreService(firestore, authService)
    }

    @Provides
    @Singleton
    fun provideFirebaseStorageService(
        storage: FirebaseStorage
    ): FirebaseStorageService {
        return FirebaseStorageService(storage)
    }
}
