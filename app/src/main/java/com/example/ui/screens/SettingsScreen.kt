package com.example.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.OfflineTtsPlayer
import com.example.ui.theme.AppThemeMode
import com.example.ui.viewmodel.PageNestViewModel

@Composable
fun SettingsScreen(
  viewModel: PageNestViewModel,
  modifier: Modifier = Modifier
) {
  val currentTheme by viewModel.appThemeMode.collectAsState()
  val storageBytes by viewModel.storageUsageBytes.collectAsState()
  val playbackState by viewModel.playbackState.collectAsState()

  var showThemeDialog by remember { mutableStateOf(false) }
  var showVoiceDialog by remember { mutableStateOf(false) }
  var showStorageDialog by remember { mutableStateOf(false) }
  var showHelpDialog by remember { mutableStateOf(false) }
  var showAboutDialog by remember { mutableStateOf(false) }

  val voiceOptions = remember { viewModel.ttsPlayer.getAvailableVoiceOptions() }
  val storageMb = String.format("%.1f MB", (storageBytes / (1024.0 * 1024.0)).coerceAtLeast(0.1))

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 20.dp)
      .testTag("settings_screen"),
    contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // 1. Header
    item {
      Text(
        text = "Settings",
        style = MaterialTheme.typography.headlineMedium.copy(
          fontFamily = FontFamily.Serif,
          fontWeight = FontWeight.Bold
        )
      )
    }

    // 2. Settings Group Card
    item {
      Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = androidx.compose.foundation.BorderStroke(
          1.dp,
          MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
        ),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.fillMaxWidth()) {
          // Theme Option
          SettingsRow(
            icon = Icons.Default.Palette,
            title = "Theme",
            subtitle = when (currentTheme) {
              AppThemeMode.LIGHT -> "Light"
              AppThemeMode.DARK -> "Dark"
              AppThemeMode.WARM_PAPER -> "Warm Paper"
            },
            onClick = { showThemeDialog = true }
          )

          SettingsDivider()

          // Audio Settings / Voice
          SettingsRow(
            icon = Icons.Default.Headphones,
            title = "Audio Settings",
            subtitle = playbackState.currentVoiceName,
            onClick = { showVoiceDialog = true }
          )

          SettingsDivider()

          // Storage Option
          SettingsRow(
            icon = Icons.Default.Storage,
            title = "Storage",
            subtitle = "$storageMb used",
            onClick = { showStorageDialog = true }
          )

          // Storage Option
          SettingsRow(
            icon = Icons.Default.Storage,
            title = "Storage",
            subtitle = "$storageMb used",
            onClick = { showStorageDialog = true }
          )

          SettingsDivider()

          // Help & Support
          SettingsRow(
            icon = Icons.Default.HelpOutline,
            title = "Help & Support",
            subtitle = "Guides & offline tips",
            onClick = { showHelpDialog = true }
          )

          SettingsDivider()

          // About Unfold (renamed from PageNest per user request)
          SettingsRow(
            icon = Icons.Default.Info,
            title = "About Unfold",
            subtitle = "v1.0 • Offline Digital Reader",
            onClick = { showAboutDialog = true }
          )
        }
      }
    }
  }

  // Theme Picker Dialog
  if (showThemeDialog) {
    AlertDialog(
      onDismissRequest = { showThemeDialog = false },
      title = { Text("Choose Color Theme") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          ThemeRowItem(title = "Light (Warm Cream)", selected = currentTheme == AppThemeMode.LIGHT) {
            viewModel.setAppThemeMode(AppThemeMode.LIGHT)
            showThemeDialog = false
          }
          ThemeRowItem(title = "Dark (Midnight Charcoal)", selected = currentTheme == AppThemeMode.DARK) {
            viewModel.setAppThemeMode(AppThemeMode.DARK)
            showThemeDialog = false
          }
          ThemeRowItem(title = "Warm Reading Paper", selected = currentTheme == AppThemeMode.WARM_PAPER) {
            viewModel.setAppThemeMode(AppThemeMode.WARM_PAPER)
            showThemeDialog = false
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { showThemeDialog = false }) { Text("Close") }
      }
    )
  }

  // Voice Picker Dialog with Playable, Clickable Natural Voices
  if (showVoiceDialog) {
    var previewingVoiceId by remember { mutableStateOf<String?>(null) }

    AlertDialog(
      onDismissRequest = { showVoiceDialog = false },
      title = {
        Text(
          text = "Text-to-Speech Voices",
          style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold)
        )
      },
      text = {
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Text(
            text = "Tap to select a voice. Tap the play button to hear a sample.",
            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
          )
          Spacer(modifier = Modifier.height(6.dp))

          LazyColumn(
            modifier = Modifier
              .fillMaxWidth()
              .height(300.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            items(voiceOptions, key = { it.id }) { option ->
              val isSelected = playbackState.currentVoiceName == option.name
              val isPreviewing = previewingVoiceId == option.id

              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(12.dp))
                  .background(
                    if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                  )
                  .clickable {
                    viewModel.ttsPlayer.setVoiceOption(option)
                    viewModel.ttsPlayer.previewVoice(option.id)
                    previewingVoiceId = option.id
                  }
                  .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Row(
                  modifier = Modifier.weight(1f),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Icon(
                    imageVector = if (isSelected) Icons.Default.VolumeUp else Icons.Default.Headphones,
                    contentDescription = null,
                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                  )
                  Spacer(modifier = Modifier.width(10.dp))
                  Column {
                    Text(
                      text = option.name,
                      style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 13.sp
                      )
                    )
                    Text(
                      text = option.language,
                      style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                      )
                    )
                  }
                }

                // Playable Preview Button
                Box(
                  modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                    .clickable {
                      viewModel.ttsPlayer.previewVoice(option.id)
                      previewingVoiceId = option.id
                    },
                  contentAlignment = Alignment.Center
                ) {
                  Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Preview voice",
                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                  )
                }
              }
            }
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { showVoiceDialog = false }) { Text("Done") }
      }
    )
  }

  // Storage Dialog
  if (showStorageDialog) {
    AlertDialog(
      onDismissRequest = { showStorageDialog = false },
      title = { Text("Local Storage Usage") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("Books & PDF Storage: $storageMb")
          Text("All documents and text extracts are stored strictly on-device in app private storage.")
          Text("No cloud bandwidth or external servers are contacted.")
        }
      },
      confirmButton = {
        TextButton(onClick = { showStorageDialog = false }) { Text("Close") }
      }
    )
  }

  // Help Dialog
  if (showHelpDialog) {
    AlertDialog(
      onDismissRequest = { showHelpDialog = false },
      title = { Text("Unfold Help & Offline Guide") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text("• How to Import:", style = MaterialTheme.typography.titleSmall)
          Text("Tap 'Upload PDF Books' or 'Add Your PDF' to choose any PDF from your device.")
          Spacer(modifier = Modifier.height(4.dp))
          Text("• Listening to Audiobooks:", style = MaterialTheme.typography.titleSmall)
          Text("Tap the headphone icon on any book or inside the reader. Unfold uses Android's native offline TTS.")
          Spacer(modifier = Modifier.height(4.dp))
          Text("• Highlights & Notes:", style = MaterialTheme.typography.titleSmall)
          Text("Tap any passage in Clean Reading mode to highlight in Yellow, Lavender, Green or Pink.")
        }
      },
      confirmButton = {
        TextButton(onClick = { showHelpDialog = false }) { Text("Got it") }
      }
    )
  }

  // About Unfold Dialog
  if (showAboutDialog) {
    AlertDialog(
      onDismissRequest = { showAboutDialog = false },
      title = { Text("About Unfold") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text("Unfold: Read. Listen. Download.", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
          Text("A complete personal digital reading and audiobook player for Android.")
          Spacer(modifier = Modifier.height(4.dp))
          Text("• 100% on-device local storage")
          Text("• High-resolution PDF rendering")
          Text("• Offline text-to-speech audio player")
          Text("• Annotations, bookmarks & highlights")
        }
      },
      confirmButton = {
        TextButton(onClick = { showAboutDialog = false }) { Text("Close") }
      }
    )
  }
}

@Composable
private fun SettingsRow(
  icon: ImageVector,
  title: String,
  subtitle: String,
  onClick: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onClick() }
      .padding(horizontal = 16.dp, vertical = 14.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(20.dp)
      )
      Spacer(modifier = Modifier.width(14.dp))
      Text(
        text = title,
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
      )
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
      )
      Spacer(modifier = Modifier.width(6.dp))
      Icon(
        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.size(12.dp)
      )
    }
  }
}

@Composable
private fun SettingsDivider() {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .height(1.dp)
      .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
  )
}

@Composable
private fun ThemeRowItem(
  title: String,
  selected: Boolean,
  onClick: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(8.dp))
      .clickable { onClick() }
      .padding(horizontal = 8.dp, vertical = 10.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Text(text = title, style = MaterialTheme.typography.bodyMedium)
    if (selected) {
      Text("Active", style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold))
    }
  }
}
