package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.AppDatabase
import com.example.data.model.AnnotationEntity
import com.example.data.model.BookEntity
import com.example.data.repository.BookRepository
import com.example.pdf.PdfStructureParser
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Unfold", appName)
  }

  @Test
  fun `database inserts, renames, and retrieves book entity`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = AppDatabase.getDatabase(context)
    val testBook = BookEntity(
      id = "test_book_1",
      title = "Introduction to Electronics",
      author = "Engineering Press",
      filePath = "/dummy/Introduction_to_Electronics_Final_Version.pdf",
      totalPages = 10,
      currentPage = 1,
      readingProgress = 0.1f,
      originalFileName = "Introduction_to_Electronics_Final_Version.pdf"
    )
    db.bookDao().insertBook(testBook)
    val retrieved = db.bookDao().getBookByIdSync("test_book_1")
    assertNotNull(retrieved)
    assertEquals("Introduction to Electronics", retrieved?.title)
    assertEquals("Introduction_to_Electronics_Final_Version.pdf", retrieved?.originalFileName)

    // Test Rename
    db.bookDao().renameBook("test_book_1", "Intro to Electronics 2nd Ed")
    val renamed = db.bookDao().getBookByIdSync("test_book_1")
    assertEquals("Intro to Electronics 2nd Ed", renamed?.title)
    // Original file name and path unchanged
    assertEquals("Introduction_to_Electronics_Final_Version.pdf", renamed?.originalFileName)
    assertEquals("/dummy/Introduction_to_Electronics_Final_Version.pdf", renamed?.filePath)
  }

  @Test
  fun `pdf structure parser preserves paragraphs, headings, and lists`() {
    val rawPdfText = """
      Atomic Habits — James Clear 42
      
      Chapter 1: The Fundamentals
      
      This is the first sentence of the paragraph which was wrapped across
      multiple lines because of page width.
      
      Here are the key laws of behavior change:
      1. Make it obvious.
      2. Make it attractive.
      3. Make it easy.
      
      • Bullet item one
      • Bullet item two
      
      [Illustration page with visual layout]
      
      42
    """.trimIndent()

    val structured = PdfStructureParser.reconstructPageStructure(
      rawPageText = rawPdfText,
      pageNumber = 42,
      documentTitle = "Atomic Habits"
    )

    // Running header and footer page number removed
    assertFalse(structured.contains("Atomic Habits — James Clear 42"))
    assertFalse(structured.endsWith("\n42"))

    // Heading preserved as distinct block
    assertTrue(structured.contains("Chapter 1: The Fundamentals"))

    // Wrapped sentence reconstructed with space
    assertTrue(structured.contains("This is the first sentence of the paragraph which was wrapped across multiple lines because of page width."))

    // Numbered list preserved
    assertTrue(structured.contains("1. Make it obvious."))
    assertTrue(structured.contains("2. Make it attractive."))

    // Bullet list preserved
    assertTrue(structured.contains("• Bullet item one"))

    // Image/illustration placeholder ignored and excluded
    assertFalse(structured.contains("[Illustration"))
    assertFalse(structured.contains("[Image]"))
  }

  @Test
  fun `cleanupSampleBooks clears sample books safely`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repository = BookRepository(context)
    val db = AppDatabase.getDatabase(context)

    // Insert a sample book and a real user book
    val sampleBook = BookEntity(
      id = "atomic_habits",
      title = "Atomic Habits",
      author = "James Clear",
      filePath = "/dummy/sample.pdf",
      isSample = true
    )
    val userBook = BookEntity(
      id = "user_uploaded_123",
      title = "My Real Document",
      author = "Me",
      filePath = "/dummy/real.pdf",
      isSample = false,
      originalFileName = "My Real Document.pdf"
    )
    db.bookDao().insertBook(sampleBook)
    db.bookDao().insertBook(userBook)

    // Run cleanup
    repository.cleanupSampleBooks()

    // Verify sample was deleted but real user book remains intact
    val sampleAfter = db.bookDao().getBookByIdSync("atomic_habits")
    val userAfter = db.bookDao().getBookByIdSync("user_uploaded_123")

    assertEquals(null, sampleAfter)
    assertNotNull(userAfter)
    assertEquals("My Real Document", userAfter?.title)
  }

  @Test
  fun `annotations and highlights add, update note, delete, and prevent duplicates`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repository = BookRepository(context)
    val db = AppDatabase.getDatabase(context)

    val bookId = "annotation_test_book"
    val testBook = BookEntity(
      id = bookId,
      title = "Design Patterns",
      author = "GoF",
      filePath = "/dummy/patterns.pdf",
      totalPages = 50,
      currentPage = 3,
      readingProgress = 0.06f
    )
    db.bookDao().insertBook(testBook)

    // 1. Add Highlight
    val highlight1 = AnnotationEntity(
      id = "ann_1",
      bookId = bookId,
      bookTitle = testBook.title,
      pageNumber = 3,
      type = "HIGHLIGHT",
      color = "yellow",
      selectedText = "Favor object composition over class inheritance.",
      noteContent = "",
      chapterName = "Chapter 1"
    )
    repository.addAnnotation(highlight1)

    var pageAnns = repository.getAnnotationsForPage(bookId, 3).first()
    assertEquals(1, pageAnns.size)
    assertEquals("yellow", pageAnns[0].color)
    assertEquals("Favor object composition over class inheritance.", pageAnns[0].selectedText)

    // 2. Add / Edit Note to the same annotation (updating rather than creating duplicate)
    val updatedNote = highlight1.copy(
      noteContent = "Crucial architectural principle for extensibility.",
      type = "NOTE",
      color = "lavender"
    )
    repository.addAnnotation(updatedNote)

    pageAnns = repository.getAnnotationsForPage(bookId, 3).first()
    assertEquals(1, pageAnns.size)
    assertEquals("NOTE", pageAnns[0].type)
    assertEquals("lavender", pageAnns[0].color)
    assertEquals("Crucial architectural principle for extensibility.", pageAnns[0].noteContent)

    // 3. Search annotations
    val searchResults = repository.searchAnnotations("composition").first()
    assertEquals(1, searchResults.size)
    assertEquals("Favor object composition over class inheritance.", searchResults[0].selectedText)

    // 4. Delete annotation
    repository.deleteAnnotation("ann_1")
    val afterDelete = repository.getAnnotationsForPage(bookId, 3).first()
    assertEquals(0, afterDelete.size)

    // 5. Test cascading deletion when deleting book
    repository.addAnnotation(highlight1)
    assertEquals(1, repository.getAnnotationsForPage(bookId, 3).first().size)
    repository.deleteBook(bookId)
    val afterBookDelete = repository.getAnnotationsForPage(bookId, 3).first()
    assertEquals(0, afterBookDelete.size)
  }

  @Test
  fun `pdf structure parser deduplicates identical paragraphs and overlapping text`() {
    val repeatedText = """
      "Because I don't live in either my past or my future. I'm interested only in the present."

      on the march, I just concentrate on marching. If I have to fight, it will be just as good a day to die as any other.

      "Because I don't live in either my past or my future. I'm interested only in the present."
    """.trimIndent()

    val structured = PdfStructureParser.reconstructPageStructure(
      rawPageText = repeatedText,
      pageNumber = 65,
      documentTitle = "The Alchemist"
    )

    // The repeated paragraph must appear strictly once
    val firstIndex = structured.indexOf("Because I don't live in either my past or my future")
    val lastIndex = structured.lastIndexOf("Because I don't live in either my past or my future")
    assertEquals("Paragraph must appear only once", firstIndex, lastIndex)
    assertTrue(structured.contains("on the march, I just concentrate on marching"))
  }
}
