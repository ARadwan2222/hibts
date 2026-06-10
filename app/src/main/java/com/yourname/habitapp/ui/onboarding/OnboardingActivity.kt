package com.yourname.habitapp.ui.onboarding

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.yourname.habitapp.R
import com.yourname.habitapp.data.AppDatabase
import com.yourname.habitapp.ui.MainActivity
import kotlinx.coroutines.launch
import java.util.*

class OnboardingActivity : AppCompatActivity() {

    private var selectedAvatarEmoji = "👤"
    private var selectedImageUri: String? = null
    private var selectedBirthdate: Long? = null
    private var languageChosen = false
    private lateinit var gestureDetector: GestureDetector
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var googleSignInClient: GoogleSignInClient

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            selectedImageUri = it.toString()
            updateAvatarUI()
        }
    }

    private val googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                showLoading(false)
                Toast.makeText(this, "Google sign in failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            showLoading(false)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val prefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val gender = prefs.getString("user_gender", "Male")
        if (gender == "Female") {
            setTheme(R.style.Theme_HabitApp_Female)
        } else {
            setTheme(R.style.Theme_HabitApp_Male)
        }

        val settingsPrefs = getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)
        if (settingsPrefs.getBoolean("dark_mode", false)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
        languageChosen = settingsPrefs.getBoolean("language_chosen", false)

        super.onCreate(savedInstanceState)
        
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        val onboardingDone = prefs.getBoolean("onboarding_done", false)

        setContentView(R.layout.activity_onboarding)

        // Main Layouts
        val layoutGuide = findViewById<View>(R.id.layoutGuide)
        val layoutAuthChoice = findViewById<View>(R.id.layoutAuthChoice)
        val layoutRegistration = findViewById<View>(R.id.layoutRegistration)
        val layoutLogin = findViewById<View>(R.id.layoutLogin)
        val layoutVerification = findViewById<View>(R.id.layoutVerification)

        val currentUser = auth.currentUser
        
        // Auto-login check
        if (currentUser != null) {
            if (currentUser.isEmailVerified && onboardingDone) {
                applyUserTheme(prefs.getString("user_gender", "Male"))
                startActivity(Intent(this, MainActivity::class.java))
                finish()
                return
            } else if (!currentUser.isEmailVerified) {
                switchPage(layoutVerification)
                findViewById<TextView>(R.id.tvVerifyDesc).text = getString(R.string.verify_reminder, currentUser.email)
            }
        } else if (onboardingDone) {
            // Guest auto-login check
            applyUserTheme(prefs.getString("user_gender", "Male"))
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        // Choice Landing Logic
        findViewById<View>(R.id.btnGoToRegister).setOnClickListener { switchPage(layoutRegistration) }
        findViewById<View>(R.id.btnGoToLogin).setOnClickListener { switchPage(layoutLogin) }
        findViewById<View>(R.id.btnGoogleSignInChoice).setOnClickListener { 
            showLoading(true, getString(R.string.sign_in_google))
            googleSignInLauncher.launch(googleSignInClient.signInIntent) 
        }
        findViewById<View>(R.id.btnGuestLoginLanding).setOnClickListener { handleGuestLogin() }

        // Back Buttons
        findViewById<View>(R.id.tvBackToLandingFromReg).setOnClickListener { switchPage(layoutAuthChoice) }
        findViewById<View>(R.id.tvBackToLandingFromLogin).setOnClickListener { switchPage(layoutAuthChoice) }
        
        // Shortcuts between Login and Register
        findViewById<View>(R.id.tvSwitchToLogin).setOnClickListener { switchPage(layoutLogin) }
        findViewById<View>(R.id.tvSwitchToRegister).setOnClickListener { switchPage(layoutRegistration) }

        // Registration Views
        val etRegName = findViewById<EditText>(R.id.etRegName)
        val etRegEmail = findViewById<EditText>(R.id.etRegEmail)
        val etRegPassword = findViewById<EditText>(R.id.etRegPassword)
        val rbRegMale = findViewById<RadioButton>(R.id.rbRegMale)
        val btnRegBirth = findViewById<Button>(R.id.btnRegSelectBirthdate)
        val spinnerRegPurpose = findViewById<Spinner>(R.id.spinnerRegPurpose)
        val cbRegTerms = findViewById<CheckBox>(R.id.cbRegTerms)

        btnRegBirth.setOnClickListener { showDatePicker(btnRegBirth) }
        setupPurposeSpinner(spinnerRegPurpose)

        findViewById<View>(R.id.tvRegTermsLink).setOnClickListener {
            AlertDialog.Builder(this, R.style.PurpleAlertDialog)
                .setTitle(R.string.terms_of_use)
                .setMessage(android.text.Html.fromHtml(getString(R.string.terms_of_use_text), android.text.Html.FROM_HTML_MODE_LEGACY))
                .setPositiveButton("OK", null)
                .show()
        }

        findViewById<View>(R.id.btnDoRegister).setOnClickListener {
            handleEmailRegister(etRegEmail, etRegPassword, etRegName, rbRegMale, spinnerRegPurpose, cbRegTerms)
        }

        // Login Views
        val etLoginEmail = findViewById<EditText>(R.id.etLoginEmail)
        val etLoginPassword = findViewById<EditText>(R.id.etLoginPassword)
        findViewById<View>(R.id.btnDoLogin).setOnClickListener {
            handleEmailLogin(etLoginEmail, etLoginPassword)
        }

        // Verification logic
        findViewById<View>(R.id.btnConfirmVerify).setOnClickListener {
            showLoading(true)
            val user = auth.currentUser
            user?.reload()?.addOnCompleteListener {
                val updated = auth.currentUser
                if (updated != null && updated.isEmailVerified) {
                    fetchProfileAndGo(updated.uid, updated.email ?: "")
                } else {
                    showLoading(false)
                    Toast.makeText(this, getString(R.string.verify_reminder, updated?.email ?: ""), Toast.LENGTH_LONG).show()
                }
            } ?: showLoading(false)
        }

        findViewById<View>(R.id.btnResendEmail).setOnClickListener {
            auth.currentUser?.sendEmailVerification()?.addOnCompleteListener { task ->
                if (task.isSuccessful) Toast.makeText(this, "✅", Toast.LENGTH_SHORT).show()
            }
        }

        // Guide / ViewFlipper Logic
        val guideFlipper = findViewById<ViewFlipper>(R.id.guideFlipper)
        val nextStep = {
            if (guideFlipper.displayedChild < guideFlipper.childCount - 1) {
                guideFlipper.setInAnimation(this, R.anim.slide_in_right)
                guideFlipper.setOutAnimation(this, R.anim.slide_out_left)
                guideFlipper.showNext()
            } else {
                switchPage(layoutAuthChoice)
            }
        }
        findViewById<View>(R.id.btnNextGuide).setOnClickListener { nextStep() }
        
        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(e1: MotionEvent?, e2: MotionEvent, vX: Float, vY: Float): Boolean {
                if (e1 != null && e2.x - e1.x < -100) { nextStep(); return true }
                return false
            }
        })
        layoutGuide.setOnTouchListener { _, event -> gestureDetector.onTouchEvent(event); true }

        // Avatar Container Click
        findViewById<View>(R.id.layoutAvatarContainer).setOnClickListener {
            val options = arrayOf("Choose Avatar", "Pick from Gallery")
            AlertDialog.Builder(this, R.style.PurpleAlertDialog)
                .setTitle("Select Profile Picture")
                .setItems(options) { _, which ->
                    if (which == 0) showAvatarEmojiDialog() else pickImage.launch("image/*")
                }.show()
        }

        // Language Spinner Logic
        val spinnerLang = findViewById<Spinner>(R.id.spinnerLanguage)
        val languages = listOf("English", "العربية", "Deutsch")
        spinnerLang.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, languages)
        spinnerLang.setSelection(if (Locale.getDefault().language == "ar") 1 else if (Locale.getDefault().language == "de") 2 else 0)
        spinnerLang.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                val lang = if (pos == 1) "ar" else if (pos == 2) "de" else "en"
                if (lang != Locale.getDefault().language) {
                    getSharedPreferences("settings_prefs", Context.MODE_PRIVATE).edit().putBoolean("language_chosen", true).apply()
                    updateLocale(lang)
                }
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
        
        // Initial visibility check
        switchPage(layoutGuide)
    }

    private fun switchPage(target: View) {
        val pages = listOf(
            R.id.layoutGuide, R.id.layoutAuthChoice, R.id.layoutRegistration, 
            R.id.layoutLogin, R.id.layoutVerification
        )
        pages.forEach { id -> findViewById<View>(id).visibility = View.GONE }
        target.visibility = View.VISIBLE

        // Language chooser remains visible in ALL welcoming screens as requested
        val isWelcomingPage = target.id == R.id.layoutGuide || target.id == R.id.layoutAuthChoice || 
                              target.id == R.id.layoutRegistration || target.id == R.id.layoutLogin
        findViewById<View>(R.id.layoutLanguageContainer).visibility = if (isWelcomingPage) View.VISIBLE else View.GONE

        // Guide bottom button visibility
        findViewById<View>(R.id.layoutBottomButtons).visibility = if (target.id == R.id.layoutGuide) View.VISIBLE else View.GONE
    }

    private fun showLoading(show: Boolean, text: String? = null) {
        val overlay = findViewById<View>(R.id.layoutLoadingOverlay)
        overlay.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun clearLocalData() {
        val prefsToClear = listOf("user_prefs", "settings_prefs", "habit_prefs", "achievement_prefs")
        prefsToClear.forEach { getSharedPreferences(it, Context.MODE_PRIVATE).edit().clear().commit() }
        try { 
            AppDatabase.closeInstance()
            deleteDatabase("habit_app_database")
        } catch (e: Exception) { }
    }

    private fun handleEmailRegister(etE: EditText, etP: EditText, etN: EditText, rbM: RadioButton, sp: Spinner, cb: CheckBox) {
        val email = etE.text.toString().trim()
        val pass = etP.text.toString().trim()
        val name = etN.text.toString().trim()
        
        var hasError = false
        if (email.isEmpty()) { etE.error = getString(R.string.error_empty_field); hasError = true }
        if (pass.isEmpty()) { etP.error = getString(R.string.error_empty_field); hasError = true }
        if (name.isEmpty()) { etN.error = getString(R.string.error_empty_field); hasError = true }
        if (hasError) return

        if (selectedBirthdate == null) { Toast.makeText(this, R.string.onboarding_age_hint, Toast.LENGTH_SHORT).show(); return }
        if (!cb.isChecked) { Toast.makeText(this, R.string.error_accept_terms, Toast.LENGTH_SHORT).show(); return }

        showLoading(true, getString(R.string.register))

        lifecycleScope.launch {
            clearLocalData()
            auth.createUserWithEmailAndPassword(email, pass).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    user?.sendEmailVerification()
                    val profile = hashMapOf(
                        "email" to email, "name" to name, "birthdate" to selectedBirthdate,
                        "gender" to (if (rbM.isChecked) "Male" else "Female"),
                        "purpose" to sp.selectedItem.toString(), "avatar" to selectedAvatarEmoji
                    )
                    db.collection("users").document(user!!.uid).set(profile)
                        .addOnSuccessListener {
                            showLoading(false)
                            switchPage(findViewById(R.id.layoutVerification))
                            findViewById<TextView>(R.id.tvVerifyDesc).text = getString(R.string.verification_sent, email)
                        }
                        .addOnFailureListener {
                            showLoading(false)
                            Toast.makeText(this@OnboardingActivity, "Profile save failed", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    showLoading(false)
                    Toast.makeText(this@OnboardingActivity, task.exception?.localizedMessage, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun handleEmailLogin(etE: EditText, etP: EditText) {
        val email = etE.text.toString().trim()
        val pass = etP.text.toString().trim()
        
        var hasError = false
        if (email.isEmpty()) { etE.error = getString(R.string.error_empty_field); hasError = true }
        if (pass.isEmpty()) { etP.error = getString(R.string.error_empty_field); hasError = true }
        if (hasError) return

        showLoading(true, getString(R.string.login))

        auth.signInWithEmailAndPassword(email, pass).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val user = auth.currentUser
                if (user?.isEmailVerified == true) {
                    val lastUserEmail = getSharedPreferences("persistent_prefs", Context.MODE_PRIVATE)
                        .getString("last_logged_in_email", "")
                    
                    lifecycleScope.launch {
                        // Clear data ONLY if the new user is different from the last logged in user
                        if (email != lastUserEmail && lastUserEmail != "") {
                            clearLocalData()
                        }
                        // Update persistent email
                        getSharedPreferences("persistent_prefs", Context.MODE_PRIVATE)
                            .edit().putString("last_logged_in_email", email).apply()

                        fetchProfileAndGo(user.uid, user.email ?: "")
                    }
                } else {
                    showLoading(false)
                    switchPage(findViewById(R.id.layoutVerification))
                    findViewById<TextView>(R.id.tvVerifyDesc).text = getString(R.string.verify_reminder, user?.email ?: "")
                }
            } else {
                showLoading(false)
                Toast.makeText(this, task.exception?.localizedMessage, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun handleGuestLogin() {
        showLoading(true, getString(R.string.guest_login))
        lifecycleScope.launch {
            try { FirebaseAuth.getInstance().signOut() } catch (e: Exception) {}
            clearLocalData()
            // Reset last logged in email when using Guest
            getSharedPreferences("persistent_prefs", Context.MODE_PRIVATE)
                .edit().putString("last_logged_in_email", "guest").apply()
            
            saveLocalAndGo(getString(R.string.guest_login), "guest@hibts.app", "Male", 0L, getString(R.string.app_purpose), "👤")
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        showLoading(true, getString(R.string.sign_in_google))
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val user = auth.currentUser!!
                val email = user.email ?: ""
                val lastUserEmail = getSharedPreferences("persistent_prefs", Context.MODE_PRIVATE)
                    .getString("last_logged_in_email", "")

                lifecycleScope.launch {
                    if (email != lastUserEmail && lastUserEmail != "") {
                        clearLocalData()
                    }
                    getSharedPreferences("persistent_prefs", Context.MODE_PRIVATE)
                        .edit().putString("last_logged_in_email", email).apply()

                    db.collection("users").document(user.uid).get()
                        .addOnSuccessListener { doc ->
                            if (doc.exists()) {
                                fetchProfileAndGo(user.uid, user.email ?: "")
                            } else {
                                val profile = hashMapOf(
                                    "email" to user.email, "name" to user.displayName, "birthdate" to 0L,
                                    "gender" to "Male", "purpose" to "", "avatar" to "👤"
                                )
                                db.collection("users").document(user.uid).set(profile).addOnSuccessListener {
                                    saveLocalAndGo(user.displayName ?: "User", user.email ?: "", "Male", 0L, "", "👤")
                                }.addOnFailureListener { showLoading(false) }
                            }
                        }
                        .addOnFailureListener {
                            showLoading(false)
                            Toast.makeText(this@OnboardingActivity, "Profile fetch failed", Toast.LENGTH_SHORT).show()
                        }
                }
            } else {
                showLoading(false)
                Toast.makeText(this, "Firebase Auth failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun fetchProfileAndGo(uid: String, email: String) {
        showLoading(true)
        db.collection("users").document(uid).get().addOnSuccessListener { doc ->
            if (doc.exists()) {
                FirebaseFirestore.getInstance().collection("backups").document(uid).get()
                    .addOnSuccessListener { backupDoc ->
                        if (backupDoc.exists()) {
                            Toast.makeText(this, "تم استعادة البيانات بنجاح ✅", Toast.LENGTH_SHORT).show()
                        }
                        saveLocalAndGo(doc.getString("name") ?: "User", email, doc.getString("gender") ?: "Male", 
                            doc.getLong("birthdate") ?: 0L, doc.getString("purpose") ?: "", doc.getString("avatar") ?: "👤")
                    }
                    .addOnFailureListener {
                        saveLocalAndGo(doc.getString("name") ?: "User", email, doc.getString("gender") ?: "Male", 
                            doc.getLong("birthdate") ?: 0L, doc.getString("purpose") ?: "", doc.getString("avatar") ?: "👤")
                    }
            } else {
                showLoading(false)
                Toast.makeText(this, "No Profile Found", Toast.LENGTH_SHORT).show()
                switchPage(findViewById(R.id.layoutRegistration))
            }
        }.addOnFailureListener { showLoading(false) }
    }

    private fun saveLocalAndGo(name: String, email: String, gender: String, birthdate: Long, purpose: String, avatar: String) {
        showLoading(false)
        val prefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString("user_name", name); putString("user_email", email); putLong("user_birthdate", birthdate)
            putString("user_gender", gender); putString("user_purpose", purpose); putString("user_avatar", avatar)
            putBoolean("onboarding_done", true); apply()
        }
        applyUserTheme(gender)
        startActivity(Intent(this, MainActivity::class.java)); finish()
    }

    private fun showDatePicker(btn: Button) {
        val cal = Calendar.getInstance()
        DatePickerDialog(this, R.style.PurpleAlertDialog, { _, y, m, d ->
            val selected = Calendar.getInstance().apply { set(y, m, d) }
            if (cal.get(Calendar.YEAR) - y in 6..100) {
                selectedBirthdate = selected.timeInMillis
                btn.text = String.format(Locale.getDefault(), "%02d/%02d/%d", d, m + 1, y)
            } else Toast.makeText(this, getString(R.string.age_error), Toast.LENGTH_SHORT).show()
        }, cal.get(Calendar.YEAR) - 20, cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun setupPurposeSpinner(spinner: Spinner) {
        val purposes = listOf(getString(R.string.purpose_work), getString(R.string.purpose_sport), getString(R.string.purpose_health), getString(R.string.purpose_learning), getString(R.string.purpose_other))
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, purposes)
    }

    private fun updateAvatarUI() {
        val ivP = findViewById<ImageView>(R.id.ivProfilePic)
        val tvA = findViewById<TextView>(R.id.tvAvatarEmoji)
        if (selectedImageUri != null) {
            ivP.setImageURI(Uri.parse(selectedImageUri))
            ivP.visibility = View.VISIBLE; tvA.visibility = View.GONE
        } else {
            ivP.visibility = View.GONE; tvA.text = selectedAvatarEmoji; tvA.visibility = View.VISIBLE
        }
    }

    private fun showAvatarEmojiDialog() {
        val avatars = listOf("👤", "🐱", "🐶", "🦊", "🦁", "🤖", "🚀", "🌈")
        AlertDialog.Builder(this, R.style.PurpleAlertDialog).setItems(avatars.toTypedArray()) { _, which ->
            selectedAvatarEmoji = avatars[which]; selectedImageUri = null; updateAvatarUI()
        }.show()
    }

    private fun updateLocale(langCode: String) {
        val locale = Locale(langCode); Locale.setDefault(locale)
        val config = resources.configuration; config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics); recreate()
    }

    private fun applyUserTheme(gender: String?) {
        setTheme(if (gender == "Female") R.style.Theme_HabitApp_Female else R.style.Theme_HabitApp_Male)
    }
}
