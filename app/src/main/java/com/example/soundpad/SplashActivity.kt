package com.example.soundpad

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    private val SPLASH_DISPLAY_LENGTH = 1500 // 1.5 seconds
    private val PREFS_NAME = "SoundpadPrefs"
    private val FIRST_LAUNCH_KEY = "isFirstLaunch"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Hide the action bar
        supportActionBar?.hide()

        // After the splash screen duration, navigate to the main activity
        Handler(Looper.getMainLooper()).postDelayed({
            val mainIntent = Intent(this, MainActivity::class.java)
            startActivity(mainIntent)
            finish()
        }, SPLASH_DISPLAY_LENGTH.toLong())
    }
} 