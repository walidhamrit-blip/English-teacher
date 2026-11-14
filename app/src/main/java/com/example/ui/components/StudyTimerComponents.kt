package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.StudyTimeSummary
import com.example.ui.theme.NotoSansArabicFamily
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DailyStudyTimeGoalCard(
    summary: StudyTimeSummary,
    onSelectStudyGoalMinutes: (Int) -> Unit,
    onToggleTimer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = summary.progressFraction,
        animationSpec = tween(durationMillis = 350),
        label = "dailyStudyTimeProgress"
    )
    var showCustomDialog by rememberSaveable { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("home_study_time_card")
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
                        color = if (summary.isGoalCompleted) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.secondaryContainer
                        },
                        shape = CircleShape,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (summary.isGoalCompleted) {
                                    Icons.Filled.CheckCircle
                                } else {
                                    Icons.Filled.Timer
                                },
                                contentDescription = "Daily English study time goal",
                                tint = if (summary.isGoalCompleted) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.secondary
                                },
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Daily Study Time",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "حدّد وقت دراستك اليومي وتابع المؤقت التنازلي",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = NotoSansArabicFamily,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    color = if (summary.isGoalCompleted) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else if (summary.isRunning) {
                        MaterialTheme.colorScheme.tertiaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    shape = RoundedCornerShape(50.dp)
                ) {
                    Text(
                        text = when {
                            summary.isGoalCompleted -> "مكتمل ✓"
                            summary.isRunning -> "يعمل الآن"
                            else -> "${summary.safeGoalMinutes} دقيقة"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        fontFamily = NotoSansArabicFamily,
                        color = when {
                            summary.isGoalCompleted -> MaterialTheme.colorScheme.onPrimaryContainer
                            summary.isRunning -> MaterialTheme.colorScheme.onTertiaryContainer
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                            .testTag("home_study_status_badge")
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = summary.formattedRemaining,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (summary.isGoalCompleted) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.secondary
                        },
                        modifier = Modifier.testTag("home_study_countdown_text")
                    )
                    Text(
                        text = if (summary.isGoalCompleted) {
                            "أتممت ${summary.safeGoalMinutes} دقيقة دراسة اليوم"
                        } else {
                            "الوقت المتبقي من هدف ${summary.safeGoalMinutes} دقيقة"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = NotoSansArabicFamily,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = "${(summary.progressFraction * 100f).roundToInt()}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("home_study_percent_text")
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(9.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .testTag("home_study_progress_bar"),
                color = if (summary.isGoalCompleted) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.secondary
                },
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "اختر هدف وقت الدراسة اليومي:",
                style = MaterialTheme.typography.labelMedium,
                fontFamily = NotoSansArabicFamily,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(6.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                StudyTimeSummary.PRESET_MINUTES.forEach { preset ->
                    val selected = summary.safeGoalMinutes == preset
                    FilterChip(
                        selected = selected,
                        onClick = { onSelectStudyGoalMinutes(preset) },
                        label = {
                            Text(
                                text = "$preset دقيقة",
                                fontFamily = NotoSansArabicFamily
                            )
                        },
                        leadingIcon = if (selected) {
                            {
                                Icon(
                                    imageVector = Icons.Filled.Timer,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else null,
                        modifier = Modifier.testTag("study_goal_chip_$preset")
                    )
                }
                val isCustom = summary.safeGoalMinutes !in StudyTimeSummary.PRESET_MINUTES
                FilterChip(
                    selected = isCustom,
                    onClick = { showCustomDialog = true },
                    label = {
                        Text(
                            text = if (isCustom) "مخصص: ${summary.safeGoalMinutes} د" else "مخصص",
                            fontFamily = NotoSansArabicFamily
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Tune,
                            contentDescription = "Custom study time goal",
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    modifier = Modifier.testTag("study_goal_chip_custom")
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            FilledTonalButton(
                onClick = onToggleTimer,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag("home_study_timer_toggle_button")
            ) {
                Icon(
                    imageVector = if (summary.isRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when {
                        summary.isRunning -> "إيقاف مؤقت للمؤقت"
                        summary.clampedElapsedSeconds > 0 && !summary.isGoalCompleted -> "استئناف جلسة الدراسة"
                        else -> "ابدأ جلسة الدراسة"
                    },
                    fontFamily = NotoSansArabicFamily
                )
            }
        }
    }

    if (showCustomDialog) {
        StudyTimeGoalDialog(
            currentMinutes = summary.safeGoalMinutes,
            onDismiss = { showCustomDialog = false },
            onConfirmMinutes = { chosen ->
                showCustomDialog = false
                onSelectStudyGoalMinutes(chosen)
            }
        )
    }
}

@Composable
fun StudySessionCountdownBar(
    summary: StudyTimeSummary,
    onToggleTimer: () -> Unit,
    onSelectStudyGoalMinutes: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = summary.progressFraction,
        animationSpec = tween(durationMillis = 350),
        label = "sessionCountdownProgress"
    )
    var showGoalDialog by rememberSaveable { mutableStateOf(false) }

    Surface(
        color = if (summary.isGoalCompleted) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
        } else {
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
        },
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (summary.isGoalCompleted) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
            } else {
                MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f)
            }
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag("session_countdown_bar")
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (summary.isGoalCompleted) {
                            Icons.Filled.CheckCircle
                        } else {
                            Icons.Filled.Timer
                        },
                        contentDescription = "Study session countdown",
                        tint = if (summary.isGoalCompleted) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.secondary
                        },
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = summary.formattedRemaining,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.testTag("session_countdown_timer_text")
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (summary.isGoalCompleted) {
                            "تم هدف اليوم! ✓"
                        } else if (summary.isRunning) {
                            "متبقي للدراسة"
                        } else {
                            "متوقف مؤقتاً"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        fontFamily = NotoSansArabicFamily,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        onClick = { showGoalDialog = true },
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(50.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                        modifier = Modifier.testTag("session_study_goal_button")
                    ) {
                        Text(
                            text = "الهدف: ${summary.safeGoalMinutes} د",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = NotoSansArabicFamily,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .padding(horizontal = 8.dp, vertical = 5.dp)
                                .testTag("session_study_goal_label")
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    IconButton(
                        onClick = onToggleTimer,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("session_timer_pause_button")
                    ) {
                        Icon(
                            imageVector = if (summary.isRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (summary.isRunning) "Pause study timer" else "Resume study timer",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .testTag("session_study_progress_bar"),
                color = if (summary.isGoalCompleted) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.secondary
                },
                trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                strokeCap = StrokeCap.Round
            )
        }
    }

    if (showGoalDialog) {
        StudyTimeGoalDialog(
            currentMinutes = summary.safeGoalMinutes,
            onDismiss = { showGoalDialog = false },
            onConfirmMinutes = { chosen ->
                showGoalDialog = false
                onSelectStudyGoalMinutes(chosen)
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StudyTimeGoalDialog(
    currentMinutes: Int,
    onDismiss: () -> Unit,
    onConfirmMinutes: (Int) -> Unit
) {
    var minutesText by rememberSaveable(currentMinutes) { mutableStateOf(currentMinutes.toString()) }
    val parsedMinutes = minutesText.trim().toIntOrNull()
    val isValid = parsedMinutes != null &&
        parsedMinutes in StudyTimeSummary.MIN_STUDY_GOAL_MINUTES..StudyTimeSummary.MAX_STUDY_GOAL_MINUTES

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "تحديد هدف وقت الدراسة اليومي",
                fontFamily = NotoSansArabicFamily
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "اختر مدة التدريب اليومية باللغة الإنجليزية (10 أو 20 أو 30 دقيقة أو مدة مخصصة):",
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = NotoSansArabicFamily
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StudyTimeSummary.PRESET_MINUTES.forEach { preset ->
                        FilterChip(
                            selected = parsedMinutes == preset,
                            onClick = { minutesText = preset.toString() },
                            label = { Text("$preset دقيقة", fontFamily = NotoSansArabicFamily) },
                            modifier = Modifier.testTag("dialog_study_preset_$preset")
                        )
                    }
                }
                OutlinedTextField(
                    value = minutesText,
                    onValueChange = { raw ->
                        if (raw.length <= 3 && raw.all { it.isDigit() }) {
                            minutesText = raw
                        }
                    },
                    label = {
                        Text("مدة مخصصة بالدقائق (1 - 180)", fontFamily = NotoSansArabicFamily)
                    },
                    singleLine = true,
                    isError = !isValid,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_custom_study_minutes_input")
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (parsedMinutes != null &&
                        parsedMinutes in StudyTimeSummary.MIN_STUDY_GOAL_MINUTES..StudyTimeSummary.MAX_STUDY_GOAL_MINUTES
                    ) {
                        onConfirmMinutes(parsedMinutes)
                    }
                },
                enabled = isValid,
                modifier = Modifier.testTag("dialog_study_goal_save_button")
            ) {
                Text("حفظ الهدف", fontFamily = NotoSansArabicFamily)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء", fontFamily = NotoSansArabicFamily)
            }
        }
    )
}
