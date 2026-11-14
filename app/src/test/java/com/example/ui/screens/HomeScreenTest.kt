package com.example.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import com.example.data.model.DailyProgressSummary
import com.example.data.model.DailyQuizQuestion
import com.example.data.model.DailyQuizStatus
import com.example.data.model.Lesson
import com.example.data.model.QuizQuestionType
import com.example.data.model.ReminderSettings
import com.example.data.model.StudyTimeSummary
import com.example.data.model.UserProgress
import com.example.ui.components.DailyPracticeReminderCard
import com.example.ui.components.DailyQuizBannerCard
import com.example.ui.components.DailyQuizDialog
import com.example.ui.components.DailyStudyTimeGoalCard
import com.example.ui.components.MainLearningStreakBadge
import com.example.ui.components.StudySessionCountdownBar
import com.example.ui.theme.NoorEnglishTheme
import com.example.ui.viewmodel.UiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class HomeScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun mainLearningStreakBadge_displaysProgressTierAndOpensMilestonesDialog() {
        val summary = DailyProgressSummary(
            todayMessagesCount = 3,
            dailyGoal = 5,
            currentStreakDays = 7,
            longestStreakDays = 10
        )

        composeRule.setContent {
            NoorEnglishTheme {
                MainLearningStreakBadge(summary = summary)
            }
        }

        composeRule.onNodeWithTag("home_learning_streak_badge").assertIsDisplayed()
        composeRule.onNodeWithText("Learning Streak", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithTag("home_streak_days_count", useUnmergedTree = true)
            .assertIsDisplayed()
            .assertTextContains("7")
        composeRule.onNodeWithTag("home_streak_tier_badge", useUnmergedTree = true)
            .assertIsDisplayed()
            .assertTextContains("متعلّم أسبوعي")
        composeRule.onNodeWithTag("home_streak_milestone_progress", useUnmergedTree = true)
            .assertIsDisplayed()
        composeRule.onNodeWithTag("home_longest_streak_label", useUnmergedTree = true)
            .assertIsDisplayed()
            .assertTextContains("10", substring = true)

        composeRule.onNodeWithTag("home_learning_streak_badge").performClick()

        composeRule.onNodeWithTag("header_streak_details_dialog").assertIsDisplayed()
        composeRule.onNodeWithText("3 أيام ✓").assertIsDisplayed()
        composeRule.onNodeWithText("7 أيام ✓").assertIsDisplayed()
        composeRule.onNodeWithText("14 أيام").assertIsDisplayed()
    }

    @Test
    fun homeScreen_displaysHeroAndMainLearningStreakBadges() {
        val sampleLessons = listOf(
            Lesson(
                id = "lesson_1",
                number = 1,
                titleEnglish = "Greetings",
                titleArabic = "التحيات",
                subtitleArabic = "أساسيات الترحيب",
                focusTipArabic = "ابدأ بابتسامة",
                xpReward = 25,
                phrases = emptyList()
            )
        )
        val summary = DailyProgressSummary(
            todayMessagesCount = 2,
            dailyGoal = 5,
            currentStreakDays = 4,
            longestStreakDays = 6
        )

        composeRule.setContent {
            NoorEnglishTheme {
                HomeScreen(
                    displayName = "Omar",
                    lessons = sampleLessons,
                    progressState = UiState.Success(UserProgress(xp = 50L, currentStreak = 4, longestStreak = 6)),
                    vocabState = UiState.Success(emptyList()),
                    selectedLesson = null,
                    slowSpeech = false,
                    onToggleSlowSpeech = {},
                    onSelectLesson = {},
                    onCompleteLesson = {},
                    onSpeakPhrase = {},
                    onSavePhrase = { _, _, _, _ -> },
                    onOpenWrittenMode = {},
                    onOpenVocalMode = {},
                    dailyProgress = summary,
                    onSelectDailyGoal = {}
                )
            }
        }

        composeRule.onNodeWithTag("home_hero_streak_badge", useUnmergedTree = true)
            .assertIsDisplayed()
        composeRule.onNodeWithText("4 أيام", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithTag("home_learning_streak_badge").assertIsDisplayed()
        composeRule.onNodeWithTag("home_streak_days_count", useUnmergedTree = true)
            .assertTextContains("4")
        composeRule.onNodeWithTag("home_streak_tier_badge", useUnmergedTree = true)
            .assertTextContains("انطلاقة قوية")
    }

    @Test
    fun dailyStudyTimeGoalCard_selectsPresetMinutesAndTogglesCountdownTimer() {
        val selectedMinutes = mutableListOf<Int>()
        var toggled = false
        val studySummary = StudyTimeSummary(
            goalMinutes = 20,
            elapsedSecondsToday = 150,
            isRunning = false,
            isPaused = true
        )

        composeRule.setContent {
            NoorEnglishTheme {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    DailyStudyTimeGoalCard(
                        summary = studySummary,
                        onSelectStudyGoalMinutes = { selectedMinutes.add(it) },
                        onToggleTimer = { toggled = true }
                    )
                }
            }
        }

        composeRule.onNodeWithTag("home_study_time_card").assertIsDisplayed()
        composeRule.onNodeWithTag("home_study_countdown_text")
            .assertIsDisplayed()
            .assertTextContains("17:30")
        composeRule.onNodeWithTag("home_study_progress_bar").assertIsDisplayed()

        composeRule.onNodeWithTag("study_goal_chip_10").assertIsDisplayed()
        composeRule.onNodeWithTag("study_goal_chip_20").assertIsDisplayed()
        composeRule.onNodeWithTag("study_goal_chip_30").performScrollTo().performClick()
        assertEquals(listOf(30), selectedMinutes)

        composeRule.onNodeWithTag("home_study_timer_toggle_button").performScrollTo().performClick()
        assertTrue(toggled)
    }

    @Test
    fun studySessionCountdownBar_displaysRemainingTimeAndCustomGoalDialog() {
        val selectedMinutes = mutableListOf<Int>()
        var toggled = false
        val summary = StudyTimeSummary(
            goalMinutes = 10,
            elapsedSecondsToday = 75,
            isRunning = true,
            isPaused = false
        )

        composeRule.setContent {
            NoorEnglishTheme {
                StudySessionCountdownBar(
                    summary = summary,
                    onToggleTimer = { toggled = true },
                    onSelectStudyGoalMinutes = { selectedMinutes.add(it) }
                )
            }
        }

        composeRule.onNodeWithTag("session_countdown_bar").assertIsDisplayed()
        composeRule.onNodeWithTag("session_countdown_timer_text")
            .assertIsDisplayed()
            .assertTextContains("08:45")
        composeRule.onNodeWithTag("session_study_goal_label", useUnmergedTree = true)
            .assertTextContains("الهدف: 10 د")

        composeRule.onNodeWithTag("session_timer_pause_button").performClick()
        assertTrue(toggled)

        composeRule.onNodeWithTag("session_study_goal_button").performClick()
        composeRule.onNodeWithTag("dialog_custom_study_minutes_input").performTextReplacement("15")
        composeRule.onNodeWithTag("dialog_study_goal_save_button").performClick()

        assertEquals(listOf(15), selectedMinutes)
    }

    @Test
    fun dailyPracticeReminderCard_togglesSwitchSelectsPresetTimeAndSendsTest() {
        val updates = mutableListOf<Triple<Boolean, Int, Int>>()
        var testClicked = false

        composeRule.setContent {
            NoorEnglishTheme {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    DailyPracticeReminderCard(
                        settings = ReminderSettings(enabled = true, hour = 19, minute = 0),
                        onUpdateSettings = { enabled, hour, minute ->
                            updates.add(Triple(enabled, hour, minute))
                        },
                        onSendTestReminder = { testClicked = true },
                        hasNotificationPermissionOverride = true
                    )
                }
            }
        }

        composeRule.onNodeWithTag("home_reminder_card").assertIsDisplayed()
        composeRule.onNodeWithTag("reminder_time_text").assertTextContains("07:00 مساءً")

        composeRule.onNodeWithTag("reminder_preset_9").performScrollTo().performClick()
        assertEquals(listOf(Triple(true, 9, 0)), updates)

        composeRule.onNodeWithTag("reminder_enable_switch").performScrollTo().performClick()
        assertEquals(Triple(false, 19, 0), updates.last())

        composeRule.onNodeWithTag("reminder_test_button").performScrollTo().performClick()
        assertTrue(testClicked)
    }

    @Test
    fun dailyQuizBannerCard_respectsFlashcardCountAndTriggersStart() {
        var startClicks = 0

        composeRule.setContent {
            NoorEnglishTheme {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    DailyQuizBannerCard(
                        flashcardCount = 4,
                        quizStatus = DailyQuizStatus(completedToday = false, todayScore = 0),
                        onStartQuiz = { startClicks++ }
                    )
                }
            }
        }

        composeRule.onNodeWithTag("daily_quiz_banner_card").assertIsDisplayed()
        composeRule.onNodeWithTag("daily_quiz_status_badge", useUnmergedTree = true)
            .assertTextContains("5 أسئلة")
        composeRule.onNodeWithTag("start_daily_quiz_button").performScrollTo().performClick()
        assertEquals(1, startClicks)
    }

    @Test
    fun dailyQuizDialog_answersFiveQuestionsAndReportsFinalScore() {
        val sampleQuestions = (1..5).map { idx ->
            DailyQuizQuestion(
                id = "q_$idx",
                vocabId = "v_$idx",
                type = QuizQuestionType.ENGLISH_TO_ARABIC,
                promptTitleArabic = "اختر المعنى العربي الصحيح للكلمة:",
                promptMainText = "Word$idx",
                speakableEnglishText = "Word$idx",
                phoneticHint = "/wɜːrd$idx/",
                exampleEnglish = "Sample sentence $idx.",
                options = listOf("معنى $idx", "خيار ب", "خيار ج", "خيار د"),
                correctOptionIndex = 0
            )
        }
        var reportedScore = -1
        var reportedTotal = -1
        val spokenWords = mutableListOf<String>()

        composeRule.setContent {
            NoorEnglishTheme {
                DailyQuizDialog(
                    questions = sampleQuestions,
                    onSpeakEnglish = { spokenWords.add(it) },
                    onQuizCompleted = { score, total ->
                        reportedScore = score
                        reportedTotal = total
                    },
                    onRetakeQuiz = {},
                    onDismiss = {}
                )
            }
        }

        composeRule.onNodeWithTag("daily_quiz_dialog").assertIsDisplayed()
        composeRule.onNodeWithTag("daily_quiz_speak_button").performClick()
        assertEquals(listOf("Word1"), spokenWords)

        repeat(5) { step ->
            val optionIndex = if (step < 4) 0 else 1 // 4 correct, 1 wrong
            composeRule.onNodeWithTag("daily_quiz_option_$optionIndex")
                .performScrollTo()
                .performClick()
            composeRule.onNodeWithTag("daily_quiz_next_button")
                .performScrollTo()
                .performClick()
        }

        composeRule.onNodeWithTag("daily_quiz_final_score").assertIsDisplayed()
        composeRule.onNodeWithTag("daily_quiz_final_score").assertTextContains("4 / 5")
        assertEquals(4, reportedScore)
        assertEquals(5, reportedTotal)
    }
}
