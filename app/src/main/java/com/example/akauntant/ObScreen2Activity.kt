package com.example.akauntant

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class ObScreen2Activity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ob_screen2)
        
        // Find the 'Next' and 'Skip' buttons using correct IDs from layout
        val nextButton = findViewById<Button>(R.id.nextButton2)
        val skipText = findViewById<TextView>(R.id.skipText)
        
        // Set OnClickListener for the 'Next' button
        nextButton.setOnClickListener {
            // Navigate to ObScreen3Activity
            val intent = Intent(this, ObScreen3Activity::class.java)
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