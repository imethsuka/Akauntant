package com.example.akauntant

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SignInActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in)
        
        // Find the 'Sign In' button and 'Sign Up' text link using correct IDs from layout
        val signInButton = findViewById<Button>(R.id.SignIn)
        val signUpTextView = findViewById<TextView>(R.id.tvSignUp)
        
        // Set OnClickListener for the 'Sign In' button
        signInButton.setOnClickListener {
            // Navigate to MainActivity after successful sign in
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            // Close all authentication activities
            finishAffinity()
        }
        
        // Set OnClickListener for the 'Sign Up' text link
        signUpTextView.setOnClickListener {
            // Navigate back to SignUpActivity
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}