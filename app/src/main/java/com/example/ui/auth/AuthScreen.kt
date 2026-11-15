package com.example.ui.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.ui.theme.CadenceTheme

@Composable
fun AuthScreen(
  onAuthSuccess: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }

  var isLoading by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  LaunchedEffect(Unit) {
    attemptAutoSignIn(
      context = context,
      credentialManager = credentialManager,
      onAuthSuccess = onAuthSuccess,
      onUnauthenticated = {},
      scope = scope,
    )
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(
        Brush.verticalGradient(
          colors = listOf(
            MaterialTheme.colorScheme.background,
            MaterialTheme.colorScheme.surfaceContainerLow,
          )
        )
      )
      .safeDrawingPadding(),
    contentAlignment = Alignment.Center,
  ) {
    Column(
      modifier = Modifier
        .widthIn(max = 540.dp)
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 24.dp, vertical = 16.dp),
      verticalArrangement = Arrangement.SpaceBetween,
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Box(
            modifier = Modifier
              .size(38.dp)
              .clip(RoundedCornerShape(10.dp))
              .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = Icons.Outlined.Flag,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onPrimary,
              modifier = Modifier.size(20.dp),
            )
          }
          Column {
            Text(
              text = stringResource(R.string.app_name),
              style = MaterialTheme.typography.headlineSmall,
              color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
              text = "Personal Project Studio",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }

        Card(
          shape = MaterialTheme.shapes.large,
          elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
          Image(
            painter = painterResource(id = R.drawable.img_studio_hero),
            contentDescription = "Cadence project timeline workspace illustration",
            modifier = Modifier
              .fillMaxWidth()
              .height(136.dp),
            contentScale = ContentScale.Crop,
          )
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text(
            text = "Keep every project moving with clarity.",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
          )
          Text(
            text = stringResource(R.string.sign_in_tagline),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          AuthFeatureRow(
            icon = Icons.Outlined.CheckCircle,
            title = "Centralized tasks & priorities",
            subtitle = "Track daily execution across every active project.",
          )
          AuthFeatureRow(
            icon = Icons.Outlined.EventAvailable,
            title = "Smart deadline radar",
            subtitle = "Spot overdue items, today's focus, and upcoming weeks.",
          )
          AuthFeatureRow(
            icon = Icons.Outlined.Flag,
            title = "Milestone roadmaps",
            subtitle = "Link tasks directly to key deliverables and checkpoints.",
          )
        }
      }

      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        if (errorMessage != null) {
          Surface(
            color = MaterialTheme.colorScheme.errorContainer,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text(
              text = errorMessage.orEmpty(),
              color = MaterialTheme.colorScheme.onErrorContainer,
              style = MaterialTheme.typography.bodySmall,
              modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )
          }
        }

        Button(
          onClick = {
            errorMessage = null
            isLoading = true
            onGoogleSignInClicked(
              context = context,
              credentialManager = credentialManager,
              onAuthSuccess = {
                isLoading = false
                onAuthSuccess()
              },
              onAuthError = { msg ->
                isLoading = false
                errorMessage = msg
              },
              scope = scope,
              onAuthCancelled = {
                isLoading = false
              },
            )
          },
          enabled = !isLoading,
          shape = MaterialTheme.shapes.medium,
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
          ),
          modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .testTag("google_sign_in_button"),
        ) {
          if (isLoading) {
            CircularProgressIndicator(
              modifier = Modifier.size(22.dp),
              color = MaterialTheme.colorScheme.onPrimary,
              strokeWidth = 2.dp,
            )
          } else {
            Icon(
              imageVector = Icons.Filled.AccountCircle,
              contentDescription = null,
              modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = stringResource(R.string.sign_in_with_google),
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            )
          }
        }
      }
    }
  }
}

@Composable
private fun AuthFeatureRow(
  icon: ImageVector,
  title: String,
  subtitle: String,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    Box(
      modifier = Modifier
        .size(40.dp)
        .clip(CircleShape)
        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)),
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(20.dp),
      )
    }
    Column {
      Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onBackground,
      )
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Preview(showBackground = true)
@Composable
private fun AuthScreenPreview() {
  CadenceTheme {
    AuthScreen(onAuthSuccess = {})
  }
}
