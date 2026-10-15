package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.AnnotationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AnnotationDao {
  @Query("SELECT * FROM annotations ORDER BY createdAt DESC")
  fun getAllAnnotations(): Flow<List<AnnotationEntity>>

  @Query("SELECT * FROM annotations WHERE bookId = :bookId ORDER BY pageNumber ASC, createdAt DESC")
  fun getAnnotationsForBook(bookId: String): Flow<List<AnnotationEntity>>

  @Query("SELECT * FROM annotations WHERE bookId = :bookId AND pageNumber = :page ORDER BY createdAt DESC")
  fun getAnnotationsForPage(bookId: String, page: Int): Flow<List<AnnotationEntity>>

  @Query("SELECT * FROM annotations WHERE selectedText LIKE '%' || :query || '%' OR noteContent LIKE '%' || :query || '%'")
  fun searchAnnotations(query: String): Flow<List<AnnotationEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAnnotation(annotation: AnnotationEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAnnotations(annotations: List<AnnotationEntity>)

  @Update
  suspend fun updateAnnotation(annotation: AnnotationEntity)

  @Delete
  suspend fun deleteAnnotation(annotation: AnnotationEntity)

  @Query("DELETE FROM annotations WHERE id = :id")
  suspend fun deleteAnnotationById(id: String)

  @Query("DELETE FROM annotations WHERE bookId = :bookId AND pageNumber = :page AND type = 'BOOKMARK'")
  suspend fun removeBookmark(bookId: String, page: Int)

  @Query("SELECT EXISTS(SELECT 1 FROM annotations WHERE bookId = :bookId AND pageNumber = :page AND type = 'BOOKMARK')")
  fun isPageBookmarked(bookId: String, page: Int): Flow<Boolean>
}
