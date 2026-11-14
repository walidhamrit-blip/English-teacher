package com.example.data.model

import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

data class DailyHabitDay(
    val dayKey: Int,
    val arabicShortLabel: String,
    val englishShortLabel: String,
    val messageCount: Int,
    val goalMet: Boolean,
    val isToday: Boolean
)

data class DailyProgressSummary(
    val todayMessagesCount: Int = 0,
    val dailyGoal: Int = DEFAULT_DAILY_MESSAGE_GOAL,
    val totalMessagesCount: Int = 0,
    val writtenTodayCount: Int = 0,
    val vocalTodayCount: Int = 0,
    val currentStreakDays: Int = 0,
    val longestStreakDays: Int = 0,
    val lastSevenDays: List<DailyHabitDay> = emptyList()
) {
    val progressFraction: Float
        get() = if (dailyGoal <= 0) 0f else (todayMessagesCount.toFloat() / dailyGoal).coerceIn(0f, 1f)

    val isGoalReached: Boolean
        get() = todayMessagesCount >= dailyGoal

    val remainingForGoal: Int
        get() = (dailyGoal - todayMessagesCount).coerceAtLeast(0)

    val nextStreakMilestoneDays: Int
        get() = STREAK_MILESTONES.firstOrNull { it > currentStreakDays }
            ?: (((currentStreakDays / 30) + 1) * 30)

    val streakMilestoneProgress: Float
        get() = if (nextStreakMilestoneDays <= 0) 0f
        else (currentStreakDays.toFloat() / nextStreakMilestoneDays.toFloat()).coerceIn(0f, 1f)

    val remainingDaysToNextMilestone: Int
        get() = (nextStreakMilestoneDays - currentStreakDays).coerceAtLeast(0)

    val streakBadgeTitleArabic: String
        get() = when {
            currentStreakDays >= 30 -> "بطل المثابرة"
            currentStreakDays >= 14 -> "نجم الأسبوعين"
            currentStreakDays >= 7 -> "متعلّم أسبوعي"
            currentStreakDays >= 3 -> "انطلاقة قوية"
            currentStreakDays >= 1 -> "نشط اليوم"
            else -> "ابدأ سلسلتك"
        }

    val streakBadgeTitleEnglish: String
        get() = when {
            currentStreakDays >= 30 -> "30+ Day Champion"
            currentStreakDays >= 14 -> "2-Week Star"
            currentStreakDays >= 7 -> "7-Day Streak"
            currentStreakDays >= 3 -> "Strong Start"
            currentStreakDays >= 1 -> "Active Today"
            else -> "Start Today"
        }

    companion object {
        const val DEFAULT_DAILY_MESSAGE_GOAL = 5
        val STREAK_MILESTONES = listOf(3, 7, 14, 30, 60, 100)
    }
}

data class StreakUpdate(
    val currentStreak: Int,
    val longestStreak: Int,
    val lastActiveDate: String,
    val changed: Boolean
)

fun formatDayDateKey(
    millis: Long,
    timeZone: TimeZone = TimeZone.getDefault()
): String {
    val cal = Calendar.getInstance(timeZone)
    cal.timeInMillis = millis
    val year = cal.get(Calendar.YEAR)
    val month = cal.get(Calendar.MONTH) + 1
    val day = cal.get(Calendar.DAY_OF_MONTH)
    return String.format(Locale.US, "%04d-%02d-%02d", year, month, day)
}

fun yesterdayDateKey(
    nowMillis: Long = System.currentTimeMillis(),
    timeZone: TimeZone = TimeZone.getDefault()
): String {
    val cal = Calendar.getInstance(timeZone)
    cal.timeInMillis = nowMillis
    cal.add(Calendar.DAY_OF_YEAR, -1)
    return formatDayDateKey(cal.timeInMillis, timeZone)
}

fun computeUpdatedStreak(
    previousStreak: Int,
    previousLongest: Int,
    lastActiveDate: String,
    todayDate: String,
    yesterdayDate: String
): StreakUpdate {
    val cleanPrevStreak = previousStreak.coerceIn(0, 3650)
    val cleanPrevLongest = previousLongest.coerceIn(0, 3650)
    val cleanLastDate = lastActiveDate.trim()

    return when {
        cleanLastDate == todayDate && cleanPrevStreak >= 1 -> {
            val longest = maxOf(cleanPrevLongest, cleanPrevStreak)
            StreakUpdate(
                currentStreak = cleanPrevStreak,
                longestStreak = longest,
                lastActiveDate = todayDate,
                changed = longest != cleanPrevLongest
            )
        }
        cleanLastDate == yesterdayDate && cleanPrevStreak >= 1 -> {
            val next = (cleanPrevStreak + 1).coerceAtMost(3650)
            val longest = maxOf(cleanPrevLongest, next)
            StreakUpdate(
                currentStreak = next,
                longestStreak = longest,
                lastActiveDate = todayDate,
                changed = true
            )
        }
        else -> {
            val longest = maxOf(cleanPrevLongest, 1)
            StreakUpdate(
                currentStreak = 1,
                longestStreak = longest,
                lastActiveDate = todayDate,
                changed = true
            )
        }
    }
}

fun calculateDailyProgress(
    turns: List<TutorTurn>,
    dailyGoal: Int = DailyProgressSummary.DEFAULT_DAILY_MESSAGE_GOAL,
    persistedStreakDays: Int = 0,
    persistedLongestStreakDays: Int = 0,
    persistedLastActiveDate: String = "",
    nowMillis: Long = System.currentTimeMillis(),
    timeZone: TimeZone = TimeZone.getDefault()
): DailyProgressSummary {
    val calendar = Calendar.getInstance(timeZone)

    fun dayKeyForMillis(millis: Long): Int {
        calendar.timeInMillis = millis
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        return (year * 10000) + (month * 100) + day
    }

    val todayKey = dayKeyForMillis(nowMillis)
    val todayDateStr = formatDayDateKey(nowMillis, timeZone)
    val yesterdayDateStr = yesterdayDateKey(nowMillis, timeZone)

    val turnsByDay = HashMap<Int, MutableList<TutorTurn>>()
    for (turn in turns) {
        val createdMillis = turn.createdAt?.toDate()?.time ?: nowMillis
        val key = dayKeyForMillis(createdMillis)
        turnsByDay.getOrPut(key) { mutableListOf() }.add(turn)
    }

    val todayTurns = turnsByDay[todayKey].orEmpty()
    val writtenToday = todayTurns.count { it.mode != "vocal" }
    val vocalToday = todayTurns.count { it.mode == "vocal" }

    val lastSevenDays = (6 downTo 0).map { daysAgo ->
        calendar.timeInMillis = nowMillis
        calendar.add(Calendar.DAY_OF_YEAR, -daysAgo)
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val key = (year * 10000) + (month * 100) + day
        val dow = calendar.get(Calendar.DAY_OF_WEEK)
        val count = turnsByDay[key]?.size ?: 0
        DailyHabitDay(
            dayKey = key,
            arabicShortLabel = arabicDayLabel(dow),
            englishShortLabel = englishDayLabel(dow),
            messageCount = count,
            goalMet = count >= dailyGoal,
            isToday = daysAgo == 0
        )
    }

    var turnStreak = 0
    val startDaysAgo = if ((turnsByDay[todayKey]?.size ?: 0) > 0) 0 else 1
    var offset = startDaysAgo
    while (offset < 3650) {
        calendar.timeInMillis = nowMillis
        calendar.add(Calendar.DAY_OF_YEAR, -offset)
        val key = (calendar.get(Calendar.YEAR) * 10000) +
            ((calendar.get(Calendar.MONTH) + 1) * 100) +
            calendar.get(Calendar.DAY_OF_MONTH)
        val count = turnsByDay[key]?.size ?: 0
        if (count > 0) {
            turnStreak++
            offset++
        } else {
            break
        }
    }

    val activePersistedStreak = if (
        persistedLastActiveDate == todayDateStr || persistedLastActiveDate == yesterdayDateStr
    ) {
        persistedStreakDays.coerceAtLeast(0)
    } else {
        0
    }

    val effectiveStreak = maxOf(activePersistedStreak, turnStreak)
    val effectiveLongest = maxOf(persistedLongestStreakDays.coerceAtLeast(0), effectiveStreak)

    return DailyProgressSummary(
        todayMessagesCount = todayTurns.size,
        dailyGoal = dailyGoal,
        totalMessagesCount = turns.size,
        writtenTodayCount = writtenToday,
        vocalTodayCount = vocalToday,
        currentStreakDays = effectiveStreak,
        longestStreakDays = effectiveLongest,
        lastSevenDays = lastSevenDays
    )
}

private fun arabicDayLabel(dayOfWeek: Int): String = when (dayOfWeek) {
    Calendar.SUNDAY -> "أح"
    Calendar.MONDAY -> "إث"
    Calendar.TUESDAY -> "ثل"
    Calendar.WEDNESDAY -> "أر"
    Calendar.THURSDAY -> "خم"
    Calendar.FRIDAY -> "جم"
    Calendar.SATURDAY -> "سب"
    else -> ""
}

private fun englishDayLabel(dayOfWeek: Int): String = when (dayOfWeek) {
    Calendar.SUNDAY -> "Sun"
    Calendar.MONDAY -> "Mon"
    Calendar.TUESDAY -> "Tue"
    Calendar.WEDNESDAY -> "Wed"
    Calendar.THURSDAY -> "Thu"
    Calendar.FRIDAY -> "Fri"
    Calendar.SATURDAY -> "Sat"
    else -> ""
}
