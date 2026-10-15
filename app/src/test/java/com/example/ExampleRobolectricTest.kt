package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.AppDatabase
import com.example.data.model.BookEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
  fun `database inserts and retrieves book entity`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = AppDatabase.getDatabase(context)
    val testBook = BookEntity(
      id = "test_book_1",
      title = "Atomic Habits",
      author = "James Clear",
      filePath = "/dummy/path.pdf",
      totalPages = 10,
      currentPage = 1,
      readingProgress = 0.1f
    )
    db.bookDao().insertBook(testBook)
    val retrieved = db.bookDao().getBookByIdSync("test_book_1")
    assertNotNull(retrieved)
    assertEquals("Atomic Habits", retrieved?.title)
    assertEquals("James Clear", retrieved?.author)
  }
}
