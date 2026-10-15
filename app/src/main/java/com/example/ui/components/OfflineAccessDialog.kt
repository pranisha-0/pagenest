package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R

@Composable
fun OfflineAccessDialog(
  onDismiss: () -> Unit,
  storageBytesUsed: Long = 0L,
  modifier: Modifier = Modifier
) {
  Dialog(onDismissRequest = onDismiss) {
    Card(
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
      modifier = modifier
        .fillMaxWidth()
        .padding(horizontal = 8.dp)
        .testTag("offline_access_dialog")
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        // Mountain & checkmark illustration from generated asset
        Box(
          modifier = Modifier
            .size(130.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
          contentAlignment = Alignment.Center
        ) {
          Image(
            painter = painterResource(id = R.drawable.illustration_offline_ready_1791986929173),
            contentDescription = "Offline ready illustration",
            modifier = Modifier.fillMaxWidth(0.95f),
            contentScale = ContentScale.Fit
          )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
          text = "You're all set!",
          style = MaterialTheme.typography.headlineMedium.copy(
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          ),
          textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
          text = "No internet? No problem. Access your books anytime, anywhere. You can now read and listen offline without an internet connection.",
          style = MaterialTheme.typography.bodyMedium.copy(
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 21.sp
          ),
          textAlign = TextAlign.Center,
          modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Offline capability checklists
        Card(
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            OfflineCheckItem(
              icon = Icons.Default.Description,
              title = "PDF Documents Stored On-Device",
              subtitle = "Original vector pages cached locally in app storage"
            )
            OfflineCheckItem(
              icon = Icons.Default.RecordVoiceOver,
              title = "Local Android Speech Engine",
              subtitle = "100% offline text-to-speech audio synthesis"
            )
            OfflineCheckItem(
              icon = Icons.Default.CloudOff,
              title = "Zero Server Dependence",
              subtitle = "Notes, highlights and bookmarks stay strictly private"
            )
          }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
          onClick = onDismiss,
          shape = RoundedCornerShape(20.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
          ),
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("offline_dialog_done_btn")
        ) {
          Text(
            text = "Done",
            style = MaterialTheme.typography.labelLarge.copy(
              fontWeight = FontWeight.Bold,
              fontSize = 15.sp
            )
          )
        }
      }
    }
  }
}

@Composable
private fun OfflineCheckItem(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  title: String,
  subtitle: String
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier.fillMaxWidth()
  ) {
    Box(
      modifier = Modifier
        .size(32.dp)
        .clip(CircleShape)
        .background(MaterialTheme.colorScheme.primaryContainer),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(16.dp)
      )
    }

    Spacer(modifier = Modifier.width(10.dp))

    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleSmall.copy(
          fontWeight = FontWeight.SemiBold,
          fontSize = 12.sp
        )
      )
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall.copy(
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          fontSize = 10.sp
        )
      )
    }

    Icon(
      imageVector = Icons.Default.CheckCircle,
      contentDescription = "Ready",
      tint = Color(0xFF4CAF50),
      modifier = Modifier.size(16.dp)
    )
  }
}
