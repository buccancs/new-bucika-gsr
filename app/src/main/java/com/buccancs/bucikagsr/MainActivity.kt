package com.buccancs.bucikagsr

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.buccancs.bucikagsr.gsr.GSRActivity

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
        }
        
        findViewById<Button>(R.id.btnSettings).setOnClickListener {
        }
    }
}