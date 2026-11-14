package com.example.ui.screens

import androidx.compose.runtime.Composable
import com.example.data.model.Lesson
import com.example.data.model.SuggestedPhrase
import com.example.data.model.TutorTurn
import com.example.ui.viewmodel.AiTurnState
import com.example.ui.viewmodel.UiState

@Composable
fun WrittenTutorScreen(
    turnsState: UiState<List<TutorTurn>>,
    aiState: AiTurnState,
    activeTopic: Lesson?,
    suggestions: List<SuggestedPhrase>,
    slowSpeech: Boolean,
    onToggleSlowSpeech: () -> Unit,
    onClearTopic: () -> Unit,
    onSendMessage: (String) -> Unit,
    onSpeakText: (String) -> Unit,
    onSaveWord: (SuggestedPhrase) -> Unit
) {
    ChatScreen(
        turnsState = turnsState,
        aiState = aiState,
        activeTopic = activeTopic,
        suggestions = suggestions,
        slowSpeech = slowSpeech,
        onToggleSlowSpeech = onToggleSlowSpeech,
        onClearTopic = onClearTopic,
        onSendMessage = onSendMessage,
        onSpeakText = onSpeakText,
        onSaveWord = onSaveWord
    )
}
