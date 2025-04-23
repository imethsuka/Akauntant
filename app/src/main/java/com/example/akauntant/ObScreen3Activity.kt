package com.example.akauntant

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class ObScreen3Activity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ob_screen3)
        
        // Find the 'Get Started' button using correct ID from layout
        val getStartedButton = findViewById<Button>(R.id.GSButton)
        
        // Set OnClickListener for the 'Get Started' button
        getStartedButton.setOnClickListener {
            // Navigate to SignUpActivity
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
            // Close all onboarding activities
            finishAffinity()
        }
    }
}