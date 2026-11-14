package com.example.data.model

import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.Date
import java.util.TimeZone

class DailyProgressTest {

    @Test
    fun calculateDailyProgress_countsTodayWrittenVocalAndStreak() {
        val tz = TimeZone.getTimeZone("UTC")
        val cal = Calendar.getInstance(tz).apply {
            set(2026, Calendar.NOVEMBER, 12, 14, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val todayMillis = cal.timeInMillis

        val yesterdayMillis = (cal.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, -1)
        }.timeInMillis

        val twoDaysAgoMillis = (cal.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, -2)
        }.timeInMillis

        val turns = listOf(
            TutorTurn(id = "1", mode = "written", createdAt = Timestamp(Date(todayMillis))),
            TutorTurn(id = "2", mode = "written", createdAt = Timestamp(Date(todayMillis - 60_000L))),
            TutorTurn(id = "3", mode = "vocal", createdAt = Timestamp(Date(todayMillis - 120_000L))),
            TutorTurn(id = "4", mode = "written", createdAt = Timestamp(Date(yesterdayMillis))),
            TutorTurn(id = "5", mode = "vocal", createdAt = Timestamp(Date(twoDaysAgoMillis)))
        )

        val summary = calculateDailyProgress(
            turns = turns,
            dailyGoal = 5,
            nowMillis = todayMillis,
            timeZone = tz
        )

        assertEquals(3, summary.todayMessagesCount)
        assertEquals(2, summary.writtenTodayCount)
        assertEquals(1, summary.vocalTodayCount)
        assertEquals(5, summary.totalMessagesCount)
        assertEquals(3, summary.currentStreakDays)
        assertEquals(2, summary.remainingForGoal)
        assertFalse(summary.isGoalReached)
        assertEquals(7, summary.lastSevenDays.size)
        assertTrue(summary.lastSevenDays.last().isToday)
        assertEquals(3, summary.lastSevenDays.last().messageCount)
    }

    @Test
    fun calculateDailyProgress_marksGoalReachedWhenFiveOrMoreSentToday() {
        val tz = TimeZone.getTimeZone("UTC")
        val now = 1_794_500_000_000L
        val turns = (1..5).map { idx ->
            TutorTurn(
                id = "t_$idx",
                mode = "written",
                createdAt = Timestamp(Date(now - idx * 1000L))
            )
        }

        val summary = calculateDailyProgress(turns = turns, dailyGoal = 5, nowMillis = now, timeZone = tz)

        assertEquals(5, summary.todayMessagesCount)
        assertEquals(0, summary.remainingForGoal)
        assertTrue(summary.isGoalReached)
        assertEquals(1f, summary.progressFraction, 0.001f)
    }

    @Test
    fun computeUpdatedStreak_handlesFirstDaySameDayConsecutiveAndReset() {
        val first = computeUpdatedStreak(
            previousStreak = 0,
            previousLongest = 0,
            lastActiveDate = "",
            todayDate = "2026-11-10",
            yesterdayDate = "2026-11-09"
        )
        assertEquals(1, first.currentStreak)
        assertEquals(1, first.longestStreak)
        assertTrue(first.changed)

        val sameDay = computeUpdatedStreak(
            previousStreak = 3,
            previousLongest = 5,
            lastActiveDate = "2026-11-10",
            todayDate = "2026-11-10",
            yesterdayDate = "2026-11-09"
        )
        assertEquals(3, sameDay.currentStreak)
        assertEquals(5, sameDay.longestStreak)
        assertFalse(sameDay.changed)

        val consecutive = computeUpdatedStreak(
            previousStreak = 3,
            previousLongest = 3,
            lastActiveDate = "2026-11-10",
            todayDate = "2026-11-11",
            yesterdayDate = "2026-11-10"
        )
        assertEquals(4, consecutive.currentStreak)
        assertEquals(4, consecutive.longestStreak)
        assertTrue(consecutive.changed)

        val afterGap = computeUpdatedStreak(
            previousStreak = 4,
            previousLongest = 4,
            lastActiveDate = "2026-11-08",
            todayDate = "2026-11-11",
            yesterdayDate = "2026-11-10"
        )
        assertEquals(1, afterGap.currentStreak)
        assertEquals(4, afterGap.longestStreak)
        assertTrue(afterGap.changed)
    }

    @Test
    fun calculateDailyProgress_respectsActivePersistedStreakEvenWithoutTurns() {
        val tz = TimeZone.getTimeZone("UTC")
        val now = 1_794_500_000_000L
        val today = formatDayDateKey(now, tz)

        val summary = calculateDailyProgress(
            turns = emptyList(),
            dailyGoal = 5,
            persistedStreakDays = 6,
            persistedLongestStreakDays = 9,
            persistedLastActiveDate = today,
            nowMillis = now,
            timeZone = tz
        )

        assertEquals(6, summary.currentStreakDays)
        assertEquals(9, summary.longestStreakDays)
    }

    @Test
    fun dailyProgressSummary_computesStreakMilestonesAndBadgeTitles() {
        val zeroDays = DailyProgressSummary(currentStreakDays = 0, longestStreakDays = 0)
        assertEquals(3, zeroDays.nextStreakMilestoneDays)
        assertEquals(3, zeroDays.remainingDaysToNextMilestone)
        assertEquals(0f, zeroDays.streakMilestoneProgress, 0.001f)
        assertEquals("ابدأ سلسلتك", zeroDays.streakBadgeTitleArabic)
        assertEquals("Start Today", zeroDays.streakBadgeTitleEnglish)

        val oneDay = DailyProgressSummary(currentStreakDays = 1, longestStreakDays = 1)
        assertEquals(3, oneDay.nextStreakMilestoneDays)
        assertEquals(2, oneDay.remainingDaysToNextMilestone)
        assertEquals(1f / 3f, oneDay.streakMilestoneProgress, 0.001f)
        assertEquals("نشط اليوم", oneDay.streakBadgeTitleArabic)
        assertEquals("Active Today", oneDay.streakBadgeTitleEnglish)

        val fiveDays = DailyProgressSummary(currentStreakDays = 5, longestStreakDays = 5)
        assertEquals(7, fiveDays.nextStreakMilestoneDays)
        assertEquals(2, fiveDays.remainingDaysToNextMilestone)
        assertEquals(5f / 7f, fiveDays.streakMilestoneProgress, 0.001f)
        assertEquals("انطلاقة قوية", fiveDays.streakBadgeTitleArabic)
        assertEquals("Strong Start", fiveDays.streakBadgeTitleEnglish)

        val tenDays = DailyProgressSummary(currentStreakDays = 10, longestStreakDays = 14)
        assertEquals(14, tenDays.nextStreakMilestoneDays)
        assertEquals("متعلّم أسبوعي", tenDays.streakBadgeTitleArabic)
        assertEquals("7-Day Streak", tenDays.streakBadgeTitleEnglish)

        val twentyDays = DailyProgressSummary(currentStreakDays = 20, longestStreakDays = 20)
        assertEquals(30, twentyDays.nextStreakMilestoneDays)
        assertEquals("نجم الأسبوعين", twentyDays.streakBadgeTitleArabic)
        assertEquals("2-Week Star", twentyDays.streakBadgeTitleEnglish)

        val thirtyTwoDays = DailyProgressSummary(currentStreakDays = 32, longestStreakDays = 32)
        assertEquals(60, thirtyTwoDays.nextStreakMilestoneDays)
        assertEquals("بطل المثابرة", thirtyTwoDays.streakBadgeTitleArabic)
        assertEquals("30+ Day Champion", thirtyTwoDays.streakBadgeTitleEnglish)
    }
}
