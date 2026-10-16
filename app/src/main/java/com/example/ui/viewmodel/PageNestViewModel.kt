package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.OfflineTtsPlayer
import com.example.audio.PlaybackState
import com.example.data.model.AnnotationEntity
import com.example.data.model.BookEntity
import com.example.data.model.BookPageTextEntity
import com.example.data.repository.BookRepository
import com.example.pdf.PdfManager
import com.example.ui.theme.AppThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

enum class MainTab {
  HOME,
  LIBRARY,
  NOTES,
  SETTINGS
}

enum class ReadingMode {
  ORIGINAL_PDF,
  CLEAN_READER
}

enum class ActiveScreen {
  MAIN_TABS,
  READER,
  AUDIO_FULL
}

class PageNestViewModel(application: Application) : AndroidViewModel(application) {

  val repository = BookRepository(application)
  val ttsPlayer = OfflineTtsPlayer.getInstance(application)

  val allBooks: StateFlow<List<BookEntity>> = repository.allBooks
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val allAnnotations: StateFlow<List<AnnotationEntity>> = repository.allAnnotations
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val playbackState: StateFlow<PlaybackState> = ttsPlayer.playbackState

  // Navigation
  private val _activeScreen = MutableStateFlow(ActiveScreen.MAIN_TABS)
  val activeScreen: StateFlow<ActiveScreen> = _activeScreen.asStateFlow()

  private val _currentTab = MutableStateFlow(MainTab.HOME)
  val currentTab: StateFlow<MainTab> = _currentTab.asStateFlow()

  // Library Filters
  private val _libraryFilter = MutableStateFlow("All") // "All", "Reading", "Finished"
  val libraryFilter: StateFlow<String> = _libraryFilter.asStateFlow()

  private val _searchQuery = MutableStateFlow("")
  val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

  // Global Page Content Search Matches
  private val _globalSearchPageMatches = MutableStateFlow<List<BookPageTextEntity>>(emptyList())
  val globalSearchPageMatches: StateFlow<List<BookPageTextEntity>> = _globalSearchPageMatches.asStateFlow()

  // Reader state
  private val _currentReadingBook = MutableStateFlow<BookEntity?>(null)
  val currentReadingBook: StateFlow<BookEntity?> = _currentReadingBook.asStateFlow()

  private val _currentPageIndex = MutableStateFlow(0) // 0-based
  val currentPageIndex: StateFlow<Int> = _currentPageIndex.asStateFlow()

  private val _currentPdfBitmap = MutableStateFlow<Bitmap?>(null)
  val currentPdfBitmap: StateFlow<Bitmap?> = _currentPdfBitmap.asStateFlow()

  private val _nextPdfBitmap = MutableStateFlow<Bitmap?>(null)
  val nextPdfBitmap: StateFlow<Bitmap?> = _nextPdfBitmap.asStateFlow()

  private val _prevPdfBitmap = MutableStateFlow<Bitmap?>(null)
  val prevPdfBitmap: StateFlow<Bitmap?> = _prevPdfBitmap.asStateFlow()

  private val _currentPageText = MutableStateFlow<BookPageTextEntity?>(null)
  val currentPageText: StateFlow<BookPageTextEntity?> = _currentPageText.asStateFlow()

  private val _nextPageText = MutableStateFlow<BookPageTextEntity?>(null)
  val nextPageText: StateFlow<BookPageTextEntity?> = _nextPageText.asStateFlow()

  private val _prevPageText = MutableStateFlow<BookPageTextEntity?>(null)
  val prevPageText: StateFlow<BookPageTextEntity?> = _prevPageText.asStateFlow()

  private val _readingMode = MutableStateFlow(ReadingMode.CLEAN_READER)
  val readingMode: StateFlow<ReadingMode> = _readingMode.asStateFlow()

  private val _readerFontSize = MutableStateFlow(17) // sp
  val readerFontSize: StateFlow<Int> = _readerFontSize.asStateFlow()

  private val _appThemeMode = MutableStateFlow(AppThemeMode.LIGHT)
  val appThemeMode: StateFlow<AppThemeMode> = _appThemeMode.asStateFlow()

  // Annotation Filter
  private val _annotationFilter = MutableStateFlow("All") // "All", "Highlights", "Notes", "Bookmarks"
  val annotationFilter: StateFlow<String> = _annotationFilter.asStateFlow()

  private val _annotationSearchQuery = MutableStateFlow("")
  val annotationSearchQuery: StateFlow<String> = _annotationSearchQuery.asStateFlow()

  // Reader search state
  private val _readerSearchQuery = MutableStateFlow("")
  val readerSearchQuery: StateFlow<String> = _readerSearchQuery.asStateFlow()

  private val _isReaderSearchVisible = MutableStateFlow(false)
  val isReaderSearchVisible: StateFlow<Boolean> = _isReaderSearchVisible.asStateFlow()

  private val _readerSearchMatches = MutableStateFlow<List<BookPageTextEntity>>(emptyList())
  val readerSearchMatches: StateFlow<List<BookPageTextEntity>> = _readerSearchMatches.asStateFlow()

  private val _currentReaderSearchIndex = MutableStateFlow(0)
  val currentReaderSearchIndex: StateFlow<Int> = _currentReaderSearchIndex.asStateFlow()

  // Dialogs
  private val _showOfflineDialog = MutableStateFlow(false)
  val showOfflineDialog: StateFlow<Boolean> = _showOfflineDialog.asStateFlow()

  private val _isImporting = MutableStateFlow(false)
  val isImporting: StateFlow<Boolean> = _isImporting.asStateFlow()

  private val _storageUsageBytes = MutableStateFlow(0L)
  val storageUsageBytes: StateFlow<Long> = _storageUsageBytes.asStateFlow()

  init {
    viewModelScope.launch {
      repository.cleanupSampleBooks()
      refreshStorageUsage()
    }

    // Auto-advance to next page when audio finished on a page
    ttsPlayer.onPageFinishedListener = {
      viewModelScope.launch {
        advanceAudioToNextPage()
      }
    }
  }

  fun setTab(tab: MainTab) {
    _currentTab.value = tab
    _activeScreen.value = ActiveScreen.MAIN_TABS
  }

  fun setLibraryFilter(filter: String) {
    _libraryFilter.value = filter
  }

  fun setSearchQuery(query: String) {
    _searchQuery.value = query
    viewModelScope.launch {
      val trimmed = query.trim()
      if (trimmed.length >= 2) {
        _globalSearchPageMatches.value = repository.searchAcrossAllBooks(trimmed)
      } else {
        _globalSearchPageMatches.value = emptyList()
      }
    }
  }

  fun setAnnotationFilter(filter: String) {
    _annotationFilter.value = filter
  }

  fun setAnnotationSearchQuery(query: String) {
    _annotationSearchQuery.value = query
  }

  fun setAppThemeMode(theme: AppThemeMode) {
    _appThemeMode.value = theme
  }

  fun setReaderFontSize(size: Int) {
    _readerFontSize.value = size.coerceIn(12, 28)
  }

  fun toggleReadingMode() {
    _readingMode.value = if (_readingMode.value == ReadingMode.ORIGINAL_PDF) {
      ReadingMode.CLEAN_READER
    } else {
      ReadingMode.ORIGINAL_PDF
    }
  }

  fun openBook(book: BookEntity, pageToOpen: Int? = null) {
    viewModelScope.launch {
      _currentReadingBook.value = book
      val targetPage = pageToOpen ?: book.currentPage
      _currentPageIndex.value = (targetPage - 1).coerceAtLeast(0)
      _activeScreen.value = ActiveScreen.READER
      loadCurrentPageData(book, _currentPageIndex.value)
    }
  }

  fun closeReader() {
    _activeScreen.value = ActiveScreen.MAIN_TABS
  }

  fun openFullAudioPlayer() {
    _activeScreen.value = ActiveScreen.AUDIO_FULL
  }

  fun closeFullAudioPlayer() {
    _activeScreen.value = ActiveScreen.MAIN_TABS
  }

  fun goToPage(pageIndex: Int) {
    val book = _currentReadingBook.value ?: return
    val safeIndex = pageIndex.coerceIn(0, (book.totalPages - 1).coerceAtLeast(0))
    _currentPageIndex.value = safeIndex
    viewModelScope.launch {
      loadCurrentPageData(book, safeIndex)
      repository.updateReadingProgress(book.id, safeIndex + 1, book.totalPages)
    }
  }

  fun nextPage() {
    goToPage(_currentPageIndex.value + 1)
  }

  fun previousPage() {
    goToPage(_currentPageIndex.value - 1)
  }

  fun toggleReaderSearch(visible: Boolean? = null) {
    _isReaderSearchVisible.value = visible ?: !_isReaderSearchVisible.value
    if (!_isReaderSearchVisible.value) {
      _readerSearchQuery.value = ""
      _readerSearchMatches.value = emptyList()
      _currentReaderSearchIndex.value = 0
    }
  }

  fun setReaderSearchQuery(query: String) {
    _readerSearchQuery.value = query
    val book = _currentReadingBook.value ?: return
    viewModelScope.launch {
      val trimmed = query.trim()
      if (trimmed.isNotBlank()) {
        val results = repository.searchInBook(book.id, trimmed)
        _readerSearchMatches.value = results
        _currentReaderSearchIndex.value = 0
        if (results.isNotEmpty()) {
          goToPage(results.first().pageNumber - 1)
        }
      } else {
        _readerSearchMatches.value = emptyList()
        _currentReaderSearchIndex.value = 0
      }
    }
  }

  fun nextReaderSearchMatch() {
    val matches = _readerSearchMatches.value
    if (matches.isEmpty()) return
    val nextIndex = (_currentReaderSearchIndex.value + 1) % matches.size
    _currentReaderSearchIndex.value = nextIndex
    goToPage(matches[nextIndex].pageNumber - 1)
  }

  fun previousReaderSearchMatch() {
    val matches = _readerSearchMatches.value
    if (matches.isEmpty()) return
    val prevIndex = if (_currentReaderSearchIndex.value - 1 < 0) matches.size - 1 else _currentReaderSearchIndex.value - 1
    _currentReaderSearchIndex.value = prevIndex
    goToPage(matches[prevIndex].pageNumber - 1)
  }

  private suspend fun loadCurrentPageData(book: BookEntity, pageIndex: Int) {
    withContext(Dispatchers.IO) {
      val pageNum = pageIndex + 1
      var textEntity = repository.getPageContent(book.id, pageNum)
      if (textEntity == null || textEntity.textContent.isBlank() || textEntity.textContent.contains("Original PDF layout mode recommended")) {
        val extracted = PdfManager.extractPageText(getApplication(), book.filePath, pageNum)
        if (extracted.isNotBlank()) {
          val paragraphs = extracted.split("\n\n").filter { it.isNotBlank() }
          textEntity = BookPageTextEntity(
            id = textEntity?.id ?: 0,
            bookId = book.id,
            pageNumber = pageNum,
            chapterTitle = textEntity?.chapterTitle ?: "Page $pageNum",
            textContent = extracted,
            paragraphCount = paragraphs.size.coerceAtLeast(1)
          )
        }
      }
      _currentPageText.value = textEntity

      // Render PDF page for original PDF mode
      val bitmap = PdfManager.renderPdfPage(book.filePath, pageIndex, targetWidth = 1080)
      _currentPdfBitmap.value = bitmap

      // Prefetch Next Page (pageIndex + 1)
      if (pageIndex + 1 < book.totalPages) {
        val nextNum = pageIndex + 2
        var nextText = repository.getPageContent(book.id, nextNum)
        if (nextText == null || nextText.textContent.isBlank() || nextText.textContent.contains("Original PDF layout mode recommended")) {
          val extracted = PdfManager.extractPageText(getApplication(), book.filePath, nextNum)
          if (extracted.isNotBlank()) {
            val paragraphs = extracted.split("\n\n").filter { it.isNotBlank() }
            nextText = BookPageTextEntity(
              id = 0,
              bookId = book.id,
              pageNumber = nextNum,
              chapterTitle = "Page $nextNum",
              textContent = extracted,
              paragraphCount = paragraphs.size.coerceAtLeast(1)
            )
          }
        }
        _nextPageText.value = nextText
        _nextPdfBitmap.value = PdfManager.renderPdfPage(book.filePath, pageIndex + 1, targetWidth = 1080)
      } else {
        _nextPageText.value = null
        _nextPdfBitmap.value = null
      }

      // Prefetch Previous Page (pageIndex - 1)
      if (pageIndex - 1 >= 0) {
        val prevNum = pageIndex
        var prevText = repository.getPageContent(book.id, prevNum)
        if (prevText == null || prevText.textContent.isBlank() || prevText.textContent.contains("Original PDF layout mode recommended")) {
          val extracted = PdfManager.extractPageText(getApplication(), book.filePath, prevNum)
          if (extracted.isNotBlank()) {
            val paragraphs = extracted.split("\n\n").filter { it.isNotBlank() }
            prevText = BookPageTextEntity(
              id = 0,
              bookId = book.id,
              pageNumber = prevNum,
              chapterTitle = "Page $prevNum",
              textContent = extracted,
              paragraphCount = paragraphs.size.coerceAtLeast(1)
            )
          }
        }
        _prevPageText.value = prevText
        _prevPdfBitmap.value = PdfManager.renderPdfPage(book.filePath, pageIndex - 1, targetWidth = 1080)
      } else {
        _prevPageText.value = null
        _prevPdfBitmap.value = null
      }
    }
  }

  /**
   * Starts listening mode on a specific book.
   * Extracts real text from the original PDF and immediately opens the full audio player.
   */
  fun playBookAudio(book: BookEntity, startFromPage: Int? = null, openFullPlayer: Boolean = true) {
    viewModelScope.launch {
      _currentReadingBook.value = book
      var pageNum = startFromPage ?: book.currentPage
      if (pageNum < 1) pageNum = 1

      // If page 1 is a title/cover page with minimal text, find the first page with real content
      var pageData = repository.getPageContent(book.id, pageNum)
      var text = pageData?.textContent ?: ""

      if (text.isBlank() || text.length < 30 || text.contains("Title & Cover", ignoreCase = true)) {
        // Try page 2 if available
        if (book.totalPages >= 2) {
          val nextPg = repository.getPageContent(book.id, 2)
          if (nextPg != null && nextPg.textContent.isNotBlank() && nextPg.textContent.length > 50) {
            pageNum = 2
            pageData = nextPg
            text = nextPg.textContent
          }
        }
      }

      // If text is still blank, extract on demand from the original PDF file
      if (text.isBlank()) {
        val extracted = PdfManager.extractPageText(getApplication(), book.filePath, pageNum)
        if (extracted.isNotBlank()) {
          text = extracted
          val paragraphs = extracted.split("\n\n").filter { it.isNotBlank() }
          val updated = BookPageTextEntity(
            id = pageData?.id ?: 0,
            bookId = book.id,
            pageNumber = pageNum,
            chapterTitle = pageData?.chapterTitle ?: "Page $pageNum",
            textContent = extracted,
            paragraphCount = paragraphs.size.coerceAtLeast(1)
          )
          _currentPageText.value = updated
          pageData = updated
        }
      }

      _currentPageIndex.value = (pageNum - 1).coerceAtLeast(0)
      val paragraphs = text.split("\n\n").filter { it.isNotBlank() }
      val safeParagraphs = if (paragraphs.isNotEmpty()) {
        paragraphs
      } else {
        listOf("Reading ${book.title} by ${book.author}.")
      }

      ttsPlayer.loadBookForPlayback(
        bookId = book.id,
        bookTitle = book.title,
        author = book.author,
        coverColorHex = book.coverColorHex,
        coverPath = book.coverPath,
        chapterName = pageData?.chapterTitle ?: "Page $pageNum",
        pageNumber = pageNum,
        paragraphs = safeParagraphs,
        startParagraphIndex = 0,
        autoPlay = true
      )

      if (openFullPlayer) {
        _activeScreen.value = ActiveScreen.AUDIO_FULL
      }
    }
  }

  fun playCurrentBookAudio(startFromCurrentPage: Boolean = true) {
    val book = _currentReadingBook.value ?: allBooks.value.firstOrNull() ?: return
    val page = if (startFromCurrentPage) _currentPageIndex.value + 1 else book.currentPage
    playBookAudio(book, page, openFullPlayer = true)
  }

  private suspend fun advanceAudioToNextPage() {
    val currentBook = _currentReadingBook.value ?: return
    val currentPage = ttsPlayer.playbackState.value.pageNumber
    if (currentPage < currentBook.totalPages) {
      val nextPage = currentPage + 1
      val pageData = repository.getPageContent(currentBook.id, nextPage)
      var text = pageData?.textContent ?: ""
      if (text.isBlank()) {
        text = PdfManager.extractPageText(getApplication(), currentBook.filePath, nextPage)
      }
      val paragraphs = text.split("\n\n").filter { it.isNotBlank() }
      if (paragraphs.isNotEmpty()) {
        ttsPlayer.loadBookForPlayback(
          bookId = currentBook.id,
          bookTitle = currentBook.title,
          author = currentBook.author,
          coverColorHex = currentBook.coverColorHex,
          coverPath = currentBook.coverPath,
          chapterName = pageData?.chapterTitle ?: "Page $nextPage",
          pageNumber = nextPage,
          paragraphs = paragraphs,
          startParagraphIndex = 0,
          autoPlay = true
        )
        _currentPageIndex.value = nextPage - 1
        repository.updateReadingProgress(currentBook.id, nextPage, currentBook.totalPages)
      }
    }
  }

  fun toggleAudioPlayback() {
    ttsPlayer.togglePlayPause()
  }

  fun skipAudioSeconds(seconds: Int) {
    ttsPlayer.skipSeconds(seconds)
  }

  fun setPlaybackSpeed(speed: Float) {
    ttsPlayer.setPlaybackSpeed(speed)
  }

  fun setSleepTimer(minutes: Int) {
    ttsPlayer.setSleepTimer(minutes)
  }

  fun toggleBookmarkCurrentPage() {
    val book = _currentReadingBook.value ?: return
    val pageNum = _currentPageIndex.value + 1
    val chapter = _currentPageText.value?.chapterTitle ?: "Page $pageNum"
    viewModelScope.launch {
      repository.toggleBookmark(book.id, book.title, pageNum, chapter)
    }
  }

  fun addHighlight(text: String, color: String = "yellow", note: String = "") {
    val book = _currentReadingBook.value ?: return
    val pageNum = _currentPageIndex.value + 1
    val rawChapter = _currentPageText.value?.chapterTitle ?: ""
    val chapter = if (rawChapter.startsWith("Page ", ignoreCase = true)) "" else rawChapter
    val trimmed = text.trim()
    if (trimmed.isEmpty()) return

    viewModelScope.launch {
      // Find existing non-bookmark annotation on this page matching exact text
      val existing = allAnnotations.value.firstOrNull {
        it.bookId == book.id && it.pageNumber == pageNum && it.type != "BOOKMARK" &&
          it.selectedText.trim().equals(trimmed, ignoreCase = true)
      }

      if (existing != null) {
        val updated = existing.copy(
          color = color,
          noteContent = if (note.isNotBlank()) note else existing.noteContent,
          type = if (note.isNotBlank() || existing.noteContent.isNotBlank()) "NOTE" else "HIGHLIGHT"
        )
        repository.addAnnotation(updated)
      } else {
        val annotation = AnnotationEntity(
          id = UUID.randomUUID().toString(),
          bookId = book.id,
          bookTitle = book.title,
          pageNumber = pageNum,
          type = if (note.isNotBlank()) "NOTE" else "HIGHLIGHT",
          color = color,
          selectedText = trimmed,
          noteContent = note,
          chapterName = chapter,
          createdAt = System.currentTimeMillis()
        )
        repository.addAnnotation(annotation)
      }
    }
  }

  fun removeHighlightForText(text: String) {
    val book = _currentReadingBook.value ?: return
    val pageNum = _currentPageIndex.value + 1
    val trimmed = text.trim()
    viewModelScope.launch {
      val existing = allAnnotations.value.firstOrNull {
        it.bookId == book.id && it.pageNumber == pageNum && it.type != "BOOKMARK" &&
          (it.selectedText.trim().equals(trimmed, ignoreCase = true) ||
           (it.selectedText.contains(trimmed, ignoreCase = true) && trimmed.length >= 4) ||
           (trimmed.contains(it.selectedText, ignoreCase = true) && it.selectedText.length >= 4))
      }
      if (existing != null) {
        repository.deleteAnnotation(existing.id)
      }
    }
  }

  fun updateAnnotation(id: String, note: String, color: String? = null) {
    viewModelScope.launch {
      val existing = allAnnotations.value.firstOrNull { it.id == id } ?: return@launch
      val updated = existing.copy(
        noteContent = note,
        color = color ?: existing.color,
        type = if (note.isNotBlank()) "NOTE" else "HIGHLIGHT"
      )
      repository.addAnnotation(updated)
    }
  }

  fun deleteAnnotation(id: String) {
    viewModelScope.launch {
      repository.deleteAnnotation(id)
    }
  }

  fun importPdf(uri: Uri) {
    viewModelScope.launch {
      _isImporting.value = true
      try {
        val book = repository.importPdf(uri)
        refreshStorageUsage()
        openBook(book)
      } catch (e: Exception) {
        e.printStackTrace()
      } finally {
        _isImporting.value = false
      }
    }
  }

  fun deleteBook(bookId: String) {
    viewModelScope.launch {
      if (_currentReadingBook.value?.id == bookId) {
        _currentReadingBook.value = null
        _activeScreen.value = ActiveScreen.MAIN_TABS
      }
      if (ttsPlayer.playbackState.value.bookId == bookId) {
        ttsPlayer.stopAndClear()
      }
      repository.deleteBook(bookId)
      refreshStorageUsage()
    }
  }

  fun renameBook(bookId: String, newTitle: String) {
    val trimmed = newTitle.trim()
    if (trimmed.isEmpty()) return
    viewModelScope.launch {
      repository.renameBook(bookId, trimmed)
      if (_currentReadingBook.value?.id == bookId) {
        _currentReadingBook.value = _currentReadingBook.value?.copy(title = trimmed)
      }
      if (ttsPlayer.playbackState.value.bookId == bookId) {
        ttsPlayer.updateBookTitle(trimmed)
      }
    }
  }

  fun toggleOfflineDialog(show: Boolean) {
    _showOfflineDialog.value = show
  }

  fun prepareBookForOffline(bookId: String) {
    viewModelScope.launch {
      repository.prepareBookForOffline(bookId)
    }
  }

  private suspend fun refreshStorageUsage() {
    val bytes = repository.getStorageUsageBytes()
    _storageUsageBytes.value = bytes
  }
}
