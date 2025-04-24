package com.example.libra

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {
    
    // Splash screen display time in milliseconds
    private val SPLASH_DISPLAY_TIME = 2000L
    
    // SharedPreferences constants
    companion object {
        const val FIRST_RUN_KEY = "is_first_run"
        const val AUTH_KEY = "is_authenticated"
    }
    
    // Initialize SharedPreferences
    private lateinit var sharedPreferences: SharedPreferences
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        
        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE)
        
        // Use Handler to delay the transition
        Handler(Looper.getMainLooper()).postDelayed({
            // Navigate to the appropriate screen
            navigateToNextScreen()
        }, SPLASH_DISPLAY_TIME)
    }
    
    @Suppress("DEPRECATION")
    private fun navigateToNextScreen() {
        // Check if this is the first run of the app
        val isFirstRun = sharedPreferences.getBoolean(FIRST_RUN_KEY, true)
        
        if (isFirstRun) {
            // First run - go to onboarding
            startActivity(Intent(this, ObScreen1Activity::class.java))
            
            // Update shared preferences to mark first run as completed
            sharedPreferences.edit().putBoolean(FIRST_RUN_KEY, false).apply()
        } else {
            // Not first run - check if user is authenticated
            val isAuthenticated = sharedPreferences.getBoolean(AUTH_KEY, false)
            
            if (isAuthenticated) {
                // User is authenticated - go to main screen
                startActivity(Intent(this, MainActivity::class.java))
            } else {
                // User is not authenticated - go to sign up
                startActivity(Intent(this, SignUpActivity::class.java))
            }
        }
        
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        finish()
    }
}