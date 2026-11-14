package com.example.data.model

data class PhoneticSpelling(
    val phoneticEnglish: String,
    val phoneticArabic: String
)

object PronunciationHelper {

    private val curatedDictionary: Map<String, PhoneticSpelling> = mapOf(
        "hello" to PhoneticSpelling("/həˈloʊ/ • heh-LOH", "هيلو"),
        "hi" to PhoneticSpelling("/haɪ/ • HY", "هاي"),
        "morning" to PhoneticSpelling("/ˈmɔːrnɪŋ/ • MAWR-ning", "مورنينغ"),
        "good morning" to PhoneticSpelling("/ɡʊd ˈmɔːrnɪŋ/ • gud MAWR-ning", "غود مورنينغ"),
        "evening" to PhoneticSpelling("/ˈiːvnɪŋ/ • EEV-ning", "إيفنينغ"),
        "night" to PhoneticSpelling("/naɪt/ • NYT", "نايت"),
        "please" to PhoneticSpelling("/pliːz/ • PLEEZ", "پليز"),
        "thank" to PhoneticSpelling("/θæŋk/ • THANK", "ثانك"),
        "thank you" to PhoneticSpelling("/ˈθæŋk juː/ • THANK yoo", "ثانك يو"),
        "coffee" to PhoneticSpelling("/ˈkɒfi/ • KAW-fee", "كوفي"),
        "water" to PhoneticSpelling("/ˈwɔːtər/ • WAW-ter", "ووتر"),
        "tea" to PhoneticSpelling("/tiː/ • TEE", "تي"),
        "teacher" to PhoneticSpelling("/ˈtiːtʃər/ • TEE-cher", "تيتشر"),
        "student" to PhoneticSpelling("/ˈstuːdənt/ • STOO-duhnt", "ستودنت"),
        "friend" to PhoneticSpelling("/frend/ • FREND", "فريند"),
        "family" to PhoneticSpelling("/ˈfæməli/ • FAM-uh-lee", "فاميلي"),
        "book" to PhoneticSpelling("/bʊk/ • BUK", "بوك"),
        "happy" to PhoneticSpelling("/ˈhæpi/ • HAP-ee", "هاپي"),
        "today" to PhoneticSpelling("/təˈdeɪ/ • tuh-DAY", "توداي"),
        "tomorrow" to PhoneticSpelling("/təˈmɒroʊ/ • tuh-MOR-oh", "تومورو"),
        "practice" to PhoneticSpelling("/ˈpræktɪs/ • PRAK-tis", "پراكتيس"),
        "pronunciation" to PhoneticSpelling("/prəˌnʌnsiˈeɪʃən/ • pruh-nun-see-AY-shuhn", "پرونانسييشن"),
        "schedule" to PhoneticSpelling("/ˈskedʒuːl/ • SKEJ-ool", "سكيدجول"),
        "english" to PhoneticSpelling("/ˈɪŋɡlɪʃ/ • ING-glish", "إينغليش"),
        "learn" to PhoneticSpelling("/lɜːrn/ • LURN", "ليرن"),
        "speak" to PhoneticSpelling("/spiːk/ • SPEEK", "سپيك"),
        "write" to PhoneticSpelling("/raɪt/ • RYT", "رايت"),
        "listen" to PhoneticSpelling("/ˈlɪsən/ • LIS-uhn", "ليسن"),
        "read" to PhoneticSpelling("/riːd/ • REED", "ريد"),
        "vocabulary" to PhoneticSpelling("/voʊˈkæbjəleri/ • voh-KAB-yuh-ler-ee", "فوكابيوليري"),
        "breakfast" to PhoneticSpelling("/ˈbrekfəst/ • BREK-fuhst", "بريكفاست"),
        "village" to PhoneticSpelling("/ˈvɪlɪdʒ/ • VIL-ij", "فيليدج"),
        "paper" to PhoneticSpelling("/ˈpeɪpər/ • PAY-per", "پيپر"),
        "people" to PhoneticSpelling("/ˈpiːpəl/ • PEE-puhl", "پيپل"),
        "welcome" to PhoneticSpelling("/ˈwelkəm/ • WEL-kuhm", "ويلكم"),
        "excuse me" to PhoneticSpelling("/ɪkˈskjuːz miː/ • ik-SKYOOZ mee", "إكسكيوز مي"),
        "nice to meet you" to PhoneticSpelling("/naɪs tuː miːt juː/ • NYS too meet yoo", "نايس تو ميت يو")
    )

    fun resolvePhonetics(
        english: String,
        existingPhoneticEnglish: String = "",
        existingPhoneticArabic: String = ""
    ): PhoneticSpelling {
        val cleanEnglish = english.trim()
        val key = cleanEnglish.lowercase().replace(Regex("[.!?]+$"), "").trim()
        val curated = curatedDictionary[key]

        val resolvedEn = existingPhoneticEnglish.trim()
            .ifBlank { curated?.phoneticEnglish ?: buildEnglishSyllableGuide(cleanEnglish) }

        val resolvedAr = existingPhoneticArabic.trim()
            .ifBlank { curated?.phoneticArabic ?: transliterateWordToArabicScript(cleanEnglish) }

        return PhoneticSpelling(
            phoneticEnglish = resolvedEn,
            phoneticArabic = resolvedAr
        )
    }

    fun buildEnglishSyllableGuide(english: String): String {
        val words = english.trim()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
        if (words.isEmpty()) return ""

        val simplified = words.joinToString(" ") { rawWord ->
            val clean = rawWord.replace(Regex("[^A-Za-z']"), "")
            if (clean.isEmpty()) return@joinToString rawWord
            val normalized = clean.lowercase()
                .replace("tion", "-shuhn")
                .replace("sion", "-zhuhn")
                .replace("igh", "y")
                .replace("ph", "f")
                .replace("ee", "ee")
                .replace("oo", "oo")
                .replace("ou", "ow")
                .replace("ch", "ch")
                .replace("sh", "sh")
                .replace("th", "th")
            val chunks = splitIntoBeginnerSyllables(normalized)
            if (chunks.size > 1) {
                chunks.mapIndexed { idx, part ->
                    if (idx == 0) part.uppercase() else part
                }.joinToString("-")
            } else {
                normalized.uppercase()
            }
        }
        return simplified.take(120)
    }

    fun transliterateWordToArabicScript(english: String): String {
        val words = english.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        if (words.isEmpty()) return ""
        return words.joinToString(" ") { transliterateSingleWord(it) }.take(120)
    }

    private fun transliterateSingleWord(raw: String): String {
        val lower = raw.lowercase().replace(Regex("[^a-z]"), "")
        if (lower.isEmpty()) return ""
        curatedDictionary[lower]?.let { return it.phoneticArabic }

        val sb = StringBuilder()
        var i = 0
        while (i < lower.length) {
            val remaining = lower.substring(i)
            when {
                remaining.startsWith("tion") -> { sb.append("شن"); i += 4 }
                remaining.startsWith("sion") -> { sb.append("جن"); i += 4 }
                remaining.startsWith("sh") -> { sb.append("ش"); i += 2 }
                remaining.startsWith("ch") -> { sb.append("تش"); i += 2 }
                remaining.startsWith("th") -> { sb.append("ث"); i += 2 }
                remaining.startsWith("ph") -> { sb.append("ف"); i += 2 }
                remaining.startsWith("gh") && i + 2 == lower.length -> { i += 2 }
                remaining.startsWith("qu") -> { sb.append("كو"); i += 2 }
                remaining.startsWith("ee") || remaining.startsWith("ea") -> { sb.append("ي"); i += 2 }
                remaining.startsWith("oo") -> { sb.append("و"); i += 2 }
                remaining.startsWith("ai") || remaining.startsWith("ay") -> { sb.append("اي"); i += 2 }
                remaining.startsWith("ou") || remaining.startsWith("ow") -> { sb.append("او"); i += 2 }
                remaining.startsWith("ck") -> { sb.append("ك"); i += 2 }
                else -> {
                    val ch = lower[i]
                    val next = lower.getOrNull(i + 1)
                    val atEnd = i == lower.lastIndex
                    sb.append(
                        when (ch) {
                            'a' -> if (i == 0) "آ" else "ا"
                            'b' -> "ب"
                            'c' -> if (next in listOf('e', 'i', 'y')) "س" else "ك"
                            'd' -> "د"
                            'e' -> when {
                                atEnd && lower.length > 2 -> ""
                                i == 0 -> "إي"
                                else -> "ي"
                            }
                            'f' -> "ف"
                            'g' -> if (next in listOf('e', 'i', 'y')) "ج" else "غ"
                            'h' -> "ه"
                            'i' -> if (i == 0) "إي" else "ي"
                            'j' -> "ج"
                            'k' -> "ك"
                            'l' -> "ل"
                            'm' -> "م"
                            'n' -> "ن"
                            'o' -> "و"
                            'p' -> "پ"
                            'q' -> "ك"
                            'r' -> "ر"
                            's' -> "س"
                            't' -> "ت"
                            'u' -> if (i == 0) "أ" else "و"
                            'v' -> "ڤ"
                            'w' -> "و"
                            'x' -> "كس"
                            'y' -> if (i == 0) "ي" else "ي"
                            'z' -> "ز"
                            else -> ""
                        }
                    )
                    i += 1
                }
            }
        }
        return sb.toString()
    }

    private fun splitIntoBeginnerSyllables(word: String): List<String> {
        if (word.length <= 4) return listOf(word)
        val vowels = setOf('a', 'e', 'i', 'o', 'u', 'y')
        val syllables = mutableListOf<String>()
        val current = StringBuilder()

        for (i in word.indices) {
            val ch = word[i]
            if (ch == '-') {
                if (current.isNotEmpty()) {
                    syllables.add(current.toString())
                    current.clear()
                }
                continue
            }
            current.append(ch)
            if (ch in vowels && i + 2 < word.length && word[i + 1] !in vowels && word[i + 2] in vowels) {
                syllables.add(current.toString())
                current.clear()
            }
        }
        if (current.isNotEmpty()) {
            if (syllables.isNotEmpty() && current.length == 1 && current[0] !in vowels) {
                syllables[syllables.lastIndex] = syllables.last() + current.toString()
            } else {
                syllables.add(current.toString())
            }
        }
        return syllables
    }
}
