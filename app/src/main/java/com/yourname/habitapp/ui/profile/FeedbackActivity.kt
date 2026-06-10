package com.yourname.habitapp.ui.profile

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.yourname.habitapp.R

class FeedbackActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_feedback)

        val etMessage = findViewById<EditText>(R.id.etFeedbackMessage)
        val btnSubmit = findViewById<Button>(R.id.btnSubmitFeedback)

        btnSubmit.setOnClickListener {
            val msg = etMessage.text.toString().trim()
            if (msg.isNotEmpty()) {
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:")
                    putExtra(Intent.EXTRA_EMAIL, arrayOf("apphabits6@gmail.com"))
                    putExtra(Intent.EXTRA_SUBJECT, "App Feedback - Habits Pro")
                    putExtra(Intent.EXTRA_TEXT, msg)
                }
                
                try {
                    startActivity(Intent.createChooser(intent, "Send Email"))
                    finish()
                } catch (e: Exception) {
                    Toast.makeText(this, "No email app found", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Please enter your message", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
