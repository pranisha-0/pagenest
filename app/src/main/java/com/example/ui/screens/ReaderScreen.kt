package com.example.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.TextSnippet
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AnnotationEntity
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
  val pdfBitmap by viewModel.currentPdfBitmap.collectAsState()
  val pageText by viewModel.currentPageText.collectAsState()
  val readingMode by viewModel.readingMode.collectAsState()
  val fontSize by viewModel.readerFontSize.collectAsState()
  val currentTheme by viewModel.appThemeMode.collectAsState()
  val allAnnotations by viewModel.allAnnotations.collectAsState()

  val clipboardManager = LocalClipboardManager.current
  var showAppearanceDialog by remember { mutableStateOf(false) }
  var showAddNoteDialog by remember { mutableStateOf<String?>(null) }
  var activeSelectedParagraph by remember { mutableStateOf<String?>(null) }

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

  val pageAnnotations = allAnnotations.filter {
    it.bookId == currentBook.id && it.pageNumber == currentPageNum
  }

  // Transformable state for pinch-to-zoom in Original PDF Mode
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

        Text(
          text = "${currentBook.title}.pdf",
          style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Serif
          ),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f)
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
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

      // 2. Reading Content Area
      Box(
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth()
      ) {
        if (readingMode == ReadingMode.ORIGINAL_PDF) {
          // --- ORIGINAL PDF MODE (Vector bitmap rendering) ---
          Box(
            modifier = Modifier
              .fillMaxSize()
              .transformable(state = transformableState)
              .testTag("original_pdf_container"),
            contentAlignment = Alignment.Center
          ) {
            if (pdfBitmap != null) {
              Image(
                bitmap = pdfBitmap!!.asImageBitmap(),
                contentDescription = "Page $currentPageNum",
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
          }
        } else {
          // --- CLEAN READING MODE ---
          val scrollState = rememberScrollState()
          val paragraphs = remember(pageText) {
            pageText?.textContent?.split("\n\n")?.filter { it.isNotBlank() } ?: emptyList()
          }

          Column(
            modifier = Modifier
              .fillMaxSize()
              .verticalScroll(scrollState)
              .padding(horizontal = 24.dp, vertical = 16.dp)
              .testTag("clean_reading_container")
          ) {
            // Chapter Title
            Text(
              text = pageText?.chapterTitle ?: "Chapter $currentPageNum",
              style = MaterialTheme.typography.headlineMedium.copy(
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = (fontSize + 6).sp
              ),
              modifier = Modifier.padding(bottom = 16.dp)
            )

            // Paragraphs
            paragraphs.forEachIndexed { index, paragraph ->
              val matchingHighlight = pageAnnotations.firstOrNull {
                it.selectedText.isNotBlank() && (paragraph.contains(it.selectedText) || it.selectedText.contains(paragraph))
              }

              val highlightColor = when (matchingHighlight?.color) {
                "yellow" -> HighlightYellow
                "lavender" -> HighlightLavender
                "green" -> HighlightGreen
                "pink" -> HighlightPink
                else -> null
              }

              val isSelected = activeSelectedParagraph == paragraph

              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 6.dp)
                  .clip(RoundedCornerShape(6.dp))
                  .background(
                    highlightColor?.copy(alpha = 0.5f)
                      ?: if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else Color.Transparent
                  )
                  .clickable {
                    activeSelectedParagraph = if (isSelected) null else paragraph
                  }
                  .padding(horizontal = 4.dp, vertical = 2.dp)
              ) {
                Text(
                  text = paragraph,
                  style = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = FontFamily.Serif,
                    fontSize = fontSize.sp,
                    lineHeight = (fontSize * 1.6f).sp,
                    color = MaterialTheme.colorScheme.onBackground
                  )
                )

                // Inline Highlight & Note Action Bar (from showcase mockup!)
                AnimatedVisibility(
                  visible = isSelected,
                  enter = fadeIn(),
                  exit = fadeOut()
                ) {
                  Row(
                    modifier = Modifier
                      .padding(top = 8.dp, bottom = 4.dp)
                      .clip(RoundedCornerShape(20.dp))
                      .background(MaterialTheme.colorScheme.surface)
                      .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                      .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                  ) {
                    // Color dots: Yellow, Lavender, Green, Pink
                    ColorDot(color = HighlightYellow, onClick = {
                      viewModel.addHighlight(paragraph, "yellow")
                      activeSelectedParagraph = null
                    })
                    ColorDot(color = HighlightLavender, onClick = {
                      viewModel.addHighlight(paragraph, "lavender")
                      activeSelectedParagraph = null
                    })
                    ColorDot(color = HighlightGreen, onClick = {
                      viewModel.addHighlight(paragraph, "green")
                      activeSelectedParagraph = null
                    })
                    ColorDot(color = HighlightPink, onClick = {
                      viewModel.addHighlight(paragraph, "pink")
                      activeSelectedParagraph = null
                    })

                    Spacer(modifier = Modifier.width(4.dp))

                    // Add Note
                    Icon(
                      imageVector = Icons.Default.EditNote,
                      contentDescription = "Add Note",
                      tint = MaterialTheme.colorScheme.primary,
                      modifier = Modifier
                        .size(18.dp)
                        .clickable { showAddNoteDialog = paragraph }
                    )

                    // Copy text
                    Icon(
                      imageVector = Icons.Default.ContentCopy,
                      contentDescription = "Copy",
                      tint = MaterialTheme.colorScheme.onSurfaceVariant,
                      modifier = Modifier
                        .size(16.dp)
                        .clickable {
                          clipboardManager.setText(AnnotatedString(paragraph))
                          activeSelectedParagraph = null
                        }
                    )
                  }
                }
              }
            }

            Spacer(modifier = Modifier.height(60.dp))
          }
        }
      }

      // 3. Bottom Scrubber Bar (as shown in mockup: `< [======] 72 / 341 [Audio] >`)
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
          // Previous Page Button
          IconButton(
            onClick = { viewModel.previousPage() },
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

          // Quick Listen Button
          IconButton(
            onClick = {
              viewModel.playCurrentBookAudio(startFromCurrentPage = true)
              viewModel.openFullAudioPlayer()
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

          // Next Page Button
          IconButton(
            onClick = { viewModel.nextPage() },
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
            viewModel.addHighlight(showAddNoteDialog!!, "lavender", noteText)
            showAddNoteDialog = null
            activeSelectedParagraph = null
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
}

@Composable
private fun ColorDot(color: Color, onClick: () -> Unit) {
  Box(
    modifier = Modifier
      .size(18.dp)
      .clip(CircleShape)
      .background(color)
      .border(1.dp, Color.Black.copy(alpha = 0.15f), CircleShape)
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
