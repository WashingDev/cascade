package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.data.model.Milestone
import com.example.ui.components.MilestoneCard
import com.example.ui.dashboard.DashboardUiState
import com.example.ui.dashboard.MilestoneWithContext
import com.example.ui.theme.color

private enum class MilestoneFilter(val label: String) {
  ALL("All"),
  UPCOMING("Upcoming"),
  REACHED("Reached"),
}

@Composable
fun MilestonesScreen(
  state: DashboardUiState.Success,
  onToggleMilestone: (Milestone) -> Unit,
  onEditMilestone: (Milestone) -> Unit,
  modifier: Modifier = Modifier,
) {
  var filter by remember { mutableStateOf(MilestoneFilter.ALL) }

  val filtered = remember(state.milestones, filter) {
    when (filter) {
      MilestoneFilter.ALL -> state.milestones
      MilestoneFilter.UPCOMING -> state.milestones.filter { !it.milestone.isReached }
      MilestoneFilter.REACHED -> state.milestones.filter { it.milestone.isReached }
    }
  }

  Column(modifier = modifier.fillMaxSize()) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Text(
        text = "Milestone Roadmap",
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onBackground,
      )
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        MilestoneFilter.entries.forEach { option ->
          FilterChip(
            selected = filter == option,
            onClick = { filter = option },
            label = { Text(option.label) },
          )
        }
      }
    }

    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
    ) {
      if (filtered.isEmpty()) {
        item(key = "empty_milestones") {
          Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text(
              text = if (state.projects.isEmpty()) {
                "Create a project first to map out your key milestones."
              } else {
                "No milestones in this view yet. Tap + to add a checkpoint."
              },
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(20.dp),
            )
          }
        }
      } else {
        itemsIndexed(filtered, key = { _, item -> item.milestone.id }) { index, item ->
          TimelineMilestoneRow(
            item = item,
            isFirst = index == 0,
            isLast = index == filtered.lastIndex,
            onToggleReached = { onToggleMilestone(item.milestone) },
            onClick = { onEditMilestone(item.milestone) },
          )
        }
      }
    }
  }
}

@Composable
private fun TimelineMilestoneRow(
  item: MilestoneWithContext,
  isFirst: Boolean,
  isLast: Boolean,
  onToggleReached: () -> Unit,
  onClick: () -> Unit,
) {
  val isDark = isSystemInDarkTheme()
  val nodeColor: Color = if (item.milestone.isReached) {
    MaterialTheme.colorScheme.tertiary
  } else {
    item.project?.accentEnum?.color(isDark) ?: MaterialTheme.colorScheme.primary
  }
  val lineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .height(IntrinsicSize.Min),
    verticalAlignment = Alignment.Top,
  ) {
    Box(
      modifier = Modifier
        .width(26.dp)
        .fillMaxHeight(),
      contentAlignment = Alignment.TopCenter,
    ) {
      Canvas(modifier = Modifier.fillMaxSize()) {
        val centerX = size.width / 2f
        val dotY = 26.dp.toPx()
        val dotRadius = 6.dp.toPx()

        if (!isFirst) {
          drawLine(
            color = lineColor,
            start = Offset(centerX, 0f),
            end = Offset(centerX, dotY - dotRadius),
            strokeWidth = 2.dp.toPx(),
          )
        }
        if (!isLast) {
          drawLine(
            color = lineColor,
            start = Offset(centerX, dotY + dotRadius),
            end = Offset(centerX, size.height),
            strokeWidth = 2.dp.toPx(),
          )
        }
        drawCircle(
          color = nodeColor,
          radius = dotRadius,
          center = Offset(centerX, dotY),
        )
      }
    }

    Spacer(modifier = Modifier.width(8.dp))

    Box(
      modifier = Modifier
        .weight(1f)
        .padding(bottom = 12.dp),
    ) {
      MilestoneCard(
        item = item,
        showProjectTag = true,
        onToggleReached = onToggleReached,
        onClick = onClick,
      )
    }
  }
}
