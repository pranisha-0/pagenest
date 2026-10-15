package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "book_pages")
data class BookPageTextEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val bookId: String,
  val pageNumber: Int,
  val chapterTitle: String = "",
  val textContent: String = "",
  val paragraphCount: Int = 0
)
