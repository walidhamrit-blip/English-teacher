package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SlowMotionVideo
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.model.PronunciationHelper
import com.example.data.model.VocabItem
import com.example.ui.theme.NotoSansArabicFamily

enum class FlashcardDeckFilter(val labelArabic: String, val tag: String) {
    FREQUENT("الأكثر استخداماً", "flashcard_filter_frequent"),
    LEARNING("قيد المراجعة", "flashcard_filter_learning"),
    ALL("الكل", "flashcard_filter_all")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InteractiveFlashcardDeck(
    words: List<VocabItem>,
    onSpeakWord: (String) -> Unit,
    onSpeakWordSlow: (String) -> Unit = { onSpeakWord(it) },
    onStopSpeaking: () -> Unit = {},
    isSpeaking: Boolean = false,
    onGeneratePhonetics: ((VocabItem) -> Unit)? = null,
    onToggleMastered: (VocabItem) -> Unit,
    modifier: Modifier = Modifier
) {
    var deckFilter by remember { mutableStateOf(FlashcardDeckFilter.FREQUENT) }
    var shuffleSeed by remember { mutableIntStateOf(0) }
    var currentIndex by remember { mutableIntStateOf(0) }
    var isFlipped by remember { mutableStateOf(false) }

    val orderedIds = remember(
        words.map { it.id },
        deckFilter,
        shuffleSeed
    ) {
        val base = when (deckFilter) {
            FlashcardDeckFilter.FREQUENT -> words.sortedWith(
                compareByDescending<VocabItem> { it.usageCount }
                    .thenBy { it.mastered }
            )
            FlashcardDeckFilter.LEARNING -> words.filter { !it.mastered }
                .sortedByDescending { it.usageCount }
            FlashcardDeckFilter.ALL -> words
        }
        val finalOrder = if (shuffleSeed > 0) {
            base.shuffled(kotlin.random.Random(shuffleSeed))
        } else {
            base
        }
        finalOrder.map { it.id }
    }

    val wordById = remember(words) { words.associateBy { it.id } }
    val deckWords = remember(orderedIds, wordById) {
        orderedIds.mapNotNull { wordById[it] }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            FlashcardDeckFilter.entries.forEach { entry ->
                FilterChip(
                    selected = deckFilter == entry,
                    onClick = {
                        deckFilter = entry
                        currentIndex = 0
                        isFlipped = false
                    },
                    label = {
                        Text(entry.labelArabic, fontFamily = NotoSansArabicFamily)
                    },
                    modifier = Modifier.testTag(entry.tag)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (deckWords.isEmpty()) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (deckFilter == FlashcardDeckFilter.LEARNING) {
                            "أحسنت! أتقنت جميع البطاقات في هذه المجموعة"
                        } else {
                            "لا توجد بطاقات في هذه المجموعة حالياً"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = NotoSansArabicFamily,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    FilledTonalButton(
                        onClick = {
                            deckFilter = FlashcardDeckFilter.ALL
                            currentIndex = 0
                            isFlipped = false
                        },
                        modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                    ) {
                        Text("عرض كل البطاقات", fontFamily = NotoSansArabicFamily)
                    }
                }
            }
            return
        }

        val safeIndex = currentIndex.coerceIn(0, deckWords.lastIndex)
        val current = deckWords[safeIndex]
        val masteredInDeck = deckWords.count { it.mastered }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "بطاقة ${safeIndex + 1} من ${deckWords.size}",
                style = MaterialTheme.typography.labelLarge,
                fontFamily = NotoSansArabicFamily,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("flashcard_counter")
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                ) {
                    Text(
                        text = "متقن: $masteredInDeck/${deckWords.size}",
                        style = MaterialTheme.typography.labelMedium,
                        fontFamily = NotoSansArabicFamily,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                IconButton(
                    onClick = {
                        shuffleSeed += 1
                        currentIndex = 0
                        isFlipped = false
                    },
                    modifier = Modifier
                        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                        .testTag("flashcard_shuffle_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Shuffle,
                        contentDescription = "Shuffle flashcards",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        LinearProgressIndicator(
            progress = { (safeIndex + 1).toFloat() / deckWords.size.toFloat() },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(4.dp))
        )

        Spacer(modifier = Modifier.height(14.dp))

        val rotation by animateFloatAsState(
            targetValue = if (isFlipped) 180f else 0f,
            animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing),
            label = "flashcard_flip"
        )
        val density = LocalDensity.current.density

        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (rotation <= 90f) {
                    MaterialTheme.colorScheme.surface
                } else {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                }
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 250.dp)
                .graphicsLayer {
                    rotationY = rotation
                    cameraDistance = 12f * density
                }
                .clickable { isFlipped = !isFlipped }
                .semantics(mergeDescendants = false) {}
                .testTag("vocab_flashcard_card")
        ) {
            if (rotation <= 90f) {
                FlashcardFrontContent(
                    item = current,
                    isSpeaking = isSpeaking,
                    onSpeak = { onSpeakWord(current.english) },
                    onSpeakSlow = { onSpeakWordSlow(current.english) },
                    onStopSpeaking = onStopSpeaking,
                    onGeneratePhonetics = onGeneratePhonetics?.let { cb -> { cb(current) } }
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer { rotationY = 180f }
                ) {
                    FlashcardBackContent(
                        item = current,
                        onSpeak = { onSpeakWord(current.english) },
                        onSpeakSlow = { onSpeakWordSlow(current.english) },
                        onSpeakExample = {
                            if (current.exampleEnglish.isNotBlank()) {
                                onSpeakWordSlow(current.exampleEnglish)
                            }
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = {
                    isFlipped = false
                    currentIndex = if (safeIndex == 0) deckWords.lastIndex else safeIndex - 1
                },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag("flashcard_prev_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Previous card",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("السابق", fontFamily = NotoSansArabicFamily)
            }

            FilledTonalButton(
                onClick = { onToggleMastered(current) },
                shape = RoundedCornerShape(16.dp),
                colors = if (current.mastered) {
                    ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                } else {
                    ButtonDefaults.filledTonalButtonColors()
                },
                modifier = Modifier
                    .weight(1.25f)
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag("flashcard_mastered_button")
            ) {
                Icon(
                    imageVector = if (current.mastered) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (current.mastered) "متقنة ✓" else "أتقنتها",
                    fontFamily = NotoSansArabicFamily
                )
            }

            Button(
                onClick = {
                    isFlipped = false
                    currentIndex = (safeIndex + 1) % deckWords.size
                },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag("flashcard_next_button")
            ) {
                Text("التالي", fontFamily = NotoSansArabicFamily)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Next card",
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlashcardFrontContent(
    item: VocabItem,
    isSpeaking: Boolean,
    onSpeak: () -> Unit,
    onSpeakSlow: () -> Unit,
    onStopSpeaking: () -> Unit,
    onGeneratePhonetics: (() -> Unit)?
) {
    val resolvedPhonetics = remember(item.english, item.phoneticEnglish, item.phoneticArabic) {
        PronunciationHelper.resolvePhonetics(
            english = item.english,
            existingPhoneticEnglish = item.phoneticEnglish,
            existingPhoneticArabic = item.phoneticArabic
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 250.dp)
            .padding(horizontal = 20.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Text(
                    text = formatCategoryArabic(item.category),
                    style = MaterialTheme.typography.labelMedium,
                    fontFamily = NotoSansArabicFamily,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer
            ) {
                Text(
                    text = "استُخدمت ×${item.usageCount.coerceAtLeast(1)}",
                    style = MaterialTheme.typography.labelMedium,
                    fontFamily = NotoSansArabicFamily,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                        .testTag("flashcard_frequency_badge")
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = item.english,
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag("flashcard_english_word")
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Phonetic Spelling Guide Card on the Front for immediate pronunciation support
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("flashcard_phonetic_panel")
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (resolvedPhonetics.phoneticEnglish.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.RecordVoiceOver,
                            contentDescription = "Phonetic spelling",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = resolvedPhonetics.phoneticEnglish,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.testTag("flashcard_phonetic_english")
                        )
                    }
                }
                if (resolvedPhonetics.phoneticArabic.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "النطق بالعربية: ${resolvedPhonetics.phoneticArabic}",
                        style = MaterialTheme.typography.titleSmall,
                        fontFamily = NotoSansArabicFamily,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.testTag("flashcard_phonetic_arabic_front")
                    )
                }
                if ((item.phoneticEnglish.isBlank() || item.phoneticArabic.isBlank()) && onGeneratePhonetics != null) {
                    TextButton(
                        onClick = onGeneratePhonetics,
                        modifier = Modifier.defaultMinSize(minHeight = 36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "تحسين التهجئة الصوتية",
                            style = MaterialTheme.typography.labelMedium,
                            fontFamily = NotoSansArabicFamily
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Dual Audio Playback Buttons: Normal + Slow Clear Speed
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledTonalButton(
                onClick = onSpeak,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag("flashcard_speak_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = "Listen to ${item.english}",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("استمع", fontFamily = NotoSansArabicFamily, maxLines = 1)
            }

            OutlinedButton(
                onClick = onSpeakSlow,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag("flashcard_speak_slow_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.SlowMotionVideo,
                    contentDescription = "Slow pronunciation of ${item.english}",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("نطق بطيء", fontFamily = NotoSansArabicFamily, maxLines = 1)
            }

            if (isSpeaking) {
                FilledTonalButton(
                    onClick = onStopSpeaking,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    modifier = Modifier
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("flashcard_stop_audio_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Stop,
                        contentDescription = "Stop playback"
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Flip,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "اضغط على البطاقة لإظهار الترجمة العربية",
                style = MaterialTheme.typography.bodySmall,
                fontFamily = NotoSansArabicFamily,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun FlashcardBackContent(
    item: VocabItem,
    onSpeak: () -> Unit,
    onSpeakSlow: () -> Unit,
    onSpeakExample: () -> Unit
) {
    val resolvedPhonetics = remember(item.english, item.phoneticEnglish, item.phoneticArabic) {
        PronunciationHelper.resolvePhonetics(
            english = item.english,
            existingPhoneticEnglish = item.phoneticEnglish,
            existingPhoneticArabic = item.phoneticArabic
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 250.dp)
            .padding(horizontal = 20.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = item.english,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                IconButton(
                    onClick = onSpeak,
                    modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "Listen to ${item.english}",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(
                    onClick = onSpeakSlow,
                    modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.SlowMotionVideo,
                        contentDescription = "Slow pronunciation of ${item.english}",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            Text(
                text = item.arabic,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                fontFamily = NotoSansArabicFamily,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("flashcard_arabic_translation")
            )

            if (resolvedPhonetics.phoneticEnglish.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = resolvedPhonetics.phoneticEnglish,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            if (resolvedPhonetics.phoneticArabic.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "النطق: ${resolvedPhonetics.phoneticArabic}",
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = NotoSansArabicFamily,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("flashcard_phonetic")
                )
            }

            if (item.exampleEnglish.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "مثال للاستخدام:",
                                style = MaterialTheme.typography.labelMedium,
                                fontFamily = NotoSansArabicFamily,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = item.exampleEnglish,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        IconButton(
                            onClick = onSpeakExample,
                            modifier = Modifier
                                .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                .testTag("flashcard_speak_example_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.GraphicEq,
                                contentDescription = "Listen to example sentence",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Flip,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "اضغط للعودة إلى الكلمة الإنجليزية",
                style = MaterialTheme.typography.bodySmall,
                fontFamily = NotoSansArabicFamily,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

internal fun formatCategoryArabic(category: String): String {
    return when (category.trim().lowercase()) {
        "conversation" -> "من المحادثات"
        "written tutor" -> "المعلم الكتابي"
        "vocal tutor" -> "المعلم الصوتي"
        "greetings" -> "التحيات"
        "personal" -> "شخصي"
        else -> category.ifBlank { "عام" }
    }
}
