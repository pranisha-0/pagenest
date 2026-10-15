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
import kotlinx.coroutines.flow.combine
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

  // Reader state
  private val _currentReadingBook = MutableStateFlow<BookEntity?>(null)
  val currentReadingBook: StateFlow<BookEntity?> = _currentReadingBook.asStateFlow()

  private val _currentPageIndex = MutableStateFlow(0) // 0-based
  val currentPageIndex: StateFlow<Int> = _currentPageIndex.asStateFlow()

  private val _currentPdfBitmap = MutableStateFlow<Bitmap?>(null)
  val currentPdfBitmap: StateFlow<Bitmap?> = _currentPdfBitmap.asStateFlow()

  private val _currentPageText = MutableStateFlow<BookPageTextEntity?>(null)
  val currentPageText: StateFlow<BookPageTextEntity?> = _currentPageText.asStateFlow()

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

  // Dialogs
  private val _showOfflineDialog = MutableStateFlow(false)
  val showOfflineDialog: StateFlow<Boolean> = _showOfflineDialog.asStateFlow()

  private val _isImporting = MutableStateFlow(false)
  val isImporting: StateFlow<Boolean> = _isImporting.asStateFlow()

  private val _storageUsageBytes = MutableStateFlow(0L)
  val storageUsageBytes: StateFlow<Long> = _storageUsageBytes.asStateFlow()

  init {
    viewModelScope.launch {
      repository.seedStarterLibraryIfNeeded()
      refreshStorageUsage()
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

  private suspend fun loadCurrentPageData(book: BookEntity, pageIndex: Int) {
    withContext(Dispatchers.IO) {
      val pageNum = pageIndex + 1
      val textEntity = repository.getPageContent(book.id, pageNum)
      _currentPageText.value = textEntity

      // Render PDF page for original PDF mode
      val bitmap = PdfManager.renderPdfPage(book.filePath, pageIndex, targetWidth = 1080)
      _currentPdfBitmap.value = bitmap
    }
  }

  fun playCurrentBookAudio(startFromCurrentPage: Boolean = true) {
    val book = _currentReadingBook.value ?: return
    viewModelScope.launch {
      val pageNum = if (startFromCurrentPage) _currentPageIndex.value + 1 else 1
      val pageData = repository.getPageContent(book.id, pageNum)
      val text = pageData?.textContent ?: ""
      val paragraphs = text.split("\n\n").filter { it.isNotBlank() }

      ttsPlayer.loadBookForPlayback(
        bookId = book.id,
        bookTitle = book.title,
        author = book.author,
        coverColorHex = book.coverColorHex,
        coverPath = book.coverPath,
        chapterName = pageData?.chapterTitle ?: "Chapter $pageNum",
        pageNumber = pageNum,
        paragraphs = if (paragraphs.isNotEmpty()) paragraphs else listOf("Reading ${book.title} by ${book.author}."),
        startParagraphIndex = 0,
        autoPlay = true
      )
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
    val chapter = _currentPageText.value?.chapterTitle ?: "Page $pageNum"
    viewModelScope.launch {
      val annotation = AnnotationEntity(
        id = UUID.randomUUID().toString(),
        bookId = book.id,
        bookTitle = book.title,
        pageNumber = pageNum,
        type = if (note.isNotBlank()) "NOTE" else "HIGHLIGHT",
        color = color,
        selectedText = text,
        noteContent = note,
        chapterName = chapter,
        createdAt = System.currentTimeMillis()
      )
      repository.addAnnotation(annotation)
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
    viewModelScope.launch {
      repository.renameBook(bookId, newTitle)
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
