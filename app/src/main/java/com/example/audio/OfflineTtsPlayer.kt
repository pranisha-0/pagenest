package com.example.audio

import android.content.Context
import android.os.Bundle
import android.os.CountDownTimer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

data class PlaybackState(
  val bookId: String? = null,
  val bookTitle: String = "",
  val author: String = "",
  val coverColorHex: String = "#5E4F8A",
  val coverPath: String? = null,
  val chapterName: String = "",
  val pageNumber: Int = 1,
  val isPlaying: Boolean = false,
  val isReady: Boolean = false,
  val currentParagraphIndex: Int = 0,
  val paragraphs: List<String> = emptyList(),
  val playbackSpeed: Float = 1.0f,
  val currentVoiceName: String = "Default Local Voice",
  val sleepTimerMinutesLeft: Int = 0,
  val elapsedSeconds: Int = 0,
  val totalEstimatedSeconds: Int = 0
)

class OfflineTtsPlayer private constructor(private val context: Context) : TextToSpeech.OnInitListener {

  private var tts: TextToSpeech? = null
  private val scope = CoroutineScope(Dispatchers.Main)

  private val _playbackState = MutableStateFlow(PlaybackState())
  val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

  private var sleepTimer: CountDownTimer? = null
  private var isTtsInitialized = false
  private var currentUtteranceId: String? = null

  var onPageFinishedListener: (() -> Unit)? = null

  companion object {
    @Volatile
    private var INSTANCE: OfflineTtsPlayer? = null

    fun getInstance(context: Context): OfflineTtsPlayer {
      return INSTANCE ?: synchronized(this) {
        val instance = OfflineTtsPlayer(context.applicationContext)
        INSTANCE = instance
        instance
      }
    }
  }

  init {
    initializeTts()
  }

  private fun initializeTts() {
    try {
      tts = TextToSpeech(context, this)
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  override fun onInit(status: Int) {
    if (status == TextToSpeech.SUCCESS) {
      isTtsInitialized = true
      tts?.language = Locale.getDefault()
      tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) {
          _playbackState.value = _playbackState.value.copy(isPlaying = true)
        }

        override fun onDone(utteranceId: String?) {
          scope.launch {
            handleParagraphDone()
          }
        }

        override fun onError(utteranceId: String?) {
          _playbackState.value = _playbackState.value.copy(isPlaying = false)
        }
      })
      _playbackState.value = _playbackState.value.copy(isReady = true)
    }
  }

  data class VoiceOption(
    val id: String,
    val name: String,
    val language: String,
    val isSample: Boolean = false
  )

  fun getAvailableVoiceOptions(): List<VoiceOption> {
    val options = mutableListOf<VoiceOption>()
    options.add(VoiceOption("default", "Default Natural Voice", "English (Recommended)"))
    options.add(VoiceOption("en_us_natural_1", "Natural English (US) — Clear", "English (US)"))
    options.add(VoiceOption("en_us_natural_2", "Warm Narrator (US) — Deep", "English (US)"))
    options.add(VoiceOption("en_gb_storyteller", "Storyteller (UK) — Soft", "English (UK)"))
    options.add(VoiceOption("en_au_calm", "Calm Reader (AU)", "English (Australia)"))

    try {
      tts?.voices?.forEach { v ->
        if (!v.isNetworkConnectionRequired) {
          val locale = v.locale
          val langName = locale.displayLanguage
          val countryName = locale.displayCountry
          val display = if (countryName.isNotBlank()) "$langName ($countryName)" else langName
          val cleanName = when {
            v.name.contains("en-us", ignoreCase = true) -> "English (US) Voice"
            v.name.contains("en-gb", ignoreCase = true) -> "English (UK) Voice"
            v.name.contains("en-au", ignoreCase = true) -> "English (AU) Voice"
            v.name.contains("en-in", ignoreCase = true) -> "English (India) Voice"
            else -> "$display Device Voice"
          }
          if (options.none { it.id == v.name || it.name == cleanName }) {
            options.add(VoiceOption(v.name, cleanName, display))
          }
        }
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }
    return options
  }

  fun previewVoice(voiceId: String) {
    try {
      tts?.stop()
      if (voiceId != "default") {
        tts?.voices?.firstOrNull { it.name == voiceId }?.let {
          tts?.voice = it
        } ?: run {
          when (voiceId) {
            "en_us_natural_1", "en_us_natural_2" -> {
              tts?.language = Locale.US
              tts?.setPitch(if (voiceId.endsWith("2")) 0.9f else 1.05f)
            }
            "en_gb_storyteller" -> {
              tts?.language = Locale.UK
              tts?.setPitch(0.95f)
            }
            "en_au_calm" -> {
              tts?.language = Locale("en", "AU")
              tts?.setPitch(1.0f)
            }
          }
        }
      } else {
        tts?.language = Locale.getDefault()
        tts?.setPitch(1.0f)
      }

      val sampleText = "Hello, this is your reading voice on Unfold."
      val utteranceId = "preview_${System.currentTimeMillis()}"
      val params = Bundle()
      params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
      tts?.speak(sampleText, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  fun setVoiceOption(option: VoiceOption) {
    try {
      if (option.id != "default") {
        tts?.voices?.firstOrNull { it.name == option.id }?.let {
          tts?.voice = it
        } ?: run {
          when (option.id) {
            "en_us_natural_1", "en_us_natural_2" -> {
              tts?.language = Locale.US
              tts?.setPitch(if (option.id.endsWith("2")) 0.9f else 1.05f)
            }
            "en_gb_storyteller" -> {
              tts?.language = Locale.UK
              tts?.setPitch(0.95f)
            }
            "en_au_calm" -> {
              tts?.language = Locale("en", "AU")
              tts?.setPitch(1.0f)
            }
          }
        }
      } else {
        tts?.language = Locale.getDefault()
        tts?.setPitch(1.0f)
      }
      _playbackState.value = _playbackState.value.copy(currentVoiceName = option.name)
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  fun loadBookForPlayback(
    bookId: String,
    bookTitle: String,
    author: String,
    coverColorHex: String,
    coverPath: String?,
    chapterName: String,
    pageNumber: Int,
    paragraphs: List<String>,
    startParagraphIndex: Int = 0,
    autoPlay: Boolean = true
  ) {
    val filtered = paragraphs.filter { it.isNotBlank() }
    val totalWords = filtered.sumOf { it.split("\\s+".toRegex()).size }
    val estimatedSeconds = ((totalWords / 2.5f) / _playbackState.value.playbackSpeed).toInt().coerceAtLeast(30)

    val validIndex = startParagraphIndex.coerceIn(0, (filtered.size - 1).coerceAtLeast(0))

    _playbackState.value = _playbackState.value.copy(
      bookId = bookId,
      bookTitle = bookTitle,
      author = author,
      coverColorHex = coverColorHex,
      coverPath = coverPath,
      chapterName = chapterName,
      pageNumber = pageNumber,
      paragraphs = filtered,
      currentParagraphIndex = validIndex,
      elapsedSeconds = 0,
      totalEstimatedSeconds = estimatedSeconds,
      isPlaying = false
    )

    if (autoPlay && filtered.isNotEmpty()) {
      playCurrentParagraph()
    }
  }

  fun updateBookTitle(newTitle: String) {
    _playbackState.value = _playbackState.value.copy(bookTitle = newTitle)
  }

  fun play() {
    val state = _playbackState.value
    if (state.paragraphs.isEmpty()) return
    playCurrentParagraph()
  }

  fun pause() {
    tts?.stop()
    _playbackState.value = _playbackState.value.copy(isPlaying = false)
  }

  fun togglePlayPause() {
    if (_playbackState.value.isPlaying) {
      pause()
    } else {
      play()
    }
  }

  fun setPlaybackSpeed(speed: Float) {
    tts?.setSpeechRate(speed)
    val state = _playbackState.value
    val totalWords = state.paragraphs.sumOf { it.split("\\s+".toRegex()).size }
    val estimatedSeconds = ((totalWords / 2.5f) / speed).toInt().coerceAtLeast(30)
    _playbackState.value = state.copy(
      playbackSpeed = speed,
      totalEstimatedSeconds = estimatedSeconds
    )
    if (state.isPlaying) {
      playCurrentParagraph()
    }
  }

  fun nextParagraph() {
    val state = _playbackState.value
    if (state.currentParagraphIndex < state.paragraphs.size - 1) {
      _playbackState.value = state.copy(
        currentParagraphIndex = state.currentParagraphIndex + 1
      )
      if (state.isPlaying) {
        playCurrentParagraph()
      }
    } else {
      onPageFinishedListener?.invoke() ?: pause()
    }
  }

  fun previousParagraph() {
    val state = _playbackState.value
    if (state.currentParagraphIndex > 0) {
      _playbackState.value = state.copy(
        currentParagraphIndex = state.currentParagraphIndex - 1
      )
      if (state.isPlaying) {
        playCurrentParagraph()
      }
    }
  }

  fun skipSeconds(seconds: Int) {
    val state = _playbackState.value
    val paragraphsToSkip = (seconds / 15).coerceAtLeast(1)
    if (seconds > 0) {
      if (state.currentParagraphIndex + paragraphsToSkip >= state.paragraphs.size) {
        onPageFinishedListener?.invoke()
      } else {
        val newIndex = (state.currentParagraphIndex + paragraphsToSkip).coerceAtMost(state.paragraphs.size - 1)
        _playbackState.value = state.copy(currentParagraphIndex = newIndex)
        if (state.isPlaying) {
          playCurrentParagraph()
        }
      }
    } else {
      val newIndex = (state.currentParagraphIndex - paragraphsToSkip).coerceAtLeast(0)
      _playbackState.value = state.copy(currentParagraphIndex = newIndex)
      if (state.isPlaying) {
        playCurrentParagraph()
      }
    }
  }

  fun seekToParagraph(index: Int) {
    val state = _playbackState.value
    val safeIndex = index.coerceIn(0, (state.paragraphs.size - 1).coerceAtLeast(0))
    _playbackState.value = state.copy(currentParagraphIndex = safeIndex)
    if (state.isPlaying) {
      playCurrentParagraph()
    }
  }

  fun setSleepTimer(minutes: Int) {
    sleepTimer?.cancel()
    if (minutes <= 0) {
      _playbackState.value = _playbackState.value.copy(sleepTimerMinutesLeft = 0)
      return
    }

    _playbackState.value = _playbackState.value.copy(sleepTimerMinutesLeft = minutes)
    sleepTimer = object : CountDownTimer((minutes * 60 * 1000).toLong(), 60000) {
      override fun onTick(millisUntilFinished: Long) {
        val mins = (millisUntilFinished / 60000).toInt() + 1
        _playbackState.value = _playbackState.value.copy(sleepTimerMinutesLeft = mins)
      }

      override fun onFinish() {
        pause()
        _playbackState.value = _playbackState.value.copy(sleepTimerMinutesLeft = 0)
      }
    }.start()
  }

  private fun playCurrentParagraph() {
    val state = _playbackState.value
    if (state.paragraphs.isEmpty() || state.currentParagraphIndex >= state.paragraphs.size) {
      _playbackState.value = state.copy(isPlaying = false)
      return
    }

    val textToSpeak = state.paragraphs[state.currentParagraphIndex]
    tts?.setSpeechRate(state.playbackSpeed)
    val utteranceId = "utterance_${System.currentTimeMillis()}"
    currentUtteranceId = utteranceId

    val params = Bundle()
    params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)

    tts?.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
    _playbackState.value = state.copy(isPlaying = true)
  }

  private fun handleParagraphDone() {
    val state = _playbackState.value
    if (!state.isPlaying) return

    if (state.currentParagraphIndex < state.paragraphs.size - 1) {
      _playbackState.value = state.copy(
        currentParagraphIndex = state.currentParagraphIndex + 1,
        elapsedSeconds = state.elapsedSeconds + 15
      )
      playCurrentParagraph()
    } else {
      _playbackState.value = state.copy(isPlaying = false)
      onPageFinishedListener?.invoke()
    }
  }

  fun stopAndClear() {
    tts?.stop()
    sleepTimer?.cancel()
    _playbackState.value = PlaybackState(isReady = isTtsInitialized)
  }
}
