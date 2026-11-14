package com.example.data.model

data class ExtractedWordFrequency(
    val normalizedKey: String,
    val displayWord: String,
    val count: Int,
    val sampleSentence: String
)

object ConversationVocabularyExtractor {

    private val stopWords = setOf(
        "the", "a", "an", "i", "me", "my", "mine", "you", "your", "yours",
        "he", "him", "his", "she", "her", "hers", "it", "its", "we", "us",
        "our", "ours", "they", "them", "their", "theirs",
        "am", "is", "are", "was", "were", "be", "been", "being",
        "do", "does", "did", "doing", "done",
        "have", "has", "had", "having",
        "will", "would", "can", "could", "shall", "should", "may", "might", "must",
        "to", "of", "in", "on", "at", "by", "for", "with", "from", "as", "into", "about",
        "and", "or", "but", "so", "not", "no", "yes", "ok", "okay",
        "this", "that", "these", "those", "there", "here",
        "what", "where", "when", "who", "whom", "whose", "which", "why", "how",
        "if", "then", "than", "very", "too", "just", "only", "also", "more", "some", "any",
        "all", "one", "two", "up", "out", "now",
        "voice", "recording", "message"
    )

    private val allowedTwoLetterWords = setOf("hi", "go")

    private val wordRegex = Regex("[A-Za-z]+(?:'[A-Za-z]+)?")

    fun extractFrequentWords(
        texts: List<String>,
        minFrequency: Int = 1,
        limit: Int = 12
    ): List<ExtractedWordFrequency> {
        if (texts.isEmpty() || limit <= 0) return emptyList()

        val counts = linkedMapOf<String, Int>()
        val sampleSentences = linkedMapOf<String, String>()

        for (rawText in texts) {
            val trimmed = rawText.trim()
            if (trimmed.isBlank()) continue
            if (trimmed.equals("(Voice recording)", ignoreCase = true) ||
                trimmed.equals("(Voice message)", ignoreCase = true)
            ) {
                continue
            }

            val cleanSample = trimmed.replace(Regex("\\s+"), " ").take(200)
            for (match in wordRegex.findAll(trimmed)) {
                val normalized = normalizeToken(match.value) ?: continue
                counts[normalized] = (counts[normalized] ?: 0) + 1
                sampleSentences.putIfAbsent(normalized, cleanSample)
            }
        }

        return counts.entries
            .asSequence()
            .filter { it.value >= minFrequency }
            .sortedWith(
                compareByDescending<Map.Entry<String, Int>> { it.value }
                    .thenBy { it.key }
            )
            .take(limit)
            .map { (key, count) ->
                ExtractedWordFrequency(
                    normalizedKey = key,
                    displayWord = formatDisplayWord(key),
                    count = count,
                    sampleSentence = sampleSentences[key].orEmpty()
                )
            }
            .toList()
    }

    fun extractFrequentWordsFromTurns(
        turns: List<TutorTurn>,
        minFrequency: Int = 1,
        limit: Int = 12
    ): List<ExtractedWordFrequency> {
        val inputs = turns.mapNotNull { turn ->
            turn.userInput.trim().takeIf { it.isNotBlank() }
        }
        return extractFrequentWords(texts = inputs, minFrequency = minFrequency, limit = limit)
    }

    fun normalizeSingleWord(raw: String): String? = normalizeToken(raw)

    private fun normalizeToken(rawToken: String): String? {
        var token = rawToken.trim().lowercase()
        if (token.isBlank()) return null

        // Strip common English contraction / possessive endings for cleaner base word matching
        val apostropheIndex = token.indexOf('\'')
        if (apostropheIndex > 0) {
            val suffix = token.substring(apostropheIndex)
            token = when (suffix) {
                "'s", "'m", "'re", "'ve", "'ll", "'d" -> token.substring(0, apostropheIndex)
                "'t" -> if (token.endsWith("n't") && token.length > 3) {
                    token.dropLast(3)
                } else {
                    token.substring(0, apostropheIndex)
                }
                else -> token.substring(0, apostropheIndex)
            }
        }

        if (token.isBlank()) return null
        if (!token.all { it in 'a'..'z' }) return null
        if (token in stopWords) return null
        if (token.length < 3 && token !in allowedTwoLetterWords) return null
        return token
    }

    private fun formatDisplayWord(normalized: String): String {
        return normalized.replaceFirstChar { ch ->
            if (ch.isLowerCase()) ch.titlecase() else ch.toString()
        }
    }
}
