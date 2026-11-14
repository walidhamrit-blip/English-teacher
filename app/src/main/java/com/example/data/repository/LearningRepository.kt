package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.core.OperationType
import com.example.core.handleFirestoreError
import com.example.data.model.PronunciationHelper
import com.example.data.model.TutorTurn
import com.example.data.model.UserProgress
import com.example.data.model.VocabItem
import com.example.data.model.computeUpdatedStreak
import com.example.data.model.formatDayDateKey
import com.example.data.model.yesterdayDateKey
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class LearningRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        db = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        auth = Firebase.auth
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun observeUserProgress(userId: String): Flow<UserProgress?> =
        db.collection("users").document(userId)
            .snapshots()
            .map { snapshot ->
                if (!snapshot.exists()) {
                    null
                } else {
                    snapshot.toObject(
                        UserProgress::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val exceptionToLog = firestoreError ?: (error as? Exception)
                if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                    handleFirestoreError(exceptionToLog, OperationType.GET, "users/$userId")
                }
                throw error
            }

    suspend fun ensureUserProgress(displayName: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        val cleanName = displayName.trim().take(80).ifBlank { "English Learner" }
        try {
            val snap = docRef.get().await()
            if (!snap.exists()) {
                val payload = mapOf(
                    "userId" to uid,
                    "displayName" to cleanName,
                    "xp" to 0L,
                    "completedLessonIds" to emptyList<String>(),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    suspend fun completeLesson(lessonId: String, xpReward: Long, displayName: String): Result<Unit> =
        runCatching {
            val uid = requireUserId()
            val docRef = db.collection("users").document(uid)
            val cleanName = displayName.trim().take(80).ifBlank { "English Learner" }
            try {
                val snap = docRef.get().await()
                if (!snap.exists()) {
                    val payload = mapOf(
                        "userId" to uid,
                        "displayName" to cleanName,
                        "xp" to xpReward.coerceAtLeast(0L),
                        "completedLessonIds" to listOf(lessonId),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    docRef.set(payload).await()
                } else {
                    val existing = snap.toObject(UserProgress::class.java)
                    val currentIds = existing?.completedLessonIds.orEmpty()
                    val updatedIds = if (currentIds.contains(lessonId)) {
                        currentIds
                    } else {
                        (currentIds + lessonId).take(30)
                    }
                    val addedXp = if (currentIds.contains(lessonId)) 0L else xpReward.coerceAtLeast(0L)
                    val newXp = ((existing?.xp ?: 0L) + addedXp).coerceIn(0L, 1_000_000L)
                    docRef.update(
                        mapOf(
                            "xp" to newXp,
                            "completedLessonIds" to updatedIds,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.UPDATE, docRef.path)
                throw e
            }
        }

    suspend fun awardPracticeXp(xpAmount: Long, displayName: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        val cleanName = displayName.trim().take(80).ifBlank { "English Learner" }
        try {
            val snap = docRef.get().await()
            if (!snap.exists()) {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "displayName" to cleanName,
                        "xp" to xpAmount.coerceAtLeast(0L),
                        "completedLessonIds" to emptyList<String>(),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                val currentXp = snap.getLong("xp") ?: 0L
                val newXp = (currentXp + xpAmount.coerceAtLeast(0L)).coerceIn(0L, 1_000_000L)
                docRef.update(
                    mapOf(
                        "xp" to newXp,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun updateLastLesson(lessonId: String, displayName: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        val cleanLessonId = lessonId.trim().take(64)
        val cleanName = displayName.trim().take(80).ifBlank { "English Learner" }
        try {
            val snap = docRef.get().await()
            if (!snap.exists()) {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "displayName" to cleanName,
                        "xp" to 0L,
                        "completedLessonIds" to emptyList<String>(),
                        "lastLessonId" to cleanLessonId,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.update(
                    mapOf(
                        "lastLessonId" to cleanLessonId,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun updateDailyGoal(goal: Int, displayName: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        val cleanGoal = goal.coerceIn(1, 100)
        val cleanName = displayName.trim().take(80).ifBlank { "English Learner" }
        try {
            val snap = docRef.get().await()
            if (!snap.exists()) {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "displayName" to cleanName,
                        "xp" to 0L,
                        "completedLessonIds" to emptyList<String>(),
                        "dailyGoal" to cleanGoal,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.update(
                    mapOf(
                        "dailyGoal" to cleanGoal,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun updateDailyStudyGoalMinutes(minutes: Int, displayName: String): Result<Unit> =
        runCatching {
            val uid = requireUserId()
            val docRef = db.collection("users").document(uid)
            val cleanMinutes = minutes.coerceIn(1, 180)
            val cleanName = displayName.trim().take(80).ifBlank { "English Learner" }
            try {
                val snap = docRef.get().await()
                if (!snap.exists()) {
                    docRef.set(
                        mapOf(
                            "userId" to uid,
                            "displayName" to cleanName,
                            "xp" to 0L,
                            "completedLessonIds" to emptyList<String>(),
                            "dailyStudyMinutesGoal" to cleanMinutes,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                } else {
                    docRef.update(
                        mapOf(
                            "dailyStudyMinutesGoal" to cleanMinutes,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.UPDATE, docRef.path)
                throw e
            }
        }

    suspend fun saveDailyStudySeconds(
        seconds: Int,
        studyDate: String,
        displayName: String
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        val cleanSeconds = seconds.coerceIn(0, 86400)
        val cleanDate = studyDate.trim().take(10)
        val cleanName = displayName.trim().take(80).ifBlank { "English Learner" }
        try {
            val snap = docRef.get().await()
            if (!snap.exists()) {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "displayName" to cleanName,
                        "xp" to 0L,
                        "completedLessonIds" to emptyList<String>(),
                        "todayStudySeconds" to cleanSeconds,
                        "lastStudyDate" to cleanDate,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.update(
                    mapOf(
                        "todayStudySeconds" to cleanSeconds,
                        "lastStudyDate" to cleanDate,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun updateReminderSettings(
        enabled: Boolean,
        hour: Int,
        minute: Int,
        displayName: String
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        val cleanHour = hour.coerceIn(0, 23)
        val cleanMinute = minute.coerceIn(0, 59)
        val cleanName = displayName.trim().take(80).ifBlank { "English Learner" }
        try {
            val snap = docRef.get().await()
            if (!snap.exists()) {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "displayName" to cleanName,
                        "xp" to 0L,
                        "completedLessonIds" to emptyList<String>(),
                        "reminderEnabled" to enabled,
                        "reminderHour" to cleanHour,
                        "reminderMinute" to cleanMinute,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.update(
                    mapOf(
                        "reminderEnabled" to enabled,
                        "reminderHour" to cleanHour,
                        "reminderMinute" to cleanMinute,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun recordDailyQuizResult(
        score: Int,
        quizDate: String,
        xpEarned: Long,
        displayName: String
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        val cleanScore = score.coerceIn(0, 5)
        val cleanDate = quizDate.trim().take(10)
        val cleanXp = xpEarned.coerceAtLeast(0L)
        val cleanName = displayName.trim().take(80).ifBlank { "English Learner" }
        try {
            val snap = docRef.get().await()
            if (!snap.exists()) {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "displayName" to cleanName,
                        "xp" to cleanXp,
                        "completedLessonIds" to emptyList<String>(),
                        "lastDailyQuizDate" to cleanDate,
                        "lastDailyQuizScore" to cleanScore,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                val currentXp = snap.getLong("xp") ?: 0L
                val newXp = (currentXp + cleanXp).coerceIn(0L, 1_000_000L)
                docRef.update(
                    mapOf(
                        "xp" to newXp,
                        "lastDailyQuizDate" to cleanDate,
                        "lastDailyQuizScore" to cleanScore,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun recordDailyUsage(
        displayName: String,
        nowMillis: Long = System.currentTimeMillis()
    ): Result<Int> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        val cleanName = displayName.trim().take(80).ifBlank { "English Learner" }
        val todayDate = formatDayDateKey(nowMillis)
        val yesterdayDate = yesterdayDateKey(nowMillis)
        try {
            val snap = docRef.get().await()
            if (!snap.exists()) {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "displayName" to cleanName,
                        "xp" to 0L,
                        "completedLessonIds" to emptyList<String>(),
                        "currentStreak" to 1,
                        "longestStreak" to 1,
                        "lastActiveDate" to todayDate,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                1
            } else {
                val existing = snap.toObject(UserProgress::class.java)
                val update = computeUpdatedStreak(
                    previousStreak = existing?.currentStreak ?: 0,
                    previousLongest = existing?.longestStreak ?: 0,
                    lastActiveDate = existing?.lastActiveDate.orEmpty(),
                    todayDate = todayDate,
                    yesterdayDate = yesterdayDate
                )
                if (update.changed) {
                    docRef.update(
                        mapOf(
                            "currentStreak" to update.currentStreak,
                            "longestStreak" to update.longestStreak,
                            "lastActiveDate" to update.lastActiveDate,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
                update.currentStreak
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    fun observeVocab(userId: String): Flow<List<VocabItem>> =
        db.collection("users").document(userId).collection("vocab")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    VocabItem::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val exceptionToLog = firestoreError ?: (error as? Exception)
                if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                    handleFirestoreError(exceptionToLog, OperationType.LIST, "users/$userId/vocab")
                }
                throw error
            }

    suspend fun addVocabItem(
        english: String,
        arabic: String,
        phoneticArabic: String,
        exampleEnglish: String,
        category: String,
        usageCount: Int = 1,
        phoneticEnglish: String = ""
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val colRef = db.collection("users").document(uid).collection("vocab")
        val docRef = colRef.document()
        val cleanEnglish = english.trim().take(120)
        val cleanArabic = arabic.trim().take(160).ifBlank { cleanEnglish }
        val resolvedPhonetics = PronunciationHelper.resolvePhonetics(
            english = cleanEnglish,
            existingPhoneticEnglish = phoneticEnglish,
            existingPhoneticArabic = phoneticArabic
        )
        val payload = mapOf(
            "userId" to uid,
            "english" to cleanEnglish,
            "arabic" to cleanArabic,
            "phoneticArabic" to resolvedPhonetics.phoneticArabic.take(160),
            "phoneticEnglish" to resolvedPhonetics.phoneticEnglish.take(160),
            "exampleEnglish" to exampleEnglish.trim().take(240),
            "category" to category.trim().ifBlank { "General" }.take(40),
            "mastered" to false,
            "usageCount" to usageCount.coerceIn(1, 10000),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun upsertConversationVocab(
        english: String,
        arabic: String,
        phoneticArabic: String,
        exampleEnglish: String,
        category: String = "Conversation",
        countIncrement: Int = 1,
        minimumTotalCount: Int? = null,
        phoneticEnglish: String = ""
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val cleanEnglish = english.trim().take(120)
        require(cleanEnglish.isNotBlank()) { "English word must not be blank" }
        val cleanArabic = arabic.trim().take(160).ifBlank { cleanEnglish }
        val resolvedPhonetics = PronunciationHelper.resolvePhonetics(
            english = cleanEnglish,
            existingPhoneticEnglish = phoneticEnglish,
            existingPhoneticArabic = phoneticArabic
        )
        val cleanPhonetic = resolvedPhonetics.phoneticArabic.take(160)
        val cleanPhoneticEn = resolvedPhonetics.phoneticEnglish.take(160)
        val cleanExample = exampleEnglish.trim().take(240)
        val cleanCategory = category.trim().ifBlank { "Conversation" }.take(40)

        val colRef = db.collection("users").document(uid).collection("vocab")
        try {
            val existingDocs = colRef.get().await().documents
            val match = existingDocs.firstOrNull { doc ->
                doc.getString("english")?.trim()?.equals(cleanEnglish, ignoreCase = true) == true
            }

            if (match != null) {
                val previousCount = (match.getLong("usageCount") ?: 1L).toInt().coerceIn(1, 10000)
                val targetCount = if (minimumTotalCount != null) {
                    maxOf(previousCount, minimumTotalCount)
                } else {
                    previousCount + countIncrement.coerceAtLeast(1)
                }.coerceIn(1, 10000)

                val updates = mutableMapOf<String, Any>(
                    "usageCount" to targetCount,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (match.getString("arabic").isNullOrBlank() && cleanArabic.isNotBlank()) {
                    updates["arabic"] = cleanArabic
                }
                if (match.getString("phoneticArabic").isNullOrBlank() && cleanPhonetic.isNotBlank()) {
                    updates["phoneticArabic"] = cleanPhonetic
                }
                if (match.getString("phoneticEnglish").isNullOrBlank() && cleanPhoneticEn.isNotBlank()) {
                    updates["phoneticEnglish"] = cleanPhoneticEn
                }
                if (match.getString("exampleEnglish").isNullOrBlank() && cleanExample.isNotBlank()) {
                    updates["exampleEnglish"] = cleanExample
                }
                match.reference.update(updates).await()
                match.id
            } else {
                val docRef = colRef.document()
                val initialCount = (minimumTotalCount ?: countIncrement).coerceIn(1, 10000)
                val payload = mapOf(
                    "userId" to uid,
                    "english" to cleanEnglish,
                    "arabic" to cleanArabic,
                    "phoneticArabic" to cleanPhonetic,
                    "phoneticEnglish" to cleanPhoneticEn,
                    "exampleEnglish" to cleanExample,
                    "category" to cleanCategory,
                    "mastered" to false,
                    "usageCount" to initialCount,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
                docRef.id
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, colRef.path)
            throw e
        }
    }

    suspend fun updateVocabPhonetics(
        wordId: String,
        phoneticEnglish: String,
        phoneticArabic: String
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        require(wordId.isNotBlank()) { "Word ID must not be blank" }
        val docRef = db.collection("users").document(uid).collection("vocab").document(wordId)
        try {
            docRef.update(
                mapOf(
                    "phoneticEnglish" to phoneticEnglish.trim().take(160),
                    "phoneticArabic" to phoneticArabic.trim().take(160),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun toggleVocabMastered(wordId: String, mastered: Boolean): Result<Unit> = runCatching {
        val uid = requireUserId()
        require(wordId.isNotBlank()) { "Word ID must not be blank" }
        val docRef = db.collection("users").document(uid).collection("vocab").document(wordId)
        try {
            docRef.update(
                mapOf(
                    "mastered" to mastered,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteVocabItem(wordId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        require(wordId.isNotBlank()) { "Word ID must not be blank" }
        val docRef = db.collection("users").document(uid).collection("vocab").document(wordId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    fun observeTutorTurns(userId: String): Flow<List<TutorTurn>> =
        db.collection("users").document(userId).collection("tutor_turns")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    TutorTurn::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val exceptionToLog = firestoreError ?: (error as? Exception)
                if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                    handleFirestoreError(
                        exceptionToLog,
                        OperationType.LIST,
                        "users/$userId/tutor_turns"
                    )
                }
                throw error
            }

    suspend fun saveTutorTurn(
        mode: String,
        userInput: String,
        englishReply: String,
        arabicTranslation: String,
        feedbackArabic: String,
        phoneticHint: String
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("tutor_turns").document()
        val cleanMode = if (mode == "vocal") "vocal" else "written"
        val payload = mapOf(
            "userId" to uid,
            "mode" to cleanMode,
            "userInput" to userInput.trim().take(500).ifBlank { "(Voice recording)" },
            "englishReply" to englishReply.trim().take(1500).ifBlank { "Let's try again!" },
            "arabicTranslation" to arabicTranslation.trim().take(1500),
            "feedbackArabic" to feedbackArabic.trim().take(1500),
            "phoneticHint" to phoneticHint.trim().take(400),
            "createdAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun getChatHistory(userId: String, mode: String): Result<List<TutorTurn>> = runCatching {
        val cleanMode = if (mode == "vocal") "vocal" else "written"
        val colRef = db.collection("users").document(userId).collection("tutor_turns")
        try {
            colRef
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .get()
                .await()
                .toObjects(
                    TutorTurn::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
                .filter { it.mode == cleanMode }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, colRef.path)
            throw e
        }
    }

    suspend fun clearTutorTurns(mode: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val cleanMode = if (mode == "vocal") "vocal" else "written"
        val colRef = db.collection("users").document(uid).collection("tutor_turns")
        try {
            val snapshot = colRef.get().await()
            val toDelete = snapshot.documents.filter { it.getString("mode") == cleanMode }
            if (toDelete.isNotEmpty()) {
                val batch = db.batch()
                toDelete.forEach { batch.delete(it.reference) }
                batch.commit().await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, colRef.path)
            throw e
        }
    }

    suspend fun deleteTutorTurn(turnId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        require(turnId.isNotBlank()) { "Turn ID must not be blank" }
        val docRef = db.collection("users").document(uid).collection("tutor_turns").document(turnId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }
}
