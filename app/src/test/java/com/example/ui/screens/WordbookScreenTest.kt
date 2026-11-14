package com.example.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.data.model.VocabItem
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
class WordbookScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val sampleWords = listOf(
        VocabItem(
            id = "w1",
            userId = "u1",
            english = "Teacher",
            arabic = "معلم",
            phoneticArabic = "تيتشر",
            exampleEnglish = "My teacher helps me speak English.",
            category = "Personal",
            mastered = false,
            usageCount = 1
        ),
        VocabItem(
            id = "w2",
            userId = "u1",
            english = "Coffee",
            arabic = "قهوة",
            phoneticArabic = "كوفي",
            exampleEnglish = "I drink coffee every morning.",
            category = "Conversation",
            mastered = false,
            usageCount = 4
        )
    )

    @Test
    fun wordbookScreen_showsFrequentConversationWordFlashcardAndFlipsOnTap() {
        val spokenWords = mutableListOf<String>()
        val slowSpokenWords = mutableListOf<String>()
        val toggledItems = mutableListOf<VocabItem>()

        composeRule.setContent {
            NoorEnglishTheme {
                WordbookScreen(
                    vocabState = UiState.Success(sampleWords),
                    isTranslatingWord = false,
                    onSpeakWord = { spokenWords.add(it) },
                    onSpeakWordSlow = { slowSpokenWords.add(it) },
                    onToggleMastered = { toggledItems.add(it) },
                    onDeleteWord = {},
                    onTranslateAndSave = {},
                    onAddManualWord = { _, _, _, _ -> }
                )
            }
        }

        // Highest usageCount word ("Coffee" x4) appears first with phonetic spelling on front
        composeRule.onNodeWithTag("flashcard_english_word", useUnmergedTree = true)
            .assertTextContains("Coffee")
        composeRule.onNodeWithTag("flashcard_frequency_badge", useUnmergedTree = true)
            .assertTextContains("4", substring = true)
        composeRule.onNodeWithTag("flashcard_phonetic_english", useUnmergedTree = true)
            .assertTextContains("KAW-fee", substring = true)
        composeRule.onNodeWithTag("flashcard_phonetic_arabic_front", useUnmergedTree = true)
            .assertTextContains("كوفي", substring = true)

        // Normal and slow audio playback buttons trigger TTS callbacks
        composeRule.onNodeWithTag("flashcard_speak_button", useUnmergedTree = true)
            .performScrollTo().performClick()
        assertEquals(listOf("Coffee"), spokenWords)

        composeRule.onNodeWithTag("flashcard_speak_slow_button", useUnmergedTree = true)
            .performScrollTo().performClick()
        assertEquals(listOf("Coffee"), slowSpokenWords)

        // Tap flashcard to flip and reveal Arabic meaning, phonetics, and example audio button
        composeRule.onNodeWithTag("vocab_flashcard_card").performClick()
        composeRule.mainClock.advanceTimeBy(600)
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("flashcard_arabic_translation", useUnmergedTree = true)
            .assertIsDisplayed()
            .assertTextContains("قهوة")
        composeRule.onNodeWithTag("flashcard_phonetic", useUnmergedTree = true)
            .assertTextContains("كوفي", substring = true)

        composeRule.onNodeWithTag("flashcard_speak_example_button", useUnmergedTree = true)
            .performScrollTo().performClick()
        assertEquals(listOf("Coffee", "I drink coffee every morning."), slowSpokenWords)

        // Mark mastered and advance to next card
        composeRule.onNodeWithTag("flashcard_mastered_button").performScrollTo().performClick()
        assertEquals("w2", toggledItems.single().id)

        composeRule.onNodeWithTag("flashcard_next_button").performScrollTo().performClick()
        composeRule.mainClock.advanceTimeBy(600)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("flashcard_english_word", useUnmergedTree = true)
            .assertTextContains("Teacher")
    }

    @Test
    fun wordbookScreen_switchesToListModeAndSyncsConversations() {
        var syncTriggered = false
        val slowSpoken = mutableListOf<String>()

        composeRule.setContent {
            NoorEnglishTheme {
                WordbookScreen(
                    vocabState = UiState.Success(sampleWords),
                    isTranslatingWord = false,
                    hasConversationTurns = true,
                    onSyncConversationVocab = { syncTriggered = true },
                    onSpeakWord = {},
                    onSpeakWordSlow = { slowSpoken.add(it) },
                    onToggleMastered = {},
                    onDeleteWord = {},
                    onTranslateAndSave = {},
                    onAddManualWord = { _, _, _, _ -> }
                )
            }
        }

        composeRule.onNodeWithTag("vocab_sync_chats_button").performClick()
        assertTrue(syncTriggered)

        composeRule.onNodeWithTag("vocab_mode_list").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Coffee").assertIsDisplayed()
        composeRule.onNodeWithTag("vocab_frequency_w2").assertTextContains("×4")
        composeRule.onNodeWithTag("vocab_phonetic_en_w2").assertTextContains("KAW-fee", substring = true)
        composeRule.onNodeWithTag("vocab_speak_slow_w2").performClick()
        assertEquals(listOf("Coffee"), slowSpoken)
    }
}
