package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.BookPageTextEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookPageTextDao {
  @Query("SELECT * FROM book_pages WHERE bookId = :bookId ORDER BY pageNumber ASC")
  fun getPagesForBook(bookId: String): Flow<List<BookPageTextEntity>>

  @Query("SELECT * FROM book_pages WHERE bookId = :bookId ORDER BY pageNumber ASC")
  suspend fun getPagesForBookSync(bookId: String): List<BookPageTextEntity>

  @Query("SELECT * FROM book_pages WHERE bookId = :bookId AND pageNumber = :page LIMIT 1")
  fun getPage(bookId: String, page: Int): Flow<BookPageTextEntity?>

  @Query("SELECT * FROM book_pages WHERE bookId = :bookId AND pageNumber = :page LIMIT 1")
  suspend fun getPageSync(bookId: String, page: Int): BookPageTextEntity?

  @Query("SELECT * FROM book_pages WHERE bookId = :bookId AND textContent LIKE '%' || :query || '%'")
  suspend fun searchInBook(bookId: String, query: String): List<BookPageTextEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertPage(page: BookPageTextEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertPages(pages: List<BookPageTextEntity>)

  @Query("DELETE FROM book_pages WHERE bookId = :bookId")
  suspend fun deletePagesForBook(bookId: String)
}
