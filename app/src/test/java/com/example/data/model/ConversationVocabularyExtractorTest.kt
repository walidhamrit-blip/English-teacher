package com.example.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversationVocabularyExtractorTest {

    @Test
    fun extractFrequentWords_filtersStopWordsAndRanksByFrequency() {
        val texts = listOf(
            "Good morning! I want coffee, please.",
            "Can I have hot coffee this morning?",
            "Thank you for the coffee, teacher!"
        )

        val result = ConversationVocabularyExtractor.extractFrequentWords(texts, limit = 10)
        val byKey = result.associateBy { it.normalizedKey }

        assertEquals("coffee", result.first().normalizedKey)
        assertEquals(3, result.first().count)
        assertEquals("Coffee", result.first().displayWord)

        assertEquals(2, byKey["morning"]?.count)
        assertEquals(1, byKey["teacher"]?.count)
        assertEquals(1, byKey["thank"]?.count)

        assertFalse(byKey.containsKey("the"))
        assertFalse(byKey.containsKey("for"))
        assertFalse(byKey.containsKey("can"))
        assertFalse(byKey.containsKey("you"))
        assertFalse(byKey.containsKey("i"))
    }

    @Test
    fun extractFrequentWordsFromTurns_ignoresPlaceholdersAndHandlesContractions() {
        val turns = listOf(
            TutorTurn(id = "1", userInput = "(Voice recording)"),
            TutorTurn(id = "2", userInput = "I'm happy today!"),
            TutorTurn(id = "3", userInput = "My friend's book is great, happy reading!")
        )

        val result = ConversationVocabularyExtractor.extractFrequentWordsFromTurns(turns)
        val keys = result.map { it.normalizedKey }

        assertTrue(keys.contains("happy"))
        assertTrue(keys.contains("friend"))
        assertTrue(keys.contains("book"))
        assertEquals(2, result.first { it.normalizedKey == "happy" }.count)
        assertFalse(keys.contains("voice"))
        assertFalse(keys.contains("recording"))
    }
}
