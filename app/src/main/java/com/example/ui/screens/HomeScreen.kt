package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SlowMotionVideo
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.DailyProgressSummary
import com.example.data.model.DailyQuizStatus
import com.example.data.model.Lesson
import com.example.data.model.ReminderSettings
import com.example.data.model.StudyTimeSummary
import com.example.data.model.UserProgress
import com.example.data.model.VocabItem
import com.example.ui.components.DailyPracticeReminderCard
import com.example.ui.components.DailyProgressTrackerCard
import com.example.ui.components.DailyQuizBannerCard
import com.example.ui.components.DailyStudyTimeGoalCard
import com.example.ui.components.MainLearningStreakBadge
import com.example.ui.components.StudySessionCountdownBar
import com.example.ui.theme.NotoSansArabicFamily
import com.example.ui.viewmodel.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    displayName: String,
    lessons: List<Lesson>,
    progressState: UiState<UserProgress?>,
    vocabState: UiState<List<VocabItem>>,
    selectedLesson: Lesson?,
    slowSpeech: Boolean,
    onToggleSlowSpeech: () -> Unit,
    onSelectLesson: (Lesson?) -> Unit,
    onCompleteLesson: (Lesson) -> Unit,
    onSpeakPhrase: (String) -> Unit,
    onSavePhrase: (english: String, arabic: String, phonetic: String, category: String) -> Unit,
    onOpenWrittenMode: (Lesson?) -> Unit,
    onOpenVocalMode: (Lesson?) -> Unit,
    dailyProgress: DailyProgressSummary = DailyProgressSummary(),
    onSelectDailyGoal: (Int) -> Unit = {},
    studyTimeSummary: StudyTimeSummary = StudyTimeSummary(),
    onSelectStudyGoalMinutes: (Int) -> Unit = {},
    onToggleStudyTimer: () -> Unit = {},
    reminderSettings: ReminderSettings = ReminderSettings(),
    onUpdateReminderSettings: (Boolean, Int, Int) -> Unit = { _, _, _ -> },
    onSendTestReminder: () -> Unit = {},
    dailyQuizStatus: DailyQuizStatus = DailyQuizStatus(),
    onStartDailyQuiz: () -> Unit = {}
) {
    val progress = (progressState as? UiState.Success)?.data
    val completedIds = progress?.completedLessonIds.orEmpty().toSet()
    val xp = progress?.xp ?: 0L
    val savedWordsCount = (vocabState as? UiState.Success)?.data?.size ?: 0

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                HeroModeBanner(
                    displayName = displayName,
                    xp = xp,
                    completedCount = completedIds.size,
                    totalLessons = lessons.size,
                    savedWordsCount = savedWordsCount,
                    streakDays = dailyProgress.currentStreakDays,
                    onOpenWrittenMode = { onOpenWrittenMode(null) },
                    onOpenVocalMode = { onOpenVocalMode(null) }
                )
            }

            item {
                MainLearningStreakBadge(summary = dailyProgress)
            }

            item {
                DailyQuizBannerCard(
                    flashcardCount = savedWordsCount,
                    quizStatus = dailyQuizStatus,
                    onStartQuiz = onStartDailyQuiz
                )
            }

            item {
                DailyProgressTrackerCard(
                    summary = dailyProgress,
                    onPracticeClick = { onOpenWrittenMode(null) },
                    onSelectDailyGoal = onSelectDailyGoal
                )
            }

            item {
                DailyStudyTimeGoalCard(
                    summary = studyTimeSummary,
                    onSelectStudyGoalMinutes = onSelectStudyGoalMinutes,
                    onToggleTimer = onToggleStudyTimer
                )
            }

            item {
                DailyPracticeReminderCard(
                    settings = reminderSettings,
                    onUpdateSettings = onUpdateReminderSettings,
                    onSendTestReminder = onSendTestReminder
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Beginner Curriculum",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "دروس تأسيسية مصممة للمتحدثين بالعربية",
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = NotoSansArabicFamily,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(50.dp)
                    ) {
                        Text(
                            text = "${completedIds.size}/${lessons.size}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            items(lessons, key = { it.id }) { lesson ->
                val isCompleted = completedIds.contains(lesson.id)
                LessonCard(
                    lesson = lesson,
                    isCompleted = isCompleted,
                    onClick = { onSelectLesson(lesson) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (selectedLesson != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { onSelectLesson(null) },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            LessonDetailSheetContent(
                lesson = selectedLesson,
                isCompleted = completedIds.contains(selectedLesson.id),
                slowSpeech = slowSpeech,
                onToggleSlowSpeech = onToggleSlowSpeech,
                onSpeakPhrase = onSpeakPhrase,
                onSavePhrase = { phrase ->
                    onSavePhrase(
                        phrase.english,
                        phrase.arabic,
                        phrase.phoneticArabic,
                        selectedLesson.titleEnglish
                    )
                },
                onCompleteLesson = { onCompleteLesson(selectedLesson) },
                onPracticeWritten = {
                    val target = selectedLesson
                    onSelectLesson(null)
                    onOpenWrittenMode(target)
                },
                onPracticeVocal = {
                    val target = selectedLesson
                    onSelectLesson(null)
                    onOpenVocalMode(target)
                },
                studyTimeSummary = studyTimeSummary,
                onToggleStudyTimer = onToggleStudyTimer,
                onSelectStudyGoalMinutes = onSelectStudyGoalMinutes
            )
        }
    }
}

@Composable
private fun HeroModeBanner(
    displayName: String,
    xp: Long,
    completedCount: Int,
    totalLessons: Int,
    savedWordsCount: Int,
    streakDays: Int,
    onOpenWrittenMode: () -> Unit,
    onOpenVocalMode: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(164.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_hero_oasis),
                    contentDescription = "Noor English learning studio",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.12f),
                                    Color.Black.copy(alpha = 0.68f)
                                )
                            )
                        )
                )
                Surface(
                    color = Color.Black.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(50.dp),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .testTag("home_hero_streak_badge")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LocalFireDepartment,
                            contentDescription = "Learning streak",
                            tint = Color(0xFFFFB74D),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (streakDays == 1) "1 يوم" else "$streakDays أيام",
                            style = MaterialTheme.typography.labelMedium,
                            fontFamily = NotoSansArabicFamily,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Marhaba, $displayName",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "اختر وضع التدريب المناسب لك الآن",
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = NotoSansArabicFamily,
                        color = Color.White.copy(alpha = 0.92f)
                    )
                }
            }

            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatPill(
                        icon = Icons.Filled.Star,
                        value = "$xp XP",
                        label = "نقاطك",
                        modifier = Modifier.weight(1f)
                    )
                    StatPill(
                        icon = Icons.Filled.School,
                        value = "$completedCount/$totalLessons",
                        label = "الدروس",
                        modifier = Modifier.weight(1f)
                    )
                    StatPill(
                        icon = Icons.Filled.BookmarkAdd,
                        value = "$savedWordsCount",
                        label = "كلماتك",
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onOpenWrittenMode,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("home_written_mode_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.EditNote,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Written",
                                style = MaterialTheme.typography.labelLarge
                            )
                            Text(
                                text = "كتابي",
                                style = MaterialTheme.typography.labelMedium,
                                fontFamily = NotoSansArabicFamily
                            )
                        }
                    }

                    FilledTonalButton(
                        onClick = onOpenVocalMode,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("home_vocal_mode_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.GraphicEq,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Vocal",
                                style = MaterialTheme.typography.labelLarge
                            )
                            Text(
                                text = "صوتي",
                                style = MaterialTheme.typography.labelMedium,
                                fontFamily = NotoSansArabicFamily
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    fontFamily = NotoSansArabicFamily,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun LessonCard(
    lesson: Lesson,
    isCompleted: Boolean,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("lesson_card_${lesson.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = if (isCompleted) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                shape = CircleShape,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "${lesson.number}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isCompleted) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = lesson.titleEnglish,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = lesson.titleArabic,
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = NotoSansArabicFamily,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = lesson.subtitleArabic,
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = NotoSansArabicFamily,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                Icon(
                    imageVector = if (isCompleted) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                    contentDescription = if (isCompleted) "Completed" else "Not completed",
                    tint = if (isCompleted) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outline
                    },
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "+${lesson.xpReward} XP",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

@Composable
private fun LessonDetailSheetContent(
    lesson: Lesson,
    isCompleted: Boolean,
    slowSpeech: Boolean,
    onToggleSlowSpeech: () -> Unit,
    onSpeakPhrase: (String) -> Unit,
    onSavePhrase: (com.example.data.model.LessonPhrase) -> Unit,
    onCompleteLesson: () -> Unit,
    onPracticeWritten: () -> Unit,
    onPracticeVocal: () -> Unit,
    studyTimeSummary: StudyTimeSummary = StudyTimeSummary(),
    onToggleStudyTimer: () -> Unit = {},
    onSelectStudyGoalMinutes: (Int) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        StudySessionCountdownBar(
            summary = studyTimeSummary,
            onToggleTimer = onToggleStudyTimer,
            onSelectStudyGoalMinutes = onSelectStudyGoalMinutes
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = lesson.titleEnglish,
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = lesson.titleArabic,
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = NotoSansArabicFamily,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            FilterChip(
                selected = slowSpeech,
                onClick = onToggleSlowSpeech,
                label = {
                    Text(
                        text = if (slowSpeech) "بطيء 0.7x" else "سرعة عادية",
                        fontFamily = NotoSansArabicFamily
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.SlowMotionVideo,
                        contentDescription = "Slow speech toggle",
                        modifier = Modifier.size(18.dp)
                    )
                }
            )
        }

        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.75f),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "ملاحظة ذهبية للمتحدث بالعربية",
                    style = MaterialTheme.typography.labelLarge,
                    fontFamily = NotoSansArabicFamily,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = lesson.focusTipArabic,
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = NotoSansArabicFamily,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }

        lesson.phrases.forEach { phrase ->
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = phrase.english,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = phrase.arabic,
                                style = MaterialTheme.typography.bodyMedium,
                                fontFamily = NotoSansArabicFamily,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "النطق: ${phrase.phoneticArabic}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontFamily = NotoSansArabicFamily,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { onSpeakPhrase(phrase.english) }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Listen to ${phrase.english}",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(onClick = { onSavePhrase(phrase) }) {
                            Icon(
                                imageVector = Icons.Filled.BookmarkAdd,
                                contentDescription = "Save ${phrase.english} to wordbook",
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                    if (phrase.tipArabic.isNotBlank()) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                        )
                        Text(
                            text = phrase.tipArabic,
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = NotoSansArabicFamily,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onPracticeWritten,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Icon(Icons.Filled.EditNote, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("تدريب كتابي", fontFamily = NotoSansArabicFamily)
            }
            OutlinedButton(
                onClick = onPracticeVocal,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Icon(Icons.Filled.GraphicEq, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("تدريب صوتي", fontFamily = NotoSansArabicFamily)
            }
        }

        Button(
            onClick = onCompleteLesson,
            enabled = !isCompleted,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("complete_lesson_button")
        ) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isCompleted) "تم إتمام الدرس ✓" else "إتمام الدرس (+${lesson.xpReward} XP)",
                fontFamily = NotoSansArabicFamily
            )
        }
    }
}
