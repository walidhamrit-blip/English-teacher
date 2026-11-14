package com.example.data.model

import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

data class ReminderPresetTime(
    val hour: Int,
    val minute: Int,
    val arabicLabel: String
)

data class ReminderSettings(
    val enabled: Boolean = false,
    val hour: Int = DEFAULT_REMINDER_HOUR,
    val minute: Int = DEFAULT_REMINDER_MINUTE
) {
    val safeHour: Int
        get() = hour.coerceIn(0, 23)

    val safeMinute: Int
        get() = minute.coerceIn(0, 59)

    val formattedTime24h: String
        get() = String.format(Locale.US, "%02d:%02d", safeHour, safeMinute)

    val formattedTimeArabic: String
        get() = formatReminderTimeArabic(safeHour, safeMinute)

    companion object {
        const val DEFAULT_REMINDER_HOUR = 19
        const val DEFAULT_REMINDER_MINUTE = 0

        val PRESET_TIMES = listOf(
            ReminderPresetTime(9, 0, "9:00 صباحاً"),
            ReminderPresetTime(14, 0, "2:00 ظهراً"),
            ReminderPresetTime(19, 0, "7:00 مساءً"),
            ReminderPresetTime(21, 0, "9:00 مساءً")
        )
    }
}

fun formatReminderTimeArabic(hour: Int, minute: Int): String {
    val h24 = hour.coerceIn(0, 23)
    val min = minute.coerceIn(0, 59)
    val period = if (h24 < 12) "صباحاً" else "مساءً"
    val h12 = when {
        h24 == 0 -> 12
        h24 > 12 -> h24 - 12
        else -> h24
    }
    return String.format(Locale.US, "%02d:%02d %s", h12, min, period)
}

fun computeNextReminderTriggerMillis(
    hour: Int,
    minute: Int,
    nowMillis: Long = System.currentTimeMillis(),
    timeZone: TimeZone = TimeZone.getDefault()
): Long {
    val cleanHour = hour.coerceIn(0, 23)
    val cleanMinute = minute.coerceIn(0, 59)
    val target = Calendar.getInstance(timeZone).apply {
        timeInMillis = nowMillis
        set(Calendar.HOUR_OF_DAY, cleanHour)
        set(Calendar.MINUTE, cleanMinute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    if (target.timeInMillis <= nowMillis) {
        target.add(Calendar.DAY_OF_YEAR, 1)
    }
    return target.timeInMillis
}
