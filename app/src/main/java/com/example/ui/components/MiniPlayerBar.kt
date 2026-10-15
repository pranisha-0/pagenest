package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.PlaybackState

@Composable
fun MiniPlayerBar(
  state: PlaybackState,
  onExpand: () -> Unit,
  onPlayPause: () -> Unit,
  onClose: () -> Unit,
  modifier: Modifier = Modifier
) {
  val isVisible = state.bookId != null

  AnimatedVisibility(
    visible = isVisible,
    enter = slideInVertically(initialOffsetY = { it }),
    exit = slideOutVertically(targetOffsetY = { it }),
    modifier = modifier
  ) {
    Card(
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surface
      ),
      elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
      border = androidx.compose.foundation.BorderStroke(
        1.dp,
        MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
      ),
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 6.dp)
        .clickable { onExpand() }
        .testTag("mini_player_bar")
    ) {
      Column(modifier = Modifier.fillMaxWidth()) {
        // Subtle progress line on top of card
        val progress = if (state.paragraphs.isNotEmpty()) {
          (state.currentParagraphIndex.toFloat() / state.paragraphs.size.toFloat()).coerceIn(0f, 1f)
        } else 0f

        LinearProgressIndicator(
          progress = { progress },
          modifier = Modifier
            .fillMaxWidth()
            .height(2.5.dp),
          color = MaterialTheme.colorScheme.primary,
          trackColor = MaterialTheme.colorScheme.surfaceVariant
        )

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          // Left Cover / Audio icon
          val coverBg = try {
            Color(android.graphics.Color.parseColor(state.coverColorHex))
          } catch (e: Exception) {
            MaterialTheme.colorScheme.primary
          }

          Box(
            modifier = Modifier
              .size(42.dp)
              .clip(RoundedCornerShape(8.dp))
              .background(coverBg),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.VolumeUp,
              contentDescription = "Audio playing",
              tint = Color.White,
              modifier = Modifier.size(20.dp)
            )
          }

          Spacer(modifier = Modifier.width(10.dp))

          // Title & Chapter
          Column(
            modifier = Modifier.weight(1f)
          ) {
            Text(
              text = state.bookTitle,
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            Text(
              text = if (state.chapterName.isNotBlank()) state.chapterName else state.author,
              style = MaterialTheme.typography.bodySmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
              ),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }

          Spacer(modifier = Modifier.width(8.dp))

          // Controls
          Row(
            verticalAlignment = Alignment.CenterVertically
          ) {
            // Play / Pause
            Box(
              modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .clickable { onPlayPause() }
                .testTag("mini_player_play_pause"),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (state.isPlaying) "Pause" else "Play",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(20.dp)
              )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Close
            IconButton(
              onClick = { onClose() },
              modifier = Modifier.size(28.dp)
            ) {
              Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Dismiss",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
              )
            }
          }
        }
      }
    }
  }
}
