package com.example.core

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.ReminderSettings
import com.example.data.model.computeNextReminderTriggerMillis

interface PracticeReminderScheduler {
    fun syncSchedule(settings: ReminderSettings)
    fun showImmediateReminder(): Boolean
}

class AndroidPracticeReminderScheduler(
    context: Context
) : PracticeReminderScheduler {

    private val appContext = context.applicationContext

    override fun syncSchedule(settings: ReminderSettings) {
        saveCachedReminderSettings(appContext, settings)
        ensureNotificationChannel(appContext)
        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pendingIntent = buildReminderPendingIntent(appContext)

        if (!settings.enabled) {
            alarmManager.cancel(pendingIntent)
            return
        }

        val triggerAtMillis = computeNextReminderTriggerMillis(
            hour = settings.safeHour,
            minute = settings.safeMinute
        )
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            pendingIntent
        )
    }

    override fun showImmediateReminder(): Boolean {
        return showPracticeReminderNotification(appContext)
    }

    companion object {
        const val CHANNEL_ID = "noor_daily_practice_channel"
        const val NOTIFICATION_ID = 4102
        private const val REQUEST_CODE_REMINDER = 4101
        private const val PREFS_NAME = "noor_reminder_prefs"
        private const val KEY_ENABLED = "reminder_enabled"
        private const val KEY_HOUR = "reminder_hour"
        private const val KEY_MINUTE = "reminder_minute"

        fun ensureNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                    ?: return
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.reminder_channel_name),
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = context.getString(R.string.reminder_channel_desc)
                }
                manager.createNotificationChannel(channel)
            }
        }

        fun hasNotificationPermission(context: Context): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        }

        fun saveCachedReminderSettings(context: Context, settings: ReminderSettings) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_ENABLED, settings.enabled)
                .putInt(KEY_HOUR, settings.safeHour)
                .putInt(KEY_MINUTE, settings.safeMinute)
                .apply()
        }

        fun loadCachedReminderSettings(context: Context): ReminderSettings {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return ReminderSettings(
                enabled = prefs.getBoolean(KEY_ENABLED, false),
                hour = prefs.getInt(KEY_HOUR, ReminderSettings.DEFAULT_REMINDER_HOUR),
                minute = prefs.getInt(KEY_MINUTE, ReminderSettings.DEFAULT_REMINDER_MINUTE)
            )
        }

        fun buildReminderPendingIntent(context: Context): PendingIntent {
            val intent = Intent(context, PracticeReminderReceiver::class.java)
            return PendingIntent.getBroadcast(
                context,
                REQUEST_CODE_REMINDER,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        fun showPracticeReminderNotification(context: Context): Boolean {
            if (!hasNotificationPermission(context)) return false
            ensureNotificationChannel(context)

            val launchIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            val contentPendingIntent = PendingIntent.getActivity(
                context,
                REQUEST_CODE_REMINDER + 1,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val title = context.getString(R.string.reminder_notification_title)
            val body = context.getString(R.string.reminder_notification_body)

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification_bell)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(contentPendingIntent)
                .build()

            return try {
                NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
                true
            } catch (_: SecurityException) {
                false
            }
        }
    }
}

class PracticeReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val settings = AndroidPracticeReminderScheduler.loadCachedReminderSettings(context)
        if (!settings.enabled) return
        AndroidPracticeReminderScheduler.showPracticeReminderNotification(context)
        AndroidPracticeReminderScheduler(context).syncSchedule(settings)
    }
}

class ReminderBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        val settings = AndroidPracticeReminderScheduler.loadCachedReminderSettings(context)
        if (settings.enabled) {
            AndroidPracticeReminderScheduler(context).syncSchedule(settings)
        }
    }
}
