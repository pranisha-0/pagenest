package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.AnnotationEntity
import com.example.data.model.BookEntity
import com.example.data.model.BookPageTextEntity

@Database(
  entities = [BookEntity::class, AnnotationEntity::class, BookPageTextEntity::class],
  version = 1,
  exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
  abstract fun bookDao(): BookDao
  abstract fun annotationDao(): AnnotationDao
  abstract fun bookPageTextDao(): BookPageTextDao

  companion object {
    @Volatile
    private var INSTANCE: AppDatabase? = null

    fun getDatabase(context: Context): AppDatabase {
      return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
          context.applicationContext,
          AppDatabase::class.java,
          "pagenest_database"
        )
          .fallbackToDestructiveMigration()
          .build()
        INSTANCE = instance
        instance
      }
    }
  }
}
