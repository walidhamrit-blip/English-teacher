package com.example.ui.components

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.AndroidPracticeReminderScheduler
import com.example.data.model.ReminderSettings
import com.example.ui.theme.NotoSansArabicFamily

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DailyPracticeReminderCard(
    settings: ReminderSettings,
    onUpdateSettings: (enabled: Boolean, hour: Int, minute: Int) -> Unit,
    onSendTestReminder: () -> Unit,
    hasNotificationPermissionOverride: Boolean? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var hasNotifPermission by remember(context, hasNotificationPermissionOverride) {
        mutableStateOf(
            hasNotificationPermissionOverride
                ?: AndroidPracticeReminderScheduler.hasNotificationPermission(context)
        )
    }
    var permissionDeniedMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingEnableAfterPermission by rememberSaveable { mutableStateOf(false) }
    var pendingTestAfterPermission by rememberSaveable { mutableStateOf(false) }
    var showTimePickerDialog by rememberSaveable { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotifPermission = granted
        if (granted) {
            permissionDeniedMessage = null
            if (pendingEnableAfterPermission) {
                pendingEnableAfterPermission = false
                onUpdateSettings(true, settings.safeHour, settings.safeMinute)
            }
            if (pendingTestAfterPermission) {
                pendingTestAfterPermission = false
                onSendTestReminder()
            }
        } else {
            pendingEnableAfterPermission = false
            pendingTestAfterPermission = false
            permissionDeniedMessage =
                "لتلقي تذكيرات الممارسة اليومية، يرجى السماح بإذن الإشعارات من إعدادات الجهاز."
        }
    }

    fun requestOrRun(enableReminder: Boolean, testReminder: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotifPermission) {
            pendingEnableAfterPermission = enableReminder
            pendingTestAfterPermission = testReminder
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            permissionDeniedMessage = null
            if (enableReminder) {
                onUpdateSettings(true, settings.safeHour, settings.safeMinute)
            }
            if (testReminder) {
                onSendTestReminder()
            }
        }
    }

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("home_reminder_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        color = if (settings.enabled) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        shape = CircleShape,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (settings.enabled) {
                                    Icons.Filled.NotificationsActive
                                } else {
                                    Icons.Filled.NotificationsOff
                                },
                                contentDescription = "Daily practice reminder",
                                tint = if (settings.enabled) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Daily Practice Reminder",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "تذكير يومي للممارسة في الوقت المفضل لك",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = NotoSansArabicFamily,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Switch(
                    checked = settings.enabled,
                    onCheckedChange = { checked ->
                        if (checked) {
                            requestOrRun(enableReminder = true, testReminder = false)
                        } else {
                            permissionDeniedMessage = null
                            onUpdateSettings(false, settings.safeHour, settings.safeMinute)
                        }
                    },
                    modifier = Modifier.testTag("reminder_enable_switch")
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                color = if (settings.enabled) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.AccessTime,
                            contentDescription = "Preferred reminder time",
                            tint = if (settings.enabled) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (settings.enabled) "موعد التذكير اليومي:" else "الوقت المحدد (متوقف):",
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = NotoSansArabicFamily,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = settings.formattedTimeArabic,
                        style = MaterialTheme.typography.titleSmall,
                        fontFamily = NotoSansArabicFamily,
                        fontWeight = FontWeight.Bold,
                        color = if (settings.enabled) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        modifier = Modifier.testTag("reminder_time_text")
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "اختر وقت التذكير المفضل:",
                style = MaterialTheme.typography.labelMedium,
                fontFamily = NotoSansArabicFamily,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(6.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ReminderSettings.PRESET_TIMES.forEach { preset ->
                    val isSelected = settings.safeHour == preset.hour && settings.safeMinute == preset.minute
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotifPermission) {
                                onUpdateSettings(settings.enabled, preset.hour, preset.minute)
                                requestOrRun(enableReminder = true, testReminder = false)
                            } else {
                                onUpdateSettings(true, preset.hour, preset.minute)
                            }
                        },
                        label = {
                            Text(
                                text = preset.arabicLabel,
                                fontFamily = NotoSansArabicFamily
                            )
                        },
                        modifier = Modifier.testTag("reminder_preset_${preset.hour}")
                    )
                }

                FilterChip(
                    selected = ReminderSettings.PRESET_TIMES.none {
                        it.hour == settings.safeHour && it.minute == settings.safeMinute
                    },
                    onClick = { showTimePickerDialog = true },
                    label = {
                        Text("وقت مخصص", fontFamily = NotoSansArabicFamily)
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.AccessTime,
                            contentDescription = "Choose custom reminder time",
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    modifier = Modifier.testTag("reminder_custom_time_button")
                )
            }

            if (permissionDeniedMessage != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = permissionDeniedMessage.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = NotoSansArabicFamily,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        TextButton(
                            onClick = {
                                val intent = Intent(
                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    Uri.fromParts("package", context.packageName, null)
                                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                context.startActivity(intent)
                            }
                        ) {
                            Text("فتح إعدادات التطبيق", fontFamily = NotoSansArabicFamily)
                        }
                    }
                }
            }

            if (settings.enabled) {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { requestOrRun(enableReminder = false, testReminder = true) },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("reminder_test_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.NotificationsNone,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("إرسال تذكير تجريبي الآن", fontFamily = NotoSansArabicFamily)
                }
            }
        }
    }

    if (showTimePickerDialog) {
        ReminderTimePickerDialog(
            initialHour = settings.safeHour,
            initialMinute = settings.safeMinute,
            onDismiss = { showTimePickerDialog = false },
            onConfirmTime = { hour, minute ->
                showTimePickerDialog = false
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotifPermission) {
                    onUpdateSettings(settings.enabled, hour, minute)
                    requestOrRun(enableReminder = true, testReminder = false)
                } else {
                    onUpdateSettings(true, hour, minute)
                }
            }
        )
    }
}

@Composable
fun ReminderSettingsDialog(
    settings: ReminderSettings,
    onDismiss: () -> Unit,
    onUpdateSettings: (enabled: Boolean, hour: Int, minute: Int) -> Unit,
    onSendTestReminder: () -> Unit,
    hasNotificationPermissionOverride: Boolean? = null
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "إعدادات التذكير اليومي",
                fontFamily = NotoSansArabicFamily
            )
        },
        text = {
            DailyPracticeReminderCard(
                settings = settings,
                onUpdateSettings = onUpdateSettings,
                onSendTestReminder = onSendTestReminder,
                hasNotificationPermissionOverride = hasNotificationPermissionOverride
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("إغلاق", fontFamily = NotoSansArabicFamily)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderTimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirmTime: (hour: Int, minute: Int) -> Unit
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialHour.coerceIn(0, 23),
        initialMinute = initialMinute.coerceIn(0, 59),
        is24Hour = false
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "اختيار وقت التذكير",
                fontFamily = NotoSansArabicFamily
            )
        },
        text = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                TimePicker(state = timePickerState)
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirmTime(timePickerState.hour, timePickerState.minute) },
                modifier = Modifier.testTag("reminder_time_picker_save_button")
            ) {
                Text("حفظ الوقت", fontFamily = NotoSansArabicFamily)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء", fontFamily = NotoSansArabicFamily)
            }
        }
    )
}
