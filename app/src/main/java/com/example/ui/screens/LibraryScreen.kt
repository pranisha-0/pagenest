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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AddBookCard
import com.example.ui.components.BookCard
import com.example.ui.viewmodel.PageNestViewModel

@Composable
fun LibraryScreen(
  viewModel: PageNestViewModel,
  modifier: Modifier = Modifier
) {
  val books by viewModel.allBooks.collectAsState()
  val libraryFilter by viewModel.libraryFilter.collectAsState()
  val searchQuery by viewModel.searchQuery.collectAsState()

  val keyboardController = LocalSoftwareKeyboardController.current

  val pdfPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument()
  ) { uri: Uri? ->
    uri?.let { viewModel.importPdf(it) }
  }

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

  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 20.dp)
      .testTag("library_screen")
  ) {
    Spacer(modifier = Modifier.height(16.dp))

    Text(
      text = "My Library",
      style = MaterialTheme.typography.headlineMedium.copy(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold
      )
    )

    Spacer(modifier = Modifier.height(12.dp))

    // Functional Search Bar with Clear Icon and Keyboard Search
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
      trailingIcon = {
        if (searchQuery.isNotBlank()) {
          IconButton(onClick = { viewModel.setSearchQuery("") }) {
            Icon(
              imageVector = Icons.Default.Clear,
              contentDescription = "Clear search",
              tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      },
      singleLine = true,
      keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
      keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() }),
      shape = RoundedCornerShape(14.dp),
      colors = OutlinedTextFieldDefaults.colors(
        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
        focusedContainerColor = MaterialTheme.colorScheme.surface,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
        focusedBorderColor = MaterialTheme.colorScheme.primary
      ),
      modifier = Modifier
        .fillMaxWidth()
        .testTag("library_search_input")
    )

    Spacer(modifier = Modifier.height(12.dp))

    // Filter Chips: All, Reading, Finished
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
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
          modifier = Modifier.testTag("library_filter_$filter")
        )
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    if (filteredBooks.isEmpty()) {
      // Empty State: "No books found" or "No PDFs imported"
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(bottom = 90.dp),
        contentAlignment = Alignment.Center
      ) {
        Card(
          shape = RoundedCornerShape(20.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
          ),
          modifier = Modifier
            .fillMaxWidth()
            .clickable {
              if (searchQuery.isNotBlank()) viewModel.setSearchQuery("")
              else pdfPickerLauncher.launch(arrayOf("application/pdf"))
            }
            .testTag("library_empty_import_card")
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Box(
              modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = if (searchQuery.isNotBlank()) Icons.Default.Search else Icons.Default.PictureAsPdf,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
              )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
              text = when {
                searchQuery.isNotBlank() -> "No Books Matching \"$searchQuery\""
                books.isEmpty() -> "Your library is empty. Upload a PDF to get started."
                else -> "No $libraryFilter Books"
              },
              style = MaterialTheme.typography.titleLarge.copy(
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold
              ),
              textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
              text = if (searchQuery.isNotBlank()) "Tap to clear the search filter or try searching another term."
              else "Import your PDF books or documents to read, take notes, and listen.",
              style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
              ),
              textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
              onClick = {
                if (searchQuery.isNotBlank()) viewModel.setSearchQuery("")
                else pdfPickerLauncher.launch(arrayOf("application/pdf"))
              },
              shape = RoundedCornerShape(16.dp),
              colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
              modifier = Modifier
                .height(48.dp)
                .testTag("library_add_pdf_btn")
            ) {
              if (searchQuery.isNotBlank()) {
                Text(
                  text = "Clear Search",
                  style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
              } else {
                Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = "Upload PDF",
                  style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
              }
            }
          }
        }
      }
    } else {
      // Books Vertical Grid
      LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 130.dp),
        contentPadding = PaddingValues(bottom = 90.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        modifier = Modifier.fillMaxSize()
      ) {
        items(filteredBooks, key = { it.id }) { book ->
          BookCard(
            book = book,
            onClick = { viewModel.openBook(book) },
            onAudioClick = {
              viewModel.playBookAudio(book, openFullPlayer = true)
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
