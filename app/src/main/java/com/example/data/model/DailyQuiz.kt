package com.example.data.model

import kotlin.random.Random

enum class QuizQuestionType {
    ENGLISH_TO_ARABIC,
    ARABIC_TO_ENGLISH
}

data class DailyQuizQuestion(
    val id: String,
    val vocabId: String,
    val type: QuizQuestionType,
    val promptTitleArabic: String,
    val promptMainText: String,
    val speakableEnglishText: String,
    val phoneticHint: String,
    val exampleEnglish: String,
    val options: List<String>,
    val correctOptionIndex: Int
)

data class DailyQuizStatus(
    val completedToday: Boolean = false,
    val todayScore: Int = 0,
    val totalQuestions: Int = DailyQuizGenerator.QUIZ_QUESTION_COUNT
)

object DailyQuizGenerator {
    const val QUIZ_QUESTION_COUNT = 5

    private val FALLBACK_PAIRS = listOf(
        "Water" to "ماء",
        "Book" to "كتاب",
        "Morning" to "صباح",
        "Teacher" to "معلم",
        "Coffee" to "قهوة",
        "Friend" to "صديق",
        "Happy" to "سعيد",
        "House" to "منزل",
        "School" to "مدرسة",
        "Thank you" to "شكراً"
    )

    fun generateDailyQuiz(
        flashcards: List<VocabItem>,
        questionCount: Int = QUIZ_QUESTION_COUNT,
        random: Random = Random.Default
    ): List<DailyQuizQuestion> {
        val eligible = flashcards.filter {
            it.english.trim().isNotBlank() && it.arabic.trim().isNotBlank()
        }
        if (eligible.isEmpty() || questionCount <= 0) return emptyList()

        val distinctByWord = eligible.distinctBy { it.english.trim().lowercase() }
        val pool = if (distinctByWord.isNotEmpty()) distinctByWord else eligible
        val shuffledPool = pool.shuffled(random)

        val pickedEntries = mutableListOf<Pair<VocabItem, QuizQuestionType>>()
        val usedPasses = mutableMapOf<String, Int>()
        var cursor = 0
        while (pickedEntries.size < questionCount) {
            val card = shuffledPool[cursor % shuffledPool.size]
            val pass = usedPasses[card.id.ifBlank { card.english }] ?: 0
            usedPasses[card.id.ifBlank { card.english }] = pass + 1
            val type = if (pool.size >= questionCount) {
                if (pickedEntries.size % 2 == 0) {
                    QuizQuestionType.ENGLISH_TO_ARABIC
                } else {
                    QuizQuestionType.ARABIC_TO_ENGLISH
                }
            } else {
                if ((pass + pickedEntries.size) % 2 == 0) {
                    QuizQuestionType.ENGLISH_TO_ARABIC
                } else {
                    QuizQuestionType.ARABIC_TO_ENGLISH
                }
            }
            pickedEntries.add(card to type)
            cursor++
        }

        return pickedEntries.mapIndexed { idx, (card, type) ->
            val englishText = card.english.trim()
            val arabicText = card.arabic.trim()
            val resolvedPhonetics = PronunciationHelper.resolvePhonetics(
                english = englishText,
                existingPhoneticEnglish = card.phoneticEnglish,
                existingPhoneticArabic = card.phoneticArabic
            )
            val combinedPhonetic = listOf(
                resolvedPhonetics.phoneticEnglish,
                resolvedPhonetics.phoneticArabic
            ).filter { it.isNotBlank() }.joinToString(" • ")

            val correctAnswer: String
            val distractorCandidates: List<String>
            val promptTitle: String
            val promptMain: String

            if (type == QuizQuestionType.ENGLISH_TO_ARABIC) {
                correctAnswer = arabicText
                promptTitle = "اختر المعنى العربي الصحيح للكلمة:"
                promptMain = englishText
                val fromCards = pool
                    .map { it.arabic.trim() }
                    .filter { it.isNotBlank() && !it.equals(correctAnswer, ignoreCase = true) }
                    .distinct()
                    .shuffled(random)
                val fromFallback = FALLBACK_PAIRS
                    .map { it.second }
                    .filter { fb ->
                        !fb.equals(correctAnswer, ignoreCase = true) &&
                            fromCards.none { existing -> existing.equals(fb, ignoreCase = true) }
                    }
                    .shuffled(random)
                distractorCandidates = (fromCards + fromFallback).take(3)
            } else {
                correctAnswer = englishText
                promptTitle = "اختر الكلمة الإنجليزية المطابقة للمعنى:"
                promptMain = arabicText
                val fromCards = pool
                    .map { it.english.trim() }
                    .filter { it.isNotBlank() && !it.equals(correctAnswer, ignoreCase = true) }
                    .distinct()
                    .shuffled(random)
                val fromFallback = FALLBACK_PAIRS
                    .map { it.first }
                    .filter { fb ->
                        !fb.equals(correctAnswer, ignoreCase = true) &&
                            fromCards.none { existing -> existing.equals(fb, ignoreCase = true) }
                    }
                    .shuffled(random)
                distractorCandidates = (fromCards + fromFallback).take(3)
            }

            val options = (listOf(correctAnswer) + distractorCandidates).shuffled(random)
            val correctIndex = options.indexOfFirst { it.equals(correctAnswer, ignoreCase = true) }
                .coerceAtLeast(0)

            DailyQuizQuestion(
                id = "q_${idx}_${card.id.ifBlank { englishText }}",
                vocabId = card.id,
                type = type,
                promptTitleArabic = promptTitle,
                promptMainText = promptMain,
                speakableEnglishText = englishText,
                phoneticHint = combinedPhonetic,
                exampleEnglish = card.exampleEnglish.trim(),
                options = options,
                correctOptionIndex = correctIndex
            )
        }
    }
}
