package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AnnotationEntity
import com.example.ui.theme.HighlightGreen
import com.example.ui.theme.HighlightLavender
import com.example.ui.theme.HighlightPink
import com.example.ui.theme.HighlightYellow
import com.example.ui.viewmodel.PageNestViewModel

@Composable
fun AnnotationsScreen(
  viewModel: PageNestViewModel,
  modifier: Modifier = Modifier
) {
  val annotations by viewModel.allAnnotations.collectAsState()
  val allBooks by viewModel.allBooks.collectAsState()
  val filter by viewModel.annotationFilter.collectAsState()
  val searchQuery by viewModel.annotationSearchQuery.collectAsState()
  var editingAnnotation by remember { mutableStateOf<AnnotationEntity?>(null) }

  val filteredAnnotations = annotations.filter { ann ->
    val matchesFilter = when (filter) {
      "Highlights" -> ann.type == "HIGHLIGHT"
      "Notes" -> ann.type == "NOTE"
      "Bookmarks" -> ann.type == "BOOKMARK"
      else -> true
    }
    val matchesSearch = searchQuery.isBlank() ||
      ann.selectedText.contains(searchQuery, ignoreCase = true) ||
      ann.noteContent.contains(searchQuery, ignoreCase = true) ||
      ann.bookTitle.contains(searchQuery, ignoreCase = true)

    matchesFilter && matchesSearch
  }

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 20.dp)
      .testTag("annotations_screen_list"),
    contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    // 1. Header
    item {
      Column(modifier = Modifier.fillMaxWidth()) {
        Text(
          text = "Highlights & Notes",
          style = MaterialTheme.typography.headlineMedium.copy(
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold
          )
        )
        Text(
          text = "Keep track of what matters. Revisit your thoughts anytime.",
          style = MaterialTheme.typography.bodySmall.copy(
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Search Input
        OutlinedTextField(
          value = searchQuery,
          onValueChange = { viewModel.setAnnotationSearchQuery(it) },
          placeholder = { Text("Search your notes & quotes...") },
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
            focusedContainerColor = MaterialTheme.colorScheme.surface
          ),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("search_annotations_input")
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Filter Pills: All, Highlights, Notes, Bookmarks
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          listOf("All", "Highlights", "Notes", "Bookmarks").forEach { item ->
            val isSelected = filter == item
            FilterChip(
              selected = isSelected,
              onClick = { viewModel.setAnnotationFilter(item) },
              label = {
                Text(
                  text = item,
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
              modifier = Modifier.testTag("annotation_filter_$item")
            )
          }
        }
      }
    }

    // Empty state
    if (filteredAnnotations.isEmpty()) {
      item {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
          contentAlignment = Alignment.Center
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
              imageVector = Icons.Default.EditNote,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
              modifier = Modifier.size(52.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
              text = "No annotations yet",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(
              text = "Tap on passages while reading to highlight or add notes.",
              style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )
          }
        }
      }
    }

    // Annotation Cards
    items(filteredAnnotations, key = { it.id }) { item ->
      val dotColor = when (item.color) {
        "yellow" -> HighlightYellow
        "lavender" -> HighlightLavender
        "green" -> HighlightGreen
        "pink" -> HighlightPink
        else -> HighlightYellow
      }

      var showMenu by remember { mutableStateOf(false) }

      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = androidx.compose.foundation.BorderStroke(
          1.dp,
          MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
        ),
        modifier = Modifier
          .fillMaxWidth()
          .clickable {
            // Jump to book and page
            allBooks.find { it.id == item.bookId }?.let { book ->
              viewModel.openBook(book, item.pageNumber)
            }
          }
          .testTag("annotation_card_${item.id}")
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            // Left colored dot
            Box(
              modifier = Modifier
                .padding(top = 4.dp)
                .size(10.dp)
                .clip(CircleShape)
                .background(dotColor)
                .border(1.dp, Color.Black.copy(alpha = 0.1f), CircleShape)
            )

            Spacer(modifier = Modifier.width(10.dp))

            // Main Quote & Text
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "\"${item.selectedText}\"",
                style = MaterialTheme.typography.bodyLarge.copy(
                  fontFamily = FontFamily.Serif,
                  fontWeight = FontWeight.Medium,
                  lineHeight = 22.sp,
                  color = MaterialTheme.colorScheme.onSurface
                ),
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
              )

              // Note body if present
              if (item.noteContent.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                  modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(8.dp)
                ) {
                  Text(
                    text = item.noteContent,
                    style = MaterialTheme.typography.bodySmall.copy(
                      fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  )
                }
              }

              Spacer(modifier = Modifier.height(8.dp))

              // Metadata subtitle (e.g. "Chapter 3 • Page 42 • Highlight")
              val chapterPrefix = if (item.chapterName.isNotBlank()) "${item.chapterName} • " else ""
              val typeLabel = when (item.type) {
                "NOTE" -> "Note"
                "BOOKMARK" -> "Bookmark"
                else -> "Highlight"
              }
              Text(
                text = "${item.bookTitle} • ${chapterPrefix}Page ${item.pageNumber} • $typeLabel",
                style = MaterialTheme.typography.bodySmall.copy(
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  fontSize = 11.sp
                )
              )
            }

            // Options menu
            Box {
              IconButton(
                onClick = { showMenu = true },
                modifier = Modifier.size(28.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.MoreVert,
                  contentDescription = "More",
                  tint = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.size(16.dp)
                )
              }

              DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
              ) {
                DropdownMenuItem(
                  text = { Text("Go to Page") },
                  onClick = {
                    showMenu = false
                    allBooks.find { it.id == item.bookId }?.let { book ->
                      viewModel.openBook(book, item.pageNumber)
                    }
                  },
                  leadingIcon = { Icon(Icons.Default.OpenInNew, contentDescription = null) }
                )
                DropdownMenuItem(
                  text = { Text(if (item.noteContent.isNotBlank()) "Edit Note" else "Add Note") },
                  onClick = {
                    showMenu = false
                    editingAnnotation = item
                  },
                  leadingIcon = { Icon(Icons.Default.EditNote, contentDescription = null) }
                )
                DropdownMenuItem(
                  text = { Text("Delete") },
                  onClick = {
                    showMenu = false
                    viewModel.deleteAnnotation(item.id)
                  },
                  leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) }
                )
              }
            }
          }
        }
      }
    }
  }

  // Edit Note Dialog
  if (editingAnnotation != null) {
    var noteInput by remember(editingAnnotation) { mutableStateOf(editingAnnotation?.noteContent ?: "") }
    AlertDialog(
      onDismissRequest = { editingAnnotation = null },
      title = { Text(if (editingAnnotation?.noteContent?.isNotBlank() == true) "Edit Note" else "Add Note") },
      text = {
        Column(modifier = Modifier.fillMaxWidth()) {
          Text(
            text = "\"${editingAnnotation?.selectedText}\"",
            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
          )
          Spacer(modifier = Modifier.height(10.dp))
          OutlinedTextField(
            value = noteInput,
            onValueChange = { noteInput = it },
            placeholder = { Text("Type your note or thought here...") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
          )
        }
      },
      confirmButton = {
        TextButton(
          onClick = {
            editingAnnotation?.let { ann ->
              viewModel.updateAnnotation(ann.id, noteInput)
            }
            editingAnnotation = null
          }
        ) {
          Text("Save")
        }
      },
      dismissButton = {
        TextButton(onClick = { editingAnnotation = null }) {
          Text("Cancel")
        }
      }
    )
  }
}
