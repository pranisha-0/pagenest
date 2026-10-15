package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "annotations")
data class AnnotationEntity(
  @PrimaryKey val id: String,
  val bookId: String,
  val bookTitle: String,
  val pageNumber: Int,
  val type: String, // "HIGHLIGHT", "NOTE", "BOOKMARK"
  val color: String = "yellow", // "yellow", "lavender", "green", "pink"
  val selectedText: String = "",
  val noteContent: String = "",
  val chapterName: String = "",
  val createdAt: Long = System.currentTimeMillis()
)
