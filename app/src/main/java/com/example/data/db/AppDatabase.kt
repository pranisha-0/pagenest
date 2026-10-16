package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.AnnotationEntity
import com.example.data.model.BookEntity
import com.example.data.model.BookPageTextEntity

@Database(
  entities = [BookEntity::class, AnnotationEntity::class, BookPageTextEntity::class],
  version = 2,
  exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
  abstract fun bookDao(): BookDao
  abstract fun annotationDao(): AnnotationDao
  abstract fun bookPageTextDao(): BookPageTextDao

  companion object {
    @Volatile
    private var INSTANCE: AppDatabase? = null

    val MIGRATION_1_2 = object : Migration(1, 2) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE books ADD COLUMN originalFileName TEXT NOT NULL DEFAULT ''")
      }
    }

    fun getDatabase(context: Context): AppDatabase {
      return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
          context.applicationContext,
          AppDatabase::class.java,
          "pagenest_database"
        )
          .addMigrations(MIGRATION_1_2)
          .fallbackToDestructiveMigration()
          .build()
        INSTANCE = instance
        instance
      }
    }
  }
}
