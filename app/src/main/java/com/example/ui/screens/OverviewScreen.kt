package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Milestone
import com.example.data.model.ProjectStatus
import com.example.data.model.ProjectTask
import com.example.ui.components.CircularProgressRing
import com.example.ui.components.EmptyProjectsCard
import com.example.ui.components.MilestoneCard
import com.example.ui.components.ProjectCard
import com.example.ui.components.TaskRowCard
import com.example.ui.dashboard.DashboardUiState
import com.example.ui.dashboard.WorkspaceSummary
import com.example.util.DateFormatters
import com.example.util.DeadlineBucket
import java.util.Calendar

@Composable
fun OverviewScreen(
  state: DashboardUiState.Success,
  userDisplayName: String?,
  isWideLayout: Boolean,
  onOpenAccount: () -> Unit,
  onCreateProject: () -> Unit,
  onCreateTask: () -> Unit,
  onOpenProject: (String) -> Unit,
  onViewAllProjects: () -> Unit,
  onViewAllDeadlines: () -> Unit,
  onViewAllMilestones: () -> Unit,
  onToggleTask: (ProjectTask) -> Unit,
  onEditTask: (ProjectTask) -> Unit,
  onToggleMilestone: (Milestone) -> Unit,
  onEditMilestone: (Milestone) -> Unit,
  modifier: Modifier = Modifier,
) {
  val firstName = userDisplayName?.trim()?.substringBefore(" ")?.takeIf { it.isNotBlank() }
  val greeting = rememberGreeting(firstName)
  val initial = (firstName?.firstOrNull() ?: 'C').uppercaseChar().toString()

  val activeProjects = state.projects.filter { it.project.statusEnum == ProjectStatus.ACTIVE }
  val urgentTasks = state.tasks
    .filter { !it.task.isCompleted }
    .take(5)
  val upcomingMilestones = state.milestones
    .filter { !it.milestone.isReached }
    .take(3)

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .testTag("overview_list"),
    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
    verticalArrangement = Arrangement.spacedBy(18.dp),
  ) {
    item(key = "header") {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = DateFormatters.formatTodayHeader().uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = greeting,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
          )
        }
        Surface(
          onClick = onOpenAccount,
          shape = CircleShape,
          color = MaterialTheme.colorScheme.primaryContainer,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
          modifier = Modifier
            .size(48.dp)
            .testTag("account_avatar_button"),
        ) {
          Box(contentAlignment = Alignment.Center) {
            Text(
              text = initial,
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
          }
        }
      }
    }

    item(key = "pulse_hero") {
      StudioPulseCard(summary = state.summary)
    }

    if (state.projects.isEmpty()) {
      item(key = "empty_projects") {
        EmptyProjectsCard(onCreateProject = onCreateProject)
      }
    } else {
      item(key = "deadlines_header") {
        SectionHeader(
          title = "Upcoming Deadlines",
          actionLabel = if (urgentTasks.isNotEmpty()) "All (${state.summary.openTasksCount})" else "Add task",
          onAction = if (urgentTasks.isNotEmpty()) onViewAllDeadlines else onCreateTask,
        )
      }

      if (urgentTasks.isEmpty()) {
        item(key = "deadlines_empty") {
          QuietSectionCard(
            message = "All caught up on open tasks. Tap below to add your next action item.",
            buttonLabel = "Add Task",
            onClick = onCreateTask,
          )
        }
      } else {
        items(urgentTasks, key = { "dash_task_${it.task.id}" }) { taskItem ->
          TaskRowCard(
            item = taskItem,
            showProjectTag = true,
            onToggleComplete = { onToggleTask(taskItem.task) },
            onClick = { onEditTask(taskItem.task) },
          )
        }
      }

      item(key = "milestones_header") {
        SectionHeader(
          title = "Next Milestones",
          actionLabel = "Timeline",
          onAction = onViewAllMilestones,
        )
      }

      if (upcomingMilestones.isEmpty()) {
        item(key = "milestones_empty") {
          QuietSectionCard(
            message = "No upcoming milestones scheduled across your projects.",
            buttonLabel = "View Milestones",
            onClick = onViewAllMilestones,
          )
        }
      } else {
        items(upcomingMilestones, key = { "dash_ms_${it.milestone.id}" }) { msItem ->
          MilestoneCard(
            item = msItem,
            showProjectTag = true,
            onToggleReached = { onToggleMilestone(msItem.milestone) },
            onClick = { onEditMilestone(msItem.milestone) },
          )
        }
      }

      item(key = "projects_header") {
        SectionHeader(
          title = "Active Projects",
          actionLabel = "All (${state.projects.size})",
          onAction = onViewAllProjects,
        )
      }

      val displayProjects = activeProjects.ifEmpty { state.projects.take(3) }
      if (isWideLayout) {
        val rows = displayProjects.chunked(2)
        items(rows, key = { row -> "proj_row_${row.first().project.id}" }) { row ->
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            row.forEach { proj ->
              ProjectCard(
                item = proj,
                onClick = { onOpenProject(proj.project.id) },
                modifier = Modifier.weight(1f),
              )
            }
            if (row.size == 1) {
              Spacer(modifier = Modifier.weight(1f))
            }
          }
        }
      } else {
        items(displayProjects, key = { "dash_proj_${it.project.id}" }) { proj ->
          ProjectCard(
            item = proj,
            onClick = { onOpenProject(proj.project.id) },
          )
        }
      }
    }
  }
}

@Composable
private fun StudioPulseCard(summary: WorkspaceSummary) {
  Card(
    shape = MaterialTheme.shapes.large,
    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .background(
          Brush.linearGradient(
            colors = listOf(
              MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
              MaterialTheme.colorScheme.surface,
            )
          )
        )
        .padding(18.dp),
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Workspace Pulse",
              style = MaterialTheme.typography.titleLarge,
              color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "${summary.completedTasksCount} of ${summary.openTasksCount + summary.completedTasksCount} tasks completed",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }

          CircularProgressRing(
            progress = summary.overallProgress,
            ringColor = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            size = 66.dp,
            strokeWidth = 7.dp,
          )
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          MetricTile(
            value = summary.activeProjectsCount.toString(),
            label = "Active\nProjects",
            modifier = Modifier.weight(1f),
          )
          MetricTile(
            value = summary.openTasksCount.toString(),
            label = "Open\nTasks",
            modifier = Modifier.weight(1f),
          )
          MetricTile(
            value = summary.upcomingMilestonesCount.toString(),
            label = "Pending\nMilestones",
            modifier = Modifier.weight(1f),
          )
        }

        if (summary.overdueCount > 0 || summary.dueTodayCount > 0) {
          Surface(
            color = if (summary.overdueCount > 0) {
              MaterialTheme.colorScheme.errorContainer
            } else {
              MaterialTheme.colorScheme.secondaryContainer
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Icon(
                imageVector = Icons.Outlined.WarningAmber,
                contentDescription = null,
                tint = if (summary.overdueCount > 0) {
                  MaterialTheme.colorScheme.onErrorContainer
                } else {
                  MaterialTheme.colorScheme.onSecondaryContainer
                },
                modifier = Modifier.size(18.dp),
              )
              val alertText = buildString {
                if (summary.overdueCount > 0) {
                  append("${summary.overdueCount} overdue")
                }
                if (summary.dueTodayCount > 0) {
                  if (isNotEmpty()) append(" • ")
                  append("${summary.dueTodayCount} due today")
                }
              }
              Text(
                text = alertText,
                style = MaterialTheme.typography.labelLarge,
                color = if (summary.overdueCount > 0) {
                  MaterialTheme.colorScheme.onErrorContainer
                } else {
                  MaterialTheme.colorScheme.onSecondaryContainer
                },
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun MetricTile(
  value: String,
  label: String,
  modifier: Modifier = Modifier,
) {
  Surface(
    color = MaterialTheme.colorScheme.surfaceContainerLow,
    shape = RoundedCornerShape(14.dp),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
    modifier = modifier,
  ) {
    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
      Text(
        text = value,
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
private fun SectionHeader(
  title: String,
  actionLabel: String,
  onAction: () -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Text(
      text = title,
      style = MaterialTheme.typography.titleLarge,
      color = MaterialTheme.colorScheme.onBackground,
    )
    TextButton(onClick = onAction) {
      Text(text = actionLabel)
      Spacer(modifier = Modifier.width(4.dp))
      Icon(
        imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
        contentDescription = null,
        modifier = Modifier.size(16.dp),
      )
    }
  }
}

@Composable
private fun QuietSectionCard(
  message: String,
  buttonLabel: String,
  onClick: () -> Unit,
) {
  Surface(
    shape = MaterialTheme.shapes.medium,
    color = MaterialTheme.colorScheme.surfaceContainerLow,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier.padding(14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Text(
        text = message,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.weight(1f),
      )
      Spacer(modifier = Modifier.width(12.dp))
      TextButton(onClick = onClick) {
        Icon(imageVector = Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(buttonLabel)
      }
    }
  }
}

private fun rememberGreeting(firstName: String?): String {
  val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
  val timeWord = when (hour) {
    in 5..11 -> "Good morning"
    in 12..16 -> "Good afternoon"
    else -> "Good evening"
  }
  return if (firstName != null) "$timeWord, $firstName" else timeWord
}
