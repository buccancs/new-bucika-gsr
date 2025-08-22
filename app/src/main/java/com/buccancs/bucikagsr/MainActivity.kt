package com.buccancs.bucikagsr

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.buccancs.bucikagsr.gsr.GSRActivity

/**
 * Main activity for BucikaGSR application
 * Provides access to thermal imaging and GSR monitoring features
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        setupUI()
    }
    
    private fun setupUI() {
        findViewById<Button>(R.id.btnGSR).setOnClickListener {
            startActivity(Intent(this, GSRActivity::class.java))
        }
        
        findViewById<Button>(R.id.btnThermal).setOnClickListener {
            // TODO: Launch thermal camera activity when implemented
            // startActivity(Intent(this, ThermalActivity::class.java))
        }
        
        findViewById<Button>(R.id.btnSettings).setOnClickListener {
            // TODO: Launch settings activity when implemented
            // startActivity(Intent(this, SettingsActivity::class.java))
        }
    }
}