package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.SlowMotionVideo
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.model.DailyQuizStatus
import com.example.data.model.PronunciationHelper
import com.example.data.model.StudyTimeSummary
import com.example.data.model.VocabItem
import com.example.ui.components.InteractiveFlashcardDeck
import com.example.ui.components.StudySessionCountdownBar
import com.example.ui.components.formatCategoryArabic
import com.example.ui.theme.NotoSansArabicFamily
import com.example.ui.viewmodel.UiState

private enum class VocabularyViewMode {
    FLASHCARDS,
    LIST
}

private enum class WordFilter(val labelArabic: String, val tag: String) {
    ALL("الكل", "vocab_filter_all"),
    FREQUENT("من المحادثات", "vocab_filter_conversation"),
    LEARNING("قيد التعلّم", "vocab_filter_learning"),
    MASTERED("متقنة", "vocab_filter_mastered")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WordbookScreen(
    vocabState: UiState<List<VocabItem>>,
    isTranslatingWord: Boolean,
    isSyncingConversationVocab: Boolean = false,
    hasConversationTurns: Boolean = false,
    isSpeaking: Boolean = false,
    onSyncConversationVocab: () -> Unit = {},
    onSpeakWord: (String) -> Unit,
    onSpeakWordSlow: (String) -> Unit = { onSpeakWord(it) },
    onStopSpeaking: () -> Unit = {},
    onGeneratePhonetics: ((VocabItem) -> Unit)? = null,
    onToggleMastered: (VocabItem) -> Unit,
    onDeleteWord: (String) -> Unit,
    onTranslateAndSave: (String) -> Unit,
    onAddManualWord: (english: String, arabic: String, phonetic: String, example: String) -> Unit,
    studyTimeSummary: StudyTimeSummary? = null,
    onToggleStudyTimer: (() -> Unit)? = null,
    onSelectStudyGoalMinutes: ((Int) -> Unit)? = null,
    dailyQuizStatus: DailyQuizStatus = DailyQuizStatus(),
    onStartDailyQuiz: (() -> Unit)? = null
) {
    var viewMode by remember { mutableStateOf(VocabularyViewMode.FLASHCARDS) }
    var filter by remember { mutableStateOf(WordFilter.ALL) }
    var showAddDialog by remember { mutableStateOf(false) }

    val words = (vocabState as? UiState.Success)?.data.orEmpty()
    val filteredWords = remember(words, filter) {
        when (filter) {
            WordFilter.ALL -> words.sortedByDescending { it.usageCount }
            WordFilter.FREQUENT -> words
                .filter { it.category.equals("Conversation", ignoreCase = true) || it.usageCount > 1 }
                .sortedByDescending { it.usageCount }
            WordFilter.LEARNING -> words.filter { !it.mastered }.sortedByDescending { it.usageCount }
            WordFilter.MASTERED -> words.filter { it.mastered }.sortedByDescending { it.usageCount }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp)
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Vocabulary • المفردات",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = "كلمات محادثاتك الأكثر استخداماً وبطاقات المراجعة",
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = NotoSansArabicFamily,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(
                        onClick = onSyncConversationVocab,
                        enabled = !isSyncingConversationVocab,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("vocab_sync_chats_button")
                    ) {
                        if (isSyncingConversationVocab) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(16.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Sync,
                                contentDescription = "Sync frequent words from conversations",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("من محادثاتك", fontFamily = NotoSansArabicFamily)
                    }

                    Button(
                        onClick = { showAddDialog = true },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("add_word_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Add word",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("إضافة", fontFamily = NotoSansArabicFamily)
                    }
                }
            }

            if (studyTimeSummary != null) {
                Spacer(modifier = Modifier.height(10.dp))
                StudySessionCountdownBar(
                    summary = studyTimeSummary,
                    onToggleTimer = { onToggleStudyTimer?.invoke() },
                    onSelectStudyGoalMinutes = { mins -> onSelectStudyGoalMinutes?.invoke(mins) }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            when (vocabState) {
                is UiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is UiState.Error -> {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                        )
                    ) {
                        Text(
                            text = vocabState.message,
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }

                is UiState.Success -> {
                    if (words.isEmpty()) {
                        EmptyWordbookCard(
                            hasConversationTurns = hasConversationTurns,
                            isSyncing = isSyncingConversationVocab,
                            onSyncClick = onSyncConversationVocab,
                            onAddClick = { showAddDialog = true }
                        )
                    } else {
                        if (onStartDailyQuiz != null) {
                            FilledTonalButton(
                                onClick = onStartDailyQuiz,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .defaultMinSize(minHeight = 48.dp)
                                    .testTag("wordbook_daily_quiz_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Quiz,
                                    contentDescription = "Start daily flashcard quiz",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (dailyQuizStatus.completedToday) {
                                        "اختبار اليوم السريع (${dailyQuizStatus.todayScore}/${dailyQuizStatus.totalQuestions} ✓) • اختبار جديد"
                                    } else {
                                        "ابدأ اختبار اليوم (5 أسئلة من بطاقاتك)"
                                    },
                                    fontFamily = NotoSansArabicFamily
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        SingleChoiceSegmentedButtonRow(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            SegmentedButton(
                                selected = viewMode == VocabularyViewMode.FLASHCARDS,
                                onClick = { viewMode = VocabularyViewMode.FLASHCARDS },
                                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                                icon = {
                                    Icon(
                                        imageVector = Icons.Filled.Style,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                modifier = Modifier.testTag("vocab_mode_flashcards")
                            ) {
                                Text("البطاقات التفاعلية", fontFamily = NotoSansArabicFamily)
                            }
                            SegmentedButton(
                                selected = viewMode == VocabularyViewMode.LIST,
                                onClick = { viewMode = VocabularyViewMode.LIST },
                                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                                icon = {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                modifier = Modifier.testTag("vocab_mode_list")
                            ) {
                                Text("قائمة المفردات (${words.size})", fontFamily = NotoSansArabicFamily)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (viewMode == VocabularyViewMode.FLASHCARDS) {
                            InteractiveFlashcardDeck(
                                words = words,
                                onSpeakWord = onSpeakWord,
                                onSpeakWordSlow = onSpeakWordSlow,
                                onStopSpeaking = onStopSpeaking,
                                isSpeaking = isSpeaking,
                                onGeneratePhonetics = onGeneratePhonetics,
                                onToggleMastered = onToggleMastered,
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                WordFilter.entries.forEach { entry ->
                                    FilterChip(
                                        selected = filter == entry,
                                        onClick = { filter = entry },
                                        label = {
                                            Text(entry.labelArabic, fontFamily = NotoSansArabicFamily)
                                        },
                                        modifier = Modifier.testTag(entry.tag)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            if (filteredWords.isEmpty()) {
                                EmptyWordbookCard(
                                    hasConversationTurns = hasConversationTurns,
                                    isSyncing = isSyncingConversationVocab,
                                    onSyncClick = onSyncConversationVocab,
                                    onAddClick = { showAddDialog = true }
                                )
                            } else {
                                LazyColumn(
                                    contentPadding = PaddingValues(bottom = 24.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(filteredWords, key = { it.id }) { item ->
                                        VocabCard(
                                            item = item,
                                            onSpeak = { onSpeakWord(item.english) },
                                            onSpeakSlow = { onSpeakWordSlow(item.english) },
                                            onToggleMastered = { onToggleMastered(item) },
                                            onDelete = { onDeleteWord(item.id) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddWordDialog(
            isTranslating = isTranslatingWord,
            onDismiss = { showAddDialog = false },
            onSpeakPreview = { word -> onSpeakWord(word) },
            onAiTranslateSave = { input ->
                onTranslateAndSave(input)
                showAddDialog = false
            },
            onManualSave = { en, ar, ph, ex ->
                onAddManualWord(en, ar, ph, ex)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun EmptyWordbookCard(
    hasConversationTurns: Boolean,
    isSyncing: Boolean,
    onSyncClick: () -> Unit,
    onAddClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Filled.Style,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "بطاقات مفرداتك جاهزة للتعلّم",
                style = MaterialTheme.typography.titleLarge,
                fontFamily = NotoSansArabicFamily,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "تُحفظ هنا تلقائياً الكلمات الإنجليزية التي تستخدمها بكثرة في محادثاتك الكتابية والصوتية، مع التهجئة الصوتية والنطق المسموع للمراجعة!",
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = NotoSansArabicFamily,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (hasConversationTurns) {
                    OutlinedButton(
                        onClick = onSyncClick,
                        enabled = !isSyncing,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                    ) {
                        Icon(Icons.Filled.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("استخراج من محادثاتك", fontFamily = NotoSansArabicFamily)
                    }
                }
                Button(
                    onClick = onAddClick,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("أضف كلمة", fontFamily = NotoSansArabicFamily)
                }
            }
        }
    }
}

@Composable
private fun VocabCard(
    item: VocabItem,
    onSpeak: () -> Unit,
    onSpeakSlow: () -> Unit,
    onToggleMastered: () -> Unit,
    onDelete: () -> Unit
) {
    val resolvedPhonetics = remember(item.english, item.phoneticEnglish, item.phoneticArabic) {
        PronunciationHelper.resolvePhonetics(
            english = item.english,
            existingPhoneticEnglish = item.phoneticEnglish,
            existingPhoneticArabic = item.phoneticArabic
        )
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("vocab_list_card_${item.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onToggleMastered,
                    modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                ) {
                    Icon(
                        imageVector = if (item.mastered) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                        contentDescription = if (item.mastered) "Mastered" else "Mark mastered",
                        tint = if (item.mastered) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = item.english,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer
                        ) {
                            Text(
                                text = "×${item.usageCount.coerceAtLeast(1)}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier
                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                                    .testTag("vocab_frequency_${item.id}")
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                        ) {
                            Text(
                                text = formatCategoryArabic(item.category),
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = NotoSansArabicFamily,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = item.arabic,
                        style = MaterialTheme.typography.bodyLarge,
                        fontFamily = NotoSansArabicFamily,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (resolvedPhonetics.phoneticEnglish.isNotBlank()) {
                        Text(
                            text = resolvedPhonetics.phoneticEnglish,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.testTag("vocab_phonetic_en_${item.id}")
                        )
                    }
                    if (resolvedPhonetics.phoneticArabic.isNotBlank()) {
                        Text(
                            text = "النطق: ${resolvedPhonetics.phoneticArabic}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = NotoSansArabicFamily,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.testTag("vocab_phonetic_ar_${item.id}")
                        )
                    }
                }
                IconButton(
                    onClick = onSpeak,
                    modifier = Modifier
                        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                        .testTag("vocab_speak_${item.id}")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "Listen to ${item.english}",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(
                    onClick = onSpeakSlow,
                    modifier = Modifier
                        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                        .testTag("vocab_speak_slow_${item.id}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.SlowMotionVideo,
                        contentDescription = "Slow pronunciation of ${item.english}",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.DeleteOutline,
                        contentDescription = "Delete ${item.english}",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (item.exampleEnglish.isNotBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, top = 4.dp)
                ) {
                    Text(
                        text = "Example: ${item.exampleEnglish}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AddWordDialog(
    isTranslating: Boolean,
    onDismiss: () -> Unit,
    onSpeakPreview: (String) -> Unit,
    onAiTranslateSave: (String) -> Unit,
    onManualSave: (english: String, arabic: String, phonetic: String, example: String) -> Unit
) {
    var quickWord by remember { mutableStateOf("") }
    var englishInput by remember { mutableStateOf("") }
    var arabicInput by remember { mutableStateOf("") }
    var phoneticInput by remember { mutableStateOf("") }

    val previewPhonetics = remember(englishInput, phoneticInput) {
        if (englishInput.isBlank()) null else {
            PronunciationHelper.resolvePhonetics(
                english = englishInput,
                existingPhoneticArabic = phoneticInput
            )
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("إضافة كلمة جديدة للبطاقات", fontFamily = NotoSansArabicFamily)
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "اكتب كلمة أو جملة بالعربية أو الإنجليزية ليقوم الذكاء الاصطناعي بترجمتها وإضافة التهجئة الصوتية والنطق تلقائياً:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = NotoSansArabicFamily
                )
                OutlinedTextField(
                    value = quickWord,
                    onValueChange = { quickWord = it },
                    label = { Text("كلمة سريعة (عربي أو إنجليزي)", fontFamily = NotoSansArabicFamily) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_word_quick_input")
                )
                Button(
                    onClick = { onAiTranslateSave(quickWord) },
                    enabled = quickWord.isNotBlank() && !isTranslating,
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("add_word_ai_save_button")
                ) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("ترجمة ذكية وإضافة النطق", fontFamily = NotoSansArabicFamily)
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "أو أضف يدوياً (مع معاينة النطق الصوتي):",
                    style = MaterialTheme.typography.labelLarge,
                    fontFamily = NotoSansArabicFamily,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = englishInput,
                    onValueChange = { englishInput = it },
                    label = { Text("English") },
                    trailingIcon = {
                        if (englishInput.isNotBlank()) {
                            IconButton(onClick = { onSpeakPreview(englishInput) }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = "Preview audio pronunciation"
                                )
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_word_english_input")
                )
                OutlinedTextField(
                    value = arabicInput,
                    onValueChange = { arabicInput = it },
                    label = { Text("المعنى بالعربية", fontFamily = NotoSansArabicFamily) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_word_arabic_input")
                )
                OutlinedTextField(
                    value = phoneticInput,
                    onValueChange = { phoneticInput = it },
                    label = { Text("النطق بالحروف العربية (يُضاف تلقائياً إذا تُرك فارغاً)", fontFamily = NotoSansArabicFamily) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_word_phonetic_input")
                )
                if (previewPhonetics != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                            Text(
                                text = "معاينة النطق: ${previewPhonetics.phoneticEnglish}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "بالعربية: ${previewPhonetics.phoneticArabic}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontFamily = NotoSansArabicFamily,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onManualSave(englishInput, arabicInput, phoneticInput, "") },
                enabled = englishInput.isNotBlank() && arabicInput.isNotBlank(),
                modifier = Modifier.testTag("add_word_manual_save_button")
            ) {
                Text("حفظ في البطاقات", fontFamily = NotoSansArabicFamily)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء", fontFamily = NotoSansArabicFamily)
            }
        }
    )
}
