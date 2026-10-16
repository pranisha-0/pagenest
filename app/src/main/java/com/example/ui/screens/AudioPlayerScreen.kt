package com.example.ui.screens

import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.ActiveScreen
import com.example.ui.viewmodel.MainTab
import com.example.ui.viewmodel.PageNestViewModel
import java.io.File

@Composable
fun AudioPlayerScreen(
  viewModel: PageNestViewModel,
  modifier: Modifier = Modifier
) {
  val playbackState by viewModel.playbackState.collectAsState()
  var showSpeedDropdown by remember { mutableStateOf(false) }
  var showSleepTimerMenu by remember { mutableStateOf(false) }

  BackHandler {
    viewModel.closeFullAudioPlayer()
  }

  val baseColor = try {
    Color(android.graphics.Color.parseColor(playbackState.coverColorHex))
  } catch (e: Exception) {
    MaterialTheme.colorScheme.primary
  }

  val coverBitmap = remember(playbackState.coverPath) {
    playbackState.coverPath?.let { path ->
      val file = File(path)
      if (file.exists()) {
        BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
      } else null
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
        .padding(horizontal = 24.dp)
        .verticalScroll(rememberScrollState()),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.SpaceBetween
    ) {
      // 1. Top Bar
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 10.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        IconButton(
          onClick = { viewModel.closeFullAudioPlayer() },
          modifier = Modifier.testTag("audio_player_close_btn")
        ) {
          Icon(
            imageVector = Icons.Default.KeyboardArrowDown,
            contentDescription = "Minimize Player",
            tint = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.size(30.dp)
          )
        }

        Text(
          text = "Audio Player (Offline TTS)",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onBackground
        )

        // Sleep Timer Button
        Box {
          IconButton(
            onClick = { showSleepTimerMenu = true },
            modifier = Modifier.testTag("audio_sleep_timer_btn")
          ) {
            Icon(
              imageVector = Icons.Default.Bedtime,
              contentDescription = "Sleep Timer",
              tint = if (playbackState.sleepTimerMinutesLeft > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
          }

          DropdownMenu(
            expanded = showSleepTimerMenu,
            onDismissRequest = { showSleepTimerMenu = false }
          ) {
            DropdownMenuItem(
              text = { Text("Timer: Off") },
              onClick = {
                viewModel.setSleepTimer(0)
                showSleepTimerMenu = false
              }
            )
            listOf(15, 30, 45, 60).forEach { mins ->
              DropdownMenuItem(
                text = { Text("$mins minutes") },
                onClick = {
                  viewModel.setSleepTimer(mins)
                  showSleepTimerMenu = false
                }
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // 2. Large Book Cover Art Card
      Card(
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier
          .width(220.dp)
          .aspectRatio(0.75f)
          .testTag("audio_cover_card")
      ) {
        Box(modifier = Modifier.fillMaxSize()) {
          if (coverBitmap != null) {
            Image(
              bitmap = coverBitmap,
              contentDescription = playbackState.bookTitle,
              modifier = Modifier.fillMaxSize(),
              contentScale = ContentScale.Crop
            )
          } else {
            Box(
              modifier = Modifier
                .fillMaxSize()
                .background(baseColor)
                .padding(16.dp)
            ) {
              Box(
                modifier = Modifier
                  .fillMaxSize()
                  .border(1.5.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
              )
              Column(
                modifier = Modifier
                  .fillMaxSize()
                  .padding(12.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
              ) {
                Text(
                  text = playbackState.bookTitle,
                  style = MaterialTheme.typography.headlineSmall.copy(
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                  ),
                  textAlign = TextAlign.Center,
                  maxLines = 3
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                  text = playbackState.author,
                  style = MaterialTheme.typography.bodyMedium.copy(color = Color.White.copy(alpha = 0.85f)),
                  textAlign = TextAlign.Center,
                  maxLines = 1
                )
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      // 3. Title & Chapter
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
      ) {
        Text(
          text = playbackState.bookTitle,
          style = MaterialTheme.typography.headlineMedium.copy(
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold
          ),
          textAlign = TextAlign.Center,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = playbackState.author,
          style = MaterialTheme.typography.titleMedium.copy(
            color = MaterialTheme.colorScheme.onSurfaceVariant
          ),
          textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = if (playbackState.chapterName.isNotBlank()) playbackState.chapterName else "Chapter 1",
          style = MaterialTheme.typography.bodySmall.copy(
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
          ),
          textAlign = TextAlign.Center
        )
      }

      Spacer(modifier = Modifier.height(16.dp))

      // 4. Live Paragraph Text Card (highlighting speech in real-time)
      if (playbackState.paragraphs.isNotEmpty()) {
        val currentParagraphText = playbackState.paragraphs.getOrNull(playbackState.currentParagraphIndex) ?: ""
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 100.dp, max = 160.dp)
            .padding(horizontal = 4.dp)
        ) {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .padding(14.dp)
              .verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = currentParagraphText,
              style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = FontFamily.Serif,
                lineHeight = 22.sp,
                color = MaterialTheme.colorScheme.onSurface
              ),
              textAlign = TextAlign.Center
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // 5. Scrubber & Time
      Column(modifier = Modifier.fillMaxWidth()) {
        val currentP = playbackState.currentParagraphIndex
        val totalP = (playbackState.paragraphs.size - 1).coerceAtLeast(1)

        Slider(
          value = currentP.toFloat(),
          onValueChange = { newIndex ->
            viewModel.ttsPlayer.seekToParagraph(newIndex.toInt())
          },
          valueRange = 0f..totalP.toFloat(),
          colors = SliderDefaults.colors(
            thumbColor = MaterialTheme.colorScheme.primary,
            activeTrackColor = MaterialTheme.colorScheme.primary,
            inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
          ),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("audio_scrubber_slider")
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          val elapsedMin = (playbackState.elapsedSeconds / 60)
          val elapsedSec = (playbackState.elapsedSeconds % 60)
          val remainingSec = (playbackState.totalEstimatedSeconds - playbackState.elapsedSeconds).coerceAtLeast(0)
          val remMin = remainingSec / 60
          val remSec = remainingSec % 60

          Text(
            text = String.format("%02d:%02d", elapsedMin, elapsedSec),
            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
          )
          Text(
            text = String.format("-%02d:%02d", remMin, remSec),
            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // 6. Playback Controls Row (15s rewind, large Play/Pause, 15s forward)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
      ) {
        // 15s Rewind
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier = Modifier
            .clickable { viewModel.skipAudioSeconds(-15) }
            .padding(12.dp)
            .testTag("audio_rewind_15_btn")
        ) {
          Icon(
            imageVector = Icons.Default.Replay10,
            contentDescription = "Rewind 15 seconds",
            tint = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.size(34.dp)
          )
          Text(
            text = "15s",
            style = MaterialTheme.typography.labelSmall.copy(
              fontSize = 9.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          )
        }

        Spacer(modifier = Modifier.width(20.dp))

        // Large Play / Pause Pill
        Box(
          modifier = Modifier
            .size(68.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary)
            .clickable { viewModel.toggleAudioPlayback() }
            .testTag("audio_full_play_pause_btn"),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
            contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(36.dp)
          )
        }

        Spacer(modifier = Modifier.width(20.dp))

        // 15s Forward
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier = Modifier
            .clickable { viewModel.skipAudioSeconds(15) }
            .padding(12.dp)
            .testTag("audio_forward_15_btn")
        ) {
          Icon(
            imageVector = Icons.Default.Forward10,
            contentDescription = "Forward 15 seconds",
            tint = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.size(34.dp)
          )
          Text(
            text = "15s",
            style = MaterialTheme.typography.labelSmall.copy(
              fontSize = 9.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // 7. Playback Speed Selector (from screenshot: "Playback Speed: 1.0x v")
      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(12.dp))
          .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
          .clickable { showSpeedDropdown = true }
          .padding(horizontal = 16.dp, vertical = 8.dp)
          .testTag("audio_speed_selector")
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = "Playback Speed",
            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "${playbackState.playbackSpeed}x",
            style = MaterialTheme.typography.labelMedium.copy(
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary
            )
          )
          Icon(
            imageVector = Icons.Default.KeyboardArrowDown,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
          )
        }

        DropdownMenu(
          expanded = showSpeedDropdown,
          onDismissRequest = { showSpeedDropdown = false }
        ) {
          listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
            DropdownMenuItem(
              text = { Text("${speed}x") },
              onClick = {
                viewModel.setPlaybackSpeed(speed)
                showSpeedDropdown = false
              }
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      // 8. Bottom Navigation Switcher: [Reader] [Audio (Active)] [Notes] (from screenshot!)
      Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 12.dp)
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp),
          horizontalArrangement = Arrangement.SpaceEvenly,
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Reader tab
          Box(
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(20.dp))
              .clickable {
                viewModel.closeFullAudioPlayer()
                val currentBook = viewModel.currentReadingBook.value
                if (currentBook != null) {
                  viewModel.openBook(currentBook)
                }
              }
              .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.AutoStories,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Reader",
                style = MaterialTheme.typography.labelMedium.copy(
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              )
            }
          }

          // Audio tab (Active)
          Box(
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(20.dp))
              .background(MaterialTheme.colorScheme.primary)
              .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.Headphones,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Audio",
                style = MaterialTheme.typography.labelMedium.copy(
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onPrimary
                )
              )
            }
          }

          // Notes tab
          Box(
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(20.dp))
              .clickable {
                viewModel.closeFullAudioPlayer()
                viewModel.setTab(MainTab.NOTES)
              }
              .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.EditNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Notes",
                style = MaterialTheme.typography.labelMedium.copy(
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              )
            }
          }
        }
      }
    }
  }
}
