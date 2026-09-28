package com.yourname.habitapp.utils

import org.junit.Assert.assertTrue
import org.junit.Test

class TaskTemplatesTest {

    @Test
    fun testAllCategoriesNotEmpty() {
        val categories = TaskTemplates.ALL_CATEGORIES
        assertTrue(categories.isNotEmpty())
    }

    @Test
    fun testTemplateSuggestionsHaveTitles() {
        val categories = TaskTemplates.ALL_CATEGORIES
        for (category in categories) {
            assertTrue(category.tasks.isNotEmpty())
            for (task in category.tasks) {
                assertTrue(task.title.isNotBlank())
            }
        }
    }

    @Test
    fun testGetAllFlatList() {
        val allTasks = TaskTemplates.getAll()
        assertTrue(allTasks.isNotEmpty())
    }
}
