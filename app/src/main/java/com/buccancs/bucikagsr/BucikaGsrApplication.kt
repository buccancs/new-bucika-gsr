package com.buccancs.bucikagsr

import android.app.Application

class BucikaGsrApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        initializeComponents()
    }
    
    private fun initializeComponents() {
    }
    
    companion object {
        const val TAG = "BucikaGSR"
        const val VERSION = "1.0.0"
    }
}