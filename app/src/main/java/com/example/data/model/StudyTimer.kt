package com.example.data.model

import java.util.Locale

data class StudyTimeSummary(
    val goalMinutes: Int = DEFAULT_STUDY_GOAL_MINUTES,
    val elapsedSecondsToday: Int = 0,
    val isRunning: Boolean = false,
    val isPaused: Boolean = false
) {
    val safeGoalMinutes: Int
        get() = goalMinutes.coerceIn(MIN_STUDY_GOAL_MINUTES, MAX_STUDY_GOAL_MINUTES)

    val goalSeconds: Int
        get() = safeGoalMinutes * 60

    val clampedElapsedSeconds: Int
        get() = elapsedSecondsToday.coerceIn(0, MAX_DAILY_STUDY_SECONDS)

    val remainingSeconds: Int
        get() = (goalSeconds - clampedElapsedSeconds).coerceAtLeast(0)

    val progressFraction: Float
        get() = if (goalSeconds <= 0) 0f
        else (clampedElapsedSeconds.toFloat() / goalSeconds.toFloat()).coerceIn(0f, 1f)

    val isGoalCompleted: Boolean
        get() = clampedElapsedSeconds >= goalSeconds

    val formattedRemaining: String
        get() = formatCountdownClock(remainingSeconds)

    val formattedElapsed: String
        get() = formatCountdownClock(clampedElapsedSeconds)

    val elapsedMinutesWhole: Int
        get() = clampedElapsedSeconds / 60

    companion object {
        const val DEFAULT_STUDY_GOAL_MINUTES = 10
        const val MIN_STUDY_GOAL_MINUTES = 1
        const val MAX_STUDY_GOAL_MINUTES = 180
        const val MAX_DAILY_STUDY_SECONDS = 86400
        val PRESET_MINUTES = listOf(10, 20, 30)
    }
}

fun formatCountdownClock(totalSeconds: Int): String {
    val clean = totalSeconds.coerceAtLeast(0)
    val minutes = clean / 60
    val seconds = clean % 60
    return String.format(Locale.US, "%02d:%02d", minutes, seconds)
}

fun resolveTodayStudySeconds(
    storedSeconds: Int,
    lastStudyDate: String,
    todayDate: String
): Int {
    return if (lastStudyDate.trim() == todayDate.trim()) {
        storedSeconds.coerceIn(0, StudyTimeSummary.MAX_DAILY_STUDY_SECONDS)
    } else {
        0
    }
}
