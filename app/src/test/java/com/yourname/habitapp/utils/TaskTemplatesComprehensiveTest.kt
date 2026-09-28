package com.yourname.habitapp.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskTemplatesComprehensiveTest {

    @Test
    fun taskTemplates01_allCategoriesCount() {
        val count = TaskTemplates.ALL_CATEGORIES.size
        assertTrue(count >= 8)
    }

    @Test
    fun taskTemplates02_flatListNotEmpty() {
        val flatList = TaskTemplates.getAll()
        assertTrue(flatList.isNotEmpty())
    }

    @Test
    fun taskTemplates03_category1HasTasks() {
        val cat1 = TaskTemplates.ALL_CATEGORIES[0]
        assertTrue(cat1.tasks.isNotEmpty())
    }

    @Test
    fun taskTemplates04_category2HasTasks() {
        val cat2 = TaskTemplates.ALL_CATEGORIES[1]
        assertTrue(cat2.tasks.isNotEmpty())
    }

    @Test
    fun taskTemplates05_category3HasTasks() {
        val cat3 = TaskTemplates.ALL_CATEGORIES[2]
        assertTrue(cat3.tasks.isNotEmpty())
    }

    @Test
    fun taskTemplates06_category4HasTasks() {
        val cat4 = TaskTemplates.ALL_CATEGORIES[3]
        assertTrue(cat4.tasks.isNotEmpty())
    }

    @Test
    fun taskTemplates07_category5HasTasks() {
        val cat5 = TaskTemplates.ALL_CATEGORIES[4]
        assertTrue(cat5.tasks.isNotEmpty())
    }

    @Test
    fun taskTemplates08_category6HasTasks() {
        val cat6 = TaskTemplates.ALL_CATEGORIES[5]
        assertTrue(cat6.tasks.isNotEmpty())
    }

    @Test
    fun taskTemplates09_category7HasTasks() {
        val cat7 = TaskTemplates.ALL_CATEGORIES[6]
        assertTrue(cat7.tasks.isNotEmpty())
    }

    @Test
    fun taskTemplates10_category8HasTasks() {
        val cat8 = TaskTemplates.ALL_CATEGORIES[7]
        assertTrue(cat8.tasks.isNotEmpty())
    }

    @Test
    fun taskTemplates11_allCategoryIconsNotEmpty() {
        for (category in TaskTemplates.ALL_CATEGORIES) {
            assertTrue(category.icon.isNotBlank())
        }
    }

    @Test
    fun taskTemplates12_allCategoryNamesNotEmpty() {
        for (category in TaskTemplates.ALL_CATEGORIES) {
            assertTrue(category.name.isNotBlank())
        }
    }

    @Test
    fun taskTemplates13_allTaskIconsNotEmpty() {
        for (task in TaskTemplates.getAll()) {
            assertTrue(task.icon.isNotBlank())
        }
    }

    @Test
    fun taskTemplates14_allTaskTitlesNotEmpty() {
        for (task in TaskTemplates.getAll()) {
            assertTrue(task.title.isNotBlank())
        }
    }

    @Test
    fun taskTemplates15_flatListCountMatchesSum() {
        val totalSum = TaskTemplates.ALL_CATEGORIES.sumOf { it.tasks.size }
        assertEquals(totalSum, TaskTemplates.getAll().size)
    }
}
