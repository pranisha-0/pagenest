package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.BookEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {
  @Query("SELECT * FROM books ORDER BY lastReadTime DESC")
  fun getAllBooks(): Flow<List<BookEntity>>

  @Query("SELECT * FROM books WHERE id = :id LIMIT 1")
  fun getBookById(id: String): Flow<BookEntity?>

  @Query("SELECT * FROM books WHERE id = :id LIMIT 1")
  suspend fun getBookByIdSync(id: String): BookEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertBook(book: BookEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertBooks(books: List<BookEntity>)

  @Update
  suspend fun updateBook(book: BookEntity)

  @Query("UPDATE books SET currentPage = :page, readingProgress = :progress, lastReadTime = :time WHERE id = :bookId")
  suspend fun updateProgress(bookId: String, page: Int, progress: Float, time: Long = System.currentTimeMillis())

  @Query("UPDATE books SET isOfflineAudioReady = :isReady WHERE id = :bookId")
  suspend fun updateOfflineAudioReady(bookId: String, isReady: Boolean)

  @Query("UPDATE books SET totalReadingSeconds = totalReadingSeconds + :addedSeconds WHERE id = :bookId")
  suspend fun addReadingSeconds(bookId: String, addedSeconds: Long)

  @Query("UPDATE books SET title = :newTitle WHERE id = :bookId")
  suspend fun renameBook(bookId: String, newTitle: String)

  @Query("SELECT * FROM books WHERE isSample = 1 OR id IN ('atomic_habits', 'psychology_of_money', 'dopamine_nation')")
  suspend fun getSampleBooks(): List<BookEntity>

  @Query("DELETE FROM books WHERE isSample = 1 OR id IN ('atomic_habits', 'psychology_of_money', 'dopamine_nation')")
  suspend fun deleteSampleBooks()

  @Delete
  suspend fun deleteBook(book: BookEntity)

  @Query("DELETE FROM books WHERE id = :id")
  suspend fun deleteBookById(id: String)
}
