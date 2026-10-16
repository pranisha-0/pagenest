package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "books")
data class BookEntity(
  @PrimaryKey val id: String,
  val title: String,
  val author: String,
  val filePath: String,
  val coverPath: String? = null,
  val coverColorHex: String = "#5E4F8A",
  val totalPages: Int = 1,
  val currentPage: Int = 1,
  val readingProgress: Float = 0f,
  val dateAdded: Long = System.currentTimeMillis(),
  val lastReadTime: Long = System.currentTimeMillis(),
  val isOfflineAudioReady: Boolean = false,
  val isSample: Boolean = false,
  val category: String = "All", // "Reading", "Finished", "All"
  val totalReadingSeconds: Long = 0L,
  val description: String = "",
  val originalFileName: String = ""
)
