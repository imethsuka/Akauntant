package com.example.libra

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SignUpActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)
        
        // Find the 'Sign Up' button and 'Sign In' text link using correct IDs from layout
        val signUpButton = findViewById<Button>(R.id.SignUpButton)
        val signInTextView = findViewById<TextView>(R.id.SignInPrompt)
        
        // Set OnClickListener for the 'Sign Up' button
        signUpButton.setOnClickListener {
            // Navigate to SignInActivity after successful sign up
            val intent = Intent(this, SignInActivity::class.java)
            startActivity(intent)
            finish()
        }
        
        // Set OnClickListener for the 'Sign In' text link
        signInTextView.setOnClickListener {
            // Navigate to SignInActivity
            val intent = Intent(this, SignInActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}