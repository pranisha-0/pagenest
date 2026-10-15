package com.example.data.repository

import android.content.Context
import android.graphics.Color
import android.net.Uri
import com.example.data.db.AppDatabase
import com.example.data.model.AnnotationEntity
import com.example.data.model.BookEntity
import com.example.data.model.BookPageTextEntity
import com.example.pdf.PdfManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class BookRepository(private val context: Context) {

  private val database = AppDatabase.getDatabase(context)
  private val bookDao = database.bookDao()
  private val annotationDao = database.annotationDao()
  private val bookPageTextDao = database.bookPageTextDao()

  val allBooks: Flow<List<BookEntity>> = bookDao.getAllBooks()
  val allAnnotations: Flow<List<AnnotationEntity>> = annotationDao.getAllAnnotations()

  fun getBook(id: String): Flow<BookEntity?> = bookDao.getBookById(id)

  fun getAnnotationsForBook(bookId: String): Flow<List<AnnotationEntity>> =
    annotationDao.getAnnotationsForBook(bookId)

  fun getAnnotationsForPage(bookId: String, page: Int): Flow<List<AnnotationEntity>> =
    annotationDao.getAnnotationsForPage(bookId, page)

  fun isPageBookmarked(bookId: String, page: Int): Flow<Boolean> =
    annotationDao.isPageBookmarked(bookId, page)

  fun getPagesForBook(bookId: String): Flow<List<BookPageTextEntity>> =
    bookPageTextDao.getPagesForBook(bookId)

  suspend fun getPageContent(bookId: String, page: Int): BookPageTextEntity? =
    bookPageTextDao.getPageSync(bookId, page)

  suspend fun updateReadingProgress(bookId: String, page: Int, totalPages: Int) = withContext(Dispatchers.IO) {
    val progress = (page.toFloat() / totalPages.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
    bookDao.updateProgress(bookId, page, progress)
  }

  suspend fun addReadingTime(bookId: String, seconds: Long) = withContext(Dispatchers.IO) {
    bookDao.addReadingSeconds(bookId, seconds)
  }

  suspend fun renameBook(bookId: String, newTitle: String) = withContext(Dispatchers.IO) {
    bookDao.renameBook(bookId, newTitle)
  }

  suspend fun deleteBook(bookId: String) = withContext(Dispatchers.IO) {
    val book = bookDao.getBookByIdSync(bookId)
    if (book != null) {
      try {
        val file = File(book.filePath)
        if (file.exists()) file.delete()
        if (book.coverPath != null) {
          val cover = File(book.coverPath)
          if (cover.exists()) cover.delete()
        }
      } catch (e: Exception) {
        e.printStackTrace()
      }
      bookDao.deleteBookById(bookId)
      annotationDao.deleteAnnotationById(bookId)
      bookPageTextDao.deletePagesForBook(bookId)
    }
  }

  suspend fun addAnnotation(annotation: AnnotationEntity) = withContext(Dispatchers.IO) {
    annotationDao.insertAnnotation(annotation)
  }

  suspend fun toggleBookmark(bookId: String, bookTitle: String, page: Int, chapterName: String) = withContext(Dispatchers.IO) {
    // Check if bookmarked
    val current = annotationDao.getAnnotationsForPage(bookId, page)
    // We can insert or remove
    val bookmark = AnnotationEntity(
      id = "bm_${bookId}_$page",
      bookId = bookId,
      bookTitle = bookTitle,
      pageNumber = page,
      type = "BOOKMARK",
      color = "lavender",
      selectedText = "Page $page Bookmark",
      noteContent = "Bookmarked on page $page",
      chapterName = chapterName,
      createdAt = System.currentTimeMillis()
    )
    annotationDao.insertAnnotation(bookmark)
  }

  suspend fun removeBookmark(bookId: String, page: Int) = withContext(Dispatchers.IO) {
    annotationDao.removeBookmark(bookId, page)
  }

  suspend fun deleteAnnotation(annotationId: String) = withContext(Dispatchers.IO) {
    annotationDao.deleteAnnotationById(annotationId)
  }

  suspend fun updateAnnotation(annotation: AnnotationEntity) = withContext(Dispatchers.IO) {
    annotationDao.updateAnnotation(annotation)
  }

  fun searchAnnotations(query: String): Flow<List<AnnotationEntity>> =
    annotationDao.searchAnnotations(query)

  suspend fun searchInBook(bookId: String, query: String): List<BookPageTextEntity> =
    withContext(Dispatchers.IO) {
      bookPageTextDao.searchInBook(bookId, query)
    }

  suspend fun importPdf(uri: Uri): BookEntity = withContext(Dispatchers.IO) {
    val (book, pages) = PdfManager.importPdfFromUri(context, uri)
    bookDao.insertBook(book)
    if (pages.isNotEmpty()) {
      bookPageTextDao.insertPages(pages)
    }
    book
  }

  suspend fun prepareBookForOffline(bookId: String): Boolean = withContext(Dispatchers.IO) {
    val book = bookDao.getBookByIdSync(bookId) ?: return@withContext false
    val existingPages = bookPageTextDao.getPagesForBookSync(bookId)
    if (existingPages.isEmpty()) {
      val file = File(book.filePath)
      if (file.exists()) {
        val (newBook, pages) = PdfManager.importPdfFromUri(context, Uri.fromFile(file))
        bookPageTextDao.insertPages(pages)
      }
    }
    bookDao.updateOfflineAudioReady(bookId, true)
    true
  }

  suspend fun getStorageUsageBytes(): Long = withContext(Dispatchers.IO) {
    var size = 0L
    try {
      val booksDir = File(context.filesDir, "books")
      if (booksDir.exists()) {
        booksDir.listFiles()?.forEach { size += it.length() }
      }
      val coversDir = File(context.filesDir, "covers")
      if (coversDir.exists()) {
        coversDir.listFiles()?.forEach { size += it.length() }
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }
    size
  }

  /**
   * Ensures no mock/pre-seeded books or annotations remain.
   * All content is authentically imported and created by the user.
   */
  suspend fun seedStarterLibraryIfNeeded() = withContext(Dispatchers.IO) {
    val sampleIds = listOf("atomic_habits", "psychology_of_money", "dopamine_nation", "deep_work")
    for (id in sampleIds) {
      val b = bookDao.getBookByIdSync(id)
      if (b != null) {
        deleteBook(id)
      }
    }
    // Also remove sample annotations
    val sampleAnnIds = listOf("ann_1", "ann_2", "ann_3", "ann_4")
    for (annId in sampleAnnIds) {
      annotationDao.deleteAnnotationById(annId)
    }
  }
}
