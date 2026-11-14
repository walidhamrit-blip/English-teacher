package com.example.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StudyTimerTest {

    @Test
    fun studyTimeSummary_computesCountdownProgressAndCompletion() {
        val initial = StudyTimeSummary(
            goalMinutes = 10,
            elapsedSecondsToday = 0,
            isRunning = false,
            isPaused = false
        )
        assertEquals(600, initial.goalSeconds)
        assertEquals(600, initial.remainingSeconds)
        assertEquals("10:00", initial.formattedRemaining)
        assertEquals(0f, initial.progressFraction, 0.001f)
        assertFalse(initial.isGoalCompleted)

        val midSession = StudyTimeSummary(
            goalMinutes = 20,
            elapsedSecondsToday = 315,
            isRunning = true,
            isPaused = false
        )
        assertEquals(1200, midSession.goalSeconds)
        assertEquals(885, midSession.remainingSeconds)
        assertEquals("14:45", midSession.formattedRemaining)
        assertEquals(315f / 1200f, midSession.progressFraction, 0.001f)
        assertFalse(midSession.isGoalCompleted)

        val completed = StudyTimeSummary(
            goalMinutes = 10,
            elapsedSecondsToday = 640,
            isRunning = true,
            isPaused = false
        )
        assertEquals(0, completed.remainingSeconds)
        assertEquals("00:00", completed.formattedRemaining)
        assertEquals(1f, completed.progressFraction, 0.001f)
        assertTrue(completed.isGoalCompleted)
    }

    @Test
    fun resolveTodayStudySeconds_resetsOnNewDateAndPreservesSameDay() {
        assertEquals(
            420,
            resolveTodayStudySeconds(
                storedSeconds = 420,
                lastStudyDate = "2026-11-13",
                todayDate = "2026-11-13"
            )
        )
        assertEquals(
            0,
            resolveTodayStudySeconds(
                storedSeconds = 420,
                lastStudyDate = "2026-11-12",
                todayDate = "2026-11-13"
            )
        )
        assertEquals(
            0,
            resolveTodayStudySeconds(
                storedSeconds = 0,
                lastStudyDate = "",
                todayDate = "2026-11-13"
            )
        )
    }
}
