package com.example.libra

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class ObScreen1Activity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ob_screen1)
        
        // Find the 'Next' and 'Skip' buttons using correct IDs from layout
        val nextButton = findViewById<Button>(R.id.nextButton1)
        val skipText = findViewById<TextView>(R.id.skipText)
        
        // Set OnClickListener for the 'Next' button
        nextButton.setOnClickListener {
            // Navigate to ObScreen2Activity
            val intent = Intent(this, ObScreen2Activity::class.java)
            startActivity(intent)
        }
        
        // Set OnClickListener for the 'Skip' text
        skipText.setOnClickListener {
            // Skip directly to SignUpActivity
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
            // Close all onboarding activities
            finishAffinity()
        }
    }
}