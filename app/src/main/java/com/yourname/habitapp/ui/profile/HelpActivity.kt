package com.yourname.habitapp.ui.profile

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.ViewFlipper
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.yourname.habitapp.R

class HelpActivity : AppCompatActivity() {

    private lateinit var flipper: ViewFlipper
    private lateinit var progress: LinearProgressIndicator
    private lateinit var btnNext: Button
    private lateinit var btnPrev: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_help)

        flipper = findViewById(R.id.helpFlipper)
        progress = findViewById(R.id.helpProgress)
        btnNext = findViewById(R.id.btnNextHelp)
        btnPrev = findViewById(R.id.btnPrevHelp)

        setupPages()
        updateUI()

        btnNext.setOnClickListener {
            if (flipper.displayedChild < flipper.childCount - 1) {
                flipper.setInAnimation(this, R.anim.slide_in_right)
                flipper.setOutAnimation(this, R.anim.slide_out_left)
                flipper.showNext()
                updateUI()
            } else {
                finish()
            }
        }

        btnPrev.setOnClickListener {
            if (flipper.displayedChild > 0) {
                flipper.setInAnimation(this, R.anim.slide_in_left)
                flipper.setOutAnimation(this, R.anim.slide_out_right)
                flipper.showPrevious()
                updateUI()
            }
        }
    }

    private fun setupPages() {
        val titles = listOf(
            R.string.help_page1_title, R.string.help_page2_title, R.string.help_page3_title,
            R.string.help_page4_title, R.string.help_page5_title, R.string.help_page6_title
        )
        val contents = listOf(
            R.string.help_page1_content, R.string.help_page2_content, R.string.help_page3_content,
            R.string.help_page4_content, R.string.help_page5_content, R.string.help_page6_content
        )

        for (i in 0 until flipper.childCount) {
            val page = flipper.getChildAt(i)
            page.findViewById<TextView>(R.id.tvHelpPageTitle).text = getString(titles[i])
            page.findViewById<TextView>(R.id.tvHelpPageContent).text = android.text.Html.fromHtml(getString(contents[i]), android.text.Html.FROM_HTML_MODE_LEGACY)
        }
    }

    private fun updateUI() {
        val current = flipper.displayedChild
        val total = flipper.childCount
        
        progress.progress = ((current + 1) * 100) / total
        
        btnPrev.visibility = if (current == 0) View.INVISIBLE else View.VISIBLE
        btnNext.text = if (current == total - 1) getString(R.string.finish) else getString(R.string.next)
    }
}