package com.yourname.habitapp.utils

import com.yourname.habitapp.data.models.HabitCategory
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HabitTemplatesComprehensiveTest {

    @Test
    fun habitTemplates01_suggestionsNotEmpty() {
        assertTrue(HabitTemplates.ALL_SUGGESTIONS.isNotEmpty())
    }

    @Test
    fun habitTemplates02_hasHealthCategory() {
        val healthList = HabitTemplates.ALL_SUGGESTIONS.filter { it.category == HabitCategory.HEALTH }
        assertTrue(healthList.isNotEmpty())
    }

    @Test
    fun habitTemplates03_hasFitnessCategory() {
        val fitnessList = HabitTemplates.ALL_SUGGESTIONS.filter { it.category == HabitCategory.FITNESS }
        assertTrue(fitnessList.isNotEmpty())
    }

    @Test
    fun habitTemplates04_hasProductivityCategory() {
        val prodList = HabitTemplates.ALL_SUGGESTIONS.filter { it.category == HabitCategory.PRODUCTIVITY }
        assertTrue(prodList.isNotEmpty())
    }

    @Test
    fun habitTemplates05_hasLearningCategory() {
        val learnList = HabitTemplates.ALL_SUGGESTIONS.filter { it.category == HabitCategory.LEARNING }
        assertTrue(learnList.isNotEmpty())
    }

    @Test
    fun habitTemplates06_hasSpiritualCategory() {
        val spiritList = HabitTemplates.ALL_SUGGESTIONS.filter { it.category == HabitCategory.SPIRITUAL }
        assertTrue(spiritList.isNotEmpty())
    }

    @Test
    fun habitTemplates07_hasSocialCategory() {
        val socialList = HabitTemplates.ALL_SUGGESTIONS.filter { it.category == HabitCategory.SOCIAL }
        assertTrue(socialList.isNotEmpty())
    }

    @Test
    fun habitTemplates08_hasFinanceCategory() {
        val finList = HabitTemplates.ALL_SUGGESTIONS.filter { it.category == HabitCategory.FINANCE }
        assertTrue(finList.isNotEmpty())
    }

    @Test
    fun habitTemplates09_hasHobbyCategory() {
        val hobbyList = HabitTemplates.ALL_SUGGESTIONS.filter { it.category == HabitCategory.HOBBY }
        assertTrue(hobbyList.isNotEmpty())
    }

    @Test
    fun habitTemplates10_hasHomeCategory() {
        val homeList = HabitTemplates.ALL_SUGGESTIONS.filter { it.category == HabitCategory.HOME }
        assertTrue(homeList.isNotEmpty())
    }

    @Test
    fun habitTemplates11_hasSelfImprovementCategory() {
        val selfList = HabitTemplates.ALL_SUGGESTIONS.filter { it.category == HabitCategory.SELF_IMPROVEMENT }
        assertTrue(selfList.isNotEmpty())
    }

    @Test
    fun habitTemplates12_eachSuggestionHasValidIcon() {
        for (suggestion in HabitTemplates.ALL_SUGGESTIONS) {
            assertTrue(suggestion.icon.isNotBlank())
        }
    }

    @Test
    fun habitTemplates13_eachSuggestionHasValidName() {
        for (suggestion in HabitTemplates.ALL_SUGGESTIONS) {
            assertTrue(suggestion.name.isNotBlank())
        }
    }

    @Test
    fun habitTemplates14_categoryNamesMapHasHealth() {
        val name = HabitTemplates.CATEGORY_NAMES[HabitCategory.HEALTH]
        assertNotNull(name)
    }

    @Test
    fun habitTemplates15_allCategoriesHaveMapNames() {
        for (category in HabitCategory.entries) {
            if (category != HabitCategory.OTHER) {
                assertNotNull(HabitTemplates.CATEGORY_NAMES[category])
            }
        }
    }
}
