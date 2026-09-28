package com.yourname.habitapp.utils

import com.yourname.habitapp.data.models.HabitCategory
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HabitTemplatesTest {

    @Test
    fun testSuggestionsNotEmpty() {
        val suggestions = HabitTemplates.ALL_SUGGESTIONS
        assertTrue(suggestions.isNotEmpty())
    }

    @Test
    fun testCategoryFiltering() {
        val healthSuggestions = HabitTemplates.ALL_SUGGESTIONS.filter { it.category == HabitCategory.HEALTH }
        assertTrue(healthSuggestions.isNotEmpty())
        assertTrue(healthSuggestions.all { it.category == HabitCategory.HEALTH })
    }

    @Test
    fun testCategoryNamesMap() {
        val healthName = HabitTemplates.CATEGORY_NAMES[HabitCategory.HEALTH]
        assertNotNull(healthName)
    }
}
