package com.yourname.habitapp.ui.habits

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.yourname.habitapp.R
import com.yourname.habitapp.data.AppDatabase
import com.yourname.habitapp.data.models.Habit
import com.yourname.habitapp.utils.AchievementEngine
import com.yourname.habitapp.utils.NotificationHelper
import kotlinx.coroutines.launch

class HabitsFragment : Fragment() {

    private lateinit var adapter: HabitAdapter
    private lateinit var db: AppDatabase

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_habits, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = AppDatabase.getInstance(requireContext())

        // إعداد RecyclerView
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerHabits)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        adapter = HabitAdapter(
            onCompleteClick = { habit -> onHabitCompleted(habit) },
            onLongClick = { habit -> onHabitLongClick(habit) }
        )
        recyclerView.adapter = adapter

        // FAB لإضافة عادة جديدة
        view.findViewById<FloatingActionButton>(R.id.fabAddHabit).setOnClickListener {
            startActivity(Intent(requireContext(), AddHabitActivity::class.java))
        }

        // مراقبة البيانات
        db.habitDao().getAllHabits().observe(viewLifecycleOwner) { habits ->
            adapter.submitList(habits)
            updateProgressHeader(view, habits)
        }
    }

    // عند الضغط على CheckBox العادة
    private fun onHabitCompleted(habit: Habit) {
        lifecycleScope.launch {
            val newCompleted = !habit.isCompletedToday
            var newStreak = habit.streak
            var newLongest = habit.longestStreak

            if (newCompleted) {
                newStreak++
                if (newStreak > newLongest) newLongest = newStreak
                // تنبيه إتمام
                NotificationHelper.showHabitCompleteNotification(requireContext(), habit.name, newStreak)
                // تحقق من Streak مميز
                if (newStreak in listOf(7, 30, 100)) {
                    NotificationHelper.showStreakMilestoneNotification(requireContext(), habit.name, newStreak)
                }
                // XP
                AchievementEngine.addXP(requireContext(), 10)
                // تحقق من إنجازات
                AchievementEngine.checkAndUnlock(requireContext(), "STREAK_UPDATE", newStreak)
            } else {
                if (newStreak > 0) newStreak--
            }

            db.habitDao().updateHabitStreak(habit.id, newCompleted, newStreak, newLongest)
        }
    }

    private fun onHabitLongClick(habit: Habit) {
        // خيارات: تعديل / حذف / مشاركة
        // يمكن عرض BottomSheet هنا
    }

    private fun updateProgressHeader(view: View, habits: List<Habit>) {
        val total = habits.size
        val completed = habits.count { it.isCompletedToday }
        val percent = if (total > 0) (completed * 100) / total else 0

        view.findViewById<TextView>(R.id.tvProgress)?.text = "$completed / $total مكتملة"
        view.findViewById<LinearProgressIndicator>(R.id.progressBar)?.progress = percent
    }
}
