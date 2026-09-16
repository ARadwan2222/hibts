package com.yourname.habitapp.ui.onboarding

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.yourname.habitapp.R
import com.yourname.habitapp.ui.MainActivity
import kotlinx.coroutines.launch
import java.util.*

class OnboardingActivity : AppCompatActivity() {

    private var selectedAvatarEmoji = "👤"
    private var selectedImageUri: String? = null
    private var selectedBirthdate: Long? = null
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var googleSignInClient: GoogleSignInClient
    private var lastClickTime: Long = 0

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            selectedImageUri = it.toString()
            updateAvatarUI()
        }
    }

    private val googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        showLoading(false)
        if (result.resultCode == RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                Toast.makeText(this, "Google Error: ${e.statusCode}", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val settingsPrefs = getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)
        val userPrefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        
        val themeName = settingsPrefs.getString("app_theme", "Male")
        setTheme(when(themeName) {
            "Female" -> R.style.Theme_HabitApp_Female
            "Cats" -> R.style.Theme_HabitApp_Cats
            "Dogs" -> R.style.Theme_HabitApp_Dogs
            "Travel" -> R.style.Theme_HabitApp_Travel
            "Nature" -> R.style.Theme_HabitApp_Nature
            "Ocean" -> R.style.Theme_HabitApp_Ocean
            "Sunset" -> R.style.Theme_HabitApp_Sunset
            "Space" -> R.style.Theme_HabitApp_Space
            "Coffee" -> R.style.Theme_HabitApp_Coffee
            "Tech" -> R.style.Theme_HabitApp_Tech
            "Minimal" -> R.style.Theme_HabitApp_Minimal
            "Pastel" -> R.style.Theme_HabitApp_Pastel
            "Vintage" -> R.style.Theme_HabitApp_Vintage
            "Gold" -> R.style.Theme_HabitApp_Gold
            "Classic" -> R.style.Theme_HabitApp_Classic
            else -> R.style.Theme_HabitApp_Male
        })

        super.onCreate(savedInstanceState)
        
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Session Check
        val currentUser = auth.currentUser
        if (userPrefs.getBoolean("onboarding_done", false)) {
            if (currentUser == null || currentUser.isEmailVerified || userPrefs.getString("user_email", "") == "guest@hibts.app") {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
                return
            }
        }

        setContentView(R.layout.activity_onboarding)

        // UI Listeners
        findViewById<View>(R.id.btnGoToRegister).setOnClickListener { switchPage(findViewById(R.id.layoutRegistration)) }
        findViewById<View>(R.id.btnGoToLogin).setOnClickListener { switchPage(findViewById(R.id.layoutLogin)) }
        findViewById<View>(R.id.btnGoogleSignInChoice).setOnClickListener { 
            showLoading(true)
            googleSignInLauncher.launch(googleSignInClient.signInIntent) 
        }
        findViewById<View>(R.id.btnGuestLoginLanding).setOnClickListener { handleGuestLogin() }
        findViewById<View>(R.id.btnRegSelectBirthdate).setOnClickListener { showDatePicker(it as Button) }
        findViewById<View>(R.id.btnDoRegister).setOnClickListener { 
            handleEmailRegister(
                findViewById(R.id.etRegEmail), findViewById(R.id.etRegPassword),
                findViewById(R.id.etRegName), findViewById(R.id.rbRegMale),
                findViewById(R.id.spinnerRegPurpose), findViewById(R.id.cbRegTerms)
            )
        }
        findViewById<View>(R.id.btnDoLogin).setOnClickListener {
            handleEmailLogin(findViewById(R.id.etLoginEmail), findViewById(R.id.etLoginPassword))
        }
        findViewById<View>(R.id.btnConfirmVerify).setOnClickListener { checkVerificationStatus() }
        findViewById<View>(R.id.btnNextGuide).setOnClickListener { switchPage(findViewById(R.id.layoutAuthChoice)) }
        findViewById<View>(R.id.tvBackToLandingFromReg).setOnClickListener { switchPage(findViewById(R.id.layoutAuthChoice)) }
        findViewById<View>(R.id.tvBackToLandingFromLogin).setOnClickListener { switchPage(findViewById(R.id.layoutAuthChoice)) }
        
        // Resume Logic: If user is logged in but unverified, force Verification page
        if (currentUser != null && !currentUser.isEmailVerified && !currentUser.isAnonymous) {
            switchPage(findViewById(R.id.layoutVerification))
            findViewById<TextView>(R.id.tvVerifyDesc).text = getString(R.string.verify_reminder, currentUser.email)
        } else {
            val lastPageId = settingsPrefs.getInt("last_onboarding_page", R.id.layoutGuide)
            val targetLayout = findViewById<View>(lastPageId) ?: findViewById(R.id.layoutGuide)
            switchPage(targetLayout)
        }

        // Lang Spinner
        val spinnerLang = findViewById<Spinner>(R.id.spinnerLanguage)
        val languages = listOf("English", "العربية", "Deutsch")
        spinnerLang.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, languages)
        spinnerLang.setSelection(if (Locale.getDefault().language == "ar") 1 else if (Locale.getDefault().language == "de") 2 else 0)
        spinnerLang.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                val lang = if (pos == 1) "ar" else if (pos == 2) "de" else "en"
                if (lang != Locale.getDefault().language) updateLocale(lang)
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
    }

    override fun onResume() {
        super.onResume()
        checkVerificationStatus(silent = true)
    }

    private fun checkVerificationStatus(silent: Boolean = false) {
        val user = auth.currentUser
        if (user != null && !user.isEmailVerified && !user.isAnonymous) {
            if (!silent) showLoading(true)
            user.reload().addOnCompleteListener {
                if (auth.currentUser?.isEmailVerified == true) {
                    fetchProfileAndGo(user.uid, user.email ?: "")
                } else if (!silent) {
                    showLoading(false)
                    Toast.makeText(this, getString(R.string.verify_reminder, user.email), Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun showDatePicker(btn: Button) {
        try {
            val cal = Calendar.getInstance()
            DatePickerDialog(this, { _, y, m, d ->
                val selected = Calendar.getInstance().apply { set(y, m, d) }
                if (cal.get(Calendar.YEAR) - y in 3..100) {
                    selectedBirthdate = selected.timeInMillis
                    btn.text = "$d/${m + 1}/$y"
                } else {
                    Toast.makeText(this, getString(R.string.age_error), Toast.LENGTH_SHORT).show()
                }
            }, cal.get(Calendar.YEAR) - 20, cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun switchPage(target: View) {
        val pages = listOf(R.id.layoutGuide, R.id.layoutAuthChoice, R.id.layoutRegistration, R.id.layoutLogin, R.id.layoutVerification)
        pages.forEach { id -> findViewById<View>(id).visibility = View.GONE }
        target.visibility = View.VISIBLE
        getSharedPreferences("settings_prefs", Context.MODE_PRIVATE).edit().putInt("last_onboarding_page", target.id).apply()
    }

    private fun showLoading(show: Boolean) {
        findViewById<View>(R.id.layoutLoadingOverlay).visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun handleGuestLogin() {
        saveLocalAndGo("Guest", "guest@hibts.app", "Male", 0L, "Other", "👤")
    }

    private fun handleEmailRegister(etE: EditText, etP: EditText, etN: EditText, rbM: RadioButton, sp: Spinner, cb: CheckBox) {
        val email = etE.text.toString().trim(); val pass = etP.text.toString().trim(); val name = etN.text.toString().trim()
        if (email.isEmpty() || pass.isEmpty() || name.isEmpty() || selectedBirthdate == null || !cb.isChecked) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show(); return
        }
        showLoading(true)
        auth.createUserWithEmailAndPassword(email, pass).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val user = auth.currentUser
                user?.sendEmailVerification()
                val profile = hashMapOf("name" to name, "gender" to (if (rbM.isChecked) "Male" else "Female"), "birthdate" to selectedBirthdate)
                db.collection("users").document(user!!.uid).set(profile).addOnSuccessListener {
                    switchPage(findViewById(R.id.layoutVerification))
                    findViewById<TextView>(R.id.tvVerifyDesc).text = getString(R.string.verification_sent, email)
                    showLoading(false)
                }
            } else {
                showLoading(false)
                Toast.makeText(this, task.exception?.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun handleEmailLogin(etE: EditText, etP: EditText) {
        val email = etE.text.toString().trim(); val pass = etP.text.toString().trim()
        if (email.isEmpty() || pass.isEmpty()) return
        showLoading(true)
        auth.signInWithEmailAndPassword(email, pass).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val user = auth.currentUser
                if (user?.isEmailVerified == true) fetchProfileAndGo(user.uid, email)
                else {
                    showLoading(false)
                    switchPage(findViewById(R.id.layoutVerification))
                    findViewById<TextView>(R.id.tvVerifyDesc).text = getString(R.string.verify_reminder, user?.email ?: "")
                }
            } else {
                showLoading(false)
                Toast.makeText(this, task.exception?.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        showLoading(true)
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val user = auth.currentUser!!
                db.collection("users").document(user.uid).get().addOnSuccessListener { doc ->
                    if (doc.exists()) fetchProfileAndGo(user.uid, user.email ?: "")
                    else {
                        val profile = hashMapOf("name" to user.displayName, "gender" to "Male", "birthdate" to 0L)
                        db.collection("users").document(user.uid).set(profile).addOnSuccessListener {
                            saveLocalAndGo(user.displayName ?: "User", user.email ?: "", "Male", 0L, "Other", "👤")
                        }
                    }
                }
            } else showLoading(false)
        }
    }

    private fun fetchProfileAndGo(uid: String, email: String) {
        showLoading(true)
        db.collection("users").document(uid).get().addOnSuccessListener { doc ->
            saveLocalAndGo(doc.getString("name") ?: "User", email, doc.getString("gender") ?: "Male", 
                doc.getLong("birthdate") ?: 0L, doc.getString("purpose") ?: "Other", doc.getString("avatar") ?: "👤")
        }.addOnFailureListener { showLoading(false) }
    }

    private fun saveLocalAndGo(name: String, email: String, gender: String, birthdate: Long, purpose: String, avatar: String) {
        showLoading(false)
        getSharedPreferences("user_prefs", Context.MODE_PRIVATE).edit().apply {
            putString("user_name", name); putString("user_email", email); putLong("user_birthdate", birthdate)
            putString("user_gender", gender); putString("user_purpose", purpose); putString("user_avatar", avatar)
            putBoolean("onboarding_done", true); apply()
        }
        startActivity(Intent(this, MainActivity::class.java)); finish()
    }

    private fun updateAvatarUI() {
        val ivP = findViewById<ImageView>(R.id.ivProfilePic); val tvA = findViewById<TextView>(R.id.tvAvatarEmoji)
        if (selectedImageUri != null) {
            ivP.setImageURI(Uri.parse(selectedImageUri))
            ivP.visibility = View.VISIBLE; tvA.visibility = View.GONE
        }
    }

    private fun updateLocale(langCode: String) {
        val locale = Locale(langCode); Locale.setDefault(locale)
        val config = resources.configuration; config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics); recreate()
    }
}
