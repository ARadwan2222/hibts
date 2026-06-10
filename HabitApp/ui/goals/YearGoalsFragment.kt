package com.yourname.habitapp.ui.goals

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.yourname.habitapp.R
import com.yourname.habitapp.data.AppDatabase
import com.yourname.habitapp.data.models.GoalStep
import com.yourname.habitapp.utils.AchievementEngine
import kotlinx.coroutines.launch
import java.util.Calendar

class YearGoalsFragment : Fragment() {

    private lateinit var adapter: YearGoalAdapter
    private lateinit var db: AppDatabase

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_year_goals, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = AppDatabase.getInstance(requireContext())

        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerGoals)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        adapter = YearGoalAdapter(
            onStepCompleted = { goalId, step -> onStepCompleted(goalId, step) },
            onGoalClick = { goal ->
                // فتح تفاصيل الهدف
            }
        )
        recyclerView.adapter = adapter

        view.findViewById<FloatingActionButton>(R.id.fabAddGoal).setOnClickListener {
            startActivity(Intent(requireContext(), AddGoalActivity::class.java))
        }

        val year = Calendar.getInstance().get(Calendar.YEAR)
        db.yearGoalDao().getGoalsByYear(year).observe(viewLifecycleOwner) { goals ->
            adapter.submitList(goals)
        }
    }

    private fun onStepCompleted(goalId: Int, step: GoalStep) {
        lifecycleScope.launch {
            db.yearGoalDao().updateStep(step.copy(isCompleted = !step.isCompleted))
            db.yearGoalDao().recalculateProgress(goalId)

            // XP للخطوة
            AchievementEngine.addXP(requireContext(), 15)

            // فحص إنجازات الأهداف
            val completedCount = db.yearGoalDao().getCompletedGoalsCount()
            AchievementEngine.checkAndUnlock(requireContext(), "GOAL_COMPLETED", completedCount)
        }
    }
}
