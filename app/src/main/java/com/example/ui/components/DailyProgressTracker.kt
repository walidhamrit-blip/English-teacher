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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.DailyHabitDay
import com.example.data.model.DailyProgressSummary
import com.example.ui.theme.NotoSansArabicFamily
import kotlin.math.roundToInt

val PRESET_DAILY_GOALS = listOf(5, 10, 20)

@Composable
fun HeaderStreakBadge(
    summary: DailyProgressSummary,
    modifier: Modifier = Modifier
) {
    var showStreakDetails by rememberSaveable { mutableStateOf(false) }
    val streakCount = summary.currentStreakDays.coerceAtLeast(0)
    val isActive = streakCount > 0

    Surface(
        onClick = { showStreakDetails = true },
        color = if (summary.isGoalReached) {
            MaterialTheme.colorScheme.primaryContainer
        } else if (isActive) {
            MaterialTheme.colorScheme.tertiaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        shape = RoundedCornerShape(50.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (summary.isGoalReached) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
            } else {
                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f)
            }
        ),
        modifier = modifier
            .minimumInteractiveComponentSize()
            .semantics(mergeDescendants = true) {}
            .testTag("header_daily_streak_badge")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.LocalFireDepartment,
                contentDescription = "Daily streak",
                tint = if (summary.isGoalReached) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.tertiary
                },
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "$streakCount",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (summary.isGoalReached) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onTertiaryContainer
                }
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = if (streakCount == 1) "يوم" else "أيام",
                style = MaterialTheme.typography.labelMedium,
                fontFamily = NotoSansArabicFamily,
                color = if (summary.isGoalReached) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onTertiaryContainer
                }
            )
        }
    }

    if (showStreakDetails) {
        StreakDetailsDialog(
            summary = summary,
            onDismiss = { showStreakDetails = false }
        )
    }
}

@Composable
fun MainLearningStreakBadge(
    summary: DailyProgressSummary,
    modifier: Modifier = Modifier
) {
    var showMilestones by rememberSaveable { mutableStateOf(false) }
    val streakCount = summary.currentStreakDays.coerceAtLeast(0)
    val longest = maxOf(summary.longestStreakDays, streakCount)
    val animatedMilestoneProgress by animateFloatAsState(
        targetValue = summary.streakMilestoneProgress,
        animationSpec = tween(durationMillis = 500),
        label = "streakMilestoneProgress"
    )

    Card(
        onClick = { showMilestones = true },
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (streakCount > 0) {
                MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (streakCount > 0) {
                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.45f)
            } else {
                MaterialTheme.colorScheme.outlineVariant
            }
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag("home_learning_streak_badge")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(64.dp)
            ) {
                androidx.compose.material3.CircularProgressIndicator(
                    progress = { animatedMilestoneProgress },
                    modifier = Modifier.size(64.dp),
                    color = MaterialTheme.colorScheme.tertiary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeWidth = 6.dp,
                    strokeCap = StrokeCap.Round
                )
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.LocalFireDepartment,
                            contentDescription = "Learning streak flame",
                            tint = if (streakCount > 0) {
                                MaterialTheme.colorScheme.tertiary
                            } else {
                                MaterialTheme.colorScheme.outline
                            },
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Learning Streak",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(50.dp)
                    ) {
                        Text(
                            text = summary.streakBadgeTitleArabic,
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = NotoSansArabicFamily,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                .testTag("home_streak_tier_badge")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "$streakCount",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.testTag("home_streak_days_count")
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (streakCount == 1) "يوم دراسة متواصل" else "أيام دراسة متواصلة",
                        style = MaterialTheme.typography.titleSmall,
                        fontFamily = NotoSansArabicFamily,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                LinearProgressIndicator(
                    progress = { animatedMilestoneProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .testTag("home_streak_milestone_progress"),
                    color = MaterialTheme.colorScheme.tertiary,
                    trackColor = MaterialTheme.colorScheme.surface,
                    strokeCap = StrokeCap.Round
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (summary.remainingDaysToNextMilestone > 0) {
                            "الشارة التالية: ${summary.nextStreakMilestoneDays} أيام"
                        } else {
                            "حققت شارة الإنجاز!"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        fontFamily = NotoSansArabicFamily,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "الأطول: $longest يوم",
                        style = MaterialTheme.typography.labelMedium,
                        fontFamily = NotoSansArabicFamily,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.testTag("home_longest_streak_label")
                    )
                }
            }
        }
    }

    if (showMilestones) {
        StreakDetailsDialog(
            summary = summary,
            onDismiss = { showMilestones = false }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StreakDetailsDialog(
    summary: DailyProgressSummary,
    onDismiss: () -> Unit
) {
    val streakCount = summary.currentStreakDays.coerceAtLeast(0)
    val longest = maxOf(summary.longestStreakDays, streakCount)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.LocalFireDepartment,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "سلسلة التعلّم المتواصلة",
                    fontFamily = NotoSansArabicFamily
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.testTag("header_streak_details_dialog")
            ) {
                Text(
                    text = "السلسلة الحالية: $streakCount يوم متواصل",
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = NotoSansArabicFamily,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "أطول سلسلة: $longest يوم",
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = NotoSansArabicFamily
                )
                Text(
                    text = "رسائل اليوم: ${summary.todayMessagesCount} / ${summary.dailyGoal}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = NotoSansArabicFamily
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(3, 7, 14, 30).forEach { milestone ->
                        val unlocked = longest >= milestone
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (unlocked) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            }
                        ) {
                            Text(
                                text = if (unlocked) "$milestone أيام ✓" else "$milestone أيام",
                                style = MaterialTheme.typography.labelMedium,
                                fontFamily = NotoSansArabicFamily,
                                color = if (unlocked) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
                Text(
                    text = "افتح التطبيق وتدرّب على الدروس أو المحادثة أو البطاقات كل يوم لبناء عادة قوية والحفاظ على سلسلتك!",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = NotoSansArabicFamily,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("حسناً", fontFamily = NotoSansArabicFamily)
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DailyProgressTrackerCard(
    summary: DailyProgressSummary,
    onPracticeClick: (() -> Unit)? = null,
    onSelectDailyGoal: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = summary.progressFraction,
        animationSpec = tween(durationMillis = 450),
        label = "dailyProgressBar"
    )
    val percentComplete = (summary.progressFraction * 100f).roundToInt()
    var showGoalDialog by rememberSaveable { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("home_daily_progress_tracker")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = if (summary.isGoalReached) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.tertiaryContainer
                        },
                        shape = CircleShape,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.LocalFireDepartment,
                                contentDescription = "Daily learning streak",
                                tint = if (summary.isGoalReached) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.tertiary
                                },
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Daily Habit Tracker",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "متابعة رسائلك اليومية لبناء عادة التعلم",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = NotoSansArabicFamily,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    color = if (summary.currentStreakDays > 0) {
                        MaterialTheme.colorScheme.tertiaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    shape = RoundedCornerShape(50.dp),
                    modifier = Modifier
                        .semantics(mergeDescendants = true) {}
                        .testTag("daily_streak_badge")
                ) {
                    Text(
                        text = if (summary.currentStreakDays > 0) {
                            "${summary.currentStreakDays} يوم 🔥"
                        } else {
                            "ابدأ اليوم"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        fontFamily = NotoSansArabicFamily,
                        color = if (summary.currentStreakDays > 0) {
                            MaterialTheme.colorScheme.onTertiaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "${summary.todayMessagesCount}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.testTag("daily_today_message_count")
                    )
                    Text(
                        text = " / ${summary.dailyGoal} رسائل اليوم",
                        style = MaterialTheme.typography.titleSmall,
                        fontFamily = NotoSansArabicFamily,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .padding(bottom = 3.dp)
                            .testTag("daily_goal_target_label")
                    )
                }

                Text(
                    text = if (summary.isGoalReached) {
                        "100% مكتمل ✓"
                    } else {
                        "$percentComplete% • متبقي ${summary.remainingForGoal}"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    fontFamily = NotoSansArabicFamily,
                    color = if (summary.isGoalReached) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.secondary
                    },
                    modifier = Modifier.testTag("daily_progress_percent")
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .testTag("daily_progress_bar"),
                color = if (summary.isGoalReached) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.tertiary
                },
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round
            )

            if (onSelectDailyGoal != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "هدف الرسائل اليومي:",
                    style = MaterialTheme.typography.labelMedium,
                    fontFamily = NotoSansArabicFamily,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PRESET_DAILY_GOALS.forEach { preset ->
                        val isSelected = summary.dailyGoal == preset
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSelectDailyGoal(preset) },
                            label = {
                                Text(
                                    text = "$preset رسائل",
                                    fontFamily = NotoSansArabicFamily
                                )
                            },
                            leadingIcon = if (isSelected) {
                                {
                                    Icon(
                                        imageVector = Icons.Filled.Flag,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            } else null,
                            modifier = Modifier.testTag("daily_goal_chip_$preset")
                        )
                    }
                    val isCustom = summary.dailyGoal !in PRESET_DAILY_GOALS
                    FilterChip(
                        selected = isCustom,
                        onClick = { showGoalDialog = true },
                        label = {
                            Text(
                                text = if (isCustom) "مخصص: ${summary.dailyGoal}" else "مخصص",
                                fontFamily = NotoSansArabicFamily
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Tune,
                                contentDescription = "Custom daily goal",
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        modifier = Modifier.testTag("daily_goal_chip_custom")
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                HabitCountChip(label = "كتابي اليوم: ${summary.writtenTodayCount}")
                HabitCountChip(label = "صوتي اليوم: ${summary.vocalTodayCount}")
                HabitCountChip(label = "إجمالي الرسائل: ${summary.totalMessagesCount}")
            }

            if (summary.lastSevenDays.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    summary.lastSevenDays.forEach { day ->
                        HabitDayCell(day = day)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = when {
                    summary.isGoalReached ->
                        "ممتاز! أتممت هدفك اليومي في ممارسة الإنجليزية. استمر غداً للحفاظ على سلسلتك!"
                    summary.todayMessagesCount > 0 ->
                        "أحسنت! أرسل ${summary.remainingForGoal} رسائل إضافية بالكتابة أو النطق لإتمام هدف اليوم."
                    else ->
                        "أرسل ${summary.dailyGoal} رسائل قصيرة بالإنجليزية اليوم لتبني عادة تعلم يومية ثابتة."
                },
                style = MaterialTheme.typography.bodySmall,
                fontFamily = NotoSansArabicFamily,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (onPracticeClick != null && !summary.isGoalReached) {
                Spacer(modifier = Modifier.height(10.dp))
                FilledTonalButton(
                    onClick = onPracticeClick,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("daily_tracker_practice_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Chat,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "أرسل رسالة تدريب الآن",
                        fontFamily = NotoSansArabicFamily
                    )
                }
            }
        }
    }

    if (showGoalDialog && onSelectDailyGoal != null) {
        DailyGoalPickerDialog(
            currentGoal = summary.dailyGoal,
            onDismiss = { showGoalDialog = false },
            onConfirmGoal = { chosen ->
                showGoalDialog = false
                onSelectDailyGoal(chosen)
            }
        )
    }
}

@Composable
fun CompactDailyProgressStrip(
    summary: DailyProgressSummary,
    onSelectDailyGoal: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = summary.progressFraction,
        animationSpec = tween(durationMillis = 350),
        label = "chatDailyProgress"
    )
    var showGoalDialog by rememberSaveable { mutableStateOf(false) }

    Surface(
        color = if (summary.isGoalReached) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        },
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("chat_daily_progress_tracker")
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.LocalFireDepartment,
                        contentDescription = "Daily message goal",
                        tint = if (summary.isGoalReached) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.tertiary
                        },
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "رسائل اليوم: ${summary.todayMessagesCount} / ${summary.dailyGoal}",
                        style = MaterialTheme.typography.labelLarge,
                        fontFamily = NotoSansArabicFamily,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = when {
                            summary.isGoalReached -> "تم تحقيق هدف اليوم! ✓"
                            summary.currentStreakDays > 0 -> "سلسلة ${summary.currentStreakDays} يوم"
                            else -> "متبقي ${summary.remainingForGoal}"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        fontFamily = NotoSansArabicFamily,
                        color = if (summary.isGoalReached) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    if (onSelectDailyGoal != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            onClick = { showGoalDialog = true },
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(50.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                            modifier = Modifier.testTag("chat_daily_goal_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Tune,
                                    contentDescription = "Set daily message goal",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "الهدف: ${summary.dailyGoal}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = NotoSansArabicFamily,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .testTag("chat_daily_progress_bar"),
                color = if (summary.isGoalReached) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.tertiary
                },
                trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                strokeCap = StrokeCap.Round
            )
        }
    }

    if (showGoalDialog && onSelectDailyGoal != null) {
        DailyGoalPickerDialog(
            currentGoal = summary.dailyGoal,
            onDismiss = { showGoalDialog = false },
            onConfirmGoal = { chosen ->
                showGoalDialog = false
                onSelectDailyGoal(chosen)
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DailyGoalPickerDialog(
    currentGoal: Int,
    onDismiss: () -> Unit,
    onConfirmGoal: (Int) -> Unit
) {
    var goalText by rememberSaveable(currentGoal) { mutableStateOf(currentGoal.toString()) }
    val parsedGoal = goalText.trim().toIntOrNull()
    val isValid = parsedGoal != null && parsedGoal in 1..100

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "تحديد هدف الرسائل اليومي",
                fontFamily = NotoSansArabicFamily
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "اختر عدد الرسائل التي تريد إرسالها يومياً باللغة الإنجليزية (5 أو 10 أو 20 أو عدد مخصص):",
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = NotoSansArabicFamily
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PRESET_DAILY_GOALS.forEach { preset ->
                        FilterChip(
                            selected = parsedGoal == preset,
                            onClick = { goalText = preset.toString() },
                            label = { Text("$preset رسائل", fontFamily = NotoSansArabicFamily) },
                            modifier = Modifier.testTag("dialog_goal_preset_$preset")
                        )
                    }
                }
                OutlinedTextField(
                    value = goalText,
                    onValueChange = { raw ->
                        if (raw.length <= 3 && raw.all { it.isDigit() }) {
                            goalText = raw
                        }
                    },
                    label = {
                        Text("هدف مخصص (1 - 100)", fontFamily = NotoSansArabicFamily)
                    },
                    singleLine = true,
                    isError = !isValid,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_custom_goal_input")
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (parsedGoal != null && parsedGoal in 1..100) {
                        onConfirmGoal(parsedGoal)
                    }
                },
                enabled = isValid,
                modifier = Modifier.testTag("dialog_goal_save_button")
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

@Composable
private fun HabitCountChip(label: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        shape = RoundedCornerShape(10.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontFamily = NotoSansArabicFamily,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

@Composable
private fun HabitDayCell(day: DailyHabitDay) {
    val containerColor = when {
        day.goalMet -> MaterialTheme.colorScheme.primary
        day.messageCount > 0 -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    }
    val contentColor = when {
        day.goalMet -> MaterialTheme.colorScheme.onPrimary
        day.messageCount > 0 -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val border = if (day.isToday) {
        BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
    } else {
        null
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Surface(
            color = containerColor,
            shape = CircleShape,
            border = border,
            modifier = Modifier.size(34.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (day.goalMet) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = "${day.englishShortLabel} goal complete",
                        tint = contentColor,
                        modifier = Modifier.size(16.dp)
                    )
                } else {
                    Text(
                        text = if (day.messageCount > 0) "${day.messageCount}" else "•",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = contentColor
                    )
                }
            }
        }
        Text(
            text = day.arabicShortLabel,
            style = MaterialTheme.typography.labelSmall,
            fontFamily = NotoSansArabicFamily,
            fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Normal,
            color = if (day.isToday) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}
