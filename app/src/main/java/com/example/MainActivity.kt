package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.core.AndroidPracticeReminderScheduler
import com.example.core.SpeechManager
import com.example.core.configureAppCheck
import com.example.data.repository.AiTutorRepository
import com.example.data.repository.LearningRepository
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.authStateFlow
import com.example.ui.auth.signOut
import com.example.ui.components.DailyQuizDialog
import com.example.ui.components.HeaderStreakBadge
import com.example.ui.components.ReminderSettingsDialog
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.VocalTutorScreen
import com.example.ui.screens.WordbookScreen
import com.example.ui.theme.NoorEnglishTheme
import com.example.ui.theme.NotoSansArabicFamily
import com.example.ui.viewmodel.LearningViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

private enum class AppDestination(
    val route: String,
    val label: String,
    val arabicLabel: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOME(
        route = "home",
        label = "Lessons",
        arabicLabel = "الدروس",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home
    ),
    WRITTEN(
        route = "written",
        label = "Written",
        arabicLabel = "كتابي",
        selectedIcon = Icons.Filled.EditNote,
        unselectedIcon = Icons.Outlined.EditNote
    ),
    VOCAL(
        route = "vocal",
        label = "Vocal",
        arabicLabel = "صوتي",
        selectedIcon = Icons.Filled.GraphicEq,
        unselectedIcon = Icons.Outlined.GraphicEq
    ),
    WORDBOOK(
        route = "wordbook",
        label = "Vocabulary",
        arabicLabel = "المفردات",
        selectedIcon = Icons.AutoMirrored.Filled.MenuBook,
        unselectedIcon = Icons.AutoMirrored.Outlined.MenuBook
    )
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configure Firebase App Check before Compose UI, ViewModels, or any Firebase operation
        configureAppCheck(this, intent)

        enableEdgeToEdge()
        setContent {
            NoorEnglishTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}

@Composable
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
    val currentUser by auth.authStateFlow()
        .collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        AuthScreen(onAuthSuccess = {})
    } else {
        key(user.uid) {
            MainAppShell(
                currentUserId = user.uid,
                displayName = user.displayName?.takeIf { it.isNotBlank() } ?: "Learner"
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainAppShell(
    currentUserId: String,
    displayName: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    val viewModel: LearningViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                LearningViewModel(
                    learningRepository = LearningRepository(db),
                    aiTutorRepository = AiTutorRepository(),
                    speechManager = SpeechManager(app),
                    currentUserId = currentUserId,
                    displayName = displayName,
                    reminderScheduler = AndroidPracticeReminderScheduler(app)
                )
            }
        }
    )

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: AppDestination.HOME.route

    val progressState by viewModel.progressState.collectAsStateWithLifecycle()
    val vocabState by viewModel.vocabState.collectAsStateWithLifecycle()
    val tutorTurnsState by viewModel.tutorTurnsState.collectAsStateWithLifecycle()
    val selectedLesson by viewModel.selectedLesson.collectAsStateWithLifecycle()
    val activeTopic by viewModel.activeLessonTopic.collectAsStateWithLifecycle()
    val writtenState by viewModel.writtenState.collectAsStateWithLifecycle()
    val writtenSuggestions by viewModel.writtenSuggestions.collectAsStateWithLifecycle()
    val vocalState by viewModel.vocalState.collectAsStateWithLifecycle()
    val latestVocalResponse by viewModel.latestVocalResponse.collectAsStateWithLifecycle()
    val isSpeaking by viewModel.isSpeaking.collectAsStateWithLifecycle()
    val slowSpeech by viewModel.slowSpeech.collectAsStateWithLifecycle()
    val autoSpeakVocal by viewModel.autoSpeakVocal.collectAsStateWithLifecycle()
    val isTranslatingWord by viewModel.isTranslatingWord.collectAsStateWithLifecycle()
    val isSyncingConversationVocab by viewModel.isSyncingConversationVocab.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()
    val dailyProgress by viewModel.dailyProgress.collectAsStateWithLifecycle()
    val studyTimeSummary by viewModel.studyTimeSummary.collectAsStateWithLifecycle()
    val reminderSettings by viewModel.reminderSettings.collectAsStateWithLifecycle()
    val dailyQuizStatus by viewModel.dailyQuizStatus.collectAsStateWithLifecycle()
    val dailyQuizQuestions by viewModel.dailyQuizQuestions.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var showSignOutDialog by remember { mutableStateOf(false) }
    var showReminderDialog by remember { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                viewModel.pauseStudySession(manual = false)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(currentRoute, selectedLesson) {
        val inStudySession = currentRoute == AppDestination.WRITTEN.route ||
            currentRoute == AppDestination.VOCAL.route ||
            currentRoute == AppDestination.WORDBOOK.route ||
            selectedLesson != null
        viewModel.onStudyScreenActiveChanged(inStudySession)
    }

    LaunchedEffect(statusMessage) {
        statusMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusMessage()
        }
    }

    fun navigateTo(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Row {
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.titleLarge
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "• نور",
                                style = MaterialTheme.typography.titleLarge,
                                fontFamily = NotoSansArabicFamily,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    ),
                    actions = {
                        HeaderStreakBadge(
                            summary = dailyProgress,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        IconButton(
                            onClick = { showReminderDialog = true },
                            modifier = Modifier.testTag("header_reminder_button")
                        ) {
                            Icon(
                                imageVector = if (reminderSettings.enabled) {
                                    Icons.Filled.NotificationsActive
                                } else {
                                    Icons.Filled.NotificationsNone
                                },
                                contentDescription = "Daily practice reminder settings",
                                tint = if (reminderSettings.enabled) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                        IconButton(
                            onClick = { showSignOutDialog = true },
                            modifier = Modifier.testTag("sign_out_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Logout,
                                contentDescription = stringResource(R.string.sign_out_desc)
                            )
                        }
                    }
                )
            },
            bottomBar = {
                if (!useRail) {
                    NavigationBar {
                        AppDestination.entries.forEach { dest ->
                            val selected = currentRoute == dest.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTo(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = dest.label
                                    )
                                },
                                label = {
                                    Text(
                                        text = dest.arabicLabel,
                                        fontFamily = NotoSansArabicFamily
                                    )
                                },
                                modifier = Modifier.testTag("nav_${dest.route}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail {
                        AppDestination.entries.forEach { dest ->
                            val selected = currentRoute == dest.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTo(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = dest.label
                                    )
                                },
                                label = {
                                    Text(
                                        text = dest.arabicLabel,
                                        fontFamily = NotoSansArabicFamily
                                    )
                                },
                                modifier = Modifier.testTag("rail_${dest.route}")
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    NavHost(
                        navController = navController,
                        startDestination = AppDestination.HOME.route
                    ) {
                        composable(AppDestination.HOME.route) {
                            HomeScreen(
                                displayName = displayName,
                                lessons = viewModel.lessons,
                                progressState = progressState,
                                vocabState = vocabState,
                                selectedLesson = selectedLesson,
                                slowSpeech = slowSpeech,
                                onToggleSlowSpeech = viewModel::toggleSlowSpeech,
                                onSelectLesson = viewModel::selectLesson,
                                onCompleteLesson = viewModel::completeLesson,
                                onSpeakPhrase = viewModel::speakEnglish,
                                onSavePhrase = { en, ar, ph, cat ->
                                    viewModel.saveVocabWord(en, ar, ph, "", cat)
                                },
                                onOpenWrittenMode = { lesson ->
                                    if (lesson != null) viewModel.setActiveTopic(lesson)
                                    navigateTo(AppDestination.WRITTEN.route)
                                },
                                onOpenVocalMode = { lesson ->
                                    if (lesson != null) viewModel.setActiveTopic(lesson)
                                    navigateTo(AppDestination.VOCAL.route)
                                },
                                dailyProgress = dailyProgress,
                                onSelectDailyGoal = viewModel::updateDailyGoal,
                                studyTimeSummary = studyTimeSummary,
                                onSelectStudyGoalMinutes = viewModel::updateDailyStudyGoalMinutes,
                                onToggleStudyTimer = viewModel::toggleStudyTimer,
                                reminderSettings = reminderSettings,
                                onUpdateReminderSettings = viewModel::updateReminderSettings,
                                onSendTestReminder = viewModel::sendTestPracticeReminder,
                                dailyQuizStatus = dailyQuizStatus,
                                onStartDailyQuiz = viewModel::startDailyQuiz
                            )
                        }

                        composable(AppDestination.WRITTEN.route) {
                            ChatScreen(
                                turnsState = tutorTurnsState,
                                aiState = writtenState,
                                activeTopic = activeTopic,
                                suggestions = writtenSuggestions,
                                slowSpeech = slowSpeech,
                                onToggleSlowSpeech = viewModel::toggleSlowSpeech,
                                onClearTopic = { viewModel.setActiveTopic(null) },
                                onSendMessage = viewModel::sendWrittenMessage,
                                onSpeakText = viewModel::speakEnglish,
                                onSaveWord = { phrase ->
                                    viewModel.saveVocabWord(
                                        english = phrase.english,
                                        arabic = phrase.arabic,
                                        phoneticArabic = phrase.phoneticArabic,
                                        exampleEnglish = phrase.exampleEnglish,
                                        category = "Written Tutor",
                                        phoneticEnglish = phrase.phoneticEnglish
                                    )
                                },
                                onClearHistory = { viewModel.clearChatHistory("written") },
                                onSendSpokenMessage = { spoken ->
                                    viewModel.sendWrittenMessage(spoken, isSpokenPractice = true)
                                },
                                onStopSpeaking = viewModel::stopSpeaking,
                                dailyProgress = dailyProgress,
                                onSelectDailyGoal = viewModel::updateDailyGoal,
                                studyTimeSummary = studyTimeSummary,
                                onToggleStudyTimer = viewModel::toggleStudyTimer,
                                onSelectStudyGoalMinutes = viewModel::updateDailyStudyGoalMinutes
                            )
                        }

                        composable(AppDestination.VOCAL.route) {
                            VocalTutorScreen(
                                turnsState = tutorTurnsState,
                                aiState = vocalState,
                                latestResponse = latestVocalResponse,
                                activeTopic = activeTopic,
                                isSpeaking = isSpeaking,
                                autoSpeakVocal = autoSpeakVocal,
                                slowSpeech = slowSpeech,
                                onToggleAutoSpeak = viewModel::toggleAutoSpeakVocal,
                                onToggleSlowSpeech = viewModel::toggleSlowSpeech,
                                onClearTopic = { viewModel.setActiveTopic(null) },
                                onSendAudio = viewModel::sendVocalAudio,
                                onPracticeTextPrompt = viewModel::sendVocalTextPractice,
                                onSpeakText = viewModel::speakEnglish,
                                onStopSpeaking = viewModel::stopSpeaking,
                                onSaveWord = { phrase ->
                                    viewModel.saveVocabWord(
                                        english = phrase.english,
                                        arabic = phrase.arabic,
                                        phoneticArabic = phrase.phoneticArabic,
                                        exampleEnglish = phrase.exampleEnglish,
                                        category = "Vocal Tutor",
                                        phoneticEnglish = phrase.phoneticEnglish
                                    )
                                },
                                studyTimeSummary = studyTimeSummary,
                                onToggleStudyTimer = viewModel::toggleStudyTimer,
                                onSelectStudyGoalMinutes = viewModel::updateDailyStudyGoalMinutes
                            )
                        }

                        composable(AppDestination.WORDBOOK.route) {
                            val hasTurns = (tutorTurnsState as? com.example.ui.viewmodel.UiState.Success)
                                ?.data?.isNotEmpty() == true
                            WordbookScreen(
                                vocabState = vocabState,
                                isTranslatingWord = isTranslatingWord,
                                isSyncingConversationVocab = isSyncingConversationVocab,
                                hasConversationTurns = hasTurns,
                                isSpeaking = isSpeaking,
                                onSyncConversationVocab = { viewModel.syncConversationVocabulary(showFeedback = true) },
                                onSpeakWord = { word -> viewModel.speakEnglish(word) },
                                onSpeakWordSlow = { word -> viewModel.speakEnglish(word, slow = true) },
                                onStopSpeaking = viewModel::stopSpeaking,
                                onGeneratePhonetics = viewModel::generatePhoneticsForWord,
                                onToggleMastered = viewModel::toggleWordMastered,
                                onDeleteWord = viewModel::deleteWord,
                                onTranslateAndSave = { input -> viewModel.translateAndSaveWord(input) },
                                onAddManualWord = { en, ar, ph, ex ->
                                    viewModel.saveVocabWord(en, ar, ph, ex, "Personal")
                                },
                                studyTimeSummary = studyTimeSummary,
                                onToggleStudyTimer = viewModel::toggleStudyTimer,
                                onSelectStudyGoalMinutes = viewModel::updateDailyStudyGoalMinutes,
                                dailyQuizStatus = dailyQuizStatus,
                                onStartDailyQuiz = viewModel::startDailyQuiz
                            )
                        }
                    }
                }
            }
        }
    }

    if (dailyQuizQuestions.isNotEmpty()) {
        DailyQuizDialog(
            questions = dailyQuizQuestions,
            onSpeakEnglish = { word -> viewModel.speakEnglish(word) },
            onQuizCompleted = viewModel::completeDailyQuiz,
            onRetakeQuiz = viewModel::startDailyQuiz,
            onDismiss = viewModel::dismissDailyQuiz
        )
    }

    if (showReminderDialog) {
        ReminderSettingsDialog(
            settings = reminderSettings,
            onDismiss = { showReminderDialog = false },
            onUpdateSettings = viewModel::updateReminderSettings,
            onSendTestReminder = viewModel::sendTestPracticeReminder
        )
    }

    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text("تسجيل الخروج", fontFamily = NotoSansArabicFamily) },
            text = {
                Text(
                    "هل تريد تسجيل الخروج من حسابك؟ ستبقى كلماتك وتقدمك محفوظة في السحابة.",
                    fontFamily = NotoSansArabicFamily
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSignOutDialog = false
                        signOut(credentialManager = credentialManager, scope = scope)
                    }
                ) {
                    Text("خروج", fontFamily = NotoSansArabicFamily)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) {
                    Text("بقاء", fontFamily = NotoSansArabicFamily)
                }
            }
        )
    }
}
