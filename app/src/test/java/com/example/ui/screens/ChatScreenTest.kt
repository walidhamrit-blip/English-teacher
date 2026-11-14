package com.example.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.core.SpeechToTextController
import com.example.core.SpeechToTextState
import com.example.data.model.DailyHabitDay
import com.example.data.model.DailyProgressSummary
import com.example.ui.components.DailyProgressTrackerCard
import com.example.ui.components.HeaderStreakBadge
import com.example.ui.theme.NoorEnglishTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ChatScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun chatScreen_displaysMessagesAndSendsUserInput() {
        val sentMessages = mutableListOf<String>()
        val sampleMessages = listOf(
            ChatMessage(id = "1", text = "Hello teacher", isFromUser = true),
            ChatMessage(
                id = "2",
                text = "Hello! How are you today?",
                isFromUser = false,
                arabicTranslation = "مرحباً! كيف حالك اليوم؟"
            )
        )

        composeRule.setContent {
            NoorEnglishTheme {
                ChatScreen(
                    messages = sampleMessages,
                    isSending = false,
                    onSendMessage = { sentMessages.add(it) },
                    speechController = FakeSpeechToTextController(),
                    hasAudioPermissionOverride = true
                )
            }
        }

        composeRule.onNodeWithTag("chat_message_list").assertIsDisplayed()
        composeRule.onNodeWithText("Hello teacher").assertIsDisplayed()
        composeRule.onNodeWithText("Hello! How are you today?").assertIsDisplayed()

        composeRule.onNodeWithTag("chat_message_input").performTextInput("I am fine")
        composeRule.onNodeWithTag("chat_send_button").performClick()

        assertEquals(listOf("I am fine"), sentMessages)
    }

    @Test
    fun chatScreen_microphoneSpeechToText_populatesAndSendsSpokenMessage() {
        val writtenMessages = mutableListOf<String>()
        val spokenMessages = mutableListOf<String>()
        val fakeSpeech = FakeSpeechToTextController()

        composeRule.setContent {
            NoorEnglishTheme {
                ChatScreen(
                    messages = emptyList(),
                    isSending = false,
                    onSendMessage = { writtenMessages.add(it) },
                    onSendSpokenMessage = { spokenMessages.add(it) },
                    speechController = fakeSpeech,
                    hasAudioPermissionOverride = true
                )
            }
        }

        composeRule.onNodeWithTag("chat_mic_button").assertIsDisplayed().performClick()
        assertTrue(fakeSpeech.startedListening)

        composeRule.onNodeWithTag("chat_speech_listening_banner").assertIsDisplayed()

        fakeSpeech.emitPartial("I would like")
        composeRule.waitForIdle()
        composeRule.onNodeWithText("\"I would like\"").assertIsDisplayed()

        fakeSpeech.emitFinal("I would like a cup of tea", confidence = 0.94f)
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("chat_speech_result_banner").assertIsDisplayed()
        composeRule.onNodeWithTag("chat_message_input").assertTextContains("I would like a cup of tea")

        composeRule.onNodeWithTag("chat_send_button").performClick()

        assertEquals(listOf("I would like a cup of tea"), spokenMessages)
        assertTrue(writtenMessages.isEmpty())
    }

    @Test
    fun chatScreen_speechError_displaysErrorBanner() {
        val fakeSpeech = FakeSpeechToTextController()

        composeRule.setContent {
            NoorEnglishTheme {
                ChatScreen(
                    messages = emptyList(),
                    isSending = false,
                    onSendMessage = {},
                    speechController = fakeSpeech,
                    hasAudioPermissionOverride = true
                )
            }
        }

        fakeSpeech.reportError("لم أتمكن من تمييز الكلمات الإنجليزية. حاول التحدث ببطء ووضوح.")
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("chat_speech_error_banner").assertIsDisplayed()
    }

    @Test
    fun dailyProgressTracker_displaysTodayMessageCountAndGoalProgress() {
        val selectedGoals = mutableListOf<Int>()
        val summary = DailyProgressSummary(
            todayMessagesCount = 3,
            dailyGoal = 10,
            totalMessagesCount = 12,
            writtenTodayCount = 2,
            vocalTodayCount = 1,
            currentStreakDays = 4,
            lastSevenDays = listOf(
                DailyHabitDay(20261112, "خم", "Thu", 3, goalMet = false, isToday = true)
            )
        )

        composeRule.setContent {
            NoorEnglishTheme {
                DailyProgressTrackerCard(
                    summary = summary,
                    onSelectDailyGoal = { selectedGoals.add(it) }
                )
            }
        }

        composeRule.onNodeWithTag("home_daily_progress_tracker").assertIsDisplayed()
        composeRule.onNodeWithTag("daily_today_message_count").assertTextContains("3")
        composeRule.onNodeWithTag("daily_progress_bar").assertIsDisplayed()
        composeRule.onNodeWithTag("daily_progress_percent").assertTextContains("30%", substring = true)
        composeRule.onNodeWithTag("daily_streak_badge").assertTextContains("4 يوم", substring = true)

        composeRule.onNodeWithTag("daily_goal_chip_5").assertIsDisplayed()
        composeRule.onNodeWithTag("daily_goal_chip_10").assertIsDisplayed()
        composeRule.onNodeWithTag("daily_goal_chip_20").performClick()

        assertEquals(listOf(20), selectedGoals)
    }

    @Test
    fun chatScreen_displaysCompactDailyProgressStrip() {
        val selectedGoals = mutableListOf<Int>()
        val summary = DailyProgressSummary(
            todayMessagesCount = 5,
            dailyGoal = 5,
            totalMessagesCount = 5,
            writtenTodayCount = 5,
            vocalTodayCount = 0,
            currentStreakDays = 1
        )

        composeRule.setContent {
            NoorEnglishTheme {
                ChatScreen(
                    messages = emptyList(),
                    isSending = false,
                    onSendMessage = {},
                    speechController = FakeSpeechToTextController(),
                    hasAudioPermissionOverride = true,
                    dailyProgress = summary,
                    onSelectDailyGoal = { selectedGoals.add(it) }
                )
            }
        }

        composeRule.onNodeWithTag("chat_daily_progress_tracker").assertIsDisplayed()
        composeRule.onNodeWithTag("chat_daily_progress_bar").assertIsDisplayed()
        composeRule.onNodeWithText("رسائل اليوم: 5 / 5").assertIsDisplayed()
        composeRule.onNodeWithText("تم تحقيق هدف اليوم! ✓").assertIsDisplayed()

        composeRule.onNodeWithTag("chat_daily_goal_button").performClick()
        composeRule.onNodeWithTag("dialog_goal_preset_10").performClick()
        composeRule.onNodeWithTag("dialog_goal_save_button").performClick()

        assertEquals(listOf(10), selectedGoals)
    }

    @Test
    fun headerStreakBadge_displaysStreakCountAndOpensDetailsDialog() {
        val summary = DailyProgressSummary(
            todayMessagesCount = 4,
            dailyGoal = 5,
            currentStreakDays = 7,
            longestStreakDays = 12
        )

        composeRule.setContent {
            NoorEnglishTheme {
                HeaderStreakBadge(summary = summary)
            }
        }

        composeRule.onNodeWithTag("header_daily_streak_badge")
            .assertIsDisplayed()
            .assertTextContains("7", substring = true)
            .performClick()

        composeRule.onNodeWithTag("header_streak_details_dialog").assertIsDisplayed()
        composeRule.onNodeWithText("السلسلة الحالية: 7 يوم متواصل").assertIsDisplayed()
        composeRule.onNodeWithText("أطول سلسلة: 12 يوم").assertIsDisplayed()
    }

    private class FakeSpeechToTextController : SpeechToTextController {
        private val _state = MutableStateFlow(SpeechToTextState())
        override val state: StateFlow<SpeechToTextState> = _state.asStateFlow()

        var startedListening = false
            private set

        override fun isDirectRecognitionAvailable(): Boolean = true

        override fun startListening(languageTag: String) {
            startedListening = true
            _state.value = SpeechToTextState(isListening = true, rmsLevel = 0.5f)
        }

        override fun stopListening() {
            _state.value = _state.value.copy(isListening = false, rmsLevel = 0f)
        }

        override fun cancel() {
            _state.value = _state.value.copy(isListening = false, rmsLevel = 0f, partialText = "")
        }

        override fun clearResult() {
            _state.value = _state.value.copy(
                partialText = "",
                recognizedText = null,
                confidence = null,
                errorMessage = null
            )
        }

        override fun applyExternalResult(transcript: String, confidence: Float?) {
            emitFinal(transcript, confidence)
        }

        override fun reportError(message: String) {
            _state.value = _state.value.copy(isListening = false, errorMessage = message)
        }

        override fun destroy() = Unit

        fun emitPartial(partial: String) {
            _state.value = _state.value.copy(isListening = true, isSpeechDetected = true, partialText = partial)
        }

        fun emitFinal(text: String, confidence: Float? = null) {
            _state.value = SpeechToTextState(
                isListening = false,
                isSpeechDetected = true,
                recognizedText = text,
                confidence = confidence
            )
        }
    }
}
