package com.example.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BookEntity
import java.io.File

@Composable
fun BookCard(
  book: BookEntity,
  onClick: () -> Unit,
  onAudioClick: () -> Unit,
  onDeleteClick: () -> Unit,
  onRenameClick: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  var showMenu by remember { mutableStateOf(false) }
  var showRenameDialog by remember { mutableStateOf(false) }
  var editedTitle by remember(book.title, showRenameDialog) { mutableStateOf(book.title) }
  var isTitleError by remember { mutableStateOf(false) }

  val coverBitmap = remember(book.coverPath) {
    book.coverPath?.let { path ->
      val file = File(path)
      if (file.exists()) {
        BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
      } else null
    }
  }

  val baseColor = try {
    Color(android.graphics.Color.parseColor(book.coverColorHex))
  } catch (e: Exception) {
    MaterialTheme.colorScheme.primary
  }

  Column(
    modifier = modifier
      .width(135.dp)
      .clickable { onClick() }
      .testTag("book_card_${book.id}"),
    horizontalAlignment = Alignment.Start
  ) {
    // Book Cover Container
    Card(
      shape = RoundedCornerShape(12.dp),
      elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
      modifier = Modifier
        .fillMaxWidth()
        .aspectRatio(0.72f)
    ) {
      Box(modifier = Modifier.fillMaxSize()) {
        if (coverBitmap != null) {
          Image(
            bitmap = coverBitmap,
            contentDescription = book.title,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
          )
        } else {
          // Elegant Fallback Cover
          Box(
            modifier = Modifier
              .fillMaxSize()
              .background(baseColor)
              .padding(10.dp)
          ) {
            Box(
              modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxSize()
                .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(4.dp))
            )
            Column(
              modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
              verticalArrangement = Arrangement.Center,
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Text(
                text = book.title,
                style = MaterialTheme.typography.titleMedium.copy(
                  color = Color.White,
                  fontFamily = FontFamily.Serif,
                  fontWeight = FontWeight.Bold,
                  fontSize = 13.sp
                ),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
              )
              Spacer(modifier = Modifier.height(6.dp))
              Text(
                text = book.author,
                style = MaterialTheme.typography.bodySmall.copy(
                  color = Color.White.copy(alpha = 0.8f),
                  fontSize = 10.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
              )
            }
          }
        }

        // Offline Ready pill indicator
        if (book.isOfflineAudioReady) {
          Box(
            modifier = Modifier
              .align(Alignment.TopStart)
              .padding(6.dp)
              .clip(CircleShape)
              .background(Color.Black.copy(alpha = 0.45f))
              .padding(4.dp)
          ) {
            Icon(
              imageVector = Icons.Default.WifiOff,
              contentDescription = "Offline Ready",
              tint = Color.White,
              modifier = Modifier.size(10.dp)
            )
          }
        }

        // Quick Listen Button Overlay
        Box(
          modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(6.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
            .clickable { onAudioClick() }
            .padding(6.dp)
            .testTag("book_audio_btn_${book.id}")
        ) {
          Icon(
            imageVector = Icons.Default.Headphones,
            contentDescription = "Listen",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(14.dp)
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(8.dp))

    // Title & Options row
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = book.title,
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.weight(1f)
      )

      Box {
        IconButton(
          onClick = { showMenu = true },
          modifier = Modifier.size(20.dp)
        ) {
          Icon(
            imageVector = Icons.Default.MoreVert,
            contentDescription = "Options",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(14.dp)
          )
        }

        DropdownMenu(
          expanded = showMenu,
          onDismissRequest = { showMenu = false }
        ) {
          DropdownMenuItem(
            text = { Text("Rename Book") },
            onClick = {
              showMenu = false
              editedTitle = book.title
              isTitleError = false
              showRenameDialog = true
            },
            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
          )
          DropdownMenuItem(
            text = { Text("Listen to Audiobook") },
            onClick = {
              showMenu = false
              onAudioClick()
            },
            leadingIcon = { Icon(Icons.Default.Headphones, contentDescription = null) }
          )
          DropdownMenuItem(
            text = { Text("Delete Book") },
            onClick = {
              showMenu = false
              onDeleteClick()
            },
            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) }
          )
        }
      }
    }

    Text(
      text = book.author,
      style = MaterialTheme.typography.bodySmall.copy(
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 11.sp
      ),
      maxLines = 1,
      overflow = TextOverflow.Ellipsis
    )

    Spacer(modifier = Modifier.height(4.dp))

    // Reading Progress
    val percent = (book.readingProgress * 100).toInt()
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      LinearProgressIndicator(
        progress = { book.readingProgress },
        modifier = Modifier
          .weight(1f)
          .height(3.dp)
          .clip(RoundedCornerShape(2.dp)),
        color = MaterialTheme.colorScheme.primary,
        trackColor = MaterialTheme.colorScheme.surfaceVariant
      )
      Spacer(modifier = Modifier.width(6.dp))
      Text(
        text = "$percent%",
        style = MaterialTheme.typography.labelSmall.copy(
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          fontSize = 10.sp
        )
      )
    }
  }

  // Rename Book Dialog
  if (showRenameDialog) {
    AlertDialog(
      onDismissRequest = { showRenameDialog = false },
      title = {
        Text(
          text = "Rename Book",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )
      },
      text = {
        Column(modifier = Modifier.fillMaxWidth()) {
          if (book.originalFileName.isNotBlank()) {
            Text(
              text = "Original file: ${book.originalFileName}",
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
            placeholder = { Text("Enter book title") },
            singleLine = true,
            isError = isTitleError,
            supportingText = {
              if (isTitleError) {
                Text("Title cannot be empty")
              }
            },
            modifier = Modifier
              .fillMaxWidth()
              .testTag("rename_title_input_${book.id}")
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            val trimmed = editedTitle.trim()
            if (trimmed.isNotEmpty()) {
              onRenameClick(trimmed)
              showRenameDialog = false
            } else {
              isTitleError = true
            }
          },
          modifier = Modifier.testTag("rename_save_btn_${book.id}")
        ) {
          Text("Save")
        }
      },
      dismissButton = {
        TextButton(
          onClick = { showRenameDialog = false },
          modifier = Modifier.testTag("rename_cancel_btn_${book.id}")
        ) {
          Text("Cancel")
        }
      }
    )
  }
}

@Composable
fun AddBookCard(
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Card(
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
    border = androidx.compose.foundation.BorderStroke(
      1.dp,
      MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
    ),
    modifier = modifier
      .width(135.dp)
      .aspectRatio(0.72f)
      .clickable { onClick() }
      .testTag("add_book_card")
  ) {
    Column(
      modifier = Modifier.fillMaxSize(),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Box(
        modifier = Modifier
          .size(40.dp)
          .clip(CircleShape)
          .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Default.Add,
          contentDescription = "Add Book",
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(24.dp)
        )
      }
      Spacer(modifier = Modifier.height(10.dp))
      Text(
        text = "Add Book",
        style = MaterialTheme.typography.labelLarge.copy(
          color = MaterialTheme.colorScheme.primary,
          fontWeight = FontWeight.SemiBold
        )
      )
      Text(
        text = "Import PDF",
        style = MaterialTheme.typography.bodySmall.copy(
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          fontSize = 10.sp
        )
      )
    }
  }
}
