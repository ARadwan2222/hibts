package com.yourname.habitapp.ui

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.google.firebase.auth.FirebaseAuth
import com.yourname.habitapp.R
import com.yourname.habitapp.ui.onboarding.OnboardingActivity

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Ensure notification channels are created early
        com.yourname.habitapp.utils.NotificationHelper.createNotificationChannels(this)

        val checks = listOf(
            findViewById<ImageView>(R.id.ivCheck1),
            findViewById<ImageView>(R.id.ivCheck2),
            findViewById<ImageView>(R.id.ivCheck3),
            findViewById<ImageView>(R.id.ivCheck4)
        )
        val lines = listOf(
            findViewById<android.view.View>(R.id.viewLine1),
            findViewById<android.view.View>(R.id.viewLine2),
            findViewById<android.view.View>(R.id.viewLine3),
            findViewById<android.view.View>(R.id.viewLine4)
        )
        val phrase = findViewById<TextView>(R.id.tvSplashPhrase)
        
        val colors = listOf(
            "#4CAF50", // Green
            "#2196F3", // Blue
            "#FF9800", // Orange
            "#E91E63"  // Pink
        )

        val fadeIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)

        checks.forEachIndexed { index, imageView ->
            if (imageView != null && index < lines.size && lines[index] != null) {
                Handler(Looper.getMainLooper()).postDelayed({
                    imageView.setImageResource(R.drawable.ic_pencil_check)
                    imageView.setColorFilter(Color.parseColor(colors[index % colors.size]))
                    imageView.startAnimation(fadeIn)
                    
                    lines[index]?.setBackgroundColor(Color.parseColor(colors[index % colors.size]))
                    lines[index]?.alpha = 1.0f
                    
                    if (index == checks.size - 1 && phrase != null) {
                        phrase.animate().alpha(1f).setDuration(600).start()
                    }
                }, (index * 600L) + 400L)
            }
        }

        Handler(Looper.getMainLooper()).postDelayed({
            if (!isFinishing) {
                val auth = FirebaseAuth.getInstance()
                val user = auth.currentUser
                val prefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                val settingsPrefs = getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)
                val onboardingDone = prefs.getBoolean("onboarding_done", false)

                // Apply saved theme/dark mode settings on splash
                val isDarkMode = settingsPrefs.getBoolean("dark_mode", false)
                AppCompatDelegate.setDefaultNightMode(if (isDarkMode) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO)

                // Set Theme from settings before moving forward
                val themeName = settingsPrefs.getString("app_theme", "Male")
                // No need to call setTheme here as it doesn't have UI, but ensures consistency if needed

                if (user != null) {
                    if (user.isEmailVerified && onboardingDone) {
                        // Fully registered and verified
                        startActivity(Intent(this, MainActivity::class.java))
                    } else {
                        // Logged in but NOT verified or profile not saved locally
                        startActivity(Intent(this, OnboardingActivity::class.java))
                    }
                } else {
                    // Not logged in at all
                    startActivity(Intent(this, OnboardingActivity::class.java))
                }
                finish()
            }
        }, 3200)
    }
}