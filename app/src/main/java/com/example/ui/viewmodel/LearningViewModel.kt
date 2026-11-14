package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.PracticeReminderScheduler
import com.example.core.SpeechManager
import com.example.data.curriculum.BeginnerCurriculum
import com.example.data.model.ConversationVocabularyExtractor
import com.example.data.model.DailyProgressSummary
import com.example.data.model.DailyQuizGenerator
import com.example.data.model.DailyQuizQuestion
import com.example.data.model.DailyQuizStatus
import com.example.data.model.Lesson
import com.example.data.model.ReminderSettings
import com.example.data.model.StudyTimeSummary
import com.example.data.model.SuggestedPhrase
import com.example.data.model.TutorResponse
import com.example.data.model.TutorTurn
import com.example.data.model.UserProgress
import com.example.data.model.VocabItem
import com.example.data.model.calculateDailyProgress
import com.example.data.model.formatDayDateKey
import com.example.data.model.resolveTodayStudySeconds
import com.example.data.repository.AiTutorRepository
import com.example.data.repository.LearningRepository
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.content
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

sealed interface AiTurnState {
    data object Idle : AiTurnState
    data object Processing : AiTurnState
    data class Error(val message: String) : AiTurnState
}

class LearningViewModel(
    private val learningRepository: LearningRepository,
    private val aiTutorRepository: AiTutorRepository,
    private val speechManager: SpeechManager,
    private val currentUserId: String,
    private val displayName: String,
    private val reminderScheduler: PracticeReminderScheduler? = null
) : ViewModel() {

    val lessons: List<Lesson> = BeginnerCurriculum.lessons

    val isSpeaking: StateFlow<Boolean> = speechManager.isSpeaking

    val progressState: StateFlow<UiState<UserProgress?>> =
        learningRepository.observeUserProgress(currentUserId)
            .map<UserProgress?, UiState<UserProgress?>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing user progress", rootError)
                emit(UiState.Error(rootError.message ?: "Failed to load progress"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val vocabState: StateFlow<UiState<List<VocabItem>>> =
        learningRepository.observeVocab(currentUserId)
            .map<List<VocabItem>, UiState<List<VocabItem>>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing vocab", rootError)
                emit(UiState.Error(rootError.message ?: "Failed to load vocabulary"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val tutorTurnsState: StateFlow<UiState<List<TutorTurn>>> =
        learningRepository.observeTutorTurns(currentUserId)
            .map<List<TutorTurn>, UiState<List<TutorTurn>>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing tutor turns", rootError)
                emit(UiState.Error(rootError.message ?: "Failed to load tutor history"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val dailyProgress: StateFlow<DailyProgressSummary> =
        combine(tutorTurnsState, progressState) { turnsState, progress ->
            val turns = (turnsState as? UiState.Success)?.data.orEmpty()
            val userProgress = (progress as? UiState.Success)?.data
            val savedGoal = (userProgress?.dailyGoal
                ?: DailyProgressSummary.DEFAULT_DAILY_MESSAGE_GOAL).coerceIn(1, 100)
            calculateDailyProgress(
                turns = turns,
                dailyGoal = savedGoal,
                persistedStreakDays = userProgress?.currentStreak ?: 0,
                persistedLongestStreakDays = userProgress?.longestStreak ?: 0,
                persistedLastActiveDate = userProgress?.lastActiveDate.orEmpty()
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = DailyProgressSummary()
        )

    private val _selectedLesson = MutableStateFlow<Lesson?>(null)
    val selectedLesson: StateFlow<Lesson?> = _selectedLesson.asStateFlow()

    private val _activeLessonTopic = MutableStateFlow<Lesson?>(null)
    val activeLessonTopic: StateFlow<Lesson?> = _activeLessonTopic.asStateFlow()

    private val _writtenState = MutableStateFlow<AiTurnState>(AiTurnState.Idle)
    val writtenState: StateFlow<AiTurnState> = _writtenState.asStateFlow()

    private val _writtenSuggestions = MutableStateFlow<List<SuggestedPhrase>>(emptyList())
    val writtenSuggestions: StateFlow<List<SuggestedPhrase>> = _writtenSuggestions.asStateFlow()

    private val _vocalState = MutableStateFlow<AiTurnState>(AiTurnState.Idle)
    val vocalState: StateFlow<AiTurnState> = _vocalState.asStateFlow()

    private val _latestVocalResponse = MutableStateFlow<TutorResponse?>(null)
    val latestVocalResponse: StateFlow<TutorResponse?> = _latestVocalResponse.asStateFlow()

    private val _slowSpeech = MutableStateFlow(false)
    val slowSpeech: StateFlow<Boolean> = _slowSpeech.asStateFlow()

    private val _autoSpeakVocal = MutableStateFlow(true)
    val autoSpeakVocal: StateFlow<Boolean> = _autoSpeakVocal.asStateFlow()

    private val _isTranslatingWord = MutableStateFlow(false)
    val isTranslatingWord: StateFlow<Boolean> = _isTranslatingWord.asStateFlow()

    private val _isSyncingConversationVocab = MutableStateFlow(false)
    val isSyncingConversationVocab: StateFlow<Boolean> = _isSyncingConversationVocab.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _elapsedStudySecondsToday = MutableStateFlow(0)
    private val _isStudyTimerRunning = MutableStateFlow(false)
    private val _isStudyTimerPaused = MutableStateFlow(false)

    val studyTimeSummary: StateFlow<StudyTimeSummary> = combine(
        progressState,
        _elapsedStudySecondsToday,
        _isStudyTimerRunning,
        _isStudyTimerPaused
    ) { progress, elapsed, running, paused ->
        val goalMinutes = ((progress as? UiState.Success)?.data?.dailyStudyMinutesGoal
            ?: StudyTimeSummary.DEFAULT_STUDY_GOAL_MINUTES)
            .coerceIn(StudyTimeSummary.MIN_STUDY_GOAL_MINUTES, StudyTimeSummary.MAX_STUDY_GOAL_MINUTES)
        StudyTimeSummary(
            goalMinutes = goalMinutes,
            elapsedSecondsToday = elapsed,
            isRunning = running,
            isPaused = paused
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = StudyTimeSummary()
    )

    val reminderSettings: StateFlow<ReminderSettings> = progressState
        .map { state ->
            val data = (state as? UiState.Success)?.data
            ReminderSettings(
                enabled = data?.reminderEnabled ?: false,
                hour = (data?.reminderHour ?: ReminderSettings.DEFAULT_REMINDER_HOUR).coerceIn(0, 23),
                minute = (data?.reminderMinute ?: ReminderSettings.DEFAULT_REMINDER_MINUTE).coerceIn(0, 59)
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = ReminderSettings()
        )

    val dailyQuizStatus: StateFlow<DailyQuizStatus> = progressState
        .map { state ->
            val data = (state as? UiState.Success)?.data
            val todayKey = formatDayDateKey(System.currentTimeMillis())
            val completedToday = data?.lastDailyQuizDate == todayKey
            DailyQuizStatus(
                completedToday = completedToday,
                todayScore = if (completedToday) {
                    data?.lastDailyQuizScore?.coerceIn(0, DailyQuizGenerator.QUIZ_QUESTION_COUNT) ?: 0
                } else {
                    0
                },
                totalQuestions = DailyQuizGenerator.QUIZ_QUESTION_COUNT
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = DailyQuizStatus()
        )

    private val _dailyQuizQuestions = MutableStateFlow<List<DailyQuizQuestion>>(emptyList())
    val dailyQuizQuestions: StateFlow<List<DailyQuizQuestion>> = _dailyQuizQuestions.asStateFlow()

    private var restoredLastLesson = false
    private var autoSyncedConversationVocab = false
    private var hydratedStudyProgress = false
    private var lastSyncedReminderSettings: ReminderSettings? = null
    private var lastRecordedUsageDate: String? = null
    private var currentStudyDateKey: String = formatDayDateKey(System.currentTimeMillis())
    private var studyGoalCelebratedForDate: String? = null
    private var studyTickJob: Job? = null
    private var userManuallyPausedStudyTimer = false

    init {
        viewModelScope.launch {
            learningRepository.ensureUserProgress(displayName)
            recordDailyUsageIfNeeded()
        }
        viewModelScope.launch {
            progressState.collect { state ->
                if (state is UiState.Success) {
                    val data = state.data
                    if (!restoredLastLesson) {
                        val savedId = data?.lastLessonId.orEmpty()
                        if (savedId.isNotBlank() && _activeLessonTopic.value == null) {
                            _activeLessonTopic.value = lessons.firstOrNull { it.id == savedId }
                        }
                        restoredLastLesson = true
                    }
                    if (!hydratedStudyProgress && data != null) {
                        val todayKey = formatDayDateKey(System.currentTimeMillis())
                        currentStudyDateKey = todayKey
                        val restoredSeconds = resolveTodayStudySeconds(
                            storedSeconds = data.todayStudySeconds,
                            lastStudyDate = data.lastStudyDate,
                            todayDate = todayKey
                        )
                        _elapsedStudySecondsToday.value = maxOf(_elapsedStudySecondsToday.value, restoredSeconds)
                        val goalSecs = data.dailyStudyMinutesGoal
                            .coerceIn(StudyTimeSummary.MIN_STUDY_GOAL_MINUTES, StudyTimeSummary.MAX_STUDY_GOAL_MINUTES) * 60
                        if (restoredSeconds >= goalSecs) {
                            studyGoalCelebratedForDate = todayKey
                        }
                        hydratedStudyProgress = true
                    }
                    if (data != null) {
                        val synced = ReminderSettings(
                            enabled = data.reminderEnabled,
                            hour = data.reminderHour.coerceIn(0, 23),
                            minute = data.reminderMinute.coerceIn(0, 59)
                        )
                        if (lastSyncedReminderSettings != synced) {
                            lastSyncedReminderSettings = synced
                            reminderScheduler?.syncSchedule(synced)
                        }
                    }
                }
            }
        }
        viewModelScope.launch {
            combine(vocabState, tutorTurnsState) { vocab, turns -> vocab to turns }
                .collect { (vocab, turns) ->
                    if (!autoSyncedConversationVocab &&
                        vocab is UiState.Success &&
                        turns is UiState.Success
                    ) {
                        autoSyncedConversationVocab = true
                        val hasConversationWords = vocab.data.any {
                            it.category.equals("Conversation", ignoreCase = true)
                        }
                        if (!hasConversationWords && turns.data.isNotEmpty()) {
                            syncConversationVocabulary(showFeedback = false)
                        }
                    }
                }
        }
    }

    private suspend fun recordDailyUsageIfNeeded() {
        val today = formatDayDateKey(System.currentTimeMillis())
        if (lastRecordedUsageDate != today) {
            learningRepository.recordDailyUsage(displayName)
                .onSuccess { lastRecordedUsageDate = today }
        }
    }

    fun selectLesson(lesson: Lesson?) {
        _selectedLesson.value = lesson
    }

    fun setActiveTopic(lesson: Lesson?) {
        _activeLessonTopic.value = lesson
        viewModelScope.launch {
            learningRepository.updateLastLesson(lesson?.id.orEmpty(), displayName)
        }
    }

    fun updateDailyGoal(goal: Int) {
        val cleanGoal = goal.coerceIn(1, 100)
        viewModelScope.launch {
            learningRepository.updateDailyGoal(cleanGoal, displayName)
                .onSuccess {
                    _statusMessage.value = "تم تحديد هدفك اليومي: $cleanGoal رسائل"
                }
                .onFailure { err ->
                    _statusMessage.value = err.localizedMessage ?: "تعذر حفظ الهدف اليومي"
                }
        }
    }

    fun updateReminderSettings(enabled: Boolean, hour: Int, minute: Int) {
        val cleanHour = hour.coerceIn(0, 23)
        val cleanMinute = minute.coerceIn(0, 59)
        val nextSettings = ReminderSettings(
            enabled = enabled,
            hour = cleanHour,
            minute = cleanMinute
        )
        lastSyncedReminderSettings = nextSettings
        reminderScheduler?.syncSchedule(nextSettings)
        viewModelScope.launch {
            learningRepository.updateReminderSettings(
                enabled = enabled,
                hour = cleanHour,
                minute = cleanMinute,
                displayName = displayName
            ).onSuccess {
                _statusMessage.value = if (enabled) {
                    "تم تفعيل التذكير اليومي الساعة ${nextSettings.formattedTimeArabic}"
                } else {
                    "تم إيقاف التذكير اليومي"
                }
            }.onFailure { err ->
                _statusMessage.value = err.localizedMessage ?: "تعذر حفظ إعدادات التذكير"
            }
        }
    }

    fun sendTestPracticeReminder() {
        val posted = reminderScheduler?.showImmediateReminder() == true
        _statusMessage.value = if (posted) {
            "تم إرسال إشعار تذكير تجريبي!"
        } else {
            "يرجى منح إذن الإشعارات لاستلام التذكيرات اليومية."
        }
    }

    fun startDailyQuiz() {
        val savedWords = (vocabState.value as? UiState.Success)?.data.orEmpty()
        val generated = DailyQuizGenerator.generateDailyQuiz(flashcards = savedWords)
        if (generated.isEmpty()) {
            _statusMessage.value = "أضف كلمة إلى بطاقات المفردات أولاً لبدء الاختبار"
            return
        }
        _dailyQuizQuestions.value = generated
    }

    fun dismissDailyQuiz() {
        _dailyQuizQuestions.value = emptyList()
    }

    fun completeDailyQuiz(score: Int, total: Int) {
        val safeTotal = total.coerceAtLeast(1)
        val cleanScore = score.coerceIn(0, safeTotal)
        val xpEarned = maxOf(15L, cleanScore * 10L)
        val quizDate = formatDayDateKey(System.currentTimeMillis())
        viewModelScope.launch {
            learningRepository.recordDailyQuizResult(
                score = cleanScore,
                quizDate = quizDate,
                xpEarned = xpEarned,
                displayName = displayName
            ).onSuccess {
                recordDailyUsageIfNeeded()
                _statusMessage.value = "أتممت اختبار اليوم: $cleanScore/$safeTotal (+$xpEarned XP)"
            }.onFailure { err ->
                _statusMessage.value = err.localizedMessage ?: "تعذر حفظ نتيجة الاختبار"
            }
        }
    }

    fun updateDailyStudyGoalMinutes(minutes: Int) {
        val cleanMinutes = minutes.coerceIn(
            StudyTimeSummary.MIN_STUDY_GOAL_MINUTES,
            StudyTimeSummary.MAX_STUDY_GOAL_MINUTES
        )
        if (_elapsedStudySecondsToday.value < cleanMinutes * 60) {
            studyGoalCelebratedForDate = null
        }
        viewModelScope.launch {
            learningRepository.updateDailyStudyGoalMinutes(cleanMinutes, displayName)
                .onSuccess {
                    _statusMessage.value = "تم تحديد هدف وقت الدراسة اليومي: $cleanMinutes دقيقة"
                }
                .onFailure { err ->
                    _statusMessage.value = err.localizedMessage ?: "تعذر حفظ هدف وقت الدراسة"
                }
        }
    }

    fun onStudyScreenActiveChanged(active: Boolean) {
        if (active) {
            startStudySession(autoStarted = true)
        } else if (_isStudyTimerRunning.value) {
            studyTickJob?.cancel()
            studyTickJob = null
            _isStudyTimerRunning.value = false
            _isStudyTimerPaused.value = _elapsedStudySecondsToday.value > 0
            persistStudyTimeAsync()
        }
    }

    fun toggleStudyTimer() {
        if (_isStudyTimerRunning.value) {
            pauseStudySession(manual = true)
        } else {
            userManuallyPausedStudyTimer = false
            startStudySession(autoStarted = false)
        }
    }

    fun startStudySession(autoStarted: Boolean = false) {
        if (autoStarted && userManuallyPausedStudyTimer) return
        if (_isStudyTimerRunning.value) return
        _isStudyTimerRunning.value = true
        _isStudyTimerPaused.value = false
        studyTickJob?.cancel()
        studyTickJob = viewModelScope.launch {
            while (_isStudyTimerRunning.value) {
                delay(1000L)
                val nowDate = formatDayDateKey(System.currentTimeMillis())
                if (nowDate != currentStudyDateKey) {
                    currentStudyDateKey = nowDate
                    _elapsedStudySecondsToday.value = 0
                    studyGoalCelebratedForDate = null
                }
                val nextSeconds = (_elapsedStudySecondsToday.value + 1)
                    .coerceAtMost(StudyTimeSummary.MAX_DAILY_STUDY_SECONDS)
                _elapsedStudySecondsToday.value = nextSeconds

                val goalSeconds = studyTimeSummary.value.goalSeconds
                if (nextSeconds >= goalSeconds && studyGoalCelebratedForDate != currentStudyDateKey) {
                    studyGoalCelebratedForDate = currentStudyDateKey
                    _statusMessage.value =
                        "أحسنت! أتممت هدف وقت الدراسة اليومي (${studyTimeSummary.value.safeGoalMinutes} دقيقة)"
                    learningRepository.saveDailyStudySeconds(nextSeconds, currentStudyDateKey, displayName)
                    recordDailyUsageIfNeeded()
                } else if (nextSeconds % 15 == 0) {
                    learningRepository.saveDailyStudySeconds(nextSeconds, currentStudyDateKey, displayName)
                    recordDailyUsageIfNeeded()
                }
            }
        }
    }

    fun pauseStudySession(manual: Boolean = true) {
        if (manual) {
            userManuallyPausedStudyTimer = true
        }
        studyTickJob?.cancel()
        studyTickJob = null
        _isStudyTimerRunning.value = false
        _isStudyTimerPaused.value = _elapsedStudySecondsToday.value > 0
        persistStudyTimeAsync()
    }

    private fun persistStudyTimeAsync() {
        val elapsed = _elapsedStudySecondsToday.value
        val studyDate = currentStudyDateKey
        if (elapsed <= 0) return
        viewModelScope.launch {
            learningRepository.saveDailyStudySeconds(elapsed, studyDate, displayName)
            recordDailyUsageIfNeeded()
        }
    }

    fun toggleSlowSpeech() {
        _slowSpeech.value = !_slowSpeech.value
    }

    fun toggleAutoSpeakVocal() {
        _autoSpeakVocal.value = !_autoSpeakVocal.value
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun speakEnglish(text: String, slow: Boolean = _slowSpeech.value) {
        speechManager.speak(text = text, slow = slow)
    }

    fun stopSpeaking() {
        speechManager.stop()
    }

    fun completeLesson(lesson: Lesson) {
        viewModelScope.launch {
            learningRepository.completeLesson(lesson.id, lesson.xpReward, displayName)
                .onSuccess {
                    recordDailyUsageIfNeeded()
                    _statusMessage.value = "أحسنت! تم إتمام درس ${lesson.titleArabic} (+${lesson.xpReward} XP)"
                }
                .onFailure { err ->
                    _statusMessage.value = err.localizedMessage ?: "تعذر حفظ التقدم"
                }
        }
    }

    fun saveVocabWord(
        english: String,
        arabic: String,
        phoneticArabic: String,
        exampleEnglish: String,
        category: String,
        phoneticEnglish: String = ""
    ) {
        val cleanEnglish = english.trim()
        if (cleanEnglish.isBlank()) return
        val needsAiEnrichment = phoneticArabic.isBlank() || phoneticEnglish.isBlank()
        viewModelScope.launch {
            learningRepository.addVocabItem(
                english = cleanEnglish,
                arabic = arabic,
                phoneticArabic = phoneticArabic,
                exampleEnglish = exampleEnglish,
                category = category,
                phoneticEnglish = phoneticEnglish
            ).onSuccess { newDocId ->
                recordDailyUsageIfNeeded()
                _statusMessage.value = "تمت إضافة \"$cleanEnglish\" مع النطق الصوتي إلى بطاقاتك"
                if (needsAiEnrichment) {
                    aiTutorRepository.translateWordForWordbook(cleanEnglish)
                        .onSuccess { translated ->
                            learningRepository.updateVocabPhonetics(
                                wordId = newDocId,
                                phoneticEnglish = phoneticEnglish.ifBlank { translated.phoneticEnglish },
                                phoneticArabic = phoneticArabic.ifBlank { translated.phoneticArabic }
                            )
                        }
                }
            }.onFailure { err ->
                _statusMessage.value = err.localizedMessage ?: "تعذر حفظ الكلمة"
            }
        }
    }

    fun generatePhoneticsForWord(item: VocabItem) {
        if (item.id.isBlank() || item.english.isBlank()) return
        viewModelScope.launch {
            _isTranslatingWord.value = true
            aiTutorRepository.translateWordForWordbook(item.english)
                .onSuccess { translated ->
                    learningRepository.updateVocabPhonetics(
                        wordId = item.id,
                        phoneticEnglish = translated.phoneticEnglish,
                        phoneticArabic = translated.phoneticArabic
                    ).onSuccess {
                        _statusMessage.value = "تم تحديث النطق الصوتي لكلمة \"${item.english}\""
                    }
                }
                .onFailure {
                    val fallback = com.example.data.model.PronunciationHelper.resolvePhonetics(
                        english = item.english,
                        existingPhoneticEnglish = item.phoneticEnglish,
                        existingPhoneticArabic = item.phoneticArabic
                    )
                    learningRepository.updateVocabPhonetics(
                        wordId = item.id,
                        phoneticEnglish = fallback.phoneticEnglish,
                        phoneticArabic = fallback.phoneticArabic
                    )
                }
            _isTranslatingWord.value = false
        }
    }

    fun translateAndSaveWord(rawInput: String, category: String = "Personal") {
        val clean = rawInput.trim()
        if (clean.isBlank()) return
        viewModelScope.launch {
            _isTranslatingWord.value = true
            aiTutorRepository.translateWordForWordbook(clean)
                .onSuccess { translated ->
                    learningRepository.addVocabItem(
                        english = translated.english,
                        arabic = translated.arabic,
                        phoneticArabic = translated.phoneticArabic,
                        exampleEnglish = translated.exampleEnglish,
                        category = category,
                        phoneticEnglish = translated.phoneticEnglish
                    ).onSuccess {
                        recordDailyUsageIfNeeded()
                        _statusMessage.value = "تم حفظ \"${translated.english}\" مع النطق والترجمة"
                    }.onFailure { err ->
                        _statusMessage.value = err.localizedMessage ?: "تعذر الحفظ"
                    }
                }
                .onFailure { err ->
                    _statusMessage.value = err.localizedMessage ?: "تعذرت الترجمة التلقائية"
                }
            _isTranslatingWord.value = false
        }
    }

    fun toggleWordMastered(item: VocabItem) {
        viewModelScope.launch {
            learningRepository.toggleVocabMastered(item.id, !item.mastered)
                .onSuccess {
                    recordDailyUsageIfNeeded()
                }
                .onFailure { err ->
                    _statusMessage.value = err.localizedMessage ?: "تعذر تحديث الحالة"
                }
        }
    }

    fun deleteWord(wordId: String) {
        viewModelScope.launch {
            learningRepository.deleteVocabItem(wordId)
                .onFailure { err ->
                    _statusMessage.value = err.localizedMessage ?: "تعذر حذف الكلمة"
                }
        }
    }

    fun sendWrittenMessage(message: String, isSpokenPractice: Boolean = false) {
        val clean = message.trim()
        if (clean.isBlank() || _writtenState.value is AiTurnState.Processing) return

        viewModelScope.launch {
            _writtenState.value = AiTurnState.Processing
            val history = buildRecentHistory(modeFilter = "written")
            val topic = _activeLessonTopic.value?.let { "${it.titleEnglish} (${it.titleArabic})" }

            aiTutorRepository.sendWrittenTurn(
                userMessage = clean,
                recentHistory = history,
                lessonTopic = topic,
                isSpokenPractice = isSpokenPractice
            ).onSuccess { response ->
                _writtenSuggestions.value = response.suggestedPhrases
                learningRepository.saveTutorTurn(
                    mode = "written",
                    userInput = clean,
                    englishReply = response.englishReply,
                    arabicTranslation = response.arabicTranslation,
                    feedbackArabic = response.feedbackArabic,
                    phoneticHint = response.phoneticHint
                )
                learningRepository.awardPracticeXp(if (isSpokenPractice) 15L else 10L, displayName)
                recordDailyUsageIfNeeded()
                extractAndStoreTurnVocabulary(
                    userMessage = clean,
                    suggestions = response.suggestedPhrases
                )
                _writtenState.value = AiTurnState.Idle
            }.onFailure { err ->
                _writtenState.value = AiTurnState.Error(
                    err.localizedMessage ?: "Failed to contact English tutor."
                )
            }
        }
    }

    fun sendVocalAudio(audioBytes: ByteArray) {
        if (audioBytes.isEmpty() || _vocalState.value is AiTurnState.Processing) return

        viewModelScope.launch {
            _vocalState.value = AiTurnState.Processing
            val history = buildRecentHistory(modeFilter = "vocal")
            val topic = _activeLessonTopic.value?.let { "${it.titleEnglish} (${it.titleArabic})" }

            aiTutorRepository.sendVocalAudioTurn(
                audioBytes = audioBytes,
                mimeType = "audio/mp4",
                recentHistory = history,
                lessonTopic = topic
            ).onSuccess { response ->
                handleVocalSuccess(response)
            }.onFailure { err ->
                _vocalState.value = AiTurnState.Error(
                    err.localizedMessage ?: "Voice analysis failed. Please try again."
                )
            }
        }
    }

    fun sendVocalTextPractice(spokenText: String) {
        val clean = spokenText.trim()
        if (clean.isBlank() || _vocalState.value is AiTurnState.Processing) return

        viewModelScope.launch {
            _vocalState.value = AiTurnState.Processing
            val history = buildRecentHistory(modeFilter = "vocal")
            val topic = _activeLessonTopic.value?.let { "${it.titleEnglish} (${it.titleArabic})" }

            aiTutorRepository.sendVocalTextPracticeTurn(
                spokenPrompt = clean,
                recentHistory = history,
                lessonTopic = topic
            ).onSuccess { response ->
                handleVocalSuccess(response)
            }.onFailure { err ->
                _vocalState.value = AiTurnState.Error(
                    err.localizedMessage ?: "Vocal tutor failed. Please try again."
                )
            }
        }
    }

    private suspend fun handleVocalSuccess(response: TutorResponse) {
        _latestVocalResponse.value = response
        val spokenText = response.whatYouSaid.ifBlank { "(Voice message)" }
        learningRepository.saveTutorTurn(
            mode = "vocal",
            userInput = spokenText,
            englishReply = response.englishReply,
            arabicTranslation = response.arabicTranslation,
            feedbackArabic = response.feedbackArabic,
            phoneticHint = response.phoneticHint
        )
        learningRepository.awardPracticeXp(15L, displayName)
        recordDailyUsageIfNeeded()
        extractAndStoreTurnVocabulary(
            userMessage = spokenText,
            suggestions = response.suggestedPhrases
        )
        _vocalState.value = AiTurnState.Idle
        if (_autoSpeakVocal.value) {
            speechManager.speak(response.englishReply, slow = _slowSpeech.value)
        }
    }

    fun syncConversationVocabulary(showFeedback: Boolean = true) {
        if (_isSyncingConversationVocab.value) return
        val turns = (tutorTurnsState.value as? UiState.Success)?.data.orEmpty()
        val frequentWords = ConversationVocabularyExtractor.extractFrequentWordsFromTurns(
            turns = turns,
            minFrequency = 1,
            limit = 10
        )
        if (frequentWords.isEmpty()) {
            if (showFeedback) {
                _statusMessage.value = "تحدث أو اكتب بالإنجليزية مع المعلمة نور أولاً لاستخراج كلماتك المتكررة"
            }
            return
        }

        viewModelScope.launch {
            _isSyncingConversationVocab.value = true
            try {
                val existingByWord = (vocabState.value as? UiState.Success)?.data.orEmpty()
                    .associateBy { it.english.trim().lowercase() }
                var updatedCount = 0
                var newTranslationsLeft = 6

                for (entry in frequentWords) {
                    val existing = existingByWord[entry.normalizedKey]
                    if (existing != null) {
                        if (entry.count > existing.usageCount) {
                            learningRepository.upsertConversationVocab(
                                english = existing.english,
                                arabic = existing.arabic,
                                phoneticArabic = existing.phoneticArabic,
                                exampleEnglish = existing.exampleEnglish.ifBlank { entry.sampleSentence },
                                category = existing.category,
                                minimumTotalCount = entry.count,
                                phoneticEnglish = existing.phoneticEnglish
                            ).onSuccess { updatedCount++ }
                        }
                    } else if (newTranslationsLeft > 0) {
                        newTranslationsLeft--
                        aiTutorRepository.translateWordForWordbook(entry.displayWord)
                            .onSuccess { translated ->
                                learningRepository.upsertConversationVocab(
                                    english = translated.english.ifBlank { entry.displayWord },
                                    arabic = translated.arabic,
                                    phoneticArabic = translated.phoneticArabic,
                                    exampleEnglish = translated.exampleEnglish.ifBlank { entry.sampleSentence },
                                    category = "Conversation",
                                    minimumTotalCount = entry.count,
                                    phoneticEnglish = translated.phoneticEnglish
                                ).onSuccess { updatedCount++ }
                            }
                    }
                }

                if (showFeedback) {
                    _statusMessage.value = if (updatedCount > 0) {
                        "تمت مزامنة $updatedCount كلمة من محادثاتك في بطاقات المفردات"
                    } else {
                        "جميع كلمات محادثاتك الأكثر استخداماً محدّثة بالفعل"
                    }
                }
            } finally {
                _isSyncingConversationVocab.value = false
            }
        }
    }

    private fun extractAndStoreTurnVocabulary(
        userMessage: String,
        suggestions: List<SuggestedPhrase>
    ) {
        val extracted = ConversationVocabularyExtractor.extractFrequentWords(
            texts = listOf(userMessage),
            minFrequency = 1,
            limit = 3
        )
        if (extracted.isEmpty()) return

        viewModelScope.launch {
            val existingByWord = (vocabState.value as? UiState.Success)?.data.orEmpty()
                .associateBy { it.english.trim().lowercase() }
            val suggestionByWord = suggestions.associateBy {
                ConversationVocabularyExtractor.normalizeSingleWord(it.english) ?: it.english.trim().lowercase()
            }
            var newTranslationsLeft = 2

            for (word in extracted) {
                val existing = existingByWord[word.normalizedKey]
                if (existing != null) {
                    learningRepository.upsertConversationVocab(
                        english = existing.english,
                        arabic = existing.arabic,
                        phoneticArabic = existing.phoneticArabic,
                        exampleEnglish = existing.exampleEnglish.ifBlank { word.sampleSentence },
                        category = existing.category,
                        countIncrement = word.count,
                        phoneticEnglish = existing.phoneticEnglish
                    )
                    continue
                }

                val suggestedMatch = suggestionByWord[word.normalizedKey]
                if (suggestedMatch != null) {
                    learningRepository.upsertConversationVocab(
                        english = suggestedMatch.english,
                        arabic = suggestedMatch.arabic,
                        phoneticArabic = suggestedMatch.phoneticArabic,
                        exampleEnglish = suggestedMatch.exampleEnglish.ifBlank { word.sampleSentence },
                        category = "Conversation",
                        countIncrement = word.count,
                        phoneticEnglish = suggestedMatch.phoneticEnglish
                    )
                } else if (newTranslationsLeft > 0) {
                    newTranslationsLeft--
                    aiTutorRepository.translateWordForWordbook(word.displayWord)
                        .onSuccess { translated ->
                            learningRepository.upsertConversationVocab(
                                english = translated.english.ifBlank { word.displayWord },
                                arabic = translated.arabic,
                                phoneticArabic = translated.phoneticArabic,
                                exampleEnglish = translated.exampleEnglish.ifBlank { word.sampleSentence },
                                category = "Conversation",
                                countIncrement = word.count,
                                phoneticEnglish = translated.phoneticEnglish
                            )
                        }
                }
            }
        }
    }

    fun deleteTutorTurn(turnId: String) {
        viewModelScope.launch {
            learningRepository.deleteTutorTurn(turnId)
        }
    }

    fun clearChatHistory(mode: String = "written") {
        viewModelScope.launch {
            learningRepository.clearTutorTurns(mode)
                .onSuccess {
                    if (mode == "written") {
                        _writtenSuggestions.value = emptyList()
                    } else {
                        _latestVocalResponse.value = null
                    }
                    _statusMessage.value = "تم مسح سجل المحادثة"
                }
                .onFailure { err ->
                    _statusMessage.value = err.localizedMessage ?: "تعذر مسح المحادثة"
                }
        }
    }

    private fun buildRecentHistory(modeFilter: String): List<Content> {
        val currentTurns = (tutorTurnsState.value as? UiState.Success)?.data.orEmpty()
            .filter { it.mode == modeFilter }
            .takeLast(6)
        return buildList {
            for (turn in currentTurns) {
                add(content("user") { text(turn.userInput) })
                add(content("model") { text(turn.englishReply) })
            }
        }
    }

    override fun onCleared() {
        studyTickJob?.cancel()
        super.onCleared()
        speechManager.shutdown()
    }

    private companion object {
        const val TAG = "LearningViewModel"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
