package com.multisensor.recording

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MultiSensorApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        
        // Enable Firebase Analytics
        FirebaseAnalytics.getInstance(this).setAnalyticsCollectionEnabled(true)
        
        // Enable Firebase Crashlytics
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)

        android.util.Log.i("MultiSensorApp", "Multi-Sensor Recording Application started with Firebase")
    }
}
