package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.UUID

class LearningRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun ensureAndCompleteLesson_validUser_updatesProgress() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = LearningRepository(firestore, auth)

        val initResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.ensureUserProgress("Alice")
        }
        assertTrue(initResult.isSuccess)

        val completeResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.completeLesson("lesson_greetings_${UUID.randomUUID()}", 50L, "Alice")
        }
        assertTrue(completeResult.isSuccess)

        val progress = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeUserProgress(uid).first { it != null && it.xp >= 50L }
        }
        assertNotNull(progress)
        assertEquals(uid, progress!!.userId)
    }

    @Test
    fun addAndObserveVocab_authenticatedOwner_succeedsAndDeletes() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = LearningRepository(firestore, auth)
        val word = "Good morning ${UUID.randomUUID().toString().take(6)}"

        val addResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.addVocabItem(
                english = word,
                arabic = "صباح الخير",
                phoneticArabic = "غود مورنينغ",
                exampleEnglish = "Good morning, teacher!",
                category = "Greetings"
            )
        }
        assertTrue(addResult.isSuccess)
        val wordId = addResult.getOrThrow()

        val emitted = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeVocab(uid).first { list -> list.any { it.id == wordId } }
        }
        val savedItem = emitted.first { it.id == wordId }
        assertEquals(word, savedItem.english)
        assertTrue(savedItem.phoneticEnglish.isNotBlank())

        val updatePhoneticsResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.updateVocabPhonetics(
                wordId = wordId,
                phoneticEnglish = "/ɡʊd ˈmɔːrnɪŋ/",
                phoneticArabic = "غود مورنينغ"
            )
        }
        assertTrue(updatePhoneticsResult.isSuccess)

        val toggleResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.toggleVocabMastered(wordId, true)
        }
        assertTrue(toggleResult.isSuccess)

        val deleteResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.deleteVocabItem(wordId)
        }
        assertTrue(deleteResult.isSuccess)
    }

    @Test
    fun upsertConversationVocab_createsAndIncrementsUsageCount() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = LearningRepository(firestore, auth)
        val word = "Coffee_${UUID.randomUUID().toString().take(5)}"

        val firstId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.upsertConversationVocab(
                english = word,
                arabic = "قهوة",
                phoneticArabic = "كوفي",
                exampleEnglish = "I like coffee.",
                category = "Conversation",
                countIncrement = 2
            ).getOrThrow()
        }

        val secondId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.upsertConversationVocab(
                english = word.lowercase(),
                arabic = "قهوة",
                phoneticArabic = "كوفي",
                exampleEnglish = "I like coffee.",
                category = "Conversation",
                countIncrement = 3
            ).getOrThrow()
        }

        assertEquals(firstId, secondId)

        val stored = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeVocab(uid).first { list ->
                list.any { it.id == firstId && it.usageCount == 5 }
            }
        }
        val item = stored.first { it.id == firstId }
        assertEquals(5, item.usageCount)
        assertEquals("Conversation", item.category)
    }

    @Test
    fun saveTutorTurn_authenticatedOwner_succeeds() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = LearningRepository(firestore, auth)
        val input = "Hello ${UUID.randomUUID().toString().take(6)}"

        val saveResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.saveTutorTurn(
                mode = "written",
                userInput = input,
                englishReply = "Hello! Nice to meet you.",
                arabicTranslation = "مرحباً! سررت بلقائك.",
                feedbackArabic = "تحية ممتازة!",
                phoneticHint = "هيلو، نايس تو ميت يو"
            )
        }
        assertTrue("saveTutorTurn failed: ${saveResult.exceptionOrNull()}", saveResult.isSuccess)
        val turnId = saveResult.getOrThrow()

        val turns = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeTutorTurns(uid).first { list -> list.any { it.id == turnId } }
        }
        assertTrue(turns.any { it.id == turnId && it.mode == "written" })

        val historyResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.getChatHistory(uid, "written")
        }
        assertTrue("getChatHistory failed: ${historyResult.exceptionOrNull()}", historyResult.isSuccess)
        assertTrue(historyResult.getOrThrow().any { it.id == turnId })

        val updateLessonResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.updateLastLesson("greetings", "Alice")
        }
        assertTrue("updateLastLesson failed: ${updateLessonResult.exceptionOrNull()}", updateLessonResult.isSuccess)

        val updateGoalResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.updateDailyGoal(20, "Alice")
        }
        assertTrue("updateDailyGoal failed: ${updateGoalResult.exceptionOrNull()}", updateGoalResult.isSuccess)

        val streakResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.recordDailyUsage("Alice")
        }
        assertTrue("recordDailyUsage failed: ${streakResult.exceptionOrNull()}", streakResult.isSuccess)
        assertTrue((streakResult.getOrNull() ?: 0) >= 1)

        val studyGoalResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.updateDailyStudyGoalMinutes(30, "Alice")
        }
        assertTrue("updateDailyStudyGoalMinutes failed: ${studyGoalResult.exceptionOrNull()}", studyGoalResult.isSuccess)

        val studyTimeResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.saveDailyStudySeconds(450, "2026-11-13", "Alice")
        }
        assertTrue("saveDailyStudySeconds failed: ${studyTimeResult.exceptionOrNull()}", studyTimeResult.isSuccess)

        val reminderResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.updateReminderSettings(enabled = true, hour = 20, minute = 45, displayName = "Alice")
        }
        assertTrue("updateReminderSettings failed: ${reminderResult.exceptionOrNull()}", reminderResult.isSuccess)

        val quizResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.recordDailyQuizResult(
                score = 4,
                quizDate = "2026-11-13",
                xpEarned = 40L,
                displayName = "Alice"
            )
        }
        assertTrue("recordDailyQuizResult failed: ${quizResult.exceptionOrNull()}", quizResult.isSuccess)

        val updatedProgress = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeUserProgress(uid).first {
                it != null &&
                    it.dailyGoal == 20 &&
                    it.currentStreak >= 1 &&
                    it.dailyStudyMinutesGoal == 30 &&
                    it.todayStudySeconds == 450 &&
                    it.reminderEnabled &&
                    it.reminderHour == 20 &&
                    it.reminderMinute == 45 &&
                    it.lastDailyQuizDate == "2026-11-13" &&
                    it.lastDailyQuizScore == 4
            }
        }
        assertEquals(20, updatedProgress?.dailyGoal)
        assertTrue((updatedProgress?.currentStreak ?: 0) >= 1)
        assertEquals(30, updatedProgress?.dailyStudyMinutesGoal)
        assertEquals(450, updatedProgress?.todayStudySeconds)
        assertEquals("2026-11-13", updatedProgress?.lastStudyDate)
        assertTrue(updatedProgress?.reminderEnabled == true)
        assertEquals(20, updatedProgress?.reminderHour)
        assertEquals(45, updatedProgress?.reminderMinute)
        assertEquals("2026-11-13", updatedProgress?.lastDailyQuizDate)
        assertEquals(4, updatedProgress?.lastDailyQuizScore)

        val clearResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.clearTutorTurns("written")
        }
        assertTrue("clearTutorTurns failed: ${clearResult.exceptionOrNull()}", clearResult.isSuccess)
    }

    @Test
    fun observeVocab_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = LearningRepository(firestore, auth)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.addVocabItem(
                english = "Water",
                arabic = "ماء",
                phoneticArabic = "ووتر",
                exampleEnglish = "Can I have some water?",
                category = "Cafe"
            ).getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = LearningRepository(firestore, auth)
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                bobRepo.observeVocab(aliceUid).first()
            }
            fail("Expected PERMISSION_DENIED for cross-user vocab read")
        } catch (e: Exception) {
            val firestoreException = checkNotNull(e.unwrapFirestoreException()) {
                "Expected FirebaseFirestoreException, got $e"
            }
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException.code)
        }
    }

    @Test
    fun observeVocab_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repository = LearningRepository(firestore, auth)

        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeVocab("some_user_123").first()
            }
            fail("Expected PERMISSION_DENIED for unauthenticated vocab read")
        } catch (e: Exception) {
            val firestoreException = checkNotNull(e.unwrapFirestoreException()) {
                "Expected FirebaseFirestoreException, got $e"
            }
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException.code)
        }
    }

    private companion object {
        const val ALICE_EMAIL = "alice_noor@test.com"
        const val BOB_EMAIL = "bob_noor@test.com"
        const val DEFAULT_TIMEOUT_MS = 10000L
        const val FLOW_TIMEOUT_MS = 8000L
    }
}
