package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Milestone
import com.example.data.model.Project
import com.example.data.model.ProjectAccent
import com.example.data.model.ProjectStatus
import com.example.data.model.ProjectTask
import com.example.data.model.TaskPriority
import com.example.ui.dashboard.WorkspaceSummary
import com.example.ui.theme.color
import com.example.util.DateFormatters

private val DefaultCategories = listOf("Personal", "Work", "Creative", "Home", "Learning", "Health")

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProjectEditorSheet(
  existingProject: Project?,
  isSaving: Boolean,
  onDismiss: () -> Unit,
  onSave: (
    title: String,
    description: String,
    category: String,
    accent: ProjectAccent,
    status: ProjectStatus,
    targetDateMs: Long?,
  ) -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val isDark = isSystemInDarkTheme()

  var title by remember(existingProject) { mutableStateOf(existingProject?.title.orEmpty()) }
  var description by remember(existingProject) { mutableStateOf(existingProject?.description.orEmpty()) }
  var category by remember(existingProject) { mutableStateOf(existingProject?.category ?: "Personal") }
  var accent by remember(existingProject) { mutableStateOf(existingProject?.accentEnum ?: ProjectAccent.COBALT) }
  var status by remember(existingProject) { mutableStateOf(existingProject?.statusEnum ?: ProjectStatus.ACTIVE) }
  var targetDateMs by remember(existingProject) { mutableStateOf(existingProject?.targetDateMs) }
  var showDatePicker by remember { mutableStateOf(false) }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp)
        .padding(bottom = 28.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Text(
        text = if (existingProject == null) "New Project" else "Edit Project",
        style = MaterialTheme.typography.headlineSmall,
      )

      OutlinedTextField(
        value = title,
        onValueChange = { if (it.length <= 100) title = it },
        label = { Text("Project title") },
        placeholder = { Text("e.g., Apartment Renovation") },
        singleLine = true,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("project_title_input"),
      )

      OutlinedTextField(
        value = description,
        onValueChange = { if (it.length <= 500) description = it },
        label = { Text("Goal & notes (optional)") },
        minLines = 2,
        maxLines = 4,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("project_description_input"),
      )

      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Category", style = MaterialTheme.typography.labelLarge)
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          DefaultCategories.forEach { cat ->
            FilterChip(
              selected = category == cat,
              onClick = { category = cat },
              label = { Text(cat) },
            )
          }
        }
      }

      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Studio Color", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
          ProjectAccent.entries.forEach { option ->
            val swatch = option.color(isDark)
            val selected = accent == option
            Box(
              modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(swatch)
                .border(
                  width = if (selected) 3.dp else 0.dp,
                  color = if (selected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                  shape = CircleShape,
                )
                .clickable { accent = option }
                .testTag("accent_${option.key}"),
              contentAlignment = Alignment.Center,
            ) {
              if (selected) {
                Icon(
                  imageVector = Icons.Filled.Check,
                  contentDescription = option.label,
                  tint = Color.White,
                  modifier = Modifier.size(18.dp),
                )
              }
            }
          }
        }
      }

      if (existingProject != null) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text("Status", style = MaterialTheme.typography.labelLarge)
          FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ProjectStatus.entries.forEach { st ->
              FilterChip(
                selected = status == st,
                onClick = { status = st },
                label = { Text(st.label) },
              )
            }
          }
        }
      }

      DateSelectorRow(
        label = "Target deadline",
        dateMs = targetDateMs,
        onPickDate = { showDatePicker = true },
        onClearDate = { targetDateMs = null },
      )

      Spacer(modifier = Modifier.height(4.dp))

      Button(
        onClick = { onSave(title, description, category, accent, status, targetDateMs) },
        enabled = !isSaving && title.isNotBlank(),
        modifier = Modifier
          .fillMaxWidth()
          .height(50.dp)
          .testTag("save_project_button"),
      ) {
        Text(if (existingProject == null) "Create Project" else "Save Changes")
      }
    }
  }

  if (showDatePicker) {
    CadenceDatePickerDialog(
      initialLocalMs = targetDateMs,
      onDateSelected = { selected ->
        targetDateMs = selected
        showDatePicker = false
      },
      onDismiss = { showDatePicker = false },
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TaskEditorSheet(
  existingTask: ProjectTask?,
  projects: List<Project>,
  milestones: List<Milestone>,
  initialProjectId: String?,
  isSaving: Boolean,
  onDismiss: () -> Unit,
  onDelete: (() -> Unit)?,
  onSave: (
    projectId: String,
    milestoneId: String?,
    title: String,
    notes: String,
    priority: TaskPriority,
    dueDateMs: Long?,
  ) -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var selectedProjectId by remember(existingTask, initialProjectId) {
    mutableStateOf(
      existingTask?.projectId
        ?: initialProjectId
        ?: projects.firstOrNull { it.statusEnum == ProjectStatus.ACTIVE }?.id
        ?: projects.firstOrNull()?.id
        ?: ""
    )
  }
  var selectedMilestoneId by remember(existingTask) { mutableStateOf(existingTask?.milestoneId) }
  var title by remember(existingTask) { mutableStateOf(existingTask?.title.orEmpty()) }
  var notes by remember(existingTask) { mutableStateOf(existingTask?.notes.orEmpty()) }
  var priority by remember(existingTask) { mutableStateOf(existingTask?.priorityEnum ?: TaskPriority.MEDIUM) }
  var dueDateMs by remember(existingTask) { mutableStateOf(existingTask?.dueDateMs) }
  var showDatePicker by remember { mutableStateOf(false) }

  val projectMilestones = remember(selectedProjectId, milestones) {
    milestones.filter { it.projectId == selectedProjectId }
  }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp)
        .padding(bottom = 28.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(
          text = if (existingTask == null) "New Task" else "Edit Task",
          style = MaterialTheme.typography.headlineSmall,
        )
        if (onDelete != null) {
          IconButton(
            onClick = onDelete,
            modifier = Modifier.testTag("delete_task_button"),
          ) {
            Icon(
              imageVector = Icons.Outlined.Delete,
              contentDescription = "Delete task",
              tint = MaterialTheme.colorScheme.error,
            )
          }
        }
      }

      if (existingTask == null && projects.isNotEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text("Project", style = MaterialTheme.typography.labelLarge)
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            projects.forEach { proj ->
              FilterChip(
                selected = selectedProjectId == proj.id,
                onClick = {
                  selectedProjectId = proj.id
                  if (milestones.none { it.id == selectedMilestoneId && it.projectId == proj.id }) {
                    selectedMilestoneId = null
                  }
                },
                label = { Text(proj.title) },
              )
            }
          }
        }
      }

      OutlinedTextField(
        value = title,
        onValueChange = { if (it.length <= 140) title = it },
        label = { Text("Task title") },
        placeholder = { Text("e.g., Finalize lighting quote") },
        singleLine = true,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("task_title_input"),
      )

      OutlinedTextField(
        value = notes,
        onValueChange = { if (it.length <= 500) notes = it },
        label = { Text("Notes (optional)") },
        minLines = 2,
        maxLines = 4,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("task_notes_input"),
      )

      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Priority", style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          TaskPriority.entries.forEach { p ->
            FilterChip(
              selected = priority == p,
              onClick = { priority = p },
              label = { Text(p.label) },
            )
          }
        }
      }

      if (projectMilestones.isNotEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text("Link to Milestone (optional)", style = MaterialTheme.typography.labelLarge)
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            FilterChip(
              selected = selectedMilestoneId == null,
              onClick = { selectedMilestoneId = null },
              label = { Text("None") },
            )
            projectMilestones.forEach { ms ->
              FilterChip(
                selected = selectedMilestoneId == ms.id,
                onClick = { selectedMilestoneId = ms.id },
                label = { Text(ms.title) },
              )
            }
          }
        }
      }

      DateSelectorRow(
        label = "Deadline",
        dateMs = dueDateMs,
        onPickDate = { showDatePicker = true },
        onClearDate = { dueDateMs = null },
      )

      Button(
        onClick = {
          onSave(selectedProjectId, selectedMilestoneId, title, notes, priority, dueDateMs)
        },
        enabled = !isSaving && title.isNotBlank() && selectedProjectId.isNotBlank(),
        modifier = Modifier
          .fillMaxWidth()
          .height(50.dp)
          .testTag("save_task_button"),
      ) {
        Text(if (existingTask == null) "Add Task" else "Save Task")
      }
    }
  }

  if (showDatePicker) {
    CadenceDatePickerDialog(
      initialLocalMs = dueDateMs,
      onDateSelected = { selected ->
        dueDateMs = selected
        showDatePicker = false
      },
      onDismiss = { showDatePicker = false },
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MilestoneEditorSheet(
  existingMilestone: Milestone?,
  projects: List<Project>,
  initialProjectId: String?,
  isSaving: Boolean,
  onDismiss: () -> Unit,
  onDelete: (() -> Unit)?,
  onSave: (
    projectId: String,
    title: String,
    description: String,
    targetDateMs: Long,
  ) -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var selectedProjectId by remember(existingMilestone, initialProjectId) {
    mutableStateOf(
      existingMilestone?.projectId
        ?: initialProjectId
        ?: projects.firstOrNull { it.statusEnum == ProjectStatus.ACTIVE }?.id
        ?: projects.firstOrNull()?.id
        ?: ""
    )
  }
  var title by remember(existingMilestone) { mutableStateOf(existingMilestone?.title.orEmpty()) }
  var description by remember(existingMilestone) { mutableStateOf(existingMilestone?.description.orEmpty()) }
  var targetDateMs by remember(existingMilestone) {
    mutableStateOf(
      existingMilestone?.targetDateMs ?: (System.currentTimeMillis() + 7 * 86_400_000L)
    )
  }
  var showDatePicker by remember { mutableStateOf(false) }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp)
        .padding(bottom = 28.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(
          text = if (existingMilestone == null) "New Milestone" else "Edit Milestone",
          style = MaterialTheme.typography.headlineSmall,
        )
        if (onDelete != null) {
          IconButton(
            onClick = onDelete,
            modifier = Modifier.testTag("delete_milestone_button"),
          ) {
            Icon(
              imageVector = Icons.Outlined.Delete,
              contentDescription = "Delete milestone",
              tint = MaterialTheme.colorScheme.error,
            )
          }
        }
      }

      if (existingMilestone == null && projects.isNotEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text("Project", style = MaterialTheme.typography.labelLarge)
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            projects.forEach { proj ->
              FilterChip(
                selected = selectedProjectId == proj.id,
                onClick = { selectedProjectId = proj.id },
                label = { Text(proj.title) },
              )
            }
          }
        }
      }

      OutlinedTextField(
        value = title,
        onValueChange = { if (it.length <= 120) title = it },
        label = { Text("Milestone title") },
        placeholder = { Text("e.g., First Prototype Ready") },
        singleLine = true,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("milestone_title_input"),
      )

      OutlinedTextField(
        value = description,
        onValueChange = { if (it.length <= 400) description = it },
        label = { Text("Deliverable details (optional)") },
        minLines = 2,
        maxLines = 4,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("milestone_description_input"),
      )

      DateSelectorRow(
        label = "Target date",
        dateMs = targetDateMs,
        onPickDate = { showDatePicker = true },
        onClearDate = null,
      )

      Button(
        onClick = { onSave(selectedProjectId, title, description, targetDateMs) },
        enabled = !isSaving && title.isNotBlank() && selectedProjectId.isNotBlank(),
        modifier = Modifier
          .fillMaxWidth()
          .height(50.dp)
          .testTag("save_milestone_button"),
      ) {
        Text(if (existingMilestone == null) "Add Milestone" else "Save Milestone")
      }
    }
  }

  if (showDatePicker) {
    CadenceDatePickerDialog(
      initialLocalMs = targetDateMs,
      onDateSelected = { selected ->
        targetDateMs = selected
        showDatePicker = false
      },
      onDismiss = { showDatePicker = false },
    )
  }
}

@Composable
private fun DateSelectorRow(
  label: String,
  dateMs: Long?,
  onPickDate: () -> Unit,
  onClearDate: (() -> Unit)?,
) {
  Surface(
    onClick = onPickDate,
    shape = MaterialTheme.shapes.small,
    color = MaterialTheme.colorScheme.surfaceContainerLow,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("date_selector_${label.lowercase().replace(" ", "_")}"),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Icon(
          imageVector = Icons.Outlined.CalendarToday,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(18.dp),
        )
        Column {
          Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          Text(
            text = dateMs?.let { DateFormatters.formatFullDate(it) } ?: "No deadline set",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface,
          )
        }
      }
      if (dateMs != null && onClearDate != null) {
        IconButton(onClick = onClearDate) {
          Icon(
            imageVector = Icons.Outlined.Close,
            contentDescription = "Clear date",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CadenceDatePickerDialog(
  initialLocalMs: Long?,
  onDateSelected: (Long) -> Unit,
  onDismiss: () -> Unit,
) {
  val initialUtc = initialLocalMs?.let { DateFormatters.localMillisToUtcPickerMillis(it) }
  val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialUtc)

  DatePickerDialog(
    onDismissRequest = onDismiss,
    confirmButton = {
      TextButton(
        onClick = {
          val selectedUtc = pickerState.selectedDateMillis
          if (selectedUtc != null) {
            onDateSelected(DateFormatters.utcPickerMillisToLocalNoon(selectedUtc))
          } else {
            onDismiss()
          }
        }
      ) {
        Text("Select")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    },
  ) {
    DatePicker(state = pickerState)
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddMenuSheet(
  hasProjects: Boolean,
  onDismiss: () -> Unit,
  onNewProject: () -> Unit,
  onNewTask: () -> Unit,
  onNewMilestone: () -> Unit,
) {
  ModalBottomSheet(onDismissRequest = onDismiss) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp)
        .padding(bottom = 28.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Text("Quick Create", style = MaterialTheme.typography.headlineSmall)
      QuickCreateOption(
        icon = Icons.Outlined.Folder,
        title = "New Project",
        subtitle = "Start a dedicated workspace with goals and color tag",
        enabled = true,
        onClick = onNewProject,
        testTag = "quick_add_project",
      )
      QuickCreateOption(
        icon = Icons.Outlined.TaskAlt,
        title = "New Task & Deadline",
        subtitle = if (hasProjects) "Add an actionable task with priority and due date" else "Create a project first",
        enabled = hasProjects,
        onClick = onNewTask,
        testTag = "quick_add_task",
      )
      QuickCreateOption(
        icon = Icons.Outlined.Flag,
        title = "New Milestone",
        subtitle = if (hasProjects) "Set a key checkpoint date to track progress" else "Create a project first",
        enabled = hasProjects,
        onClick = onNewMilestone,
        testTag = "quick_add_milestone",
      )
    }
  }
}

@Composable
private fun QuickCreateOption(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  title: String,
  subtitle: String,
  enabled: Boolean,
  onClick: () -> Unit,
  testTag: String,
) {
  Surface(
    onClick = onClick,
    enabled = enabled,
    shape = MaterialTheme.shapes.medium,
    color = MaterialTheme.colorScheme.surfaceContainerLow,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
    modifier = Modifier
      .fillMaxWidth()
      .testTag(testTag),
  ) {
    Row(
      modifier = Modifier.padding(14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Box(
        modifier = Modifier
          .size(42.dp)
          .clip(CircleShape)
          .background(
            if (enabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
          ),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      Column {
        Text(
          text = title,
          style = MaterialTheme.typography.titleMedium,
          color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

@Composable
fun AccountDialog(
  displayName: String?,
  email: String?,
  summary: WorkspaceSummary?,
  onDismiss: () -> Unit,
  onSignOut: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(displayName?.takeIf { it.isNotBlank() } ?: "Cadence Account")
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (!email.isNullOrBlank()) {
          Text(
            text = email,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        if (summary != null) {
          Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(
              modifier = Modifier.padding(12.dp),
              verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              Text(
                text = "Workspace Snapshot",
                style = MaterialTheme.typography.labelLarge,
              )
              Text(
                text = "${summary.activeProjectsCount} active projects • ${summary.openTasksCount} open tasks • ${summary.reachedMilestonesCount} reached milestones",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }
    },
    confirmButton = {
      OutlinedButton(
        onClick = onSignOut,
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
        modifier = Modifier.testTag("sign_out_button"),
      ) {
        Icon(imageVector = Icons.AutoMirrored.Outlined.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Sign out")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Close")
      }
    },
  )
}

@Composable
fun ConfirmDeleteDialog(
  title: String,
  message: String,
  onConfirm: () -> Unit,
  onDismiss: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(title) },
    text = { Text(message) },
    confirmButton = {
      Button(
        onClick = onConfirm,
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
        modifier = Modifier.testTag("confirm_delete_button"),
      ) {
        Text("Delete")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    },
  )
}
