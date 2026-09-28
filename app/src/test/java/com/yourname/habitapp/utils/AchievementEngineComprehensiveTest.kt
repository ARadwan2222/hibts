package com.yourname.habitapp.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class AchievementEngineComprehensiveTest {

    @Test
    fun level01_xp0_isLevel1() {
        val (level, _) = getLevelFromXP(0)
        assertEquals(1, level)
    }

    @Test
    fun level02_xp50_isLevel1() {
        val (level, _) = getLevelFromXP(50)
        assertEquals(1, level)
    }

    @Test
    fun level03_xp99_isLevel1() {
        val (level, _) = getLevelFromXP(99)
        assertEquals(1, level)
    }

    @Test
    fun level04_xp100_isLevel2() {
        val (level, _) = getLevelFromXP(100)
        assertEquals(2, level)
    }

    @Test
    fun level05_xp249_isLevel2() {
        val (level, _) = getLevelFromXP(249)
        assertEquals(2, level)
    }

    @Test
    fun level06_xp250_isLevel3() {
        val (level, _) = getLevelFromXP(250)
        assertEquals(3, level)
    }

    @Test
    fun level07_xp499_isLevel3() {
        val (level, _) = getLevelFromXP(499)
        assertEquals(3, level)
    }

    @Test
    fun level08_xp500_isLevel4() {
        val (level, _) = getLevelFromXP(500)
        assertEquals(4, level)
    }

    @Test
    fun level09_xp799_isLevel4() {
        val (level, _) = getLevelFromXP(799)
        assertEquals(4, level)
    }

    @Test
    fun level10_xp800_isLevel5() {
        val (level, _) = getLevelFromXP(800)
        assertEquals(5, level)
    }

    @Test
    fun level11_xp1199_isLevel5() {
        val (level, _) = getLevelFromXP(1199)
        assertEquals(5, level)
    }

    @Test
    fun level12_xp1200_isLevel6() {
        val (level, _) = getLevelFromXP(1200)
        assertEquals(6, level)
    }

    @Test
    fun level13_xp1799_isLevel6() {
        val (level, _) = getLevelFromXP(1799)
        assertEquals(6, level)
    }

    @Test
    fun level14_xp1800_isLevel7() {
        val (level, _) = getLevelFromXP(1800)
        assertEquals(7, level)
    }

    @Test
    fun level15_xp2499_isLevel7() {
        val (level, _) = getLevelFromXP(2499)
        assertEquals(7, level)
    }

    @Test
    fun level16_xp2500_isLevel8() {
        val (level, _) = getLevelFromXP(2500)
        assertEquals(8, level)
    }

    @Test
    fun level17_xp5000_isLevel8() {
        val (level, _) = getLevelFromXP(5000)
        assertEquals(8, level)
    }

    @Test
    fun trigger01_firstHabitReward() {
        val def = AchievementDef("FIRST_HABIT", "First Habit", "Added 1st habit", "🌱", 20)
        assertEquals(20, def.xpReward)
    }

    @Test
    fun trigger02_weekStreakReward() {
        val def = AchievementDef("WEEK_STREAK", "7 Days", "Streak 7 days", "🔥", 70)
        assertEquals(70, def.xpReward)
    }

    @Test
    fun trigger03_legendStreakReward() {
        val def = AchievementDef("LEGEND_STREAK", "100 Days", "Streak 100 days", "👑", 500)
        assertEquals(500, def.xpReward)
    }

    private fun getLevelFromXP(xp: Int): Pair<Int, String> = when {
        xp < 100   -> Pair(1, "Beginner")
        xp < 250   -> Pair(2, "Learner")
        xp < 500   -> Pair(3, "Active")
        xp < 800   -> Pair(4, "Committed")
        xp < 1200  -> Pair(5, "Persistent")
        xp < 1800  -> Pair(6, "Professional")
        xp < 2500  -> Pair(7, "Expert")
        else       -> Pair(8, "Legend")
    }
}
