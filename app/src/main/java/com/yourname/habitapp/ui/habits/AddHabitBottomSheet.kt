package com.yourname.habitapp.ui.habits

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.yourname.habitapp.R
import com.yourname.habitapp.data.AppDatabase
import com.yourname.habitapp.data.models.Habit
import com.yourname.habitapp.data.models.HabitCategory
import com.yourname.habitapp.data.models.HabitFrequency
import com.yourname.habitapp.utils.AchievementEngine
import com.yourname.habitapp.utils.HabitTemplates
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class AddHabitBottomSheet : BottomSheetDialogFragment() {

    private var selectedCategory: HabitCategory = HabitCategory.OTHER
    private var selectedFrequency: HabitFrequency = HabitFrequency.DAILY
    private var selectedSpecificDay: Int? = null
    private var selectedTargetDate: Long? = null
    private var editingHabit: Habit? = null
    private var showFrequencyButton: Boolean = false

    companion object {
        private const val ARG_HABIT_ID = "habit_id"
        private const val ARG_FREQUENCY = "frequency"
        private const val ARG_SHOW_FREQ_BTN = "show_freq_btn"
        private const val ARG_TARGET_DATE = "target_date"

        fun newInstance(habitId: Int = -1, frequency: HabitFrequency? = null, showFreqBtn: Boolean = false, targetDate: Long? = null): AddHabitBottomSheet {
            return AddHabitBottomSheet().apply {
                arguments = Bundle().apply {
                    putInt(ARG_HABIT_ID, habitId)
                    frequency?.let { putString(ARG_FREQUENCY, it.name) }
                    putBoolean(ARG_SHOW_FREQ_BTN, showFreqBtn)
                    targetDate?.let { putLong(ARG_TARGET_DATE, it) }
                }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.bottom_sheet_add_habit, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val habitId = arguments?.getInt(ARG_HABIT_ID) ?: -1
        val freqName = arguments?.getString(ARG_FREQUENCY)
        showFrequencyButton = arguments?.getBoolean(ARG_SHOW_FREQ_BTN) ?: false
        if (arguments?.containsKey(ARG_TARGET_DATE) == true) {
            selectedTargetDate = arguments?.getLong(ARG_TARGET_DATE)
        }

        if (freqName != null) {
            selectedFrequency = HabitFrequency.valueOf(freqName)
        }

        val etName = view.findViewById<EditText>(R.id.etHabitName)
        val btnCat = view.findViewById<Button>(R.id.btnSelectCategory)
        val btnFreq = view.findViewById<Button>(R.id.btnSelectFrequency)
        val tvIcon = view.findViewById<TextView>(R.id.tvSelectedIcon)
        val btnSave = view.findViewById<Button>(R.id.btnSaveHabit)
        val layoutSpec = view.findViewById<LinearLayout>(R.id.layoutSpecificDay)
        val btnSpecDay = view.findViewById<Button>(R.id.btnSelectSpecificDay)
        val chipGroupSugg = view.findViewById<ChipGroup>(R.id.chipGroupSuggestions)

        // Suggestions Toggle
        val layoutToggle = view.findViewById<View>(R.id.layoutSuggestionsToggle)
        val scrollSuggestions = view.findViewById<View>(R.id.scrollSuggestions)
        val ivArrow = view.findViewById<ImageView>(R.id.ivSuggestionsArrow)
        layoutToggle.setOnClickListener {
            val isVisible = scrollSuggestions.visibility == View.VISIBLE
            scrollSuggestions.visibility = if (isVisible) View.GONE else View.VISIBLE
            ivArrow?.animate()?.rotation(if (isVisible) 0f else 180f)?.setDuration(200)?.start()
        }

        btnFreq.visibility = if (showFrequencyButton) View.VISIBLE else View.GONE
        if (freqName != null) {
            btnFreq.text = getFrequencyText(selectedFrequency)
        }
        updateSpecificDayUI(layoutSpec, btnSpecDay)

        loadSuggestions(chipGroupSugg, etName, tvIcon, HabitCategory.OTHER)

        if (habitId != -1) {
            lifecycleScope.launch {
                editingHabit = AppDatabase.getInstance(requireContext()).habitDao().getHabitById(habitId)
                editingHabit?.let {
                    etName.setText(it.name)
                    selectedCategory = it.category
                    selectedFrequency = it.frequency
                    selectedSpecificDay = it.specificDay
                    tvIcon.text = it.icon
                    btnCat.text = getCategoryName(it.category)
                    btnFreq.text = getFrequencyText(it.frequency)
                    updateSpecificDayUI(layoutSpec, btnSpecDay)
                    loadSuggestions(chipGroupSugg, etName, tvIcon, it.category)
                    btnSave.text = getString(R.string.update)
                    // If editing, usually we show frequency if it was not daily
                    if (it.frequency != HabitFrequency.DAILY) btnFreq.visibility = View.VISIBLE
                }
            }
        }

        btnCat.setOnClickListener {
            val categories = HabitCategory.entries.toTypedArray()
            val names = categories.map { getCategoryName(it) }.toTypedArray()
            AlertDialog.Builder(requireContext(), R.style.PurpleAlertDialog)
                .setTitle(getString(R.string.select_category))
                .setItems(names) { _, which ->
                    selectedCategory = categories[which]
                    btnCat.text = names[which]
                    updateIcon(tvIcon, selectedCategory)
                    loadSuggestions(chipGroupSugg, etName, tvIcon, selectedCategory)
                }.show()
        }

        btnFreq.setOnClickListener {
            val freqs = arrayOf(getString(R.string.daily), getString(R.string.weekly), getString(R.string.monthly))
            AlertDialog.Builder(requireContext(), R.style.PurpleAlertDialog)
                .setTitle(getString(R.string.habit_frequency))
                .setItems(freqs) { _, which ->
                    selectedFrequency = when(which) {
                        0 -> HabitFrequency.DAILY
                        1 -> HabitFrequency.WEEKLY
                        else -> HabitFrequency.MONTHLY
                    }
                    btnFreq.text = freqs[which]
                    selectedSpecificDay = null
                    updateSpecificDayUI(layoutSpec, btnSpecDay)
                }.show()
        }

        btnSpecDay.setOnClickListener {
            if (selectedFrequency == HabitFrequency.WEEKLY) {
                val days = arrayOf(
                    getString(R.string.yes), // Placeholder for weekday names if I had them, but I'll use a better way
                    "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"
                )
                // Actually I should get weekday names properly
                val weekdayNames = (1..7).map { getDayName(it) }.toTypedArray()
                AlertDialog.Builder(requireContext(), R.style.PurpleAlertDialog)
                    .setTitle(getString(R.string.select_weekday))
                    .setItems(weekdayNames) { _, which ->
                        selectedSpecificDay = which + 1
                        btnSpecDay.text = weekdayNames[which]
                    }.show()
            } else if (selectedFrequency == HabitFrequency.MONTHLY) {
                val days = (1..31).map { it.toString() }.toTypedArray()
                AlertDialog.Builder(requireContext(), R.style.PurpleAlertDialog)
                    .setTitle(getString(R.string.select_monthday))
                    .setItems(days) { _, which ->
                        selectedSpecificDay = which + 1
                        btnSpecDay.text = getString(R.string.day_of_month_label).format(which + 1)
                    }.show()
            }
        }

        btnSave.setOnClickListener {
            val name = etName.text.toString().trim()
            if (name.isEmpty()) { etName.error = "أدخل اسم العادة"; return@setOnClickListener }

            val wordCount = name.split("\\s+".toRegex()).filter { it.isNotEmpty() }.size
            if (wordCount > 4) {
                etName.error = getString(R.string.error_max_words)
                return@setOnClickListener
            }

            if (selectedFrequency != HabitFrequency.DAILY && selectedSpecificDay == null) {
                Toast.makeText(requireContext(), "يرجى اختيار يوم التكرار", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val habit = editingHabit?.copy(
                name = name,
                category = selectedCategory,
                frequency = selectedFrequency,
                specificDay = selectedSpecificDay,
                icon = tvIcon.text.toString(),
                targetDate = selectedTargetDate
            ) ?: Habit(
                name = name,
                category = selectedCategory,
                frequency = selectedFrequency,
                specificDay = selectedSpecificDay,
                icon = tvIcon.text.toString(),
                targetDate = selectedTargetDate
            )

            lifecycleScope.launch {
                val dao = AppDatabase.getInstance(requireContext()).habitDao()
                val allHabits = dao.getAllHabitsSync()
                val isDuplicate = allHabits.any { it.name.trim().equals(name, ignoreCase = true) && it.id != (editingHabit?.id ?: -1) }
                
                if (isDuplicate) {
                    Toast.makeText(requireContext(), "هذه العادة موجودة بالفعل!", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val minOrder = allHabits.minOfOrNull { it.displayOrder } ?: 0
                val habitToSave = if (editingHabit != null) habit else habit.copy(displayOrder = minOrder - 1)

                if (editingHabit != null) dao.updateHabit(habitToSave) else dao.insertHabit(habitToSave)
                
                if (editingHabit == null) {
                    val count = dao.getHabitCount()
                    AchievementEngine.checkAndUnlock(requireContext(), "HABIT_ADDED", count)
                }
                dismiss()
            }
        }
    }

    private fun getFrequencyText(freq: HabitFrequency): String = when(freq) {
        HabitFrequency.DAILY -> getString(R.string.daily)
        HabitFrequency.WEEKLY -> getString(R.string.weekly)
        HabitFrequency.MONTHLY -> getString(R.string.monthly)
        else -> ""
    }

    private fun updateSpecificDayUI(layout: View, button: Button) {
        when (selectedFrequency) {
            HabitFrequency.DAILY -> {
                layout.visibility = View.GONE
            }
            HabitFrequency.WEEKLY -> {
                layout.visibility = View.VISIBLE
                button.text = selectedSpecificDay?.let { getDayName(it) } ?: getString(R.string.select_day)
            }
            HabitFrequency.MONTHLY -> {
                layout.visibility = View.VISIBLE
                button.text = selectedSpecificDay?.let { getString(R.string.day_of_month_label).format(it) } ?: getString(R.string.select_day)
            }
            else -> layout.visibility = View.GONE
        }
    }

    private fun getDayName(day: Int): String {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_WEEK, day)
        return SimpleDateFormat("EEEE", Locale.getDefault()).format(calendar.time)
    }

    private fun getCategoryName(category: HabitCategory): String {
        val resId = when(category) {
            HabitCategory.HEALTH -> R.string.category_health
            HabitCategory.FITNESS -> R.string.category_fitness
            HabitCategory.PRODUCTIVITY -> R.string.category_productivity
            HabitCategory.LEARNING -> R.string.category_learning
            HabitCategory.SPIRITUAL -> R.string.category_spiritual
            HabitCategory.SOCIAL -> R.string.category_social
            HabitCategory.FINANCE -> R.string.category_finance
            HabitCategory.HOBBY -> R.string.category_hobby
            HabitCategory.HOME -> R.string.category_home
            HabitCategory.SELF_IMPROVEMENT -> R.string.category_self_improvement
            else -> R.string.category_other
        }
        return getString(resId)
    }

    private fun loadSuggestions(group: ChipGroup, et: EditText, tvIcon: TextView, category: HabitCategory) {
        group.removeAllViews()
        val suggestions = if (category == HabitCategory.OTHER) {
            HabitTemplates.ALL_SUGGESTIONS.take(8)
        } else {
            HabitTemplates.ALL_SUGGESTIONS.filter { it.category == category }
        }
        
        suggestions.forEach { suggestion ->
            val chip = Chip(requireContext()).apply {
                text = "${suggestion.icon} ${suggestion.name}"
                setOnClickListener {
                    et.setText(suggestion.name)
                    tvIcon.text = suggestion.icon
                    selectedCategory = suggestion.category
                    // Hide after selection
                    view?.findViewById<View>(R.id.scrollSuggestions)?.visibility = View.GONE
                    view?.findViewById<ImageView>(R.id.ivSuggestionsArrow)?.rotation = 0f
                }
            }
            group.addView(chip)
        }
    }

    private fun updateIcon(tvIcon: TextView, category: HabitCategory) {
        tvIcon.text = when (category) {
            HabitCategory.HEALTH -> "🍎"
            HabitCategory.FITNESS -> "🏋️"
            HabitCategory.PRODUCTIVITY -> "💻"
            HabitCategory.LEARNING -> "📚"
            HabitCategory.SPIRITUAL -> "🕌"
            HabitCategory.SOCIAL -> "🤝"
            HabitCategory.FINANCE -> "💰"
            HabitCategory.HOBBY -> "🎨"
            HabitCategory.HOME -> "🧹"
            HabitCategory.SELF_IMPROVEMENT -> "🧘"
            else -> "⭐"
        }
    }
}
