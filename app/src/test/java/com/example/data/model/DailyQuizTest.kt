package com.example.data.model

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyQuizTest {

    private fun sampleCards(count: Int): List<VocabItem> {
        val pairs = listOf(
            "Hello" to "مرحباً",
            "Thank you" to "شكراً",
            "Water" to "ماء",
            "Coffee" to "قهوة",
            "Morning" to "صباح",
            "Friend" to "صديق",
            "Hospital" to "مستشفى"
        )
        return pairs.take(count).mapIndexed { index, (en, ar) ->
            VocabItem(
                id = "card_$index",
                english = en,
                arabic = ar,
                phoneticEnglish = "/sample$index/",
                phoneticArabic = "سامبل $index",
                exampleEnglish = "Example with $en.",
                category = "Conversation"
            )
        }
    }

    @Test
    fun generateDailyQuiz_emptyFlashcards_returnsEmptyList() {
        val questions = DailyQuizGenerator.generateDailyQuiz(emptyList())
        assertTrue(questions.isEmpty())
    }

    @Test
    fun generateDailyQuiz_sevenFlashcards_generatesFiveUniqueQuestionsWithFourUniqueOptions() {
        val cards = sampleCards(7)
        val questions = DailyQuizGenerator.generateDailyQuiz(
            flashcards = cards,
            random = Random(42)
        )

        assertEquals(DailyQuizGenerator.QUIZ_QUESTION_COUNT, questions.size)
        val uniqueVocabIds = questions.map { it.vocabId }.toSet()
        assertEquals(5, uniqueVocabIds.size)

        val validEnglish = cards.map { it.english }.toSet()
        val validArabic = cards.map { it.arabic }.toSet()

        questions.forEach { q ->
            assertEquals(4, q.options.size)
            assertEquals(4, q.options.distinct().size)
            assertTrue(q.correctOptionIndex in 0 until 4)
            assertTrue(q.speakableEnglishText in validEnglish)
            assertTrue(q.phoneticHint.isNotBlank())

            val correctOption = q.options[q.correctOptionIndex]
            if (q.type == QuizQuestionType.ENGLISH_TO_ARABIC) {
                assertTrue(q.promptMainText in validEnglish)
                assertTrue(correctOption in validArabic)
            } else {
                assertTrue(q.promptMainText in validArabic)
                assertTrue(correctOption in validEnglish)
            }
        }
    }

    @Test
    fun generateDailyQuiz_twoFlashcards_generatesFiveQuestionsUsingFallbackDistractors() {
        val cards = sampleCards(2)
        val questions = DailyQuizGenerator.generateDailyQuiz(
            flashcards = cards,
            random = Random(7)
        )

        assertEquals(5, questions.size)
        val types = questions.map { it.type }.toSet()
        assertTrue(QuizQuestionType.ENGLISH_TO_ARABIC in types)
        assertTrue(QuizQuestionType.ARABIC_TO_ENGLISH in types)

        questions.forEach { q ->
            assertEquals(4, q.options.size)
            assertEquals(4, q.options.distinct().size)
            assertTrue(q.correctOptionIndex in 0 until 4)
        }
    }
}
