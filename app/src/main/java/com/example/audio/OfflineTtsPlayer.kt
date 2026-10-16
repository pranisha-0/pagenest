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
  val currentVoiceName: String = "Default Natural Voice",
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
  private var currentSessionId = 0L

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
      setupBestVoice()

      tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) {
          if (utteranceId == null) return
          // Check if utterance is from current session
          val parts = utteranceId.split("_")
          if (parts.size >= 4 && parts[0] == "utt") {
            val pIdx = parts[2].toIntOrNull()
            if (pIdx != null) {
              scope.launch {
                val current = _playbackState.value
                if (current.currentParagraphIndex != pIdx || !current.isPlaying) {
                  _playbackState.value = current.copy(
                    isPlaying = true,
                    currentParagraphIndex = pIdx.coerceIn(0, (current.paragraphs.size - 1).coerceAtLeast(0))
                  )
                }
              }
            }
          }
        }

        override fun onDone(utteranceId: String?) {
          if (utteranceId == null) return
          val parts = utteranceId.split("_")
          if (parts.size >= 4 && parts[0] == "utt" && parts[1] == "last") {
            // Last sentence of the page finished
            scope.launch {
              val state = _playbackState.value
              if (state.isPlaying) {
                onPageFinishedListener?.invoke() ?: run {
                  _playbackState.value = state.copy(isPlaying = false)
                }
              }
            }
          }
        }

        override fun onError(utteranceId: String?) {
          scope.launch {
            _playbackState.value = _playbackState.value.copy(isPlaying = false)
          }
        }
      })

      _playbackState.value = _playbackState.value.copy(isReady = true)
    }
  }

  private fun setupBestVoice() {
    try {
      val defaultLocale = Locale.getDefault()
      tts?.language = defaultLocale

      // Try to find the highest quality voice for user locale
      val available = tts?.voices ?: return
      val bestVoice = available.filter { v ->
        v.locale.language == defaultLocale.language && !v.isNetworkConnectionRequired
      }.maxByOrNull { it.quality } ?: available.firstOrNull { !it.isNetworkConnectionRequired }

      if (bestVoice != null) {
        tts?.voice = bestVoice
      }
    } catch (e: Exception) {
      e.printStackTrace()
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
    options.add(VoiceOption("default", "Default Natural Voice", "System (Recommended)"))

    try {
      val deviceVoices = tts?.voices?.toList() ?: emptyList()
      // Filter non-network required voices and rank by quality
      val validVoices = deviceVoices.filter { !it.isNetworkConnectionRequired }

      // Look for high-quality voices
      for (v in validVoices) {
        val locale = v.locale
        val langName = locale.displayLanguage
        val countryName = locale.displayCountry
        val display = if (countryName.isNotBlank()) "$langName ($countryName)" else langName

        val isHighQuality = v.quality >= Voice.QUALITY_HIGH
        val cleanName = when {
          v.name.contains("neural", ignoreCase = true) || isHighQuality -> "$display — Natural Voice"
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
    } catch (e: Exception) {
      e.printStackTrace()
    }

    if (options.size == 1) {
      // Fallback presets if system returned empty voice list
      options.add(VoiceOption("en_us_clear", "Natural English (US) — Clear", "English (US)"))
      options.add(VoiceOption("en_us_warm", "Warm Narrator (US) — Expressive", "English (US)"))
      options.add(VoiceOption("en_gb_story", "Storyteller (UK) — Smooth", "English (UK)"))
      options.add(VoiceOption("en_au_calm", "Calm Reader (AU)", "English (AU)"))
    }

    return options
  }

  fun previewVoice(voiceId: String) {
    try {
      tts?.stop()
      applyVoice(voiceId)

      val sampleText = "Because I don't live in either my past or my future. I'm interested only in the present."
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
      applyVoice(option.id)
      _playbackState.value = _playbackState.value.copy(currentVoiceName = option.name)
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  private fun applyVoice(voiceId: String) {
    if (voiceId == "default") {
      setupBestVoice()
      return
    }

    val matched = tts?.voices?.firstOrNull { it.name == voiceId }
    if (matched != null) {
      tts?.voice = matched
    } else {
      when (voiceId) {
        "en_us_clear", "en_us_warm" -> {
          tts?.language = Locale.US
          tts?.setPitch(if (voiceId.endsWith("warm")) 0.94f else 1.02f)
        }
        "en_gb_story" -> {
          tts?.language = Locale.UK
          tts?.setPitch(0.96f)
        }
        "en_au_calm" -> {
          tts?.language = Locale("en", "AU")
          tts?.setPitch(1.0f)
        }
        else -> {
          tts?.language = Locale.getDefault()
        }
      }
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
      startContinuousPlayback(validIndex)
    }
  }

  fun updateBookTitle(newTitle: String) {
    _playbackState.value = _playbackState.value.copy(bookTitle = newTitle)
  }

  fun play() {
    val state = _playbackState.value
    if (state.paragraphs.isEmpty()) return
    startContinuousPlayback(state.currentParagraphIndex)
  }

  fun pause() {
    currentSessionId = System.currentTimeMillis()
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
      startContinuousPlayback(state.currentParagraphIndex)
    }
  }

  fun nextParagraph() {
    val state = _playbackState.value
    if (state.currentParagraphIndex < state.paragraphs.size - 1) {
      val nextIdx = state.currentParagraphIndex + 1
      _playbackState.value = state.copy(currentParagraphIndex = nextIdx)
      if (state.isPlaying) {
        startContinuousPlayback(nextIdx)
      }
    } else {
      onPageFinishedListener?.invoke() ?: pause()
    }
  }

  fun previousParagraph() {
    val state = _playbackState.value
    if (state.currentParagraphIndex > 0) {
      val prevIdx = state.currentParagraphIndex - 1
      _playbackState.value = state.copy(currentParagraphIndex = prevIdx)
      if (state.isPlaying) {
        startContinuousPlayback(prevIdx)
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
          startContinuousPlayback(newIndex)
        }
      }
    } else {
      val newIndex = (state.currentParagraphIndex - paragraphsToSkip).coerceAtLeast(0)
      _playbackState.value = state.copy(currentParagraphIndex = newIndex)
      if (state.isPlaying) {
        startContinuousPlayback(newIndex)
      }
    }
  }

  fun seekToParagraph(index: Int) {
    val state = _playbackState.value
    val safeIndex = index.coerceIn(0, (state.paragraphs.size - 1).coerceAtLeast(0))
    _playbackState.value = state.copy(currentParagraphIndex = safeIndex)
    if (state.isPlaying) {
      startContinuousPlayback(safeIndex)
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

  /**
   * Continuous Speech Pipeline:
   * Chunks paragraphs into sentences and queues them sequentially using QUEUE_ADD.
   * Inserts calibrated micro-pauses (80ms between sentences, 250ms between paragraphs)
   * to eliminate robotic gaps while maintaining conversational human cadence.
   */
  private fun startContinuousPlayback(fromParagraphIndex: Int) {
    val state = _playbackState.value
    if (state.paragraphs.isEmpty() || fromParagraphIndex >= state.paragraphs.size) {
      _playbackState.value = state.copy(isPlaying = false)
      return
    }

    val sessionId = System.currentTimeMillis()
    currentSessionId = sessionId

    tts?.stop()
    tts?.setSpeechRate(state.playbackSpeed)

    var isFirst = true

    for (pIdx in fromParagraphIndex until state.paragraphs.size) {
      val paragraph = state.paragraphs[pIdx].trim()
      if (paragraph.isEmpty()) continue

      val sentences = splitIntoSentences(paragraph)
      val isLastParagraph = (pIdx == state.paragraphs.size - 1)

      for (sIdx in sentences.indices) {
        val sentence = sentences[sIdx].trim()
        if (sentence.isEmpty()) continue

        val isLastSentenceOfPage = isLastParagraph && (sIdx == sentences.size - 1)
        val utteranceType = if (isLastSentenceOfPage) "last" else "norm"
        val utteranceId = "utt_${utteranceType}_${pIdx}_${sIdx}_${sessionId}"

        val params = Bundle()
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)

        val queueMode = if (isFirst) {
          isFirst = false
          TextToSpeech.QUEUE_FLUSH
        } else {
          TextToSpeech.QUEUE_ADD
        }

        tts?.speak(sentence, queueMode, params, utteranceId)

        // Micro-pause between sentences within paragraph (80ms natural pause)
        if (sIdx < sentences.size - 1) {
          tts?.playSilentUtterance(80L, TextToSpeech.QUEUE_ADD, "silence_s_${sessionId}")
        }
      }

      // Paragraph transition pause (240ms thoughtful breath, rather than mechanical dead silence)
      if (!isLastParagraph) {
        tts?.playSilentUtterance(240L, TextToSpeech.QUEUE_ADD, "silence_p_${sessionId}")
      }
    }

    _playbackState.value = state.copy(isPlaying = true, currentParagraphIndex = fromParagraphIndex)
  }

  /**
   * Splits a paragraph into clean sentences while preserving abbreviations like Mr., Dr., etc.
   */
  private fun splitIntoSentences(text: String): List<String> {
    if (text.length < 80) return listOf(text)

    // Regex to split on terminal punctuation followed by space and capital letter or quote
    val sentenceRegex = Regex("(?<=[.!?…])\\s+(?=[A-Z0-9\"“'‘])")
    val rawSplits = text.split(sentenceRegex).map { it.trim() }.filter { it.isNotBlank() }

    // Recombine accidental splits on titles/abbreviations (e.g. "Mr.", "Dr.", "e.g.", "i.e.")
    val result = mutableListOf<String>()
    var buffer = ""

    for (s in rawSplits) {
      val testBuffer = if (buffer.isEmpty()) s else "$buffer $s"
      val lastWord = (if (buffer.isNotEmpty()) buffer else s).split(" ").lastOrNull()?.lowercase() ?: ""

      val isAbbrev = lastWord in listOf("mr.", "mrs.", "ms.", "dr.", "prof.", "jr.", "sr.", "e.g.", "i.e.", "vs.", "etc.")
      if (isAbbrev) {
        buffer = testBuffer
      } else {
        result.add(testBuffer)
        buffer = ""
      }
    }

    if (buffer.isNotEmpty()) {
      result.add(buffer)
    }

    return if (result.isNotEmpty()) result else listOf(text)
  }

  fun stopAndClear() {
    currentSessionId = System.currentTimeMillis()
    tts?.stop()
    sleepTimer?.cancel()
    _playbackState.value = PlaybackState(isReady = isTtsInitialized)
  }
}
