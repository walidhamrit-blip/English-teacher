package com.example.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.SlowMotionVideo
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.core.AndroidSpeechToTextManager
import com.example.core.SpeechToTextController
import com.example.core.SpeechToTextState
import com.example.core.createEnglishRecognizeSpeechIntent
import com.example.data.model.DailyProgressSummary
import com.example.data.model.Lesson
import com.example.data.model.StudyTimeSummary
import com.example.data.model.SuggestedPhrase
import com.example.data.model.TutorTurn
import com.example.data.model.calculateDailyProgress
import com.example.ui.components.CompactDailyProgressStrip
import com.example.ui.components.StudySessionCountdownBar
import com.example.ui.theme.NotoSansArabicFamily
import com.example.ui.viewmodel.AiTurnState
import com.example.ui.viewmodel.UiState
import kotlin.math.roundToInt

data class ChatMessage(
    val id: String,
    val text: String,
    val isFromUser: Boolean,
    val arabicTranslation: String = "",
    val feedbackArabic: String = "",
    val phoneticHint: String = ""
)

fun List<TutorTurn>.toChatMessages(): List<ChatMessage> = flatMap { turn ->
    listOf(
        ChatMessage(
            id = "${turn.id}_user",
            text = turn.userInput,
            isFromUser = true
        ),
        ChatMessage(
            id = "${turn.id}_ai",
            text = turn.englishReply,
            isFromUser = false,
            arabicTranslation = turn.arabicTranslation,
            feedbackArabic = turn.feedbackArabic,
            phoneticHint = turn.phoneticHint
        )
    )
}

@Composable
fun ChatScreen(
    turnsState: UiState<List<TutorTurn>>,
    aiState: AiTurnState,
    activeTopic: Lesson?,
    suggestions: List<SuggestedPhrase>,
    slowSpeech: Boolean,
    onToggleSlowSpeech: () -> Unit,
    onClearTopic: () -> Unit,
    onSendMessage: (String) -> Unit,
    onSpeakText: (String) -> Unit,
    onSaveWord: (SuggestedPhrase) -> Unit,
    onClearHistory: (() -> Unit)? = null,
    onSendSpokenMessage: ((String) -> Unit)? = null,
    onStopSpeaking: () -> Unit = {},
    dailyProgress: DailyProgressSummary? = null,
    onSelectDailyGoal: ((Int) -> Unit)? = null,
    studyTimeSummary: StudyTimeSummary? = null,
    onToggleStudyTimer: (() -> Unit)? = null,
    onSelectStudyGoalMinutes: ((Int) -> Unit)? = null
) {
    val allTurns = (turnsState as? UiState.Success)?.data.orEmpty()
    val writtenMessages = allTurns
        .filter { it.mode == "written" }
        .toChatMessages()

    val resolvedDailyProgress = dailyProgress ?: calculateDailyProgress(allTurns)

    val errorMsg = (aiState as? AiTurnState.Error)?.message
        ?: (turnsState as? UiState.Error)?.message

    ChatScreen(
        messages = writtenMessages,
        isSending = aiState is AiTurnState.Processing,
        isLoadingHistory = turnsState is UiState.Loading,
        errorMessage = errorMsg,
        activeTopic = activeTopic,
        suggestions = suggestions,
        slowSpeech = slowSpeech,
        onToggleSlowSpeech = onToggleSlowSpeech,
        onClearTopic = onClearTopic,
        onSendMessage = onSendMessage,
        onSpeakText = onSpeakText,
        onSaveWord = onSaveWord,
        onClearHistory = onClearHistory,
        onSendSpokenMessage = onSendSpokenMessage,
        onStopSpeaking = onStopSpeaking,
        dailyProgress = resolvedDailyProgress,
        onSelectDailyGoal = onSelectDailyGoal,
        studyTimeSummary = studyTimeSummary,
        onToggleStudyTimer = onToggleStudyTimer,
        onSelectStudyGoalMinutes = onSelectStudyGoalMinutes
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChatScreen(
    messages: List<ChatMessage>,
    isSending: Boolean,
    isLoadingHistory: Boolean = false,
    errorMessage: String? = null,
    activeTopic: Lesson? = null,
    suggestions: List<SuggestedPhrase> = emptyList(),
    slowSpeech: Boolean = false,
    onToggleSlowSpeech: (() -> Unit)? = null,
    onClearTopic: () -> Unit = {},
    onSendMessage: (String) -> Unit,
    onSpeakText: (String) -> Unit = {},
    onSaveWord: (SuggestedPhrase) -> Unit = {},
    onClearHistory: (() -> Unit)? = null,
    onSendSpokenMessage: ((String) -> Unit)? = null,
    onStopSpeaking: () -> Unit = {},
    speechController: SpeechToTextController? = null,
    hasAudioPermissionOverride: Boolean? = null,
    dailyProgress: DailyProgressSummary? = null,
    onSelectDailyGoal: ((Int) -> Unit)? = null,
    studyTimeSummary: StudyTimeSummary? = null,
    onToggleStudyTimer: (() -> Unit)? = null,
    onSelectStudyGoalMinutes: ((Int) -> Unit)? = null
) {
    val context = LocalContext.current
    val controller = remember(context, speechController) {
        speechController ?: AndroidSpeechToTextManager(context.applicationContext)
    }
    val speechState by controller.state.collectAsStateWithLifecycle()

    DisposableEffect(controller, speechController) {
        onDispose {
            if (speechController == null) {
                controller.destroy()
            }
        }
    }

    var hasMicPermission by remember(context, hasAudioPermissionOverride) {
        mutableStateOf(
            hasAudioPermissionOverride ?: (
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED
            )
        )
    }
    var permissionDeniedMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var inputText by rememberSaveable { mutableStateOf("") }
    var lastDictatedText by rememberSaveable { mutableStateOf<String?>(null) }
    var targetPracticePhrase by rememberSaveable { mutableStateOf<String?>(null) }
    var showClearDialog by rememberSaveable { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val micPrompt = stringResource(R.string.chat_mic_prompt_en)

    val speechIntentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS).orEmpty()
            val scores = result.data?.getFloatArrayExtra(RecognizerIntent.EXTRA_CONFIDENCE_SCORES)
            val best = matches.firstOrNull().orEmpty()
            val conf = scores?.firstOrNull()?.takeIf { it in 0f..1f }
            controller.applyExternalResult(best, conf)
        } else {
            controller.cancel()
        }
    }

    fun launchSpeechRecognition() {
        permissionDeniedMessage = null
        controller.clearResult()
        if (controller.isDirectRecognitionAvailable()) {
            controller.startListening("en-US")
        } else {
            val fallbackIntent = createEnglishRecognizeSpeechIntent(micPrompt)
            if (fallbackIntent.resolveActivity(context.packageManager) != null) {
                speechIntentLauncher.launch(fallbackIntent)
            } else {
                controller.reportError(
                    "خدمة التعرف الفوري على الكلام غير متوفرة على هذا الجهاز. يمكنك الكتابة أو استخدام الوضع الصوتي."
                )
            }
        }
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMicPermission = granted
        if (granted) {
            permissionDeniedMessage = null
            launchSpeechRecognition()
        } else {
            permissionDeniedMessage =
                "يحتاج التحدث في المحادثة إلى إذن الميكروفون للاستماع إلى نطقك بالإنجليزية وتقييمه."
        }
    }

    fun startPronunciationCapture() {
        onStopSpeaking()
        if (hasMicPermission) {
            launchSpeechRecognition()
        } else {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    fun submitCurrentInput() {
        val trimmed = inputText.trim()
        if (trimmed.isEmpty() || isSending) return
        val wasSpoken = lastDictatedText != null &&
            trimmed.equals(lastDictatedText?.trim(), ignoreCase = true)
        inputText = ""
        lastDictatedText = null
        targetPracticePhrase = null
        controller.clearResult()
        if (wasSpoken && onSendSpokenMessage != null) {
            onSendSpokenMessage(trimmed)
        } else {
            onSendMessage(trimmed)
        }
    }

    LaunchedEffect(speechState.recognizedText) {
        val recognized = speechState.recognizedText?.trim()
        if (!recognized.isNullOrEmpty()) {
            inputText = recognized
            lastDictatedText = recognized
        }
    }

    LaunchedEffect(messages.size, isSending) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val starterPrompts = listOf(
        "Hello! Teach me how to introduce myself",
        "Correct my sentence: I happy today",
        "Explain P vs B with easy examples",
        "كيف أطلب القهوة بالإنجليزية؟"
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp)
                .imePadding()
        ) {
            // Tutor header bar
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_tutor_noor),
                            contentDescription = "Teacher Noor avatar",
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Teacher Noor • Chat",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "اكتب بالإنجليزية أو العربية للتصحيح والشرح",
                                style = MaterialTheme.typography.bodyMedium,
                                fontFamily = NotoSansArabicFamily,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (onToggleSlowSpeech != null) {
                            FilterChip(
                                selected = slowSpeech,
                                onClick = onToggleSlowSpeech,
                                label = {
                                    Text(
                                        text = if (slowSpeech) "0.7x" else "1.0x",
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.SlowMotionVideo,
                                        contentDescription = "Toggle slow speech",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                        }
                        if (messages.isNotEmpty() && onClearHistory != null) {
                            IconButton(
                                onClick = { showClearDialog = true },
                                modifier = Modifier.testTag("chat_clear_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.DeleteSweep,
                                    contentDescription = "Clear chat history",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    if (activeTopic != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        AssistChip(
                            onClick = onClearTopic,
                            label = {
                                Text(
                                    text = "Resuming: ${activeTopic.titleEnglish} (${activeTopic.titleArabic})",
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

                    if (dailyProgress != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        CompactDailyProgressStrip(
                            summary = dailyProgress,
                            onSelectDailyGoal = onSelectDailyGoal
                        )
                    }

                    if (studyTimeSummary != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        StudySessionCountdownBar(
                            summary = studyTimeSummary,
                            onToggleTimer = { onToggleStudyTimer?.invoke() },
                            onSelectStudyGoalMinutes = { mins -> onSelectStudyGoalMinutes?.invoke(mins) }
                        )
                    }
                }
            }

            // Scrollable message list
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag("chat_message_list"),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isLoadingHistory && messages.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "جاري استرجاع سجل الدروس والمحادثة...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontFamily = NotoSansArabicFamily
                                )
                            }
                        }
                    }
                } else if (messages.isEmpty()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                            ),
                            shape = RoundedCornerShape(22.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Text(
                                    text = "Chat with Teacher Noor",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "أهلاً بك! اكتب أي جملة بالإنجليزية (حتى لو بها أخطاء) أو اسأل بالعربية، وسأرد عليك وأشرح القاعدة والنطق.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontFamily = NotoSansArabicFamily,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    starterPrompts.forEach { prompt ->
                                        AssistChip(
                                            onClick = { onSendMessage(prompt) },
                                            label = {
                                                Text(
                                                    text = prompt,
                                                    fontFamily = NotoSansArabicFamily
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                items(messages, key = { it.id }) { message ->
                    if (message.isFromUser) {
                        UserChatBubble(text = message.text)
                    } else {
                        AiChatBubble(
                            message = message,
                            onSpeak = { onSpeakText(message.text) }
                        )
                    }
                }

                if (suggestions.isNotEmpty() && !isSending) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "جمل مقترحة للرد أو الحفظ:",
                                style = MaterialTheme.typography.labelLarge,
                                fontFamily = NotoSansArabicFamily,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            suggestions.forEach { phrase ->
                                ChatSuggestionCard(
                                    phrase = phrase,
                                    onUse = { onSendMessage(phrase.english) },
                                    onSpeak = { onSpeakText(phrase.english) },
                                    onPracticePronunciation = {
                                        targetPracticePhrase = phrase.english
                                        startPronunciationCapture()
                                    },
                                    onSave = { onSaveWord(phrase) }
                                )
                            }
                        }
                    }
                }

                if (isSending) {
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier.testTag("chat_loading_indicator")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "المعلمة نور تكتب الرد والتصحيح...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontFamily = NotoSansArabicFamily
                                )
                            }
                        }
                    }
                }

                if (!errorMessage.isNullOrBlank()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer
                            )
                        ) {
                            Text(
                                text = errorMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    }
                }
            }

            // Speech-to-Text live listening, pronunciation feedback, & permission banners
            SpeechPracticeBannerArea(
                speechState = speechState,
                targetPhrase = targetPracticePhrase,
                permissionDeniedMessage = permissionDeniedMessage,
                onStopListening = { controller.stopListening() },
                onClearTargetPhrase = { targetPracticePhrase = null },
                onDismissSpeechResult = { controller.clearResult() },
                onRetryPermission = {
                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                },
                onOpenAppSettings = {
                    val settingsIntent = Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", context.packageName, null)
                    ).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(settingsIntent)
                },
                onDismissError = {
                    permissionDeniedMessage = null
                    controller.clearResult()
                }
            )

            // Text input field + Microphone Speech-to-Text + Send button
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = {
                            inputText = it
                            if (lastDictatedText != null && it.trim() != lastDictatedText?.trim()) {
                                lastDictatedText = null
                            }
                        },
                        placeholder = {
                            Text(
                                text = "Write or tap mic to speak English...",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        singleLine = false,
                        maxLines = 3,
                        shape = RoundedCornerShape(22.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            onSend = { submitCurrentInput() }
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_message_input")
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilledTonalIconButton(
                        onClick = {
                            if (speechState.isListening) {
                                controller.stopListening()
                            } else {
                                startPronunciationCapture()
                            }
                        },
                        enabled = !isSending,
                        colors = if (speechState.isListening) {
                            IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.tertiary,
                                contentColor = MaterialTheme.colorScheme.onTertiary
                            )
                        } else {
                            IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("chat_mic_button")
                    ) {
                        Icon(
                            imageVector = if (speechState.isListening) Icons.Filled.Stop else Icons.Filled.Mic,
                            contentDescription = if (speechState.isListening) {
                                stringResource(R.string.chat_mic_stop_desc)
                            } else {
                                stringResource(R.string.chat_mic_start_desc)
                            }
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    FilledIconButton(
                        onClick = { submitCurrentInput() },
                        enabled = inputText.isNotBlank() && !isSending,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("chat_send_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send message"
                        )
                    }
                }
            }
        }
    }

    if (showClearDialog && onClearHistory != null) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = {
                Text("مسح سجل المحادثة", fontFamily = NotoSansArabicFamily)
            },
            text = {
                Text(
                    "هل تريد حذف رسائل هذه المحادثة والبدء من جديد؟",
                    fontFamily = NotoSansArabicFamily
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearDialog = false
                        onClearHistory()
                    }
                ) {
                    Text("مسح", fontFamily = NotoSansArabicFamily)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("إلغاء", fontFamily = NotoSansArabicFamily)
                }
            }
        )
    }
}

@Composable
private fun UserChatBubble(text: String) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.CenterEnd
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primary,
            shape = RoundedCornerShape(
                topStart = 20.dp,
                topEnd = 20.dp,
                bottomStart = 20.dp,
                bottomEnd = 6.dp
            ),
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                fontFamily = NotoSansArabicFamily,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
        }
    }
}

@Composable
private fun AiChatBubble(
    message: ChatMessage,
    onSpeak: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(
            topStart = 6.dp,
            topEnd = 22.dp,
            bottomStart = 22.dp,
            bottomEnd = 22.dp
        ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onSpeak) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "Listen to teacher reply",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (message.arabicTranslation.isNotBlank()) {
                Text(
                    text = message.arabicTranslation,
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = NotoSansArabicFamily,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (message.phoneticHint.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "النطق: ${message.phoneticHint}",
                        style = MaterialTheme.typography.labelMedium,
                        fontFamily = NotoSansArabicFamily,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            if (message.feedbackArabic.isNotBlank()) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 10.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "ملاحظة المعلمة نور:",
                            style = MaterialTheme.typography.labelLarge,
                            fontFamily = NotoSansArabicFamily,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = message.feedbackArabic,
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = NotoSansArabicFamily,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SpeechPracticeBannerArea(
    speechState: SpeechToTextState,
    targetPhrase: String?,
    permissionDeniedMessage: String?,
    onStopListening: () -> Unit,
    onClearTargetPhrase: () -> Unit,
    onDismissSpeechResult: () -> Unit,
    onRetryPermission: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onDismissError: () -> Unit
) {
    val activeError = permissionDeniedMessage ?: speechState.errorMessage

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        AnimatedVisibility(visible = !targetPhrase.isNullOrBlank()) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("chat_target_phrase_banner")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "تدرّب على نطق هذه الجملة بالإنجليزية:",
                            style = MaterialTheme.typography.labelMedium,
                            fontFamily = NotoSansArabicFamily,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = targetPhrase.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    IconButton(onClick = onClearTargetPhrase) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Dismiss target phrase",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        AnimatedVisibility(visible = speechState.isListening) {
            val animatedRms by animateFloatAsState(
                targetValue = speechState.rmsLevel.coerceIn(0.1f, 1f),
                animationSpec = tween(90),
                label = "chatMicRms"
            )
            Surface(
                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.85f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("chat_speech_listening_banner")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.height(24.dp)
                    ) {
                        listOf(0.45f, 0.85f, 1.0f, 0.7f, 0.45f).forEach { mult ->
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .height((6f + animatedRms * 18f * mult).dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(MaterialTheme.colorScheme.tertiary)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "أستمع الآن... تحدث بالإنجليزية بوضوح",
                            style = MaterialTheme.typography.labelLarge,
                            fontFamily = NotoSansArabicFamily,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        if (speechState.partialText.isNotBlank()) {
                            Text(
                                text = "\"${speechState.partialText}\"",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                    TextButton(
                        onClick = onStopListening,
                        modifier = Modifier.testTag("chat_mic_stop_button")
                    ) {
                        Text("إيقاف", fontFamily = NotoSansArabicFamily)
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = !speechState.isListening && !speechState.recognizedText.isNullOrBlank()
        ) {
            val recognized = speechState.recognizedText.orEmpty()
            val matchScore = computePronunciationMatchPercent(targetPhrase, recognized)
            val confidencePct = speechState.confidence?.let { (it * 100f).roundToInt() }
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.75f),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("chat_speech_result_banner")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.RecordVoiceOver,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        val badge = when {
                            matchScore != null -> " • تطابق الجملة: $matchScore%"
                            confidencePct != null -> " • وضوح النطق: $confidencePct%"
                            else -> ""
                        }
                        Text(
                            text = "تم التقاط نطقك$badge",
                            style = MaterialTheme.typography.labelLarge,
                            fontFamily = NotoSansArabicFamily,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = "اضغط إرسال للحصول على تقييم المعلمة نور أو اضغط الميكروفون للإعادة.",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = NotoSansArabicFamily,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                    IconButton(onClick = onDismissSpeechResult) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Dismiss speech result",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        AnimatedVisibility(visible = !activeError.isNullOrBlank()) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("chat_speech_error_banner")
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = activeError.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = NotoSansArabicFamily,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = onDismissError) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Dismiss microphone notice",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                    if (permissionDeniedMessage != null) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = onRetryPermission) {
                                Text("منح الإذن", fontFamily = NotoSansArabicFamily)
                            }
                            TextButton(onClick = onOpenAppSettings) {
                                Text("إعدادات التطبيق", fontFamily = NotoSansArabicFamily)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun computePronunciationMatchPercent(target: String?, spoken: String): Int? {
    if (target.isNullOrBlank() || spoken.isBlank()) return null
    val cleanTarget = target.lowercase().replace(Regex("[^a-z0-9\\s]"), "").split(Regex("\\s+")).filter { it.isNotBlank() }
    val cleanSpoken = spoken.lowercase().replace(Regex("[^a-z0-9\\s]"), "").split(Regex("\\s+")).filter { it.isNotBlank() }.toSet()
    if (cleanTarget.isEmpty()) return null
    val hits = cleanTarget.count { cleanSpoken.contains(it) }
    return ((hits.toFloat() / cleanTarget.size) * 100f).roundToInt().coerceIn(0, 100)
}

@Composable
private fun ChatSuggestionCard(
    phrase: SuggestedPhrase,
    onUse: () -> Unit,
    onSpeak: () -> Unit,
    onPracticePronunciation: () -> Unit,
    onSave: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 6.dp)
            ) {
                Text(
                    text = phrase.english,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = phrase.arabic,
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = NotoSansArabicFamily,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onSpeak) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = "Listen to ${phrase.english}",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = onPracticePronunciation) {
                Icon(
                    imageVector = Icons.Filled.Mic,
                    contentDescription = "Practice pronouncing ${phrase.english}",
                    tint = MaterialTheme.colorScheme.tertiary
                )
            }
            IconButton(onClick = onSave) {
                Icon(
                    imageVector = Icons.Filled.BookmarkAdd,
                    contentDescription = "Save ${phrase.english}",
                    tint = MaterialTheme.colorScheme.secondary
                )
            }
            AssistChip(
                onClick = onUse,
                label = { Text("Reply", style = MaterialTheme.typography.labelMedium) }
            )
        }
    }
}
