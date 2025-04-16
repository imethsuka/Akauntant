package com.example.akauntant

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {
    
    // Splash screen display time in milliseconds
    private val SPLASH_DISPLAY_TIME = 2000L
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        
        // Use Handler to delay the transition to MainActivity
        Handler(Looper.getMainLooper()).postDelayed({
            // Create an Intent to start the MainActivity
            val mainIntent = Intent(this, MainActivity::class.java)
            startActivity(mainIntent)
            
            // Apply fade transition animation
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
            
            // Close this activity
            finish()
        }, SPLASH_DISPLAY_TIME)
    }
}