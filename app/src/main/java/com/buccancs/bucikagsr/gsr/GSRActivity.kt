package com.buccancs.bucikagsr.gsr

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.buccancs.bucikagsr.R

class GSRActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(createLayout())
        setupActionBar()
        initializeGSR()
    }
    
    private fun createLayout(): TextView {
        return TextView(this).apply {
            text = "GSR Activity\n\nThis will contain:\n• GSR sensor connection\n• Real-time GSR data display\n• Data logging controls\n• Shimmer device management"
            textSize = 16f
            setPadding(32, 32, 32, 32)
        }
    }
    
    private fun setupActionBar() {
        supportActionBar?.apply {
            title = "GSR Monitoring"
            setDisplayHomeAsUpEnabled(true)
        }
    }
    
    private fun initializeGSR() {
    }
    
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}