package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AddBookCard
import com.example.ui.components.BookCard
import com.example.ui.viewmodel.MainTab
import com.example.ui.viewmodel.PageNestViewModel

@Composable
fun HomeScreen(
  viewModel: PageNestViewModel,
  modifier: Modifier = Modifier
) {
  val books by viewModel.allBooks.collectAsState()
  val libraryFilter by viewModel.libraryFilter.collectAsState()
  val searchQuery by viewModel.searchQuery.collectAsState()
  val isImporting by viewModel.isImporting.collectAsState()

  // PDF File Picker Launcher
  val pdfPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument()
  ) { uri: Uri? ->
    uri?.let { viewModel.importPdf(it) }
  }

  // Filter books according to search and category
  val filteredBooks = books.filter { book ->
    val matchesFilter = when (libraryFilter) {
      "Reading" -> book.readingProgress in 0.01f..0.99f
      "Finished" -> book.readingProgress >= 0.99f
      else -> true
    }
    val matchesSearch = searchQuery.isBlank() ||
      book.title.contains(searchQuery, ignoreCase = true) ||
      book.author.contains(searchQuery, ignoreCase = true)

    matchesFilter && matchesSearch
  }

  val continueReadingBook = books.maxByOrNull { it.lastReadTime }

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .testTag("home_screen_lazy_column"),
    contentPadding = PaddingValues(bottom = 90.dp)
  ) {
    // 1. Top Brand Header ("Unfold" and "Read. Listen. Download.") - Removed 100% offline badge per request
    item {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp)
      ) {
        Column {
          Text(
            text = "Unfold",
            style = MaterialTheme.typography.headlineLarge.copy(
              fontFamily = FontFamily.Serif,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onBackground
            )
          )
          Text(
            text = "Read. Listen. Download.",
            style = MaterialTheme.typography.bodySmall.copy(
              color = MaterialTheme.colorScheme.primary,
              fontWeight = FontWeight.Medium,
              letterSpacing = 0.5.sp
            )
          )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Search Bar
        OutlinedTextField(
          value = searchQuery,
          onValueChange = { viewModel.setSearchQuery(it) },
          placeholder = { Text("Search your books...", style = MaterialTheme.typography.bodyMedium) },
          leadingIcon = {
            Icon(
              imageVector = Icons.Default.Search,
              contentDescription = "Search",
              tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
          },
          singleLine = true,
          shape = RoundedCornerShape(14.dp),
          colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
            focusedBorderColor = MaterialTheme.colorScheme.primary
          ),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("search_books_input")
        )
      }
    }

    // 2. Feature Action Circular Pills (Removed Offline Access; Renamed Listen (AI Voice) -> Listen)
    item {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        ActionPill(
          icon = Icons.Default.UploadFile,
          title = "Upload\nPDF Books",
          tag = "upload_pdf_btn",
          onClick = { pdfPickerLauncher.launch(arrayOf("application/pdf")) }
        )
        ActionPill(
          icon = Icons.Default.AutoStories,
          title = "Read &\nHighlight",
          tag = "read_highlight_btn",
          onClick = {
            if (books.isNotEmpty()) {
              viewModel.openBook(books.first())
            } else {
              pdfPickerLauncher.launch(arrayOf("application/pdf"))
            }
          }
        )
        ActionPill(
          icon = Icons.Default.Headphones,
          title = "Listen",
          tag = "listen_voice_btn",
          onClick = {
            if (books.isNotEmpty()) {
              viewModel.openBook(books.first())
              viewModel.playCurrentBookAudio()
            } else {
              pdfPickerLauncher.launch(arrayOf("application/pdf"))
            }
          }
        )
        ActionPill(
          icon = Icons.Default.EditNote,
          title = "Notes &\nBookmarks",
          tag = "notes_bookmarks_btn",
          onClick = { viewModel.setTab(MainTab.NOTES) }
        )
      }
    }

    // Importing indicator
    if (isImporting) {
      item {
        Card(
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
          Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            CircularProgressIndicator(
              modifier = Modifier.size(24.dp),
              color = MaterialTheme.colorScheme.primary,
              strokeWidth = 2.5.dp
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column {
              Text(
                text = "Importing & Extracting PDF...",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
              )
              Text(
                text = "Preparing pages and offline audio on device",
                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
              )
            }
          }
        }
      }
    }

    // 3. Continue Reading / Listen Section (Replaced Atomic Habits with Listen prompt when empty)
    item {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 20.dp, vertical = 8.dp)
      ) {
        Text(
          text = if (continueReadingBook != null) "Continue Reading" else "Listen to Audiobooks",
          style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
          )
        )

        Spacer(modifier = Modifier.height(10.dp))

        if (continueReadingBook != null) {
          val book = continueReadingBook
          Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            border = androidx.compose.foundation.BorderStroke(
              1.dp,
              MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
            ),
            modifier = Modifier
              .fillMaxWidth()
              .clickable { viewModel.openBook(book) }
              .testTag("continue_reading_card")
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              val coverBg = try {
                Color(android.graphics.Color.parseColor(book.coverColorHex))
              } catch (e: Exception) {
                MaterialTheme.colorScheme.primary
              }

              Box(
                modifier = Modifier
                  .size(54.dp)
                  .clip(RoundedCornerShape(10.dp))
                  .background(coverBg),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.AutoStories,
                  contentDescription = null,
                  tint = Color.White,
                  modifier = Modifier.size(26.dp)
                )
              }

              Spacer(modifier = Modifier.width(14.dp))

              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = book.title,
                  style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
                val percent = (book.readingProgress * 100).toInt()
                Text(
                  text = "Page ${book.currentPage} of ${book.totalPages} • $percent%",
                  style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                  )
                )
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                  progress = { book.readingProgress },
                  modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                  color = MaterialTheme.colorScheme.primary,
                  trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
              }

              Spacer(modifier = Modifier.width(10.dp))

              Box(
                modifier = Modifier
                  .size(36.dp)
                  .clip(CircleShape)
                  .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                  contentDescription = "Open",
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(18.dp)
                )
              }
            }
          }
        } else {
          // Clean "Listen" Card when no book is imported
          Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            border = androidx.compose.foundation.BorderStroke(
              1.dp,
              MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
            ),
            modifier = Modifier
              .fillMaxWidth()
              .clickable { pdfPickerLauncher.launch(arrayOf("application/pdf")) }
              .testTag("listen_empty_card")
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Box(
                modifier = Modifier
                  .size(52.dp)
                  .clip(CircleShape)
                  .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.Headphones,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(26.dp)
                )
              }

              Spacer(modifier = Modifier.width(14.dp))

              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "Listen to your books",
                  style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                  text = "Import a PDF to listen with natural offline voice narration",
                  style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                  )
                )
              }

              Spacer(modifier = Modifier.width(8.dp))

              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(20.dp))
                  .background(MaterialTheme.colorScheme.primary)
                  .padding(horizontal = 14.dp, vertical = 8.dp)
              ) {
                Text(
                  text = "Add PDF",
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                  )
                )
              }
            }
          }
        }
      }
    }

    // 4. My Library Section
    item {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 16.dp)
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "My Library",
            style = MaterialTheme.typography.titleMedium.copy(
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onBackground
            )
          )

          // Filter chips
          Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("All", "Reading", "Finished").forEach { filter ->
              val isSelected = libraryFilter == filter
              FilterChip(
                selected = isSelected,
                onClick = { viewModel.setLibraryFilter(filter) },
                label = {
                  Text(
                    text = filter,
                    style = MaterialTheme.typography.labelSmall.copy(
                      fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                  )
                },
                shape = RoundedCornerShape(16.dp),
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = MaterialTheme.colorScheme.primary,
                  selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                  containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                  labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                border = null,
                modifier = Modifier.testTag("filter_chip_$filter")
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (filteredBooks.isEmpty()) {
          // Empty State Card: "No PDFs Imported - Add Your PDF"
          Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(
              1.dp,
              MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            ),
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 20.dp, vertical = 6.dp)
              .clickable { pdfPickerLauncher.launch(arrayOf("application/pdf")) }
              .testTag("empty_library_card")
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Box(
                modifier = Modifier
                  .size(54.dp)
                  .clip(CircleShape)
                  .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.PictureAsPdf,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(28.dp)
                )
              }
              Spacer(modifier = Modifier.height(12.dp))
              Text(
                text = "No PDFs Imported",
                style = MaterialTheme.typography.titleMedium.copy(
                  fontFamily = FontFamily.Serif,
                  fontWeight = FontWeight.Bold
                )
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "Tap to search and add your PDF from your device",
                style = MaterialTheme.typography.bodySmall.copy(
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  textAlign = TextAlign.Center
                ),
                textAlign = TextAlign.Center
              )
              Spacer(modifier = Modifier.height(14.dp))
              Button(
                onClick = { pdfPickerLauncher.launch(arrayOf("application/pdf")) },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.testTag("add_your_pdf_btn")
              ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add Your PDF")
              }
            }
          }
        } else {
          // Books horizontal scroll row with Add Book Card
          LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            items(filteredBooks, key = { it.id }) { book ->
              BookCard(
                book = book,
                onClick = { viewModel.openBook(book) },
                onAudioClick = {
                  viewModel.openBook(book)
                  viewModel.playCurrentBookAudio()
                },
                onDeleteClick = { viewModel.deleteBook(book.id) },
                onRenameClick = { newTitle -> viewModel.renameBook(book.id, newTitle) }
              )
            }

            item {
              AddBookCard(
                onClick = { pdfPickerLauncher.launch(arrayOf("application/pdf")) }
              )
            }
          }
        }
      }
    }

    // 5. Recently Added Section (only shown when user has added multiple books)
    if (books.size > 1) {
      item {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
          Text(
            text = "Recently Added",
            style = MaterialTheme.typography.titleMedium.copy(
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onBackground
            )
          )

          Spacer(modifier = Modifier.height(12.dp))

          LazyRow(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            items(books.reversed(), key = { "recent_${it.id}" }) { book ->
              BookCard(
                book = book,
                onClick = { viewModel.openBook(book) },
                onAudioClick = {
                  viewModel.openBook(book)
                  viewModel.playCurrentBookAudio()
                },
                onDeleteClick = { viewModel.deleteBook(book.id) },
                onRenameClick = { newTitle -> viewModel.renameBook(book.id, newTitle) }
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun ActionPill(
  icon: ImageVector,
  title: String,
  tag: String,
  onClick: () -> Unit
) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier
      .clickable { onClick() }
      .testTag(tag)
  ) {
    Box(
      modifier = Modifier
        .size(54.dp)
        .clip(CircleShape)
        .background(MaterialTheme.colorScheme.surfaceVariant),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = icon,
        contentDescription = title,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(24.dp)
      )
    }

    Spacer(modifier = Modifier.height(6.dp))

    Text(
      text = title,
      style = MaterialTheme.typography.labelSmall.copy(
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      ),
      textAlign = TextAlign.Center,
      lineHeight = 13.sp
    )
  }
}
