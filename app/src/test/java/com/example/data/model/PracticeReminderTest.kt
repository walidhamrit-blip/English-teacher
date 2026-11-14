package com.example.data.model

import android.app.AlarmManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.core.AndroidPracticeReminderScheduler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.util.Calendar
import java.util.TimeZone

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PracticeReminderTest {

    @Test
    fun formatReminderTimeArabic_formatsMorningAfternoonAndMidnight() {
        assertEquals("09:00 صباحاً", formatReminderTimeArabic(9, 0))
        assertEquals("12:00 مساءً", formatReminderTimeArabic(12, 0))
        assertEquals("07:30 مساءً", formatReminderTimeArabic(19, 30))
        assertEquals("12:15 صباحاً", formatReminderTimeArabic(0, 15))
    }

    @Test
    fun computeNextReminderTriggerMillis_schedulesSameDayOrNextDay() {
        val tz = TimeZone.getTimeZone("UTC")
        val nowCal = Calendar.getInstance(tz).apply {
            set(2026, Calendar.NOVEMBER, 13, 15, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val nowMillis = nowCal.timeInMillis

        // 19:30 is later today
        val laterToday = computeNextReminderTriggerMillis(19, 30, nowMillis, tz)
        val expectedToday = (nowCal.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 19)
            set(Calendar.MINUTE, 30)
        }.timeInMillis
        assertEquals(expectedToday, laterToday)

        // 09:00 has already passed today -> schedules for tomorrow
        val nextDay = computeNextReminderTriggerMillis(9, 0, nowMillis, tz)
        val expectedTomorrow = (nowCal.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
        }.timeInMillis
        assertEquals(expectedTomorrow, nextDay)
    }

    @Test
    fun androidPracticeReminderScheduler_schedulesAndCancelsAlarm() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val scheduler = AndroidPracticeReminderScheduler(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val shadowAlarm = shadowOf(alarmManager)

        val enabledSettings = ReminderSettings(enabled = true, hour = 20, minute = 15)
        scheduler.syncSchedule(enabledSettings)

        val loaded = AndroidPracticeReminderScheduler.loadCachedReminderSettings(context)
        assertTrue(loaded.enabled)
        assertEquals(20, loaded.hour)
        assertEquals(15, loaded.minute)
        assertNotNull(shadowAlarm.nextScheduledAlarm)

        scheduler.syncSchedule(enabledSettings.copy(enabled = false))
        assertNull(shadowAlarm.nextScheduledAlarm)
    }
}
