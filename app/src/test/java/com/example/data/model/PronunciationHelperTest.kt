package com.example.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PronunciationHelperTest {

    @Test
    fun resolvePhonetics_returnsCuratedIpaAndArabicForBeginnerWords() {
        val result = PronunciationHelper.resolvePhonetics("Coffee")
        assertTrue(result.phoneticEnglish.contains("KAW-fee"))
        assertEquals("كوفي", result.phoneticArabic)
    }

    @Test
    fun resolvePhonetics_generatesFallbackSyllablesAndArabicForNewWords() {
        val result = PronunciationHelper.resolvePhonetics("Pineapple")
        assertTrue(result.phoneticEnglish.isNotBlank())
        assertTrue(result.phoneticArabic.contains("پ"))
    }

    @Test
    fun resolvePhonetics_preservesProvidedValues() {
        val result = PronunciationHelper.resolvePhonetics(
            english = "Schedule",
            existingPhoneticEnglish = "/ˈskedʒuːl/",
            existingPhoneticArabic = "سكيدجول"
        )
        assertEquals("/ˈskedʒuːl/", result.phoneticEnglish)
        assertEquals("سكيدجول", result.phoneticArabic)
    }
}
