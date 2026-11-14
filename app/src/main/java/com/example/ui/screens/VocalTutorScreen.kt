package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.SlowMotionVideo
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.core.AudioRecorderManager
import com.example.data.model.Lesson
import com.example.data.model.StudyTimeSummary
import com.example.data.model.SuggestedPhrase
import com.example.data.model.TutorResponse
import com.example.data.model.TutorTurn
import com.example.ui.components.StudySessionCountdownBar
import com.example.ui.theme.NotoSansArabicFamily
import com.example.ui.viewmodel.AiTurnState
import com.example.ui.viewmodel.UiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VocalTutorScreen(
    turnsState: UiState<List<TutorTurn>>,
    aiState: AiTurnState,
    latestResponse: TutorResponse?,
    activeTopic: Lesson?,
    isSpeaking: Boolean,
    autoSpeakVocal: Boolean,
    slowSpeech: Boolean,
    onToggleAutoSpeak: () -> Unit,
    onToggleSlowSpeech: () -> Unit,
    onClearTopic: () -> Unit,
    onSendAudio: (ByteArray) -> Unit,
    onPracticeTextPrompt: (String) -> Unit,
    onSpeakText: (String) -> Unit,
    onStopSpeaking: () -> Unit,
    onSaveWord: (SuggestedPhrase) -> Unit,
    studyTimeSummary: StudyTimeSummary? = null,
    onToggleStudyTimer: (() -> Unit)? = null,
    onSelectStudyGoalMinutes: ((Int) -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val recorderManager = remember { AudioRecorderManager(context.applicationContext) }
    val isRecording by recorderManager.isRecording.collectAsStateWithLifecycle()
    val amplitude by recorderManager.amplitude.collectAsStateWithLifecycle()

    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    var micError by remember { mutableStateOf<String?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMicPermission = granted
        if (granted) {
            micError = null
            recorderManager.startRecording(scope).onFailure {
                micError = "تعذر تشغيل الميكروفون على هذا الجهاز."
            }
        } else {
            micError = "يحتاج الوضع الصوتي إلى إذن الميكروفون للاستماع إلى نطقك."
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            recorderManager.cancelRecording()
        }
    }

    val vocalTurns = (turnsState as? UiState.Success)?.data.orEmpty()
        .filter { it.mode == "vocal" }
        .reversed()

    val quickVoicePhrases = listOf(
        "Hello! My name is Sara.",
        "Please pass the pen.",
        "Can I have a cup of tea?",
        "How much is this?"
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                // Header & audio toggles
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_tutor_noor),
                                contentDescription = "Teacher Noor voice studio",
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Vocal Studio • الوضع الصوتي",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontFamily = NotoSansArabicFamily
                                )
                                Text(
                                    text = "تحدث بالإنجليزية أو العربية وسأحلل نطقك وأرد بصوت واضح",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontFamily = NotoSansArabicFamily,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (studyTimeSummary != null) {
                            StudySessionCountdownBar(
                                summary = studyTimeSummary,
                                onToggleTimer = { onToggleStudyTimer?.invoke() },
                                onSelectStudyGoalMinutes = { mins -> onSelectStudyGoalMinutes?.invoke(mins) }
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = autoSpeakVocal,
                                onClick = onToggleAutoSpeak,
                                label = {
                                    Text(
                                        text = if (autoSpeakVocal) "نطق تلقائي: مفعّل" else "نطق تلقائي: متوقف",
                                        fontFamily = NotoSansArabicFamily
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.RecordVoiceOver,
                                        contentDescription = "Auto-speak toggle",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                            FilterChip(
                                selected = slowSpeech,
                                onClick = onToggleSlowSpeech,
                                label = {
                                    Text(
                                        text = if (slowSpeech) "سرعة بطيئة 0.7x" else "سرعة طبيعية",
                                        fontFamily = NotoSansArabicFamily
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.SlowMotionVideo,
                                        contentDescription = "Slow speech toggle",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                            if (activeTopic != null) {
                                AssistChip(
                                    onClick = onClearTopic,
                                    label = {
                                        Text(
                                            text = activeTopic.titleArabic,
                                            fontFamily = NotoSansArabicFamily
                                        )
                                    },
                                    trailingIcon = {
                                        Icon(
                                            imageVector = Icons.Filled.Close,
                                            contentDescription = "Clear topic",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Interactive Microphone Orb Card
            item {
                VoiceCaptureStudioCard(
                    isRecording = isRecording,
                    isProcessing = aiState is AiTurnState.Processing,
                    isSpeaking = isSpeaking,
                    amplitude = amplitude,
                    hasMicPermission = hasMicPermission,
                    micError = micError,
                    onMicClick = {
                        micError = null
                        if (isSpeaking) onStopSpeaking()
                        if (!hasMicPermission) {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        } else if (!isRecording) {
                            recorderManager.startRecording(scope).onFailure {
                                micError = "تعذر بدء التسجيل الصوتي."
                            }
                        } else {
                            recorderManager.stopAndReadBytes()
                                .onSuccess { bytes -> onSendAudio(bytes) }
                                .onFailure {
                                    micError = "التسجيل قصير جداً، حاول التحدث لمدة ثانيتين على الأقل."
                                }
                        }
                    },
                    onCancelRecording = {
                        recorderManager.cancelRecording()
                    },
                    onStopSpeaking = onStopSpeaking
                )
            }

            if (aiState is AiTurnState.Error) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                        )
                    ) {
                        Text(
                            text = aiState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            }

            if (latestResponse != null) {
                item {
                    LatestVocalFeedbackCard(
                        response = latestResponse,
                        onSpeak = { onSpeakText(latestResponse.englishReply) },
                        onSpeakSuggested = { onSpeakText(it) },
                        onSaveWord = onSaveWord
                    )
                }
            }

            // Guided Quick Voice Drills (also works if mic unavailable on emulator)
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "جمل تدريبية للنطق والاستماع (اضغط للاستماع والتدريب):",
                            style = MaterialTheme.typography.labelLarge,
                            fontFamily = NotoSansArabicFamily
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            quickVoicePhrases.forEach { phrase ->
                                AssistChip(
                                    onClick = {
                                        onSpeakText(phrase)
                                        onPracticeTextPrompt(phrase)
                                    },
                                    enabled = aiState !is AiTurnState.Processing,
                                    label = { Text(phrase) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }

            if (vocalTurns.isNotEmpty()) {
                item {
                    Text(
                        text = "سجل التدريبات الصوتية السابقة",
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = NotoSansArabicFamily,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                items(vocalTurns.take(10), key = { it.id }) { turn ->
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(16.dp),
                        tonalElevation = 1.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "You: ${turn.userInput}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = turn.englishReply,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (turn.arabicTranslation.isNotBlank()) {
                                    Text(
                                        text = turn.arabicTranslation,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontFamily = NotoSansArabicFamily,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            IconButton(onClick = { onSpeakText(turn.englishReply) }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = "Replay teacher voice",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VoiceCaptureStudioCard(
    isRecording: Boolean,
    isProcessing: Boolean,
    isSpeaking: Boolean,
    amplitude: Float,
    hasMicPermission: Boolean,
    micError: String?,
    onMicClick: () -> Unit,
    onCancelRecording: () -> Unit,
    onStopSpeaking: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "micPulse")
    val idlePulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.07f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val liveScale by animateFloatAsState(
        targetValue = if (isRecording) 1f + (amplitude * 0.25f) else idlePulse,
        animationSpec = tween(90),
        label = "liveAmplitudeScale"
    )

    Card(
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isRecording) {
                MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.65f)
            } else {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
            }
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp, horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Live Waveform Bars
            Row(
                modifier = Modifier.height(36.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val multipliers = listOf(0.35f, 0.55f, 0.85f, 1.0f, 0.7f, 1.0f, 0.85f, 0.55f, 0.35f)
                multipliers.forEach { mult ->
                    val barHeight = if (isRecording) {
                        (8f + amplitude * 28f * mult).dp
                    } else if (isSpeaking) {
                        (10f + 14f * mult).dp
                    } else {
                        8.dp
                    }
                    Box(
                        modifier = Modifier
                            .width(6.dp)
                            .height(barHeight)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (isRecording) {
                                    MaterialTheme.colorScheme.tertiary
                                } else {
                                    MaterialTheme.colorScheme.primary
                                }
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(114.dp)
                        .scale(if (isProcessing) 1f else liveScale)
                        .clip(CircleShape)
                        .background(
                            if (isRecording) {
                                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.22f)
                            } else {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                            }
                        )
                )

                FloatingActionButton(
                    onClick = { if (!isProcessing) onMicClick() },
                    shape = CircleShape,
                    containerColor = if (isRecording) {
                        MaterialTheme.colorScheme.tertiary
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .size(84.dp)
                        .testTag("vocal_mic_button")
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(32.dp),
                            strokeWidth = 3.dp
                        )
                    } else {
                        Icon(
                            imageVector = if (isRecording) Icons.Filled.Stop else Icons.Filled.Mic,
                            contentDescription = if (isRecording) "Stop and send voice" else "Start speaking",
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = when {
                    isProcessing -> "جاري الاستماع وتحليل النطق..."
                    isRecording -> "أتحدّث الآن... اضغط للإرسال إلى المعلمة نور"
                    !hasMicPermission -> "اضغط لتفعيل الميكروفون وبدء التحدث"
                    else -> "اضغط للتحدث بالإنجليزية أو العربية"
                },
                style = MaterialTheme.typography.titleMedium,
                fontFamily = NotoSansArabicFamily,
                textAlign = TextAlign.Center
            )

            if (isRecording) {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onCancelRecording,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("إلغاء التسجيل", fontFamily = NotoSansArabicFamily)
                }
            }

            if (isSpeaking) {
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onStopSpeaking,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Filled.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("إيقاف الصوت", fontFamily = NotoSansArabicFamily)
                }
            }

            if (micError != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = micError,
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = NotoSansArabicFamily,
                    color = MaterialTheme.colorScheme.tertiary,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun LatestVocalFeedbackCard(
    response: TutorResponse,
    onSpeak: () -> Unit,
    onSpeakSuggested: (String) -> Unit,
    onSaveWord: (SuggestedPhrase) -> Unit
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (response.whatYouSaid.isNotBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "ما سمعته منك:",
                            style = MaterialTheme.typography.labelLarge,
                            fontFamily = NotoSansArabicFamily,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = response.whatYouSaid,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = response.englishReply,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (response.arabicTranslation.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = response.arabicTranslation,
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = NotoSansArabicFamily,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                IconButton(onClick = onSpeak) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "Replay teacher response",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (response.phoneticHint.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "طريقة النطق: ${response.phoneticHint}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = NotoSansArabicFamily,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (response.feedbackArabic.isNotBlank()) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 10.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                )
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.GraphicEq,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "تقييم النطق والقواعد:",
                                style = MaterialTheme.typography.labelLarge,
                                fontFamily = NotoSansArabicFamily,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = response.feedbackArabic,
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = NotoSansArabicFamily,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            if (response.suggestedPhrases.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "كرّر بعدي واحفظ:",
                    style = MaterialTheme.typography.labelLarge,
                    fontFamily = NotoSansArabicFamily
                )
                Spacer(modifier = Modifier.height(6.dp))
                response.suggestedPhrases.forEach { phrase ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(phrase.english, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                            Text(
                                "${phrase.arabic} (${phrase.phoneticArabic})",
                                style = MaterialTheme.typography.bodyMedium,
                                fontFamily = NotoSansArabicFamily,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { onSpeakSuggested(phrase.english) }) {
                            Icon(
                                Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Listen to ${phrase.english}",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(onClick = { onSaveWord(phrase) }) {
                            Icon(
                                Icons.Filled.BookmarkAdd,
                                contentDescription = "Save ${phrase.english}",
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }
        }
    }
}
