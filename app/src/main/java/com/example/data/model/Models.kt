package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProgress(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val xp: Long = 0L,
    val completedLessonIds: List<String> = emptyList(),
    val lastLessonId: String = "",
    val dailyGoal: Int = DailyProgressSummary.DEFAULT_DAILY_MESSAGE_GOAL,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastActiveDate: String = "",
    val dailyStudyMinutesGoal: Int = StudyTimeSummary.DEFAULT_STUDY_GOAL_MINUTES,
    val todayStudySeconds: Int = 0,
    val lastStudyDate: String = "",
    val reminderEnabled: Boolean = false,
    val reminderHour: Int = ReminderSettings.DEFAULT_REMINDER_HOUR,
    val reminderMinute: Int = ReminderSettings.DEFAULT_REMINDER_MINUTE,
    val lastDailyQuizDate: String = "",
    val lastDailyQuizScore: Int = 0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class VocabItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val english: String = "",
    val arabic: String = "",
    val phoneticArabic: String = "",
    val phoneticEnglish: String = "",
    val exampleEnglish: String = "",
    val category: String = "General",
    val mastered: Boolean = false,
    val usageCount: Int = 1,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class TutorTurn(
    @DocumentId val id: String = "",
    val userId: String = "",
    val mode: String = "written",
    val userInput: String = "",
    val englishReply: String = "",
    val arabicTranslation: String = "",
    val feedbackArabic: String = "",
    val phoneticHint: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

data class SuggestedPhrase(
    val english: String,
    val arabic: String,
    val phoneticArabic: String,
    val exampleEnglish: String = "",
    val phoneticEnglish: String = ""
)

data class TutorResponse(
    val whatYouSaid: String = "",
    val englishReply: String,
    val arabicTranslation: String,
    val feedbackArabic: String,
    val phoneticHint: String,
    val suggestedPhrases: List<SuggestedPhrase> = emptyList()
)

data class LessonPhrase(
    val english: String,
    val arabic: String,
    val phoneticArabic: String,
    val tipArabic: String = ""
)

data class Lesson(
    val id: String,
    val number: Int,
    val titleEnglish: String,
    val titleArabic: String,
    val subtitleArabic: String,
    val focusTipArabic: String,
    val xpReward: Long,
    val phrases: List<LessonPhrase>
)
