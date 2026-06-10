package com.yourname.habitapp.ui.todo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.yourname.habitapp.R
import com.yourname.habitapp.data.AppDatabase
import com.yourname.habitapp.data.models.TodoItem
import com.yourname.habitapp.utils.AchievementEngine
import com.yourname.habitapp.utils.TodoReminderScheduler
import kotlinx.coroutines.launch

class TodoFragment : Fragment() {

    private lateinit var adapter: TodoAdapter
    private lateinit var db: AppDatabase

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_todo, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = AppDatabase.getInstance(requireContext())

        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerTodos)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        adapter = TodoAdapter(
            onCompleteClick = { todo -> onTodoCompleted(todo) }
        )
        recyclerView.adapter = adapter

        // السحب للحذف (swipe to delete)
        setupSwipeToDelete(recyclerView)

        // FAB لإضافة مهمة
        view.findViewById<FloatingActionButton>(R.id.fabAddTodo).setOnClickListener {
            AddTodoBottomSheet().show(parentFragmentManager, "AddTodo")
        }

        // مراقبة البيانات
        db.todoDao().getAllTodos().observe(viewLifecycleOwner) { todos ->
            adapter.submitList(todos)
        }
    }

    private fun onTodoCompleted(todo: TodoItem) {
        lifecycleScope.launch {
            val updated = todo.copy(isCompleted = !todo.isCompleted)
            db.todoDao().update(updated)

            if (updated.isCompleted) {
                // XP حسب الأولوية
                val xp = when (todo.priority.name) {
                    "HIGH"   -> 20
                    "MEDIUM" -> 10
                    else     -> 5
                }
                AchievementEngine.addXP(requireContext(), xp)

                // إلغاء التنبيهات
                TodoReminderScheduler.cancelTodoReminders(requireContext(), todo.id)

                // فحص إنجازات
                val count = db.todoDao().getCompletedCount()
                AchievementEngine.checkAndUnlock(requireContext(), "TODO_COMPLETED", count)

                // Early Bird / Night Owl
                val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
                if (hour < 8)  AchievementEngine.checkAndUnlock(requireContext(), "EARLY_BIRD")
                if (hour >= 22) AchievementEngine.checkAndUnlock(requireContext(), "NIGHT_OWL")
            }
        }
    }

    private fun setupSwipeToDelete(recyclerView: RecyclerView) {
        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, t: RecyclerView.ViewHolder) = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val todo = adapter.currentList[viewHolder.adapterPosition]
                lifecycleScope.launch {
                    TodoReminderScheduler.cancelTodoReminders(requireContext(), todo.id)
                    db.todoDao().delete(todo)
                }
            }
        }).attachToRecyclerView(recyclerView)
    }
}
