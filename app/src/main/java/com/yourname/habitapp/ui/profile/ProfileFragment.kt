package com.yourname.habitapp.ui.profile

import android.content.Context
import android.content.Intent
import android.app.Activity
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.yourname.habitapp.R
import com.yourname.habitapp.data.AppDatabase
import com.yourname.habitapp.ui.achievements.AchievementsActivity
import com.yourname.habitapp.utils.AchievementEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.util.*

class ProfileFragment : Fragment() {

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val prefs = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
            prefs.edit().putString("user_image", it.toString()).apply()
            view?.findViewById<ImageView>(R.id.ivProfilePic)?.setImageURI(it)
            view?.findViewById<ImageView>(R.id.ivProfilePic)?.visibility = View.VISIBLE
            view?.findViewById<View>(R.id.tvAvatar)?.visibility = View.GONE
        }
    }

    private val pickCover = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val prefs = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
            prefs.edit().putString("user_cover", it.toString()).apply()
            view?.findViewById<ImageView>(R.id.ivProfileCover)?.setImageURI(it)
        }
    }

    private val createBackup = registerForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        uri?.let { exportDatabase(it) }
    }

    private val openBackup = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { importDatabase(it) }
    }

    private var tonePickerRequestCode = -1

    private val pickRingtone = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            uri?.let {
                val settingsPrefs = requireContext().getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)
                val key = when(tonePickerRequestCode) {
                    1 -> "tone_start_task"
                    2 -> "tone_end_task"
                    3 -> "tone_achievement"
                    4 -> "tone_year_goal"
                    5 -> "tone_habit"
                    6 -> "tone_birthday"
                    else -> "notification_tone"
                }
                settingsPrefs.edit().putString(key, it.toString()).apply()
                updateToneNameUI(key, it)
            }
        }
    }

    private fun updateToneNameUI(key: String, uri: Uri) {
        try {
            val ringtone = RingtoneManager.getRingtone(requireContext(), uri)
            val title = ringtone?.getTitle(requireContext()) ?: "Default"
            when(key) {
                "tone_start_task" -> view?.findViewById<TextView>(R.id.tvToneStartTask)?.text = title
                "tone_end_task" -> view?.findViewById<TextView>(R.id.tvToneEndTask)?.text = title
                "tone_achievement" -> view?.findViewById<TextView>(R.id.tvToneAchievement)?.text = title
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun startTonePicker(code: Int) {
        try {
            tonePickerRequestCode = code
            val settingsPrefs = requireContext().getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)
            val key = when(code) {
                1 -> "tone_start_task"
                2 -> "tone_end_task"
                3 -> "tone_achievement"
                else -> "notification_tone"
            }
            val currentUri = settingsPrefs.getString(key, null)?.let { Uri.parse(it) }
            
            val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, getString(R.string.notification_tone))
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, currentUri)
            pickRingtone.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error opening ringtone picker", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val prefs = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val settingsPrefs = requireContext().getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)

        val tvName = view.findViewById<TextView>(R.id.tvProfileName)
        val tvXP = view.findViewById<TextView>(R.id.tvProfileXP)
        val ivAvatarPlaceholder = view.findViewById<View>(R.id.tvAvatar)
        val ivProfilePic = view.findViewById<ImageView>(R.id.ivProfilePic)
        val ivProfileCover = view.findViewById<ImageView>(R.id.ivProfileCover)
        val tvBirthdateCountdown = view.findViewById<TextView>(R.id.tvBirthdateCountdown)
        
        val btnAchievementsRow = view.findViewById<View>(R.id.btnViewAchievementsRow)
        val btnBackupRow = view.findViewById<View>(R.id.btnBackupRow)
        val btnRestoreRow = view.findViewById<View>(R.id.btnRestoreRow)
        
        val switchNotifications = view.findViewById<com.google.android.material.materialswitch.MaterialSwitch>(R.id.switchNotifications)
        val switchSound = view.findViewById<com.google.android.material.materialswitch.MaterialSwitch>(R.id.switchSound)
        val switchDarkMode = view.findViewById<com.google.android.material.materialswitch.MaterialSwitch>(R.id.switchDarkMode)
        val switchVibration = view.findViewById<com.google.android.material.materialswitch.MaterialSwitch>(R.id.switchVibration)
        val spinnerLang = view.findViewById<Spinner>(R.id.spinnerLanguage)
        val spinnerTheme = view.findViewById<Spinner>(R.id.spinnerTheme)
        
        val btnHelpCenterRow = view.findViewById<View>(R.id.btnHelpCenterRow)
        val btnTermsRow = view.findViewById<View>(R.id.btnTermsRow)
        val btnLogoutRow = view.findViewById<View>(R.id.btnLogoutRow)
        val btnResetAppRow = view.findViewById<View>(R.id.btnResetAppRow)
        val tvVersion = view.findViewById<TextView>(R.id.tvVersionName)

        // Tone Rows
        view.findViewById<View>(R.id.btnToneStartTask)?.setOnClickListener { startTonePicker(1) }
        view.findViewById<View>(R.id.btnToneEndTask)?.setOnClickListener { startTonePicker(2) }
        view.findViewById<View>(R.id.btnToneAchievement)?.setOnClickListener { startTonePicker(3) }

        lifecycleScope.launch {
            val keys = listOf("tone_start_task", "tone_end_task", "tone_achievement")
            keys.forEach { key ->
                settingsPrefs.getString(key, null)?.let { updateToneNameUI(key, Uri.parse(it)) }
            }
        }

        // Theme Selector
        val themes = listOf("Male", "Female", "Cats", "Dogs", "Travel", "Nature", "Ocean", "Sunset", "Space", "Coffee", "Tech", "Minimal", "Pastel", "Vintage", "Gold", "Classic")
        spinnerTheme?.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, themes)
        val currentTheme = settingsPrefs.getString("app_theme", "Male")
        spinnerTheme?.setSelection(themes.indexOf(currentTheme))
        spinnerTheme?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                val selected = themes[pos]
                if (selected != currentTheme) {
                    settingsPrefs.edit().putString("app_theme", selected).apply()
                    activity?.recreate()
                }
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }

        try {
            val pInfo = requireContext().packageManager.getPackageInfo(requireContext().packageName, 0)
            tvVersion.text = getString(R.string.version_prefix).format(pInfo.versionName)
        } catch (e: Exception) {
            tvVersion.text = "Version 1.2.2"
        }

        val name = prefs.getString("user_name", "User")
        val xp = AchievementEngine.getTotalXP(requireContext())
        val (level, levelName) = AchievementEngine.getLevel(requireContext(), xp)

        tvName.text = name
        tvXP.text = getString(R.string.level_display).format(level, levelName) + " | " + getString(R.string.total_xp).format(xp)

        val imageUriString = prefs.getString("user_image", null)
        if (imageUriString != null) {
            try {
                ivProfilePic.setImageURI(Uri.parse(imageUriString))
                ivProfilePic.visibility = View.VISIBLE
                ivAvatarPlaceholder.visibility = View.GONE
            } catch (e: Exception) {
                ivProfilePic.visibility = View.GONE
                ivAvatarPlaceholder.visibility = View.VISIBLE
            }
        } else {
            ivProfilePic.visibility = View.GONE
            ivAvatarPlaceholder.visibility = View.VISIBLE
        }

        val coverUriString = prefs.getString("user_cover", null)
        if (coverUriString != null) {
            try { ivProfileCover.setImageURI(Uri.parse(coverUriString)) } catch (e: Exception) {}
        }

        ivProfilePic.setOnClickListener { pickImage.launch("image/*") }
        ivAvatarPlaceholder.setOnClickListener { pickImage.launch("image/*") }
        view.findViewById<View>(R.id.btnEditAvatarInside)?.setOnClickListener { pickImage.launch("image/*") }

        btnAchievementsRow.setOnClickListener { startActivity(Intent(requireContext(), AchievementsActivity::class.java)) }
        
        btnHelpCenterRow.setOnClickListener { 
            AlertDialog.Builder(requireContext(), R.style.PurpleAlertDialog)
                .setTitle(getString(R.string.how_to_use_hibts))
                .setMessage(android.text.Html.fromHtml(getString(R.string.how_to_use_content), android.text.Html.FROM_HTML_MODE_LEGACY))
                .setPositiveButton("OK", null)
                .show()
        }

        btnTermsRow.setOnClickListener {
            AlertDialog.Builder(requireContext(), R.style.PurpleAlertDialog)
                .setTitle(R.string.terms_of_use)
                .setMessage(android.text.Html.fromHtml(getString(R.string.terms_of_use_text), android.text.Html.FROM_HTML_MODE_LEGACY))
                .setPositiveButton("OK", null)
                .show()
        }

        btnBackupRow.setOnClickListener { createBackup.launch("hibts_backup_${System.currentTimeMillis()}.db") }
        btnRestoreRow.setOnClickListener { openBackup.launch(arrayOf("application/octet-stream", "*/*")) }

        btnLogoutRow.setOnClickListener {
            AlertDialog.Builder(requireContext(), R.style.PurpleAlertDialog)
                .setTitle(R.string.logout)
                .setMessage(getString(R.string.logout_keep_data))
                .setPositiveButton(R.string.yes) { _, _ -> 
                    FirebaseAuth.getInstance().signOut()
                    val intent = requireContext().packageManager.getLaunchIntentForPackage(requireContext().packageName)
                    intent?.let {
                        it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        startActivity(it)
                        activity?.finish()
                    }
                }
                .setNegativeButton(R.string.no, null)
                .show()
        }

        btnResetAppRow.setOnClickListener {
            AlertDialog.Builder(requireContext(), R.style.PurpleAlertDialog)
                .setTitle(getString(R.string.reset_app_data))
                .setMessage(getString(R.string.reset_app_warning))
                .setPositiveButton(getString(R.string.yes)) { _, _ -> resetEverything() }
                .setNegativeButton(getString(R.string.no), null)
                .show()
        }

        switchNotifications.isChecked = settingsPrefs.getBoolean("notifications", true)
        switchSound.isChecked = settingsPrefs.getBoolean("sound", true)
        switchDarkMode.isChecked = settingsPrefs.getBoolean("dark_mode", false)
        switchVibration.isChecked = settingsPrefs.getBoolean("vibration", true)

        switchNotifications.setOnCheckedChangeListener { _, isChecked -> settingsPrefs.edit().putBoolean("notifications", isChecked).apply() }
        switchSound.setOnCheckedChangeListener { _, isChecked -> settingsPrefs.edit().putBoolean("sound", isChecked).apply() }
        switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            settingsPrefs.edit().putBoolean("dark_mode", isChecked).apply()
            AppCompatDelegate.setDefaultNightMode(if (isChecked) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO)
        }
        switchVibration.setOnCheckedChangeListener { _, isChecked -> settingsPrefs.edit().putBoolean("vibration", isChecked).apply() }

        val languages = listOf("English", "العربية", "Deutsch")
        spinnerLang.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, languages)
        val currentLang = settingsPrefs.getString("lang", "en")
        spinnerLang.setSelection(when(currentLang) { "ar" -> 1; "de" -> 2; else -> 0 })
        spinnerLang.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                val lang = when(pos) { 1 -> "ar"; 2 -> "de"; else -> "en" }
                if (lang != currentLang) {
                    settingsPrefs.edit().putString("lang", lang).apply()
                    updateLocale(lang)
                }
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
    }

    private fun exportDatabase(uri: Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val dbFile = requireContext().getDatabasePath("habit_database")
                val inputStream: InputStream = dbFile.inputStream()
                val outputStream: OutputStream? = requireContext().contentResolver.openOutputStream(uri)
                outputStream?.use { input -> inputStream.copyTo(input) }
                withContext(Dispatchers.Main) { Toast.makeText(requireContext(), "Backup Saved ✅", Toast.LENGTH_SHORT).show() }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { Toast.makeText(requireContext(), "Backup Failed: ${e.message}", Toast.LENGTH_LONG).show() }
            }
        }
    }

    private fun importDatabase(uri: Uri) {
        AlertDialog.Builder(requireContext(), R.style.PurpleAlertDialog)
            .setTitle("Restore Data")
            .setMessage("This will replace all current data. Proceed?")
            .setPositiveButton("Restore") { _, _ ->
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val dbFile = requireContext().getDatabasePath("habit_database")
                        val inputStream: InputStream? = requireContext().contentResolver.openInputStream(uri)
                        inputStream?.use { input -> dbFile.outputStream().use { output -> input.copyTo(output) } }
                        withContext(Dispatchers.Main) {
                            Toast.makeText(requireContext(), "Restore Successful ✅", Toast.LENGTH_SHORT).show()
                            activity?.recreate()
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) { Toast.makeText(requireContext(), "Restore Failed: ${e.message}", Toast.LENGTH_LONG).show() }
                    }
                }
            }.setNegativeButton("Cancel", null).show()
    }

    private fun resetEverything() {
        lifecycleScope.launch {
            val db = AppDatabase.getInstance(requireContext())
            db.clearAllTables()
            val prefsList = listOf("user_prefs", "settings_prefs", "habit_prefs", "achievement_prefs")
            prefsList.forEach { requireContext().getSharedPreferences(it, Context.MODE_PRIVATE).edit().clear().commit() }
            FirebaseAuth.getInstance().signOut()
            val intent = requireContext().packageManager.getLaunchIntentForPackage(requireContext().packageName)
            intent?.let {
                it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                startActivity(it)
                activity?.finish()
                Runtime.getRuntime().exit(0)
            }
        }
    }

    private fun updateLocale(langCode: String) {
        val locale = Locale(langCode)
        Locale.setDefault(locale)
        val config = resources.configuration
        config.setLocale(locale)
        requireContext().resources.updateConfiguration(config, requireContext().resources.displayMetrics)
        activity?.recreate()
    }
}