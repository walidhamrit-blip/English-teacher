package com.example.data.repository

import android.util.Log
import com.example.data.model.SuggestedPhrase
import com.example.data.model.TutorResponse
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

class AiTutorRepository {

    private val tutorResponseSchema = Schema.obj(
        properties = mapOf(
            "whatYouSaid" to Schema.string(
                description = "Exact transcription of what the student said in English or Arabic. For written mode, echo the user input concisely."
            ),
            "englishReply" to Schema.string(
                description = "Your friendly beginner-level (A1-A2) English response as their English teacher. Keep sentences short, warm, and encouraging, and end with a simple follow-up question in English."
            ),
            "arabicTranslation" to Schema.string(
                description = "Natural, clear Arabic translation of your englishReply so the Arabic-speaking beginner understands every word."
            ),
            "feedbackArabic" to Schema.string(
                description = "Helpful, encouraging feedback in Arabic! Point out any grammar, spelling, word order, or Arabic-speaker pronunciation tips (such as P vs B, V vs F, missing am/is/are, or a/an) gently and clearly."
            ),
            "phoneticHint" to Schema.string(
                description = "Approximate Arabic-script pronunciation of key English words in your reply (e.g. هيلو، نايس تو ميت يو)."
            ),
            "suggestedPhrases" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "english" to Schema.string(description = "Useful English word or short phrase for the student to learn or reply with"),
                        "arabic" to Schema.string(description = "Arabic meaning"),
                        "phoneticArabic" to Schema.string(description = "Approximate pronunciation in Arabic letters"),
                        "phoneticEnglish" to Schema.string(description = "IPA and syllable phonetic spelling, e.g. /ˈmɔːrnɪŋ/ • MAWR-ning"),
                        "exampleEnglish" to Schema.string(description = "Short beginner English example sentence")
                    ),
                    optionalProperties = listOf("phoneticEnglish", "exampleEnglish")
                ),
                description = "1 to 3 helpful English phrases or reply ideas for the beginner"
            )
        ),
        optionalProperties = listOf("whatYouSaid")
    )

    private val vocabLookupSchema = Schema.obj(
        properties = mapOf(
            "english" to Schema.string(description = "Clean English word or short phrase"),
            "arabic" to Schema.string(description = "Clear Arabic translation"),
            "phoneticArabic" to Schema.string(description = "Pronunciation written clearly in vocalized Arabic letters for Arabic speakers (marking P as پ and V as ڤ)"),
            "phoneticEnglish" to Schema.string(description = "IPA transcription and hyphenated beginner syllable spelling, e.g. /ˈkɒfi/ • KAW-fee"),
            "exampleEnglish" to Schema.string(description = "Simple A1 beginner English sentence using the word")
        )
    )

    private val systemPrompt = content(role = "system") {
        text(
            """
            ROLE & IDENTITY (MANDATORY):
            You MUST ALWAYS act strictly as Noor (نور), a patient, encouraging, and expert English teacher specifically for complete beginners (CEFR A1-A2) of Arabic origin.
            Never drop this teaching persona. Every response must help an Arabic speaker learn English step by step.

            PEDAGOGICAL & ARABIC EXPLANATION RULES:
            1. Understand beginner English, Arabic, Arabizi, or mixed Arabic-English inputs kindly.
            2. Keep englishReply at simple A1-A2 beginner English: short everyday sentences, clear vocabulary, and end with one easy follow-up question in English.
            3. Always translate your entire englishReply into clear, natural Arabic in arabicTranslation.
            4. Explain grammar and vocabulary in Arabic whenever needed:
               - In feedbackArabic, always use clear Arabic to explain any grammar rule, word choice, spelling, or vocabulary meaning the student needs.
               - Proactively address common Arabic-to-English interference errors in Arabic (such as omitting am/is/are in present sentences, missing a/an indefinite articles, Arabic-to-English word order, and P/B, V/F, or TH pronunciation in spoken mode).
               - When the student asks "what does X mean?" or asks about a rule in Arabic or English, explain the vocabulary/grammar thoroughly in Arabic with simple examples, and if their input is already correct, praise them in Arabic and highlight one useful vocabulary or grammar takeaway.
            5. In phoneticHint, write an easy-to-read Arabic-script pronunciation guide for the key phrase in englishReply.
            6. In suggestedPhrases, provide 2 practical beginner words or reply phrases with Arabic meaning and Arabic phonetic guide.
            """.trimIndent()
        )
    }

    private val vocabSystemPrompt = content(role = "system") {
        text(
            """
            You are an English teacher specifically for Arabic-speaking beginners (A1-A2).
            When given any English or Arabic word or short phrase, ALWAYS provide:
            1. Clean English word or short phrase.
            2. Clear Arabic translation.
            3. phoneticEnglish: IPA transcription plus easy beginner syllable spelling (e.g. "/ˈkɒfi/ • KAW-fee").
            4. phoneticArabic: Clear Arabic-script pronunciation tailored for Arabic speakers (use پ for P and ڤ for V when helpful).
            5. A simple A1 beginner English example sentence using the word.
            """.trimIndent()
        )
    }

    private val tutorModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = tutorResponseSchema
                temperature = 0.6f
            },
            systemInstruction = systemPrompt
        )
    }

    private val vocabModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.5-flash-lite",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = vocabLookupSchema
                temperature = 0.2f
            },
            systemInstruction = vocabSystemPrompt
        )
    }

    suspend fun sendWrittenTurn(
        userMessage: String,
        recentHistory: List<Content>,
        lessonTopic: String?,
        isSpokenPractice: Boolean = false
    ): Result<TutorResponse> {
        return try {
            val chat = tutorModel.startChat(history = recentHistory)
            val modeLabel = if (isSpokenPractice) {
                "Chat Spoken Pronunciation Practice (Speech-to-Text)"
            } else {
                "Written English Practice"
            }
            val contextPrefix = if (!lessonTopic.isNullOrBlank()) {
                "[Mode: $modeLabel | Current Lesson Topic: $lessonTopic]\n"
            } else {
                "[Mode: $modeLabel]\n"
            }
            val spokenNote = if (isSpokenPractice) {
                " (Note: The student practiced speaking this aloud via Speech-to-Text; evaluate grammar, natural phrasing, and give Arabic pronunciation tips for key words or common Arabic-speaker sounds like P/B, V/F, TH.)"
            } else {
                ""
            }
            val response = withTimeout(60_000L) {
                chat.sendMessage("$contextPrefix Student input$spokenNote: $userMessage")
            }
            val jsonStr = response.text
                ?: return Result.failure(IllegalStateException("AI tutor returned an empty response."))
            Result.success(parseTutorResponse(jsonStr, fallbackSaid = userMessage))
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Written tutor request timed out", e)
            Result.failure(Exception("The tutor took too long to respond. Please try again."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Written tutor generation failed", e)
            Result.failure(e)
        }
    }

    suspend fun sendVocalAudioTurn(
        audioBytes: ByteArray,
        mimeType: String,
        recentHistory: List<Content>,
        lessonTopic: String?
    ): Result<TutorResponse> {
        return try {
            val chat = tutorModel.startChat(history = recentHistory)
            val contextNote = if (!lessonTopic.isNullOrBlank()) {
                "Focus topic: $lessonTopic. "
            } else {
                ""
            }
            val promptContent = content {
                inlineData(audioBytes, mimeType)
                text(
                    "[Mode: Vocal Speaking Practice] ${contextNote}Listen carefully to the Arabic-origin beginner student's voice recording. " +
                        "Transcribe what they said in whatYouSaid (whether English, Arabic, or mixed). " +
                        "In feedbackArabic, give warm, specific pronunciation feedback in Arabic (checking clarity, P vs B, V vs F, TH, vowels, and grammar) and tell them how to improve."
                )
            }
            val response = withTimeout(60_000L) {
                chat.sendMessage(promptContent)
            }
            val jsonStr = response.text
                ?: return Result.failure(IllegalStateException("AI tutor returned an empty response."))
            Result.success(parseTutorResponse(jsonStr, fallbackSaid = "(Voice message)"))
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Vocal tutor audio request timed out", e)
            Result.failure(Exception("Voice analysis timed out. Please try a shorter recording."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Vocal tutor audio generation failed", e)
            Result.failure(e)
        }
    }

    suspend fun sendVocalTextPracticeTurn(
        spokenPrompt: String,
        recentHistory: List<Content>,
        lessonTopic: String?
    ): Result<TutorResponse> {
        return try {
            val chat = tutorModel.startChat(history = recentHistory)
            val contextNote = if (!lessonTopic.isNullOrBlank()) "Lesson: $lessonTopic. " else ""
            val response = withTimeout(60_000L) {
                chat.sendMessage(
                    "[Mode: Vocal Speaking Practice] ${contextNote}The student practiced saying: \"$spokenPrompt\". " +
                        "Give pronunciation tips in Arabic for this sentence and continue the spoken conversation."
                )
            }
            val jsonStr = response.text
                ?: return Result.failure(IllegalStateException("AI tutor returned an empty response."))
            Result.success(parseTutorResponse(jsonStr, fallbackSaid = spokenPrompt))
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Vocal text turn timed out", e)
            Result.failure(Exception("Voice tutor timed out. Please try again."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Vocal text turn failed", e)
            Result.failure(e)
        }
    }

    suspend fun translateWordForWordbook(inputWord: String): Result<SuggestedPhrase> {
        return try {
            val response = withTimeout(45_000L) {
                vocabModel.generateContent(
                    "Translate and explain this word or short phrase for an Arabic beginner learning English (input could be English or Arabic): \"$inputWord\""
                )
            }
            val jsonStr = response.text
                ?: return Result.failure(IllegalStateException("Empty translation response."))
            val json = JSONObject(jsonStr)
            val resolvedEnglish = json.optString("english", inputWord).trim().ifBlank { inputWord }
            val resolvedPhonetics = com.example.data.model.PronunciationHelper.resolvePhonetics(
                english = resolvedEnglish,
                existingPhoneticEnglish = json.optString("phoneticEnglish", "").trim(),
                existingPhoneticArabic = json.optString("phoneticArabic", "").trim()
            )
            Result.success(
                SuggestedPhrase(
                    english = resolvedEnglish,
                    arabic = json.optString("arabic", ""),
                    phoneticArabic = resolvedPhonetics.phoneticArabic,
                    exampleEnglish = json.optString("exampleEnglish", ""),
                    phoneticEnglish = resolvedPhonetics.phoneticEnglish
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Word translation timed out", e)
            Result.failure(Exception("Translation timed out. Please try again."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Word translation failed", e)
            Result.failure(e)
        }
    }

    private fun parseTutorResponse(jsonString: String, fallbackSaid: String): TutorResponse {
        val json = JSONObject(jsonString)
        val phrasesArray = json.optJSONArray("suggestedPhrases")
        val phrases = buildList {
            if (phrasesArray != null) {
                for (i in 0 until phrasesArray.length()) {
                    val obj = phrasesArray.optJSONObject(i) ?: continue
                    val en = obj.optString("english", "").trim()
                    val ar = obj.optString("arabic", "").trim()
                    if (en.isNotEmpty() && ar.isNotEmpty()) {
                        val resolvedPhonetics = com.example.data.model.PronunciationHelper.resolvePhonetics(
                            english = en,
                            existingPhoneticEnglish = obj.optString("phoneticEnglish", "").trim(),
                            existingPhoneticArabic = obj.optString("phoneticArabic", "").trim()
                        )
                        add(
                            SuggestedPhrase(
                                english = en,
                                arabic = ar,
                                phoneticArabic = resolvedPhonetics.phoneticArabic,
                                exampleEnglish = obj.optString("exampleEnglish", "").trim(),
                                phoneticEnglish = resolvedPhonetics.phoneticEnglish
                            )
                        )
                    }
                }
            }
        }
        return TutorResponse(
            whatYouSaid = json.optString("whatYouSaid", fallbackSaid).ifBlank { fallbackSaid },
            englishReply = json.optString("englishReply", "Hello! Let's practice English together."),
            arabicTranslation = json.optString("arabicTranslation", ""),
            feedbackArabic = json.optString("feedbackArabic", ""),
            phoneticHint = json.optString("phoneticHint", ""),
            suggestedPhrases = phrases
        )
    }

    private companion object {
        const val TAG = "AiTutorRepository"
    }
}
