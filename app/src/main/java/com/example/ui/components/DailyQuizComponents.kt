package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.DailyQuizQuestion
import com.example.data.model.DailyQuizStatus
import com.example.data.model.QuizQuestionType
import com.example.ui.theme.NotoSansArabicFamily

@Composable
fun DailyQuizBannerCard(
    flashcardCount: Int,
    quizStatus: DailyQuizStatus,
    onStartQuiz: () -> Unit,
    modifier: Modifier = Modifier
) {
    val canStart = flashcardCount > 0

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (quizStatus.completedToday) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (quizStatus.completedToday) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
            } else {
                MaterialTheme.colorScheme.outlineVariant
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("daily_quiz_banner_card")
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
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = CircleShape,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (quizStatus.completedToday) {
                                    Icons.Filled.EmojiEvents
                                } else {
                                    Icons.Filled.Quiz
                                },
                                contentDescription = "Daily flashcard quiz",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Daily Flashcard Quiz",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "اختبار يومي سريع من 5 أسئلة عشوائية من بطاقاتك",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = NotoSansArabicFamily,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    color = if (quizStatus.completedToday) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.secondaryContainer
                    },
                    shape = RoundedCornerShape(50.dp)
                ) {
                    Text(
                        text = if (quizStatus.completedToday) {
                            "اليوم ${quizStatus.todayScore}/${quizStatus.totalQuestions} ✓"
                        } else {
                            "5 أسئلة"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        fontFamily = NotoSansArabicFamily,
                        color = if (quizStatus.completedToday) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        },
                        modifier = Modifier
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                            .testTag("daily_quiz_status_badge")
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = if (canStart) {
                    "راجع حفظك عبر 5 أسئلة عشوائية مأخوذة من كلماتك المحفوظة ($flashcardCount بطاقة متاحة)."
                } else {
                    "أضف كلمة واحدة على الأقل إلى بطاقات المفردات لبدء اختبار الذاكرة اليومي."
                },
                style = MaterialTheme.typography.bodySmall,
                fontFamily = NotoSansArabicFamily,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onStartQuiz,
                enabled = canStart,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag("start_daily_quiz_button")
            ) {
                Icon(
                    imageVector = if (quizStatus.completedToday) Icons.Filled.Refresh else Icons.Filled.Quiz,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (quizStatus.completedToday) {
                        "اختبار عشوائي جديد (5 أسئلة)"
                    } else {
                        "ابدأ اختبار اليوم (5 أسئلة)"
                    },
                    fontFamily = NotoSansArabicFamily
                )
            }
        }
    }
}

@Composable
fun DailyQuizDialog(
    questions: List<DailyQuizQuestion>,
    onSpeakEnglish: (String) -> Unit,
    onQuizCompleted: (score: Int, total: Int) -> Unit,
    onRetakeQuiz: () -> Unit,
    onDismiss: () -> Unit
) {
    if (questions.isEmpty()) return

    var currentIndex by remember(questions) { mutableIntStateOf(0) }
    var selectedOptionIndex by remember(questions, currentIndex) { mutableStateOf<Int?>(null) }
    var correctCount by remember(questions) { mutableIntStateOf(0) }
    var isFinished by remember(questions) { mutableStateOf(false) }

    val currentQuestion = questions[currentIndex.coerceIn(0, questions.lastIndex)]
    val isAnswered = selectedOptionIndex != null

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 560.dp)
                .padding(horizontal = 16.dp, vertical = 20.dp)
                .testTag("daily_quiz_dialog")
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isFinished) {
                            "نتيجة اختبار اليوم"
                        } else {
                            "اختبار اليوم • سؤال ${currentIndex + 1} من ${questions.size}"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = NotoSansArabicFamily,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.testTag("daily_quiz_progress_label")
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(50.dp)
                        ) {
                            Text(
                                text = "النتيجة: $correctCount/${questions.size}",
                                style = MaterialTheme.typography.labelMedium,
                                fontFamily = NotoSansArabicFamily,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                                    .testTag("daily_quiz_score_badge")
                            )
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("daily_quiz_dismiss_icon")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Close quiz"
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                val progress = if (isFinished) {
                    1f
                } else {
                    (currentIndex + if (isAnswered) 1 else 0).toFloat() / questions.size.toFloat()
                }
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    strokeCap = StrokeCap.Round
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (isFinished) {
                    val xpEarned = maxOf(15L, correctCount * 10L)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = CircleShape,
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.EmojiEvents,
                                    contentDescription = "Quiz completed",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(34.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "$correctCount / ${questions.size}",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.testTag("daily_quiz_final_score")
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = when {
                                correctCount == questions.size -> "ممتاز جداً! تذكّرت جميع الكلمات بنجاح."
                                correctCount >= 3 -> "عمل رائع! استمر في المراجعة اليومية."
                                else -> "بداية جيدة! كرّر الاختبار لتقوية ذاكرتك."
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            fontFamily = NotoSansArabicFamily,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(50.dp)
                        ) {
                            Text(
                                text = "+$xpEarned XP مكافأة المراجعة",
                                style = MaterialTheme.typography.labelLarge,
                                fontFamily = NotoSansArabicFamily,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = onDismiss,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = 48.dp)
                                    .testTag("daily_quiz_close_button")
                            ) {
                                Text("إغلاق", fontFamily = NotoSansArabicFamily)
                            }
                            Button(
                                onClick = onRetakeQuiz,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = 48.dp)
                                    .testTag("daily_quiz_retake_button")
                            ) {
                                Text("اختبار جديد", fontFamily = NotoSansArabicFamily)
                            }
                        }
                    }
                } else {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = currentQuestion.promptTitleArabic,
                                style = MaterialTheme.typography.labelLarge,
                                fontFamily = NotoSansArabicFamily,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = currentQuestion.promptMainText,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = if (currentQuestion.type == QuizQuestionType.ARABIC_TO_ENGLISH) {
                                        NotoSansArabicFamily
                                    } else {
                                        MaterialTheme.typography.headlineSmall.fontFamily
                                    },
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.testTag("daily_quiz_prompt_text")
                                )
                                if (currentQuestion.type == QuizQuestionType.ENGLISH_TO_ARABIC || isAnswered) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    IconButton(
                                        onClick = { onSpeakEnglish(currentQuestion.speakableEnglishText) },
                                        modifier = Modifier.testTag("daily_quiz_speak_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                            contentDescription = "Listen to ${currentQuestion.speakableEnglishText}",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        currentQuestion.options.forEachIndexed { optionIdx, optionText ->
                            val isSelected = selectedOptionIndex == optionIdx
                            val isCorrectOption = optionIdx == currentQuestion.correctOptionIndex
                            val containerColor = when {
                                !isAnswered -> MaterialTheme.colorScheme.surface
                                isCorrectOption -> MaterialTheme.colorScheme.primaryContainer
                                isSelected -> MaterialTheme.colorScheme.errorContainer
                                else -> MaterialTheme.colorScheme.surface
                            }
                            val borderColor = when {
                                !isAnswered -> MaterialTheme.colorScheme.outlineVariant
                                isCorrectOption -> MaterialTheme.colorScheme.primary
                                isSelected -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.outlineVariant
                            }

                            Surface(
                                onClick = {
                                    if (!isAnswered) {
                                        selectedOptionIndex = optionIdx
                                        if (optionIdx == currentQuestion.correctOptionIndex) {
                                            correctCount++
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(14.dp),
                                color = containerColor,
                                border = BorderStroke(
                                    width = if (isAnswered && (isCorrectOption || isSelected)) 1.5.dp else 1.dp,
                                    color = borderColor
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .defaultMinSize(minHeight = 48.dp)
                                    .testTag("daily_quiz_option_$optionIdx")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = optionText,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontFamily = if (currentQuestion.type == QuizQuestionType.ENGLISH_TO_ARABIC) {
                                            NotoSansArabicFamily
                                        } else {
                                            MaterialTheme.typography.titleMedium.fontFamily
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (isAnswered && isCorrectOption) {
                                        Icon(
                                            imageVector = Icons.Filled.CheckCircle,
                                            contentDescription = "Correct answer",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    } else if (isAnswered && isSelected) {
                                        Icon(
                                            imageVector = Icons.Filled.Cancel,
                                            contentDescription = "Incorrect answer",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (isAnswered) {
                        Spacer(modifier = Modifier.height(12.dp))
                        if (currentQuestion.phoneticHint.isNotBlank() || currentQuestion.exampleEnglish.isNotBlank()) {
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    if (currentQuestion.phoneticHint.isNotBlank()) {
                                        Text(
                                            text = "النطق: ${currentQuestion.phoneticHint}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontFamily = NotoSansArabicFamily,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                    if (currentQuestion.exampleEnglish.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = currentQuestion.exampleEnglish,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        Button(
                            onClick = {
                                if (currentIndex + 1 >= questions.size) {
                                    isFinished = true
                                    onQuizCompleted(correctCount, questions.size)
                                } else {
                                    currentIndex++
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("daily_quiz_next_button")
                        ) {
                            Text(
                                text = if (currentIndex + 1 >= questions.size) {
                                    "عرض النتيجة النهائية"
                                } else {
                                    "السؤال التالي"
                                },
                                fontFamily = NotoSansArabicFamily
                            )
                        }
                    }
                }
            }
        }
    }
}
