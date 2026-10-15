package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.components.MiniPlayerBar
import com.example.ui.components.OfflineAccessDialog
import com.example.ui.screens.AnnotationsScreen
import com.example.ui.screens.AudioPlayerScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.ReaderScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.PageNestTheme
import com.example.ui.viewmodel.ActiveScreen
import com.example.ui.viewmodel.MainTab
import com.example.ui.viewmodel.PageNestViewModel

class MainActivity : ComponentActivity() {

  private val viewModel: PageNestViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      val appTheme by viewModel.appThemeMode.collectAsState()

      PageNestTheme(themeMode = appTheme) {
        PageNestApp(viewModel = viewModel)
      }
    }
  }
}

@Composable
fun PageNestApp(viewModel: PageNestViewModel) {
  val activeScreen by viewModel.activeScreen.collectAsState()
  val currentTab by viewModel.currentTab.collectAsState()
  val playbackState by viewModel.playbackState.collectAsState()
  val showOfflineDialog by viewModel.showOfflineDialog.collectAsState()
  val storageBytes by viewModel.storageUsageBytes.collectAsState()

  // Hardware Back Navigation
  BackHandler(enabled = activeScreen != ActiveScreen.MAIN_TABS || currentTab != MainTab.HOME) {
    when {
      activeScreen == ActiveScreen.READER -> viewModel.closeReader()
      activeScreen == ActiveScreen.AUDIO_FULL -> viewModel.closeFullAudioPlayer()
      currentTab != MainTab.HOME -> viewModel.setTab(MainTab.HOME)
    }
  }

  Box(modifier = Modifier.fillMaxSize()) {
    AnimatedContent(
      targetState = activeScreen,
      transitionSpec = { fadeIn() togetherWith fadeOut() },
      label = "screen_transition"
    ) { screen ->
      when (screen) {
        ActiveScreen.READER -> {
          ReaderScreen(viewModel = viewModel)
        }
        ActiveScreen.AUDIO_FULL -> {
          AudioPlayerScreen(viewModel = viewModel)
        }
        ActiveScreen.MAIN_TABS -> {
          Scaffold(
            bottomBar = {
              Column {
                // Persistent Mini Player above Bottom Bar
                MiniPlayerBar(
                  state = playbackState,
                  onExpand = { viewModel.openFullAudioPlayer() },
                  onPlayPause = { viewModel.toggleAudioPlayback() },
                  onClose = { viewModel.ttsPlayer.stopAndClear() }
                )

                // Navigation Bar
                NavigationBar(
                  containerColor = MaterialTheme.colorScheme.surface,
                  tonalElevation = 6.dp,
                  modifier = Modifier.testTag("main_bottom_nav")
                ) {
                  NavigationItem(
                    selected = currentTab == MainTab.HOME,
                    onClick = { viewModel.setTab(MainTab.HOME) },
                    selectedIcon = Icons.Filled.Home,
                    unselectedIcon = Icons.Outlined.Home,
                    label = "Home",
                    tag = "nav_tab_home"
                  )
                  NavigationItem(
                    selected = currentTab == MainTab.LIBRARY,
                    onClick = { viewModel.setTab(MainTab.LIBRARY) },
                    selectedIcon = Icons.Filled.AutoStories,
                    unselectedIcon = Icons.Outlined.AutoStories,
                    label = "Library",
                    tag = "nav_tab_library"
                  )
                  NavigationItem(
                    selected = currentTab == MainTab.NOTES,
                    onClick = { viewModel.setTab(MainTab.NOTES) },
                    selectedIcon = Icons.Filled.EditNote,
                    unselectedIcon = Icons.Outlined.EditNote,
                    label = "Notes",
                    tag = "nav_tab_notes"
                  )
                  NavigationItem(
                    selected = currentTab == MainTab.SETTINGS,
                    onClick = { viewModel.setTab(MainTab.SETTINGS) },
                    selectedIcon = Icons.Filled.Settings,
                    unselectedIcon = Icons.Outlined.Settings,
                    label = "Settings",
                    tag = "nav_tab_settings"
                  )
                }
              }
            }
          ) { innerPadding ->
            Box(
              modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
            ) {
              when (currentTab) {
                MainTab.HOME -> HomeScreen(viewModel = viewModel)
                MainTab.LIBRARY -> LibraryScreen(viewModel = viewModel)
                MainTab.NOTES -> AnnotationsScreen(viewModel = viewModel)
                MainTab.SETTINGS -> SettingsScreen(viewModel = viewModel)
              }
            }
          }
        }
      }
    }

    // Offline Access Dialog
    if (showOfflineDialog) {
      OfflineAccessDialog(
        onDismiss = { viewModel.toggleOfflineDialog(false) },
        storageBytesUsed = storageBytes
      )
    }
  }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.NavigationItem(
  selected: Boolean,
  onClick: () -> Unit,
  selectedIcon: ImageVector,
  unselectedIcon: ImageVector,
  label: String,
  tag: String
) {
  NavigationBarItem(
    selected = selected,
    onClick = onClick,
    icon = {
      Icon(
        imageVector = if (selected) selectedIcon else unselectedIcon,
        contentDescription = label
      )
    },
    label = {
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall
      )
    },
    colors = NavigationBarItemDefaults.colors(
      selectedIconColor = MaterialTheme.colorScheme.primary,
      selectedTextColor = MaterialTheme.colorScheme.primary,
      indicatorColor = MaterialTheme.colorScheme.primaryContainer,
      unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
      unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
    ),
    modifier = Modifier.testTag(tag)
  )
}
