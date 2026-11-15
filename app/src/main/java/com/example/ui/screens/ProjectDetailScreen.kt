package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.Milestone
import com.example.data.model.Project
import com.example.data.model.ProjectStatus
import com.example.data.model.ProjectTask
import com.example.ui.components.CircularProgressRing
import com.example.ui.components.DeadlineChip
import com.example.ui.components.MilestoneCard
import com.example.ui.components.TaskRowCard
import com.example.ui.dashboard.DashboardUiState
import com.example.ui.theme.color
import com.example.ui.theme.containerColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDetailScreen(
  projectId: String,
  state: DashboardUiState.Success,
  onBack: () -> Unit,
  onEditProject: (Project) -> Unit,
  onChangeStatus: (ProjectStatus) -> Unit,
  onRequestDeleteProject: (Project) -> Unit,
  onAddMilestone: () -> Unit,
  onToggleMilestone: (Milestone) -> Unit,
  onEditMilestone: (Milestone) -> Unit,
  onAddTask: () -> Unit,
  onToggleTask: (ProjectTask) -> Unit,
  onEditTask: (ProjectTask) -> Unit,
  modifier: Modifier = Modifier,
) {
  val projectStats = state.projects.find { it.project.id == projectId }
  val projectMilestones = remember(state.milestones, projectId) {
    state.milestones.filter { it.milestone.projectId == projectId }
  }
  val projectTasks = remember(state.tasks, projectId) {
    state.tasks.filter { it.task.projectId == projectId }
  }

  var showCompletedTasks by remember { mutableStateOf(true) }
  val visibleTasks = remember(projectTasks, showCompletedTasks) {
    if (showCompletedTasks) projectTasks else projectTasks.filter { !it.task.isCompleted }
  }

  Scaffold(
    containerColor = MaterialTheme.colorScheme.background,
    topBar = {
      TopAppBar(
        title = { Text(projectStats?.project?.title ?: "Project") },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.background,
        ),
        navigationIcon = {
          IconButton(
            onClick = onBack,
            modifier = Modifier.testTag("project_detail_back_button"),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Back",
            )
          }
        },
        actions = {
          if (projectStats != null) {
            if (projectStats.project.statusEnum != ProjectStatus.ARCHIVED) {
              IconButton(
                onClick = { onEditProject(projectStats.project) },
                modifier = Modifier.testTag("edit_project_button"),
              ) {
                Icon(imageVector = Icons.Outlined.Edit, contentDescription = "Edit project")
              }
            }
            IconButton(
              onClick = { onRequestDeleteProject(projectStats.project) },
              modifier = Modifier.testTag("delete_project_button"),
            ) {
              Icon(
                imageVector = Icons.Outlined.Delete,
                contentDescription = "Delete project",
                tint = MaterialTheme.colorScheme.error,
              )
            }
          }
        },
      )
    },
    modifier = modifier,
  ) { innerPadding ->
    if (projectStats == null) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding)
          .padding(24.dp),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = "This project is no longer available.",
          style = MaterialTheme.typography.bodyLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      return@Scaffold
    }

    val project = projectStats.project
    val isDark = isSystemInDarkTheme()
    val accentColor = project.accentEnum.color(isDark)
    val accentBg = project.accentEnum.containerColor(isDark)
    val isArchived = project.statusEnum == ProjectStatus.ARCHIVED

    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding),
      contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 48.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      item(key = "project_summary_card") {
        Card(
          shape = MaterialTheme.shapes.large,
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
          elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
          Column(modifier = Modifier.padding(18.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  Surface(color = accentBg, shape = RoundedCornerShape(6.dp)) {
                    Text(
                      text = project.category,
                      style = MaterialTheme.typography.labelSmall,
                      color = accentColor,
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                  }
                  if (project.targetDateMs != null) {
                    DeadlineChip(
                      dueDateMs = project.targetDateMs,
                      isCompleted = project.statusEnum == ProjectStatus.COMPLETED,
                    )
                  }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                  text = project.title,
                  style = MaterialTheme.typography.headlineMedium,
                  color = MaterialTheme.colorScheme.onSurface,
                )
                if (project.description.isNotBlank()) {
                  Spacer(modifier = Modifier.height(6.dp))
                  Text(
                    text = project.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }

              Spacer(modifier = Modifier.width(14.dp))

              CircularProgressRing(
                progress = projectStats.progressFraction,
                ringColor = accentColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                size = 68.dp,
                strokeWidth = 7.dp,
              )
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (isArchived) {
              Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp),
              ) {
                Text(
                  text = "Archived project (read-only)",
                  style = MaterialTheme.typography.labelMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                )
              }
            } else {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                ProjectStatus.entries.forEach { st ->
                  FilterChip(
                    selected = project.statusEnum == st,
                    onClick = {
                      if (project.statusEnum != st) onChangeStatus(st)
                    },
                    label = { Text(st.label) },
                  )
                }
              }
            }
          }
        }
      }

      item(key = "milestones_header") {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text(
            text = "Milestones (${projectStats.reachedMilestones}/${projectStats.totalMilestones})",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
          )
          TextButton(
            onClick = onAddMilestone,
            modifier = Modifier.testTag("detail_add_milestone_button"),
          ) {
            Icon(imageVector = Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Add Milestone")
          }
        }
      }

      if (projectMilestones.isEmpty()) {
        item(key = "milestones_empty") {
          Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text(
              text = "No milestones yet. Add key checkpoints with target dates.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(16.dp),
            )
          }
        }
      } else {
        items(projectMilestones, key = { "detail_ms_${it.milestone.id}" }) { msItem ->
          MilestoneCard(
            item = msItem,
            showProjectTag = false,
            onToggleReached = { onToggleMilestone(msItem.milestone) },
            onClick = { onEditMilestone(msItem.milestone) },
          )
        }
      }

      item(key = "tasks_header") {
        Spacer(modifier = Modifier.height(6.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text(
            text = "Tasks (${projectStats.completedTasks}/${projectStats.totalTasks})",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
          )
          Row(verticalAlignment = Alignment.CenterVertically) {
            FilterChip(
              selected = showCompletedTasks,
              onClick = { showCompletedTasks = !showCompletedTasks },
              label = { Text(if (showCompletedTasks) "All" else "Open") },
            )
            Spacer(modifier = Modifier.width(4.dp))
            TextButton(
              onClick = onAddTask,
              modifier = Modifier.testTag("detail_add_task_button"),
            ) {
              Icon(imageVector = Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Add Task")
            }
          }
        }
      }

      if (visibleTasks.isEmpty()) {
        item(key = "tasks_empty") {
          Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text(
              text = if (projectTasks.isEmpty()) {
                "No tasks yet. Break this project down into concrete next steps."
              } else {
                "All tasks in this project are completed!"
              },
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(16.dp),
            )
          }
        }
      } else {
        items(visibleTasks, key = { "detail_task_${it.task.id}" }) { taskItem ->
          TaskRowCard(
            item = taskItem,
            showProjectTag = false,
            onToggleComplete = { onToggleTask(taskItem.task) },
            onClick = { onEditTask(taskItem.task) },
          )
        }
      }
    }
  }
}
