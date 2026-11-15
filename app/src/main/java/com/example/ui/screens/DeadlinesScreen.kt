package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.ProjectTask
import com.example.data.model.TaskPriority
import com.example.ui.components.TaskRowCard
import com.example.ui.dashboard.DashboardUiState
import com.example.util.DeadlineBucket

@Composable
fun DeadlinesScreen(
  state: DashboardUiState.Success,
  onToggleTask: (ProjectTask) -> Unit,
  onEditTask: (ProjectTask) -> Unit,
  modifier: Modifier = Modifier,
) {
  var selectedPriority by remember { mutableStateOf<TaskPriority?>(null) }
  var selectedProjectId by remember { mutableStateOf<String?>(null) }
  var showCompleted by remember { mutableStateOf(true) }

  val filteredTasks = remember(state.tasks, selectedPriority, selectedProjectId, showCompleted) {
    state.tasks.filter { item ->
      val pMatch = selectedPriority == null || item.task.priorityEnum == selectedPriority
      val projMatch = selectedProjectId == null || item.task.projectId == selectedProjectId
      val compMatch = showCompleted || !item.task.isCompleted
      pMatch && projMatch && compMatch
    }
  }

  val grouped = remember(filteredTasks) {
    DeadlineBucket.entries
      .mapNotNull { bucket ->
        val items = filteredTasks.filter { it.bucket == bucket }
        if (items.isEmpty()) null else bucket to items
      }
  }

  Column(modifier = modifier.fillMaxSize()) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(
          text = "Tasks & Deadlines",
          style = MaterialTheme.typography.headlineMedium,
          color = MaterialTheme.colorScheme.onBackground,
        )
        FilterChip(
          selected = showCompleted,
          onClick = { showCompleted = !showCompleted },
          label = { Text(if (showCompleted) "Showing Done" else "Open Only") },
          modifier = Modifier.testTag("toggle_completed_filter"),
        )
      }

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        FilterChip(
          selected = selectedPriority == null,
          onClick = { selectedPriority = null },
          label = { Text("All Priorities") },
        )
        TaskPriority.entries.forEach { priority ->
          FilterChip(
            selected = selectedPriority == priority,
            onClick = { selectedPriority = if (selectedPriority == priority) null else priority },
            label = { Text(priority.label) },
          )
        }
      }

      if (state.projects.size > 1) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          FilterChip(
            selected = selectedProjectId == null,
            onClick = { selectedProjectId = null },
            label = { Text("All Projects") },
          )
          state.projects.forEach { p ->
            FilterChip(
              selected = selectedProjectId == p.project.id,
              onClick = { selectedProjectId = if (selectedProjectId == p.project.id) null else p.project.id },
              label = { Text(p.project.title) },
            )
          }
        }
      }
    }

    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      if (grouped.isEmpty()) {
        item(key = "empty_tasks") {
          Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text(
              text = if (state.projects.isEmpty()) {
                "Create a project first, then add tasks with deadlines here."
              } else {
                "No tasks match the current filters."
              },
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(20.dp),
            )
          }
        }
      } else {
        grouped.forEach { (bucket, bucketTasks) ->
          item(key = "header_${bucket.name}") {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Text(
                text = bucket.label,
                style = MaterialTheme.typography.titleMedium,
                color = if (bucket == DeadlineBucket.OVERDUE) {
                  MaterialTheme.colorScheme.error
                } else {
                  MaterialTheme.colorScheme.onBackground
                },
              )
              Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.small,
              ) {
                Text(
                  text = bucketTasks.size.toString(),
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                )
              }
            }
          }

          items(bucketTasks, key = { it.task.id }) { taskItem ->
            TaskRowCard(
              item = taskItem,
              showProjectTag = true,
              onToggleComplete = { onToggleTask(taskItem.task) },
              onClick = { onEditTask(taskItem.task) },
            )
          }
        }
      }
    }
  }
}
