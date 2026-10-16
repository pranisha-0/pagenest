package com.example.ui.screens

import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FormatColorReset
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TextSnippet
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AnnotationEntity
import com.example.data.model.BookPageTextEntity
import com.example.ui.components.BookFlipContainer
import com.example.ui.components.rememberBookFlipController
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.HighlightGreen
import com.example.ui.theme.HighlightLavender
import com.example.ui.theme.HighlightPink
import com.example.ui.theme.HighlightYellow
import com.example.ui.viewmodel.PageNestViewModel
import com.example.ui.viewmodel.ReadingMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
  viewModel: PageNestViewModel,
  modifier: Modifier = Modifier
) {
  val book by viewModel.currentReadingBook.collectAsState()
  val pageIndex by viewModel.currentPageIndex.collectAsState()
  val currentPdfBitmap by viewModel.currentPdfBitmap.collectAsState()
  val nextPdfBitmap by viewModel.nextPdfBitmap.collectAsState()
  val prevPdfBitmap by viewModel.prevPdfBitmap.collectAsState()

  val currentPageText by viewModel.currentPageText.collectAsState()
  val nextPageText by viewModel.nextPageText.collectAsState()
  val prevPageText by viewModel.prevPageText.collectAsState()

  val readingMode by viewModel.readingMode.collectAsState()
  val fontSize by viewModel.readerFontSize.collectAsState()
  val currentTheme by viewModel.appThemeMode.collectAsState()
  val allAnnotations by viewModel.allAnnotations.collectAsState()

  // In-book search state
  val isReaderSearchVisible by viewModel.isReaderSearchVisible.collectAsState()
  val readerSearchQuery by viewModel.readerSearchQuery.collectAsState()
  val readerSearchMatches by viewModel.readerSearchMatches.collectAsState()
  val currentReaderSearchIndex by viewModel.currentReaderSearchIndex.collectAsState()

  val clipboardManager = LocalClipboardManager.current
  val context = LocalContext.current
  val bookFlipController = rememberBookFlipController()

  var showAppearanceDialog by remember { mutableStateOf(false) }
  var showAddNoteDialog by remember { mutableStateOf<String?>(null) }
  var viewingOrEditingAnnotation by remember { mutableStateOf<AnnotationEntity?>(null) }
  var showReaderRenameDialog by remember { mutableStateOf(false) }

  // State of text selection in Clean Reader mode
  var isTextSelecting by remember { mutableStateOf(false) }

  // Handle hardware / predictive back
  BackHandler {
    viewModel.closeReader()
  }

  if (book == null) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      CircularProgressIndicator()
    }
    return
  }

  val currentBook = book!!
  val totalPages = currentBook.totalPages.coerceAtLeast(1)
  val currentPageNum = pageIndex + 1

  val isBookmarked = allAnnotations.any {
    it.bookId == currentBook.id && it.pageNumber == currentPageNum && it.type == "BOOKMARK"
  }

  val currentPageAnnotations = remember(allAnnotations, currentBook.id, currentPageNum) {
    allAnnotations.filter { it.bookId == currentBook.id && it.pageNumber == currentPageNum }
  }

  val nextPageAnnotations = remember(allAnnotations, currentBook.id, currentPageNum) {
    allAnnotations.filter { it.bookId == currentBook.id && it.pageNumber == currentPageNum + 1 }
  }

  val prevPageAnnotations = remember(allAnnotations, currentBook.id, currentPageNum) {
    allAnnotations.filter { it.bookId == currentBook.id && it.pageNumber == currentPageNum - 1 }
  }

  Surface(
    modifier = modifier.fillMaxSize(),
    color = MaterialTheme.colorScheme.background
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .statusBarsPadding()
        .navigationBarsPadding()
    ) {
      // 1. Top Bar
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        IconButton(
          onClick = { viewModel.closeReader() },
          modifier = Modifier.testTag("reader_back_btn")
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
            tint = MaterialTheme.colorScheme.onBackground
          )
        }

        Row(
          modifier = Modifier
            .weight(1f)
            .clickable { showReaderRenameDialog = true },
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = currentBook.title,
            style = MaterialTheme.typography.titleMedium.copy(
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Serif
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          Spacer(modifier = Modifier.width(6.dp))
          Icon(
            imageVector = Icons.Default.Edit,
            contentDescription = "Rename Book",
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(14.dp)
          )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          // Search in Book Button
          IconButton(
            onClick = { viewModel.toggleReaderSearch() },
            modifier = Modifier.testTag("reader_search_btn")
          ) {
            Icon(
              imageVector = Icons.Default.Search,
              contentDescription = "Search in Book",
              tint = if (isReaderSearchVisible) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
            )
          }

          // Typography & Appearance 'Aa' Button
          IconButton(
            onClick = { showAppearanceDialog = true },
            modifier = Modifier.testTag("reader_appearance_btn")
          ) {
            Text(
              text = "Aa",
              style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif,
                color = MaterialTheme.colorScheme.onBackground
              )
            )
          }

          // Bookmark Button
          IconButton(
            onClick = { viewModel.toggleBookmarkCurrentPage() },
            modifier = Modifier.testTag("reader_bookmark_btn")
          ) {
            Icon(
              imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
              contentDescription = "Bookmark",
              tint = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
            )
          }

          // Mode Switcher (PDF vs Clean text)
          IconButton(
            onClick = { viewModel.toggleReadingMode() },
            modifier = Modifier.testTag("reader_mode_toggle_btn")
          ) {
            Icon(
              imageVector = if (readingMode == ReadingMode.ORIGINAL_PDF) Icons.Default.TextSnippet else Icons.Default.PictureAsPdf,
              contentDescription = "Toggle Reading Mode",
              tint = MaterialTheme.colorScheme.onBackground
            )
          }
        }
      }

      // In-Book Search Bar Toolbar
      AnimatedVisibility(visible = isReaderSearchVisible) {
        Card(
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            OutlinedTextField(
              value = readerSearchQuery,
              onValueChange = { viewModel.setReaderSearchQuery(it) },
              placeholder = { Text("Search text in this book...") },
              singleLine = true,
              modifier = Modifier
                .weight(1f)
                .testTag("reader_search_input")
            )

            Spacer(modifier = Modifier.width(8.dp))

            if (readerSearchMatches.isNotEmpty()) {
              Text(
                text = "${currentReaderSearchIndex + 1}/${readerSearchMatches.size}",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(horizontal = 4.dp)
              )

              IconButton(onClick = { viewModel.previousReaderSearchMatch() }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Previous Match", modifier = Modifier.size(20.dp))
              }
              IconButton(onClick = { viewModel.nextReaderSearchMatch() }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Next Match", modifier = Modifier.size(20.dp))
              }
            } else if (readerSearchQuery.isNotBlank()) {
              Text(
                text = "0 matches",
                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.error),
                modifier = Modifier.padding(horizontal = 4.dp)
              )
            }

            IconButton(onClick = { viewModel.toggleReaderSearch(false) }, modifier = Modifier.size(32.dp)) {
              Icon(Icons.Default.Close, contentDescription = "Close search", modifier = Modifier.size(18.dp))
            }
          }
        }
      }

      // 2. Reading Content Area wrapped in Realistic 3D Book Flip Container
      Box(
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth()
      ) {
        if (readingMode == ReadingMode.ORIGINAL_PDF) {
          // --- ORIGINAL PDF MODE WITH 3D FLIPBOOK ---
          BookFlipContainer(
            pageIndex = pageIndex,
            totalPages = totalPages,
            themeMode = currentTheme,
            isSelectingText = false,
            controller = bookFlipController,
            onPageChange = { targetPage -> viewModel.goToPage(targetPage) },
            modifier = Modifier.testTag("original_pdf_container"),
            currentPageContent = {
              OriginalPdfPage(
                bitmap = currentPdfBitmap,
                pageNumber = currentPageNum,
                totalPages = totalPages,
                themeMode = currentTheme
              )
            },
            nextPageContent = {
              OriginalPdfPage(
                bitmap = nextPdfBitmap,
                pageNumber = currentPageNum + 1,
                totalPages = totalPages,
                themeMode = currentTheme
              )
            },
            prevPageContent = {
              OriginalPdfPage(
                bitmap = prevPdfBitmap,
                pageNumber = currentPageNum - 1,
                totalPages = totalPages,
                themeMode = currentTheme
              )
            }
          )
        } else {
          // --- CLEAN READING MODE WITH PRECISE TEXT SELECTION & 3D FLIPBOOK ---
          BookFlipContainer(
            pageIndex = pageIndex,
            totalPages = totalPages,
            themeMode = currentTheme,
            isSelectingText = isTextSelecting,
            controller = bookFlipController,
            onPageChange = { targetPage -> viewModel.goToPage(targetPage) },
            modifier = Modifier.testTag("clean_reading_container"),
            currentPageContent = {
              CleanReaderPage(
                pageText = currentPageText,
                pageNumber = currentPageNum,
                totalPages = totalPages,
                fontSize = fontSize,
                themeMode = currentTheme,
                pageAnnotations = currentPageAnnotations,
                searchQuery = readerSearchQuery,
                onSelectingChange = { isTextSelecting = it },
                onAddHighlight = { text, color ->
                  viewModel.addHighlight(text, color)
                },
                onRemoveHighlight = { text ->
                  viewModel.removeHighlightForText(text)
                },
                onAnnotate = { text ->
                  showAddNoteDialog = text
                },
                onOpenNoteDetails = { annotation ->
                  viewingOrEditingAnnotation = annotation
                },
                onSwitchToPdfMode = {
                  viewModel.toggleReadingMode()
                }
              )
            },
            nextPageContent = {
              CleanReaderPagePreview(
                pageText = nextPageText,
                pageNumber = currentPageNum + 1,
                totalPages = totalPages,
                fontSize = fontSize,
                themeMode = currentTheme,
                pageAnnotations = nextPageAnnotations
              )
            },
            prevPageContent = {
              CleanReaderPagePreview(
                pageText = prevPageText,
                pageNumber = currentPageNum - 1,
                totalPages = totalPages,
                fontSize = fontSize,
                themeMode = currentTheme,
                pageAnnotations = prevPageAnnotations
              )
            }
          )
        }
      }

      // 3. Bottom Scrubber Bar with Page Turning Controls
      Card(
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          // Previous Page Button (Triggers realistic 3D book flip backward)
          IconButton(
            onClick = { bookFlipController.flipBackward() },
            enabled = pageIndex > 0,
            modifier = Modifier.testTag("reader_prev_page_btn")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Previous page",
              tint = if (pageIndex > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
              modifier = Modifier.size(20.dp)
            )
          }

          // Scrubber Slider
          Slider(
            value = currentPageNum.toFloat(),
            onValueChange = { targetPage ->
              viewModel.goToPage(targetPage.toInt() - 1)
            },
            valueRange = 1f..totalPages.toFloat(),
            steps = (totalPages - 2).coerceAtLeast(0),
            colors = SliderDefaults.colors(
              thumbColor = MaterialTheme.colorScheme.primary,
              activeTrackColor = MaterialTheme.colorScheme.primary,
              inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier
              .weight(1f)
              .padding(horizontal = 8.dp)
              .testTag("reader_page_slider")
          )

          // Page Number Text
          Text(
            text = "$currentPageNum / $totalPages",
            style = MaterialTheme.typography.labelMedium.copy(
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            modifier = Modifier.padding(horizontal = 6.dp)
          )

          // Quick Listen Button (Opens Audio Player Screen with extracted PDF text)
          IconButton(
            onClick = {
              viewModel.playBookAudio(currentBook, startFromPage = currentPageNum, openFullPlayer = true)
            },
            modifier = Modifier
              .clip(CircleShape)
              .background(MaterialTheme.colorScheme.primaryContainer)
              .size(36.dp)
              .testTag("reader_listen_page_btn")
          ) {
            Icon(
              imageVector = Icons.Default.Headphones,
              contentDescription = "Listen to page",
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(18.dp)
            )
          }

          // Next Page Button (Triggers realistic 3D book flip forward)
          IconButton(
            onClick = { bookFlipController.flipForward() },
            enabled = pageIndex < totalPages - 1,
            modifier = Modifier.testTag("reader_next_page_btn")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowForward,
              contentDescription = "Next page",
              tint = if (pageIndex < totalPages - 1) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
              modifier = Modifier.size(20.dp)
            )
          }
        }
      }
    }
  }

  // --- Appearance Dialog ---
  if (showAppearanceDialog) {
    AlertDialog(
      onDismissRequest = { showAppearanceDialog = false },
      title = { Text("Reading Appearance", style = MaterialTheme.typography.titleLarge) },
      text = {
        Column(modifier = Modifier.fillMaxWidth()) {
          Text("Font Size", style = MaterialTheme.typography.titleSmall)
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            TextButton(onClick = { viewModel.setReaderFontSize(fontSize - 2) }) {
              Text("A-", style = MaterialTheme.typography.titleMedium)
            }
            Text("$fontSize sp", style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = { viewModel.setReaderFontSize(fontSize + 2) }) {
              Text("A+", style = MaterialTheme.typography.titleLarge)
            }
          }

          Spacer(modifier = Modifier.height(14.dp))
          Text("Color Theme", style = MaterialTheme.typography.titleSmall)
          Spacer(modifier = Modifier.height(8.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
          ) {
            ThemeChoicePill(
              title = "Light",
              isSelected = currentTheme == AppThemeMode.LIGHT,
              bgColor = Color(0xFFFBF8F2),
              textColor = Color.Black,
              onClick = { viewModel.setAppThemeMode(AppThemeMode.LIGHT) }
            )
            ThemeChoicePill(
              title = "Dark",
              isSelected = currentTheme == AppThemeMode.DARK,
              bgColor = Color(0xFF1E1B26),
              textColor = Color.White,
              onClick = { viewModel.setAppThemeMode(AppThemeMode.DARK) }
            )
            ThemeChoicePill(
              title = "Paper",
              isSelected = currentTheme == AppThemeMode.WARM_PAPER,
              bgColor = Color(0xFFF4ECE1),
              textColor = Color(0xFF382D1D),
              onClick = { viewModel.setAppThemeMode(AppThemeMode.WARM_PAPER) }
            )
          }

          Spacer(modifier = Modifier.height(14.dp))
          Text("Display Mode", style = MaterialTheme.typography.titleSmall)
          Spacer(modifier = Modifier.height(8.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
          ) {
            FilterChipPill(
              title = "Clean Text",
              isSelected = readingMode == ReadingMode.CLEAN_READER,
              onClick = { if (readingMode != ReadingMode.CLEAN_READER) viewModel.toggleReadingMode() }
            )
            FilterChipPill(
              title = "Original PDF",
              isSelected = readingMode == ReadingMode.ORIGINAL_PDF,
              onClick = { if (readingMode != ReadingMode.ORIGINAL_PDF) viewModel.toggleReadingMode() }
            )
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { showAppearanceDialog = false }) {
          Text("Done")
        }
      }
    )
  }

  // --- Add Note Dialog ---
  if (showAddNoteDialog != null) {
    var noteText by remember { mutableStateOf("") }
    AlertDialog(
      onDismissRequest = { showAddNoteDialog = null },
      title = { Text("Add Note to Selection") },
      text = {
        Column {
          Text(
            text = "\"${showAddNoteDialog}\"",
            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
          )
          Spacer(modifier = Modifier.height(10.dp))
          OutlinedTextField(
            value = noteText,
            onValueChange = { noteText = it },
            placeholder = { Text("Type your note or thought here...") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
          )
        }
      },
      confirmButton = {
        TextButton(
          onClick = {
            viewModel.addHighlight(showAddNoteDialog!!, "yellow", noteText)
            showAddNoteDialog = null
          }
        ) {
          Text("Save Note")
        }
      },
      dismissButton = {
        TextButton(onClick = { showAddNoteDialog = null }) {
          Text("Cancel")
        }
      }
    )
  }

  // --- View, Edit, or Delete Annotation Dialog ---
  if (viewingOrEditingAnnotation != null) {
    val ann = viewingOrEditingAnnotation!!
    var editedNote by remember(ann) { mutableStateOf(ann.noteContent) }
    AlertDialog(
      onDismissRequest = { viewingOrEditingAnnotation = null },
      title = { Text(if (ann.noteContent.isNotBlank()) "Note Details" else "Highlight Details", fontWeight = FontWeight.Bold) },
      text = {
        Column(modifier = Modifier.fillMaxWidth()) {
          Text(
            text = "\"${ann.selectedText}\"",
            style = MaterialTheme.typography.bodySmall.copy(
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              fontFamily = FontFamily.Serif
            ),
            maxLines = 4,
            overflow = TextOverflow.Ellipsis
          )
          Spacer(modifier = Modifier.height(12.dp))
          OutlinedTextField(
            value = editedNote,
            onValueChange = { editedNote = it },
            label = { Text("Your Note") },
            placeholder = { Text("Add or edit thoughts for this passage...") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
          )
        }
      },
      confirmButton = {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          TextButton(
            onClick = {
              viewModel.deleteAnnotation(ann.id)
              viewingOrEditingAnnotation = null
            }
          ) {
            Text("Delete", color = MaterialTheme.colorScheme.error)
          }
          Button(
            onClick = {
              viewModel.updateAnnotation(ann.id, editedNote)
              viewingOrEditingAnnotation = null
            }
          ) {
            Text("Save")
          }
        }
      },
      dismissButton = {
        TextButton(onClick = { viewingOrEditingAnnotation = null }) {
          Text("Cancel")
        }
      }
    )
  }

  // --- Reader Rename Book Dialog ---
  if (showReaderRenameDialog) {
    var editedTitle by remember(currentBook.title, showReaderRenameDialog) { mutableStateOf(currentBook.title) }
    var isTitleError by remember { mutableStateOf(false) }

    AlertDialog(
      onDismissRequest = { showReaderRenameDialog = false },
      title = { Text("Rename Book", fontWeight = FontWeight.Bold) },
      text = {
        Column(modifier = Modifier.fillMaxWidth()) {
          if (currentBook.originalFileName.isNotBlank()) {
            Text(
              text = "Original file: ${currentBook.originalFileName}",
              style = MaterialTheme.typography.bodySmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
              )
            )
            Spacer(modifier = Modifier.height(10.dp))
          }
          OutlinedTextField(
            value = editedTitle,
            onValueChange = {
              editedTitle = it
              isTitleError = it.trim().isEmpty()
            },
            label = { Text("Book Title") },
            singleLine = true,
            isError = isTitleError,
            supportingText = {
              if (isTitleError) {
                Text("Title cannot be empty")
              }
            },
            modifier = Modifier
              .fillMaxWidth()
              .testTag("reader_rename_input")
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            val trimmed = editedTitle.trim()
            if (trimmed.isNotEmpty()) {
              viewModel.renameBook(currentBook.id, trimmed)
              showReaderRenameDialog = false
            } else {
              isTitleError = true
            }
          },
          modifier = Modifier.testTag("reader_rename_save_btn")
        ) {
          Text("Save")
        }
      },
      dismissButton = {
        TextButton(
          onClick = { showReaderRenameDialog = false },
          modifier = Modifier.testTag("reader_rename_cancel_btn")
        ) {
          Text("Cancel")
        }
      }
    )
  }
}

/**
 * Clean Reader Page Composable with Precise Text Selection, Exact Character Highlighting,
 * and Anchored Bottom-Right Page Number.
 */
@Composable
private fun CleanReaderPage(
  pageText: BookPageTextEntity?,
  pageNumber: Int,
  totalPages: Int,
  fontSize: Int,
  themeMode: AppThemeMode,
  pageAnnotations: List<AnnotationEntity>,
  searchQuery: String,
  onSelectingChange: (Boolean) -> Unit,
  onAddHighlight: (String, String) -> Unit,
  onRemoveHighlight: (String) -> Unit,
  onAnnotate: (String) -> Unit,
  onOpenNoteDetails: (AnnotationEntity) -> Unit,
  onSwitchToPdfMode: () -> Unit
) {
  val context = LocalContext.current
  val clipboardManager = LocalClipboardManager.current
  val scrollState = rememberScrollState()

  // Filter actual highlights & notes (exclude bookmarks)
  val highlights = remember(pageAnnotations) {
    pageAnnotations.filter {
      (it.type == "HIGHLIGHT" || it.type == "NOTE") && it.selectedText.isNotBlank()
    }
  }

  val pageNotes = remember(pageAnnotations) {
    pageAnnotations.filter {
      it.type != "BOOKMARK" && it.noteContent.isNotBlank()
    }
  }

  val fullText = pageText?.textContent ?: ""

  var textFieldValue by remember(fullText) {
    mutableStateOf(TextFieldValue(fullText))
  }

  val isSelectionActive = !textFieldValue.selection.collapsed
  LaunchedEffect(isSelectionActive) {
    onSelectingChange(isSelectionActive)
  }

  val selectedText = remember(textFieldValue.selection, textFieldValue.text) {
    if (!textFieldValue.selection.collapsed) {
      val min = textFieldValue.selection.min.coerceIn(0, textFieldValue.text.length)
      val max = textFieldValue.selection.max.coerceIn(0, textFieldValue.text.length)
      textFieldValue.text.substring(min, max).trim()
    } else {
      ""
    }
  }

  // Visual Transformation for Precise Character Span Highlighting
  val highlightTransformation = remember(highlights, searchQuery, fullText) {
    VisualTransformation { text ->
      val annotated = buildAnnotatedString {
        append(text.text)

        // 1. Precise Character Highlights
        for (hl in highlights) {
          val target = hl.selectedText.trim()
          if (target.isNotBlank()) {
            var startIndex = 0
            while (startIndex < text.length) {
              val idx = text.text.indexOf(target, startIndex, ignoreCase = false)
              if (idx == -1) break
              val hlColor = when (hl.color) {
                "yellow" -> HighlightYellow
                "lavender" -> HighlightLavender
                "green" -> HighlightGreen
                "pink" -> HighlightPink
                else -> HighlightYellow
              }

              addStyle(
                style = SpanStyle(
                  background = hlColor.copy(alpha = 0.5f),
                  textDecoration = if (hl.noteContent.isNotBlank()) TextDecoration.Underline else TextDecoration.None
                ),
                start = idx,
                end = (idx + target.length).coerceAtMost(text.length)
              )
              startIndex = idx + target.length
            }
          }
        }

        // 2. Search Query Highlighting
        val trimmedQuery = searchQuery.trim()
        if (trimmedQuery.isNotBlank()) {
          var searchStart = 0
          while (searchStart < text.length) {
            val idx = text.text.indexOf(trimmedQuery, searchStart, ignoreCase = true)
            if (idx == -1) break
            addStyle(
              style = SpanStyle(
                background = Color(0xFFFFB300),
                fontWeight = FontWeight.Bold
              ),
              start = idx,
              end = (idx + trimmedQuery.length).coerceAtMost(text.length)
            )
            searchStart = idx + trimmedQuery.length
          }
        }
      }
      TransformedText(annotated, OffsetMapping.Identity)
    }
  }

  // Check for authentic non-page chapter title
  val rawChapter = pageText?.chapterTitle?.trim() ?: ""
  val isRealChapterTitle = rawChapter.isNotBlank() &&
    !rawChapter.startsWith("Page", ignoreCase = true) &&
    rawChapter != "Chapter $pageNumber"

  Box(modifier = Modifier.fillMaxSize()) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(scrollState)
        .padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 16.dp)
    ) {
      // Display authentic chapter title at top ONLY if not "Page X"
      if (isRealChapterTitle) {
        Text(
          text = rawChapter,
          style = MaterialTheme.typography.titleMedium.copy(
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            fontSize = (fontSize + 3).sp
          ),
          modifier = Modifier.padding(bottom = 14.dp)
        )
      }

      if (fullText.isBlank()) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
          contentAlignment = Alignment.Center
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
              text = "No extractable text on this page.\nSwitch to Original PDF mode to view scanned content or images.",
              style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
              )
            )
            Spacer(modifier = Modifier.height(14.dp))
            Button(onClick = onSwitchToPdfMode) {
              Text("Switch to Original PDF Mode")
            }
          }
        }
      } else {
        // Native Selectable Text Field with Custom VisualTransformation for Precise Highlighting
        BasicTextField(
          value = textFieldValue,
          onValueChange = { textFieldValue = it },
          readOnly = true,
          cursorBrush = SolidColor(Color.Transparent),
          visualTransformation = highlightTransformation,
          textStyle = MaterialTheme.typography.bodyLarge.copy(
            fontFamily = FontFamily.Serif,
            fontSize = fontSize.sp,
            lineHeight = (fontSize * 1.68f).sp,
            color = when (themeMode) {
              AppThemeMode.DARK -> Color(0xFFEDE7F6)
              AppThemeMode.WARM_PAPER -> Color(0xFF2C2417)
              else -> Color(0xFF1C1B1F)
            }
          ),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("reader_selectable_text")
        )
      }

      // Notes list section on this page (if any notes attached)
      if (pageNotes.isNotEmpty()) {
        Spacer(modifier = Modifier.height(24.dp))
        Text(
          text = "Notes on this page (${pageNotes.size})",
          style = MaterialTheme.typography.labelMedium.copy(
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
          ),
          modifier = Modifier.padding(bottom = 8.dp)
        )

        pageNotes.forEach { noteAnn ->
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 4.dp)
              .clip(RoundedCornerShape(8.dp))
              .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
              .border(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
              .clickable { onOpenNoteDetails(noteAnn) }
              .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.EditNote,
              contentDescription = "View Note",
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "\"${noteAnn.selectedText}\"",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall.copy(
                  fontWeight = FontWeight.Bold,
                  fontSize = 12.sp,
                  fontFamily = FontFamily.Serif
                )
              )
              Text(
                text = noteAnn.noteContent,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp)
              )
            }
          }
        }
      }

      // Bottom Spacer ensuring text NEVER overlaps anchored bottom-right page badge
      Spacer(modifier = Modifier.height(72.dp))
    }

    // 4. FLOATING CONTEXTUAL ACTION TOOLBAR (Appears upon text selection)
    AnimatedVisibility(
      visible = isSelectionActive && selectedText.isNotBlank(),
      enter = fadeIn() + slideInVertically { -it },
      exit = fadeOut() + slideOutVertically { -it },
      modifier = Modifier
        .align(Alignment.TopCenter)
        .padding(top = 10.dp)
    ) {
      Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        modifier = Modifier.testTag("reader_selection_toolbar")
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          // Copy Button
          IconButton(
            onClick = {
              try {
                clipboardManager.setText(AnnotatedString(selectedText))
                Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
              } catch (e: Exception) {
                e.printStackTrace()
              }
              textFieldValue = textFieldValue.copy(selection = TextRange.Zero)
            },
            modifier = Modifier.size(28.dp)
          ) {
            Icon(
              imageVector = Icons.Default.ContentCopy,
              contentDescription = "Copy text",
              tint = MaterialTheme.colorScheme.onSurface,
              modifier = Modifier.size(16.dp)
            )
          }

          // Highlight Color Dots
          ColorDot(color = HighlightYellow, onClick = {
            onAddHighlight(selectedText, "yellow")
            textFieldValue = textFieldValue.copy(selection = TextRange.Zero)
          })
          ColorDot(color = HighlightLavender, onClick = {
            onAddHighlight(selectedText, "lavender")
            textFieldValue = textFieldValue.copy(selection = TextRange.Zero)
          })
          ColorDot(color = HighlightGreen, onClick = {
            onAddHighlight(selectedText, "green")
            textFieldValue = textFieldValue.copy(selection = TextRange.Zero)
          })
          ColorDot(color = HighlightPink, onClick = {
            onAddHighlight(selectedText, "pink")
            textFieldValue = textFieldValue.copy(selection = TextRange.Zero)
          })

          // Annotate / Add Note Button
          IconButton(
            onClick = {
              onAnnotate(selectedText)
              textFieldValue = textFieldValue.copy(selection = TextRange.Zero)
            },
            modifier = Modifier.size(28.dp)
          ) {
            Icon(
              imageVector = Icons.Default.EditNote,
              contentDescription = "Annotate",
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(18.dp)
            )
          }

          // Remove Highlight if selected text matches an existing highlight
          val isHighlighted = highlights.any {
            it.selectedText.trim().equals(selectedText, ignoreCase = true) ||
              it.selectedText.contains(selectedText)
          }
          if (isHighlighted) {
            IconButton(
              onClick = {
                onRemoveHighlight(selectedText)
                textFieldValue = textFieldValue.copy(selection = TextRange.Zero)
              },
              modifier = Modifier.size(28.dp)
            ) {
              Icon(
                imageVector = Icons.Default.FormatColorReset,
                contentDescription = "Remove Highlight",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(16.dp)
              )
            }
          }

          // Dismiss Selection (X)
          IconButton(
            onClick = {
              textFieldValue = textFieldValue.copy(selection = TextRange.Zero)
            },
            modifier = Modifier.size(28.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Clear selection",
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(14.dp)
            )
          }
        }
      }
    }

    // 5. ANCHORED BOTTOM-RIGHT PAGE NUMBER
    BottomRightPageBadge(
      pageNumber = pageNumber,
      totalPages = totalPages,
      themeMode = themeMode,
      modifier = Modifier
        .align(Alignment.BottomEnd)
        .padding(end = 20.dp, bottom = 12.dp)
    )
  }
}

/**
 * Preview of adjacent clean reader page for smooth 3D flipbook background rendering.
 */
@Composable
private fun CleanReaderPagePreview(
  pageText: BookPageTextEntity?,
  pageNumber: Int,
  totalPages: Int,
  fontSize: Int,
  themeMode: AppThemeMode,
  pageAnnotations: List<AnnotationEntity>
) {
  val fullText = pageText?.textContent ?: ""

  Box(modifier = Modifier.fillMaxSize()) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 16.dp)
    ) {
      val rawChapter = pageText?.chapterTitle?.trim() ?: ""
      val isRealChapterTitle = rawChapter.isNotBlank() &&
        !rawChapter.startsWith("Page", ignoreCase = true) &&
        rawChapter != "Chapter $pageNumber"

      if (isRealChapterTitle) {
        Text(
          text = rawChapter,
          style = MaterialTheme.typography.titleMedium.copy(
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            fontSize = (fontSize + 3).sp
          ),
          modifier = Modifier.padding(bottom = 14.dp)
        )
      }

      Text(
        text = fullText,
        maxLines = 14,
        overflow = TextOverflow.Ellipsis,
        style = MaterialTheme.typography.bodyLarge.copy(
          fontFamily = FontFamily.Serif,
          fontSize = fontSize.sp,
          lineHeight = (fontSize * 1.68f).sp,
          color = when (themeMode) {
            AppThemeMode.DARK -> Color(0xFFEDE7F6)
            AppThemeMode.WARM_PAPER -> Color(0xFF2C2417)
            else -> Color(0xFF1C1B1F)
          }
        )
      )
    }

    BottomRightPageBadge(
      pageNumber = pageNumber,
      totalPages = totalPages,
      themeMode = themeMode,
      modifier = Modifier
        .align(Alignment.BottomEnd)
        .padding(end = 20.dp, bottom = 12.dp)
    )
  }
}

/**
 * Original PDF Page Composable with Vector Bitmap Rendering, Pinch-to-Zoom,
 * and Anchored Bottom-Right Page Number.
 */
@Composable
private fun OriginalPdfPage(
  bitmap: Bitmap?,
  pageNumber: Int,
  totalPages: Int,
  themeMode: AppThemeMode
) {
  var scale by remember { mutableFloatStateOf(1f) }
  var offset by remember { mutableStateOf(Offset.Zero) }

  val transformableState = rememberTransformableState { zoomChange, offsetChange, _ ->
    scale = (scale * zoomChange).coerceIn(1f, 4f)
    if (scale > 1f) {
      offset += offsetChange
    } else {
      offset = Offset.Zero
    }
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .transformable(state = transformableState),
    contentAlignment = Alignment.Center
  ) {
    if (bitmap != null) {
      Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = "Page $pageNumber",
        modifier = Modifier
          .fillMaxSize()
          .graphicsLayer(
            scaleX = scale,
            scaleY = scale,
            translationX = offset.x,
            translationY = offset.y
          ),
        contentScale = ContentScale.Fit
      )
    } else {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
      ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(10.dp))
        Text("Rendering vector page...", style = MaterialTheme.typography.bodySmall)
      }
    }

    // Anchored Bottom-Right Page Badge
    BottomRightPageBadge(
      pageNumber = pageNumber,
      totalPages = totalPages,
      themeMode = themeMode,
      modifier = Modifier
        .align(Alignment.BottomEnd)
        .padding(end = 20.dp, bottom = 12.dp)
    )
  }
}

/**
 * Anchored Page Number Badge at bottom right of the page.
 */
@Composable
private fun BottomRightPageBadge(
  pageNumber: Int,
  totalPages: Int,
  themeMode: AppThemeMode,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier.testTag("reader_bottom_right_page_number")
  ) {
    Surface(
      shape = RoundedCornerShape(8.dp),
      color = when (themeMode) {
        AppThemeMode.WARM_PAPER -> Color(0xFFE8DFC9).copy(alpha = 0.88f)
        AppThemeMode.DARK -> Color(0xFF2B2536).copy(alpha = 0.88f)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.80f)
      },
      border = androidx.compose.foundation.BorderStroke(
        0.5.dp,
        MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
      )
    ) {
      Text(
        text = "Page $pageNumber of $totalPages",
        style = MaterialTheme.typography.labelSmall.copy(
          fontFamily = FontFamily.Serif,
          fontWeight = FontWeight.SemiBold,
          fontSize = 11.sp,
          letterSpacing = 0.5.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
        ),
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
      )
    }
  }
}

@Composable
private fun ColorDot(color: Color, onClick: () -> Unit) {
  Box(
    modifier = Modifier
      .size(20.dp)
      .clip(CircleShape)
      .background(color)
      .border(1.dp, Color.Black.copy(alpha = 0.18f), CircleShape)
      .clickable { onClick() }
  )
}

@Composable
private fun ThemeChoicePill(
  title: String,
  isSelected: Boolean,
  bgColor: Color,
  textColor: Color,
  onClick: () -> Unit
) {
  Box(
    modifier = Modifier
      .clip(RoundedCornerShape(12.dp))
      .background(bgColor)
      .border(
        if (isSelected) 2.dp else 1.dp,
        if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.3f),
        RoundedCornerShape(12.dp)
      )
      .clickable { onClick() }
      .padding(horizontal = 14.dp, vertical = 8.dp),
    contentAlignment = Alignment.Center
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      if (isSelected) {
        Icon(
          imageVector = Icons.Default.Check,
          contentDescription = null,
          tint = textColor,
          modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
      }
      Text(
        text = title,
        style = MaterialTheme.typography.labelSmall.copy(
          color = textColor,
          fontWeight = FontWeight.Bold
        )
      )
    }
  }
}

@Composable
private fun FilterChipPill(
  title: String,
  isSelected: Boolean,
  onClick: () -> Unit
) {
  Box(
    modifier = Modifier
      .clip(RoundedCornerShape(12.dp))
      .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
      .clickable { onClick() }
      .padding(horizontal = 16.dp, vertical = 8.dp),
    contentAlignment = Alignment.Center
  ) {
    Text(
      text = title,
      style = MaterialTheme.typography.labelMedium.copy(
        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.Bold
      )
    )
  }
}
