package com.example.ui.screens

import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.text.selection.SelectionContainer
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import kotlinx.coroutines.launch
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
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

  // In-book search state
  val isReaderSearchVisible by viewModel.isReaderSearchVisible.collectAsState()
  val readerSearchQuery by viewModel.readerSearchQuery.collectAsState()
  val readerSearchMatches by viewModel.readerSearchMatches.collectAsState()
  val currentReaderSearchIndex by viewModel.currentReaderSearchIndex.collectAsState()

  val clipboardManager = LocalClipboardManager.current
  val context = LocalContext.current
  val haptic = LocalHapticFeedback.current
  val coroutineScope = rememberCoroutineScope()
  var showAppearanceDialog by remember { mutableStateOf(false) }
  var showAddNoteDialog by remember { mutableStateOf<String?>(null) }
  var viewingOrEditingAnnotation by remember { mutableStateOf<AnnotationEntity?>(null) }
  var activeSelectedParagraph by remember { mutableStateOf<String?>(null) }
  var showReaderRenameDialog by remember { mutableStateOf(false) }

  // Flipbook interactive drag & animation state
  var dragOffset by remember { mutableFloatStateOf(0f) }
  val animOffset = remember { Animatable(0f) }
  var isFlipping by remember { mutableStateOf(false) }

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
              .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            OutlinedTextField(
              value = readerSearchQuery,
              onValueChange = { viewModel.setReaderSearchQuery(it) },
              placeholder = { Text("Search text in this book...", fontSize = 13.sp) },
              singleLine = true,
              modifier = Modifier.weight(1f),
              shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.width(6.dp))

            if (readerSearchQuery.isNotBlank()) {
              val matchText = if (readerSearchMatches.isNotEmpty()) "${currentReaderSearchIndex + 1}/${readerSearchMatches.size}" else "0/0"
              Text(
                text = matchText,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(horizontal = 4.dp)
              )
              IconButton(
                onClick = { viewModel.previousReaderSearchMatch() },
                enabled = readerSearchMatches.isNotEmpty(),
                modifier = Modifier.size(32.dp)
              ) {
                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Previous match", modifier = Modifier.size(18.dp))
              }
              IconButton(
                onClick = { viewModel.nextReaderSearchMatch() },
                enabled = readerSearchMatches.isNotEmpty(),
                modifier = Modifier.size(32.dp)
              ) {
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Next match", modifier = Modifier.size(18.dp))
              }
            }

            IconButton(
              onClick = { viewModel.toggleReaderSearch(false) },
              modifier = Modifier.size(32.dp)
            ) {
              Icon(Icons.Default.Close, contentDescription = "Close search", modifier = Modifier.size(18.dp))
            }
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
          // --- CLEAN READING MODE WITH REALISTIC FLIPBOOK INTERACTION ---
          val currentFraction = if (isFlipping) animOffset.value else (dragOffset / 350f).coerceIn(-1f, 1f)
          val flipAngle = currentFraction * -55f
          val pageRotationOrigin = if (currentFraction <= 0f) TransformOrigin(0f, 0.5f) else TransformOrigin(1f, 0.5f)
          val shadowAlpha = (kotlin.math.abs(currentFraction) * 0.28f).coerceIn(0f, 0.35f)

          val pageBgColor = when (currentTheme) {
            AppThemeMode.WARM_PAPER -> Color(0xFFFBF4E8)
            AppThemeMode.DARK -> Color(0xFF1E1A27)
            else -> Color(0xFFFFFDF9)
          }

          Box(
            modifier = Modifier
              .fillMaxSize()
              .padding(horizontal = 12.dp, vertical = 6.dp)
              .testTag("clean_reading_container")
          ) {
            // Physical Book Page Card with 3D Flipbook transforms
            Card(
              shape = RoundedCornerShape(12.dp),
              colors = CardDefaults.cardColors(containerColor = pageBgColor),
              elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
              border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
              ),
              modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                  rotationY = flipAngle
                  cameraDistance = 14f * density
                  transformOrigin = pageRotationOrigin
                }
                .pointerInput(pageIndex, totalPages, isFlipping) {
                  if (!isFlipping) {
                    detectHorizontalDragGestures(
                      onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        val newOffset = dragOffset + dragAmount
                        if ((dragAmount < 0 && pageIndex >= totalPages - 1) || (dragAmount > 0 && pageIndex <= 0)) {
                          dragOffset = newOffset * 0.25f
                        } else {
                          dragOffset = newOffset
                        }
                      },
                      onDragEnd = {
                        val threshold = 70f
                        if (dragOffset < -threshold && pageIndex < totalPages - 1) {
                          isFlipping = true
                          coroutineScope.launch {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            animOffset.animateTo(-1f, animationSpec = tween(170))
                            viewModel.nextPage()
                            animOffset.snapTo(0f)
                            dragOffset = 0f
                            isFlipping = false
                          }
                        } else if (dragOffset > threshold && pageIndex > 0) {
                          isFlipping = true
                          coroutineScope.launch {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            animOffset.animateTo(1f, animationSpec = tween(170))
                            viewModel.previousPage()
                            animOffset.snapTo(0f)
                            dragOffset = 0f
                            isFlipping = false
                          }
                        } else {
                          coroutineScope.launch {
                            animOffset.animateTo(0f, animationSpec = spring())
                            dragOffset = 0f
                          }
                        }
                      },
                      onDragCancel = {
                        coroutineScope.launch {
                          animOffset.animateTo(0f, animationSpec = spring())
                          dragOffset = 0f
                        }
                      }
                    )
                  }
                }
            ) {
              Box(modifier = Modifier.fillMaxSize()) {
                // Book Spine Crease Shadow along Left Edge
                Box(
                  modifier = Modifier
                    .align(Alignment.CenterStart)
                    .width(14.dp)
                    .fillMaxHeight()
                    .background(
                      Brush.horizontalGradient(
                        listOf(Color.Black.copy(alpha = 0.08f), Color.Transparent)
                      )
                    )
                )

                // Scrollable Text Content
                val scrollState = rememberScrollState()
                val paragraphs = remember(pageText) {
                  pageText?.textContent?.split("\n\n")?.filter { it.isNotBlank() } ?: emptyList()
                }

                // Check for authentic non-page chapter title
                val rawChapter = pageText?.chapterTitle?.trim() ?: ""
                val isRealChapterTitle = rawChapter.isNotBlank() &&
                  !rawChapter.startsWith("Page", ignoreCase = true) &&
                  rawChapter != "Chapter $currentPageNum"

                SelectionContainer {
                  Column(
                    modifier = Modifier
                      .fillMaxSize()
                      .verticalScroll(scrollState)
                      .padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 16.dp)
                  ) {
                    // Display chapter title ONLY if an authentic title exists. NO "Page 1" at top!
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

                    if (paragraphs.isEmpty()) {
                      Box(
                        modifier = Modifier
                          .fillMaxWidth()
                          .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                      ) {
                        Text(
                          text = "No extractable text on this page.\nSwitch to Original PDF mode to view images or scanned content.",
                          style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                          )
                        )
                      }
                    }

                    // Paragraphs
                    paragraphs.forEachIndexed { index, paragraph ->
                      val matchingHighlight = pageAnnotations.firstOrNull {
                        it.selectedText.isNotBlank() &&
                          (paragraph.contains(it.selectedText) || it.selectedText.contains(paragraph))
                      }

                      val highlightColor = when (matchingHighlight?.color) {
                        "yellow" -> HighlightYellow
                        "lavender" -> HighlightLavender
                        "green" -> HighlightGreen
                        "pink" -> HighlightPink
                        else -> null
                      }

                      val isSelected = activeSelectedParagraph == paragraph

                      // Highlight matched search query in text
                      val annotatedParagraph = remember(paragraph, readerSearchQuery) {
                        if (readerSearchQuery.isNotBlank() && paragraph.contains(readerSearchQuery, ignoreCase = true)) {
                          buildAnnotatedString {
                            var currentIndex = 0
                            val lowerPara = paragraph.lowercase()
                            val lowerQuery = readerSearchQuery.lowercase()
                            while (currentIndex < paragraph.length) {
                              val matchIdx = lowerPara.indexOf(lowerQuery, currentIndex)
                              if (matchIdx == -1) {
                                append(paragraph.substring(currentIndex))
                                break
                              }
                              if (matchIdx > currentIndex) {
                                append(paragraph.substring(currentIndex, matchIdx))
                              }
                              withStyle(SpanStyle(background = HighlightYellow, fontWeight = FontWeight.Bold)) {
                                append(paragraph.substring(matchIdx, matchIdx + readerSearchQuery.length))
                              }
                              currentIndex = matchIdx + readerSearchQuery.length
                            }
                          }
                        } else {
                          AnnotatedString(paragraph)
                        }
                      }

                      val isHeading = remember(paragraph) {
                        val p = paragraph.trim()
                        p.length in 3..75 && (
                          p.startsWith("Chapter", ignoreCase = true) ||
                          p.startsWith("Part", ignoreCase = true) ||
                          p.startsWith("Section", ignoreCase = true) ||
                          p.startsWith("Introduction", ignoreCase = true) ||
                          p.startsWith("Epilogue", ignoreCase = true) ||
                          p.startsWith("Prologue", ignoreCase = true) ||
                          p.startsWith("Conclusion", ignoreCase = true) ||
                          p.startsWith("#") ||
                          (p.length in 4..60 && p == p.uppercase() && p.any { it.isLetter() } && !p.endsWith("."))
                        )
                      }

                      val isListItem = remember(paragraph) {
                        val p = paragraph.trim()
                        p.startsWith("•") || p.startsWith("*") || p.startsWith("- ") || p.startsWith("–") || p.startsWith("—") ||
                          p.matches(Regex("^(\\d{1,3}[.)]|\\([0-9a-zA-Z]+\\)|[a-zA-Z][.)])\\s+.*"))
                      }

                      val isQuote = remember(paragraph) {
                        val p = paragraph.trim()
                        p.startsWith("\"") || p.startsWith("“") || p.startsWith(">")
                      }

                      Column(
                        modifier = Modifier
                          .fillMaxWidth()
                          .padding(
                            vertical = if (isHeading) 10.dp else if (isListItem) 3.dp else 6.dp,
                            horizontal = if (isListItem) 6.dp else 4.dp
                          )
                          .clip(RoundedCornerShape(6.dp))
                          .background(
                            highlightColor?.copy(alpha = 0.45f)
                              ?: if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                              else if (isQuote) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                              else Color.Transparent
                          )
                          .clickable {
                            activeSelectedParagraph = if (isSelected) null else paragraph
                          }
                          .padding(
                            horizontal = if (isQuote) 12.dp else 4.dp,
                            vertical = if (isQuote) 6.dp else 2.dp
                          )
                      ) {
                        Text(
                          text = annotatedParagraph,
                          style = if (isHeading) {
                            MaterialTheme.typography.titleLarge.copy(
                              fontFamily = FontFamily.Serif,
                              fontWeight = FontWeight.Bold,
                              fontSize = (fontSize + 3).sp,
                              color = MaterialTheme.colorScheme.primary
                            )
                          } else if (isQuote) {
                            MaterialTheme.typography.bodyLarge.copy(
                              fontFamily = FontFamily.Serif,
                              fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                              fontSize = fontSize.sp,
                              lineHeight = (fontSize * 1.6f).sp,
                              color = MaterialTheme.colorScheme.onBackground
                            )
                          } else {
                            MaterialTheme.typography.bodyLarge.copy(
                              fontFamily = FontFamily.Serif,
                              fontSize = fontSize.sp,
                              lineHeight = (fontSize * 1.65f).sp,
                              color = MaterialTheme.colorScheme.onBackground
                            )
                          }
                        )

                        // Subtle Note Marker if an annotation has an attached personal note
                        if (matchingHighlight?.noteContent?.isNotBlank() == true) {
                          Row(
                            modifier = Modifier
                              .padding(top = 6.dp, bottom = 2.dp)
                              .clip(RoundedCornerShape(8.dp))
                              .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                              .border(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                              .clickable {
                                viewingOrEditingAnnotation = matchingHighlight
                              }
                              .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                          ) {
                            Icon(
                              imageVector = Icons.Default.EditNote,
                              contentDescription = "View Note",
                              tint = MaterialTheme.colorScheme.primary,
                              modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                              text = "Note: ${matchingHighlight.noteContent}",
                              maxLines = 1,
                              overflow = TextOverflow.Ellipsis,
                              style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 12.sp,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                color = MaterialTheme.colorScheme.onSurface
                              )
                            )
                          }
                        }

                        // Contextual Action Toolbar
                        AnimatedVisibility(
                          visible = isSelected,
                          enter = fadeIn(),
                          exit = fadeOut()
                        ) {
                          Row(
                            modifier = Modifier
                              .padding(top = 8.dp, bottom = 4.dp)
                              .clip(RoundedCornerShape(22.dp))
                              .background(MaterialTheme.colorScheme.surface)
                              .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(22.dp))
                              .padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                          ) {
                            // Color dots
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

                            // Remove highlight button if present
                            if (matchingHighlight != null) {
                              IconButton(
                                onClick = {
                                  viewModel.removeHighlightForText(paragraph)
                                  activeSelectedParagraph = null
                                },
                                modifier = Modifier.size(24.dp)
                              ) {
                                Icon(
                                  imageVector = Icons.Default.FormatColorReset,
                                  contentDescription = "Remove Highlight",
                                  tint = MaterialTheme.colorScheme.error,
                                  modifier = Modifier.size(16.dp)
                                )
                              }
                            }

                            Spacer(modifier = Modifier.width(2.dp))

                            // Add Note / Annotate Button
                            IconButton(
                              onClick = {
                                showAddNoteDialog = paragraph
                                activeSelectedParagraph = null
                              },
                              modifier = Modifier.size(24.dp)
                            ) {
                              Icon(
                                imageVector = Icons.Default.EditNote,
                                contentDescription = "Annotate",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                              )
                            }

                            // Copy text Button
                            IconButton(
                              onClick = {
                                try {
                                  clipboardManager.setText(AnnotatedString(paragraph))
                                  Toast.makeText(context, "Text copied to clipboard", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                  e.printStackTrace()
                                }
                                activeSelectedParagraph = null
                              },
                              modifier = Modifier.size(24.dp)
                            ) {
                              Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                              )
                            }

                            // Quick Listen Button
                            IconButton(
                              onClick = {
                                viewModel.playBookAudio(currentBook, startFromPage = currentPageNum, openFullPlayer = false)
                                activeSelectedParagraph = null
                              },
                              modifier = Modifier.size(24.dp)
                            ) {
                              Icon(
                                imageVector = Icons.Default.Headphones,
                                contentDescription = "Listen to page",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                              )
                            }
                          }
                        }
                      }
                    }

                    // Bottom padding ensuring text NEVER overlaps anchored bottom-right page number
                    Spacer(modifier = Modifier.height(60.dp))
                  }
                }

                // Dynamic Page Curl Shadow during Flipbook Drag
                if (currentFraction != 0f) {
                  Box(
                    modifier = Modifier
                      .fillMaxSize()
                      .background(
                        Brush.horizontalGradient(
                          if (currentFraction < 0f)
                            listOf(Color.Transparent, Color.Black.copy(alpha = shadowAlpha))
                          else
                            listOf(Color.Black.copy(alpha = shadowAlpha), Color.Transparent)
                        )
                      )
                  )
                }

                // 3. ANCHORED BOTTOM-RIGHT PAGE NUMBER
                Box(
                  modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = 12.dp)
                    .testTag("reader_bottom_right_page_number")
                ) {
                  Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when (currentTheme) {
                      AppThemeMode.WARM_PAPER -> Color(0xFFE8DFC9).copy(alpha = 0.85f)
                      AppThemeMode.DARK -> Color(0xFF2B2536).copy(alpha = 0.85f)
                      else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f)
                    },
                    border = androidx.compose.foundation.BorderStroke(
                      0.5.dp,
                      MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    )
                  ) {
                    Text(
                      text = "Page $currentPageNum of $totalPages",
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
            }
          }
        }
      }

      // 3. Bottom Scrubber Bar
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
