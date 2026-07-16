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
import com.google.firebase.firestore.FirebaseFirestore
import com.yourname.habitapp.R
import com.yourname.habitapp.data.AppDatabase
import com.yourname.habitapp.ui.achievements.AchievementsActivity
import com.yourname.habitapp.utils.AchievementEngine
import com.yourname.habitapp.utils.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
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
        val ringtone = RingtoneManager.getRingtone(requireContext(), uri)
        val title = ringtone.getTitle(requireContext())
        when(key) {
            "tone_start_task" -> view?.findViewById<TextView>(R.id.tvToneStartTask)?.text = title
            "tone_end_task" -> view?.findViewById<TextView>(R.id.tvToneEndTask)?.text = title
            "tone_achievement" -> view?.findViewById<TextView>(R.id.tvToneAchievement)?.text = title
            "tone_year_goal" -> view?.findViewById<TextView>(R.id.tvToneYearGoal)?.text = title
            "tone_habit" -> view?.findViewById<TextView>(R.id.tvToneHabit)?.text = title
            "tone_birthday" -> view?.findViewById<TextView>(R.id.tvToneBirthday)?.text = title
        }
    }

    private fun startTonePicker(code: Int) {
        tonePickerRequestCode = code
        val settingsPrefs = requireContext().getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)
        val key = when(code) {
            1 -> "tone_start_task"
            2 -> "tone_end_task"
            3 -> "tone_achievement"
            4 -> "tone_year_goal"
            5 -> "tone_habit"
            6 -> "tone_birthday"
            else -> "notification_tone"
        }
        val currentUri = settingsPrefs.getString(key, null)?.let { Uri.parse(it) }
        
        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Tone")
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, currentUri)
        pickRingtone.launch(intent)
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
        val btnEditCover = view.findViewById<View>(R.id.btnEditCover)
        val tvBirthdateCountdown = view.findViewById<TextView>(R.id.tvBirthdateCountdown)
        
        val btnAchievementsRow = view.findViewById<View>(R.id.btnViewAchievementsRow)
        val btnBackupRow = view.findViewById<View>(R.id.btnBackupRow)
        
        val switchNotifications = view.findViewById<CompoundButton>(R.id.switchNotifications)
        val switchSound = view.findViewById<CompoundButton>(R.id.switchSound)
        val switchDarkMode = view.findViewById<CompoundButton>(R.id.switchDarkMode)
        val switchVibration = view.findViewById<CompoundButton>(R.id.switchVibration)
        val btnChooseToneRow = view.findViewById<View>(R.id.btnChooseToneRow)
        val tvToneName = view.findViewById<TextView>(R.id.tvSelectedToneName)
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
        view.findViewById<View>(R.id.btnToneYearGoal)?.setOnClickListener { startTonePicker(4) }
        view.findViewById<View>(R.id.btnToneHabit)?.setOnClickListener { startTonePicker(5) }
        view.findViewById<View>(R.id.btnToneBirthday)?.setOnClickListener { startTonePicker(6) }

        // Initial Tone Names
        lifecycleScope.launch {
            val keys = listOf("tone_start_task", "tone_end_task", "tone_achievement", "tone_year_goal", "tone_habit", "tone_birthday")
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

        // Set Version Name
        try {
            val pInfo = requireContext().packageManager.getPackageInfo(requireContext().packageName, 0)
            tvVersion.text = getString(R.string.version_prefix).format(pInfo.versionName)
        } catch (e: Exception) {
            tvVersion.text = getString(R.string.version_prefix).format("1.2.0")
        }

        // User Data Display
        val name = prefs.getString("user_name", "User")
        val email = prefs.getString("user_email", "")
        val isGuest = email == "guest@hibts.app"
        
        if (isGuest) {
            btnBackupRow.isEnabled = false
            btnBackupRow.alpha = 0.5f
            Toast.makeText(requireContext(), "بعض المميزات (مثل النسخ السحابي) معطلة في وضع الضيف", Toast.LENGTH_LONG).show()
        }

        val imageUriString = prefs.getString("user_image", null)
        val coverUriString = prefs.getString("user_cover", null)
        val birthdateMillis = prefs.getLong("user_birthdate", 0)
        
        val xp = AchievementEngine.getTotalXP(requireContext())
        val (level, levelName) = AchievementEngine.getLevel(requireContext(), xp)

        tvName.text = name
        tvXP.text = getString(R.string.level_display).format(level, levelName) + " | " + getString(R.string.total_xp).format(xp)

        // Birthday Countdown
        if (birthdateMillis > 0) {
            val calBirth = Calendar.getInstance().apply { timeInMillis = birthdateMillis }
            val calNow = Calendar.getInstance()
            calBirth.set(Calendar.YEAR, calNow.get(Calendar.YEAR))
            if (calBirth.before(calNow)) calBirth.add(Calendar.YEAR, 1)
            val diff = calBirth.timeInMillis - calNow.timeInMillis
            val days = diff / (1000 * 60 * 60 * 24)
            tvBirthdateCountdown.text = if (days == 0L) getString(R.string.yes) else "Birthday in $days days 🎂"
        }

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

        if (coverUriString != null) {
            try {
                ivProfileCover.setImageURI(Uri.parse(coverUriString))
            } catch (e: Exception) {}
        }

        ivProfilePic.setOnClickListener { pickImage.launch("image/*") }
        ivAvatarPlaceholder.setOnClickListener { pickImage.launch("image/*") }
        btnEditCover?.setOnClickListener { pickCover.launch("image/*") }

        btnAchievementsRow.setOnClickListener { startActivity(Intent(requireContext(), AchievementsActivity::class.java)) }
        
        btnHelpCenterRow.setOnClickListener { 
            AlertDialog.Builder(requireContext(), R.style.PurpleAlertDialog)
                .setTitle(getString(R.string.how_to_use_label))
                .setMessage(getString(R.string.terms_of_use_text)) 
                .setPositiveButton("OK", null)
                .show()
        }

        // Terms of Use
        btnTermsRow.setOnClickListener {
            AlertDialog.Builder(requireContext(), R.style.PurpleAlertDialog)
                .setTitle(R.string.terms_of_use)
                .setMessage(android.text.Html.fromHtml(getString(R.string.terms_of_use_text), android.text.Html.FROM_HTML_MODE_LEGACY))
                .setPositiveButton("OK", null)
                .show()
        }

        // Backup Data
        btnBackupRow.setOnClickListener { performBackup() }

        // Logout - Keep Local Data
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

        // Reset App - Delete Local Data
        btnResetAppRow.setOnClickListener {
            AlertDialog.Builder(requireContext(), R.style.PurpleAlertDialog)
                .setTitle(getString(R.string.reset_app_data))
                .setMessage(getString(R.string.reset_app_warning))
                .setPositiveButton(getString(R.string.yes)) { _, _ -> resetEverything() }
                .setNegativeButton(getString(R.string.no), null)
                .show()
        }

        // Settings Listeners
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

        // Language Spinner
        val languages = listOf("English", "العربية", "Deutsch")
        spinnerLang.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, languages)
        val currentLang = settingsPrefs.getString("lang", "en")
        val selection = when(currentLang) {
            "ar" -> 1
            "de" -> 2
            else -> 0
        }
        spinnerLang.setSelection(selection)
        spinnerLang.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                val lang = when(pos) {
                    1 -> "ar"
                    2 -> "de"
                    else -> "en"
                }
                if (lang != currentLang) {
                    settingsPrefs.edit().putString("lang", lang).apply()
                    updateLocale(lang)
                }
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
    }

    private fun performBackup() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            Toast.makeText(requireContext(), "يرجى تسجيل الدخول أولاً للنسخ الاحتياطي", Toast.LENGTH_LONG).show()
            return
        }

        lifecycleScope.launch {
            val db = AppDatabase.getInstance(requireContext())
            val habits = withContext(Dispatchers.IO) { db.habitDao().getAllHabitsSync() }
            val todos = withContext(Dispatchers.IO) { db.todoDao().getAllTodosSync() }

            val backupData = hashMapOf(
                "habits" to habits,
                "todos" to todos,
                "timestamp" to System.currentTimeMillis()
            )

            FirebaseFirestore.getInstance().collection("backups").document(user.uid)
                .set(backupData)
                .addOnSuccessListener { Toast.makeText(requireContext(), "تم النسخ الاحتياطي بنجاح ✅", Toast.LENGTH_SHORT).show() }
                .addOnFailureListener { e -> Toast.makeText(requireContext(), "فشل النسخ: ${e.message}", Toast.LENGTH_SHORT).show() }
        }
    }

    private fun resetEverything() {
        val context = requireContext()
        val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val lastEmail = prefs.getString("user_email", "")
        
        context.getSharedPreferences("persistent_prefs", Context.MODE_PRIVATE)
            .edit().putString("last_logged_in_email", lastEmail).commit()

        FirebaseAuth.getInstance().signOut()
        val prefsList = listOf("user_prefs", "settings_prefs", "habit_prefs", "achievement_prefs")
        prefsList.forEach { context.getSharedPreferences(it, Context.MODE_PRIVATE).edit().clear().commit() }

        lifecycleScope.launch {
            val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
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