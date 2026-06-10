package com.yourname.habitapp.ui.achievements

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.yourname.habitapp.R
import com.yourname.habitapp.utils.AchievementDef
import com.yourname.habitapp.utils.AchievementEngine

import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers

class AchievementDetailDialog(private val def: AchievementDef) : BottomSheetDialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.bottom_sheet_achievement_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<TextView>(R.id.tvDetailIcon).text  = def.icon
        view.findViewById<TextView>(R.id.tvDetailTitle).text = def.title
        view.findViewById<TextView>(R.id.tvDetailDesc).text  = def.description
        view.findViewById<TextView>(R.id.tvDetailXP).text    = "⚡ +${def.xpReward} XP"

        // زر المشاركة
        view.findViewById<Button>(R.id.btnShareAchievement).setOnClickListener {
            shareAchievement()
        }
    }

    private fun shareAchievement() {
        val text = """
            🏆 حققت إنجازاً جديداً في تطبيق عاداتي!
            ${def.icon} ${def.title}
            ${def.description}
            
            ابدأ رحلتك مع عاداتي Pro 🚀
        """.trimIndent()

        // حساب المشاركات
        val prefs = requireContext().getSharedPreferences("habit_prefs", android.content.Context.MODE_PRIVATE)
        val shares = prefs.getInt("share_count", 0) + 1
        prefs.edit().putInt("share_count", shares).apply()

        // فحص إنجاز المشاركة
        lifecycleScope.launch(Dispatchers.IO) {
            AchievementEngine.checkAndUnlock(requireContext(), "SHARED", shares)
        }

        startActivity(Intent.createChooser(
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            },
            "مشاركة الإنجاز"
        ))
    }
}
