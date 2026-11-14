package com.example.core

import android.content.Context
import android.content.Intent
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale
import java.util.UUID

class SpeechManager(context: Context) {

    private val appContext = context.applicationContext
    private var tts: TextToSpeech? = null
    private var isReady = false

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _activeUtteranceId = MutableStateFlow<String?>(null)
    val activeUtteranceId: StateFlow<String?> = _activeUtteranceId.asStateFlow()

    init {
        tts = TextToSpeech(appContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val langResult = tts?.setLanguage(Locale.US)
                isReady = langResult != TextToSpeech.LANG_MISSING_DATA &&
                    langResult != TextToSpeech.LANG_NOT_SUPPORTED
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _isSpeaking.value = true
                        _activeUtteranceId.value = utteranceId
                    }

                    override fun onDone(utteranceId: String?) {
                        _isSpeaking.value = false
                        _activeUtteranceId.value = null
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        _isSpeaking.value = false
                        _activeUtteranceId.value = null
                    }

                    override fun onError(utteranceId: String?, errorCode: Int) {
                        _isSpeaking.value = false
                        _activeUtteranceId.value = null
                    }
                })
            } else {
                Log.w(TAG, "TextToSpeech initialization failed with status=$status")
            }
        }
    }

    fun speak(text: String, slow: Boolean = false, utteranceTag: String = UUID.randomUUID().toString()) {
        val clean = text.trim()
        if (clean.isEmpty()) return
        val engine = tts ?: return
        if (!isReady) {
            // Attempt lazy language setup in case TTS initialized slightly late
            val res = engine.setLanguage(Locale.US)
            isReady = res != TextToSpeech.LANG_MISSING_DATA && res != TextToSpeech.LANG_NOT_SUPPORTED
        }
        engine.setSpeechRate(if (slow) 0.72f else 0.98f)
        engine.setPitch(1.0f)
        val params = Bundle()
        engine.speak(clean, TextToSpeech.QUEUE_FLUSH, params, utteranceTag)
    }

    fun stop() {
        tts?.stop()
        _isSpeaking.value = false
        _activeUtteranceId.value = null
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        _isSpeaking.value = false
        _activeUtteranceId.value = null
    }

    private companion object {
        const val TAG = "SpeechManager"
    }
}

class AudioRecorderManager(private val context: Context) {

    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var amplitudeJob: Job? = null

    private val _amplitude = MutableStateFlow(0f)
    val amplitude: StateFlow<Float> = _amplitude.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    fun startRecording(scope: CoroutineScope): Result<Unit> = runCatching {
        stopInternal(deleteFile = true)
        val file = File(context.cacheDir, "noor_voice_${System.currentTimeMillis()}.m4a")
        outputFile = file

        val mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }

        mediaRecorder.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(64_000)
            setAudioSamplingRate(16_000)
            setOutputFile(file.absolutePath)
            prepare()
            start()
        }

        recorder = mediaRecorder
        _isRecording.value = true

        amplitudeJob = scope.launch(Dispatchers.Default) {
            while (isActive && _isRecording.value) {
                val amp = try {
                    recorder?.maxAmplitude ?: 0
                } catch (_: Exception) {
                    0
                }
                _amplitude.value = (amp / 32767f).coerceIn(0.05f, 1f)
                delay(65L)
            }
        }
    }.onFailure {
        Log.e(TAG, "Failed to start audio recording", it)
        stopInternal(deleteFile = true)
    }

    fun stopAndReadBytes(): Result<ByteArray> = runCatching {
        amplitudeJob?.cancel()
        amplitudeJob = null
        _isRecording.value = false
        _amplitude.value = 0f

        val activeRecorder = recorder
            ?: throw IllegalStateException("No active recording found.")
        val file = outputFile
            ?: throw IllegalStateException("No audio output file found.")

        try {
            activeRecorder.stop()
        } finally {
            activeRecorder.release()
            recorder = null
        }

        val bytes = file.readBytes()
        file.delete()
        outputFile = null
        if (bytes.isEmpty()) {
            throw IllegalStateException("Recorded audio is empty.")
        }
        bytes
    }.onFailure {
        stopInternal(deleteFile = true)
    }

    fun cancelRecording() {
        stopInternal(deleteFile = true)
    }

    private fun stopInternal(deleteFile: Boolean) {
        amplitudeJob?.cancel()
        amplitudeJob = null
        _isRecording.value = false
        _amplitude.value = 0f
        try {
            recorder?.stop()
        } catch (_: Exception) {
        }
        try {
            recorder?.release()
        } catch (_: Exception) {
        }
        recorder = null
        if (deleteFile) {
            outputFile?.delete()
            outputFile = null
        }
    }

    private companion object {
        const val TAG = "AudioRecorderManager"
    }
}

data class SpeechToTextState(
    val isListening: Boolean = false,
    val isSpeechDetected: Boolean = false,
    val rmsLevel: Float = 0f,
    val partialText: String = "",
    val recognizedText: String? = null,
    val confidence: Float? = null,
    val errorMessage: String? = null
)

interface SpeechToTextController {
    val state: StateFlow<SpeechToTextState>
    fun isDirectRecognitionAvailable(): Boolean
    fun startListening(languageTag: String = "en-US")
    fun stopListening()
    fun cancel()
    fun clearResult()
    fun applyExternalResult(transcript: String, confidence: Float? = null)
    fun reportError(message: String)
    fun destroy()
}

fun createEnglishRecognizeSpeechIntent(prompt: String): Intent {
    return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en-US")
        putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, true)
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        putExtra(RecognizerIntent.EXTRA_PROMPT, prompt)
    }
}

class AndroidSpeechToTextManager(context: Context) : SpeechToTextController {

    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private var speechRecognizer: SpeechRecognizer? = null
    private var lastRecognizedWasSuccess = false

    private val _state = MutableStateFlow(SpeechToTextState())
    override val state: StateFlow<SpeechToTextState> = _state.asStateFlow()

    override fun isDirectRecognitionAvailable(): Boolean {
        return try {
            SpeechRecognizer.isRecognitionAvailable(appContext)
        } catch (_: Exception) {
            false
        }
    }

    override fun startListening(languageTag: String) {
        runOnMain {
            if (!isDirectRecognitionAvailable()) {
                _state.value = SpeechToTextState(
                    errorMessage = "خدمة التعرف الفوري على الكلام غير متوفرة على هذا الجهاز."
                )
                return@runOnMain
            }

            try {
                lastRecognizedWasSuccess = false
                if (speechRecognizer == null) {
                    speechRecognizer = SpeechRecognizer.createSpeechRecognizer(appContext).apply {
                        setRecognitionListener(createListener())
                    }
                } else {
                    speechRecognizer?.cancel()
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(
                        RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                    )
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, languageTag)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                    putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, appContext.packageName)
                }

                _state.value = SpeechToTextState(
                    isListening = true,
                    isSpeechDetected = false,
                    rmsLevel = 0.1f,
                    partialText = "",
                    recognizedText = null,
                    confidence = null,
                    errorMessage = null
                )
                speechRecognizer?.startListening(intent)
            } catch (e: SecurityException) {
                Log.e(TAG, "Missing RECORD_AUDIO permission", e)
                _state.value = SpeechToTextState(
                    errorMessage = "يلزم منح إذن الميكروفون للتحدث بالإنجليزية."
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start SpeechRecognizer", e)
                _state.value = SpeechToTextState(
                    errorMessage = "تعذر تشغيل خدمة التعرف على النطق: ${e.localizedMessage ?: "خطأ غير معروف"}"
                )
            }
        }
    }

    override fun stopListening() {
        runOnMain {
            try {
                speechRecognizer?.stopListening()
            } catch (_: Exception) {
            }
            _state.value = _state.value.copy(
                isListening = false,
                rmsLevel = 0f
            )
        }
    }

    override fun cancel() {
        runOnMain {
            try {
                speechRecognizer?.cancel()
            } catch (_: Exception) {
            }
            _state.value = _state.value.copy(
                isListening = false,
                isSpeechDetected = false,
                rmsLevel = 0f,
                partialText = ""
            )
        }
    }

    override fun clearResult() {
        _state.value = _state.value.copy(
            partialText = "",
            recognizedText = null,
            confidence = null,
            errorMessage = null
        )
    }

    override fun applyExternalResult(transcript: String, confidence: Float?) {
        val clean = transcript.trim()
        if (clean.isEmpty()) {
            _state.value = _state.value.copy(
                isListening = false,
                errorMessage = "لم يتم التقاط أي كلام، حاول التحدث بوضوح مرة أخرى."
            )
        } else {
            _state.value = SpeechToTextState(
                isListening = false,
                isSpeechDetected = true,
                rmsLevel = 0f,
                partialText = "",
                recognizedText = clean,
                confidence = confidence,
                errorMessage = null
            )
        }
    }

    override fun reportError(message: String) {
        _state.value = _state.value.copy(
            isListening = false,
            isSpeechDetected = false,
            rmsLevel = 0f,
            errorMessage = message
        )
    }

    override fun destroy() {
        runOnMain {
            try {
                speechRecognizer?.cancel()
                speechRecognizer?.destroy()
            } catch (_: Exception) {
            }
            speechRecognizer = null
            _state.value = SpeechToTextState()
        }
    }

    private fun runOnMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            block()
        } else {
            mainHandler.post(block)
        }
    }

    private fun createListener(): RecognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            _state.value = _state.value.copy(
                isListening = true,
                errorMessage = null
            )
        }

        override fun onBeginningOfSpeech() {
            _state.value = _state.value.copy(
                isListening = true,
                isSpeechDetected = true
            )
        }

        override fun onRmsChanged(rmsdB: Float) {
            val normalized = ((rmsdB + 2f) / 12f).coerceIn(0.08f, 1f)
            _state.value = _state.value.copy(rmsLevel = normalized)
        }

        override fun onBufferReceived(buffer: ByteArray?) = Unit

        override fun onEndOfSpeech() {
            _state.value = _state.value.copy(
                isListening = false,
                rmsLevel = 0f
            )
        }

        override fun onError(error: Int) {
            if (lastRecognizedWasSuccess && error == SpeechRecognizer.ERROR_CLIENT) {
                return
            }
            val message = when (error) {
                SpeechRecognizer.ERROR_NO_MATCH ->
                    "لم أتمكن من تمييز الكلمات الإنجليزية. حاول التحدث ببطء ووضوح."
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
                    "انتهت مهلة الاستماع قبل سماع صوتك. اضغط على الميكروفون وحاول مجدداً."
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
                    "يلزم منح إذن الميكروفون لاستخدام ميزة التحدث."
                SpeechRecognizer.ERROR_AUDIO ->
                    "تعذر تسجيل الصوت من الميكروفون."
                SpeechRecognizer.ERROR_NETWORK,
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
                    "تعذر الاتصال بخدمة التعرف على الكلام. تحقق من الإنترنت."
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY ->
                    "خدمة التعرف على الصوت مشغولة حالياً، حاول بعد لحظة."
                else ->
                    "حدث خطأ أثناء التعرف على النطق. حاول مرة أخرى."
            }
            _state.value = _state.value.copy(
                isListening = false,
                isSpeechDetected = false,
                rmsLevel = 0f,
                errorMessage = message
            )
        }

        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
            val scores = results?.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)
            val best = matches.firstOrNull()?.trim().orEmpty()
            val bestConfidence = scores?.firstOrNull()?.takeIf { it in 0f..1f }

            if (best.isNotEmpty()) {
                lastRecognizedWasSuccess = true
                _state.value = SpeechToTextState(
                    isListening = false,
                    isSpeechDetected = true,
                    rmsLevel = 0f,
                    partialText = "",
                    recognizedText = best,
                    confidence = bestConfidence,
                    errorMessage = null
                )
            } else {
                _state.value = _state.value.copy(
                    isListening = false,
                    rmsLevel = 0f,
                    errorMessage = "لم يتم التقاط جملة واضحة، حاول مرة أخرى."
                )
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val partial = partialResults
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
                ?.trim()
                .orEmpty()
            if (partial.isNotEmpty()) {
                _state.value = _state.value.copy(
                    isSpeechDetected = true,
                    partialText = partial
                )
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) = Unit
    }

    private companion object {
        const val TAG = "AndroidSpeechToText"
    }
}
