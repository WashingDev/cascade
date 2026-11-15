package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.ProjectStatus
import com.example.data.model.TaskPriority
import com.example.ui.dashboard.MilestoneWithContext
import com.example.ui.dashboard.ProjectWithStats
import com.example.ui.dashboard.TaskWithContext
import com.example.ui.theme.HighAmber
import com.example.ui.theme.LowSage
import com.example.ui.theme.MediumCobalt
import com.example.ui.theme.UrgentRed
import com.example.ui.theme.color
import com.example.ui.theme.containerColor
import com.example.util.DateFormatters
import com.example.util.DeadlineBucket
import kotlin.math.roundToInt

@Composable
fun CircularProgressRing(
  progress: Float,
  ringColor: Color,
  trackColor: Color,
  size: Dp = 64.dp,
  strokeWidth: Dp = 7.dp,
  labelColor: Color = MaterialTheme.colorScheme.onSurface,
  modifier: Modifier = Modifier,
) {
  val animatedProgress by animateFloatAsState(
    targetValue = progress.coerceIn(0f, 1f),
    animationSpec = spring(stiffness = 280f),
    label = "progress_ring",
  )

  Box(
    modifier = modifier.size(size),
    contentAlignment = Alignment.Center,
  ) {
    Canvas(modifier = Modifier.size(size)) {
      val strokePx = strokeWidth.toPx()
      drawArc(
        color = trackColor,
        startAngle = 0f,
        sweepAngle = 360f,
        useCenter = false,
        style = Stroke(width = strokePx, cap = StrokeCap.Round),
      )
      if (animatedProgress > 0f) {
        drawArc(
          color = ringColor,
          startAngle = -90f,
          sweepAngle = 360f * animatedProgress,
          useCenter = false,
          style = Stroke(width = strokePx, cap = StrokeCap.Round),
        )
      }
    }
    Text(
      text = "${(animatedProgress * 100).roundToInt()}%",
      style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
      color = labelColor,
    )
  }
}

@Composable
fun DeadlineChip(
  dueDateMs: Long,
  isCompleted: Boolean,
  modifier: Modifier = Modifier,
) {
  val delta = DateFormatters.daysDeltaFromToday(dueDateMs)
  val isOverdue = !isCompleted && delta < 0
  val isToday = !isCompleted && delta == 0

  val bgColor = when {
    isCompleted -> MaterialTheme.colorScheme.surfaceVariant
    isOverdue -> MaterialTheme.colorScheme.errorContainer
    isToday -> MaterialTheme.colorScheme.secondaryContainer
    else -> MaterialTheme.colorScheme.surfaceVariant
  }
  val contentColor = when {
    isCompleted -> MaterialTheme.colorScheme.onSurfaceVariant
    isOverdue -> MaterialTheme.colorScheme.onErrorContainer
    isToday -> MaterialTheme.colorScheme.onSecondaryContainer
    else -> MaterialTheme.colorScheme.onSurfaceVariant
  }

  Surface(
    color = bgColor,
    shape = RoundedCornerShape(8.dp),
    modifier = modifier,
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Icon(
        imageVector = if (isOverdue) Icons.Outlined.Schedule else Icons.Outlined.CalendarToday,
        contentDescription = null,
        tint = contentColor,
        modifier = Modifier.size(12.dp),
      )
      Text(
        text = if (isCompleted) DateFormatters.formatShortDate(dueDateMs) else DateFormatters.formatRelativeDeadline(dueDateMs),
        style = MaterialTheme.typography.labelSmall,
        color = contentColor,
      )
    }
  }
}

@Composable
fun PriorityBadge(priority: TaskPriority) {
  val color = when (priority) {
    TaskPriority.URGENT -> UrgentRed
    TaskPriority.HIGH -> HighAmber
    TaskPriority.MEDIUM -> MediumCobalt
    TaskPriority.LOW -> LowSage
  }
  Surface(
    color = color.copy(alpha = 0.13f),
    shape = RoundedCornerShape(6.dp),
  ) {
    Text(
      text = priority.label,
      style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
      color = color,
      modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
    )
  }
}

@Composable
fun ProjectCard(
  item: ProjectWithStats,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val isDark = isSystemInDarkTheme()
  val accentColor = item.project.accentEnum.color(isDark)
  val accentBg = item.project.accentEnum.containerColor(isDark)

  Card(
    onClick = onClick,
    shape = MaterialTheme.shapes.medium,
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    modifier = modifier
      .fillMaxWidth()
      .testTag("project_card_${item.project.id}"),
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Box(
            modifier = Modifier
              .size(10.dp)
              .clip(CircleShape)
              .background(accentColor)
          )
          Surface(
            color = accentBg,
            shape = RoundedCornerShape(6.dp),
          ) {
            Text(
              text = item.project.category,
              style = MaterialTheme.typography.labelSmall,
              color = accentColor,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            )
          }
          if (item.project.statusEnum != ProjectStatus.ACTIVE) {
            Surface(
              color = MaterialTheme.colorScheme.surfaceVariant,
              shape = RoundedCornerShape(6.dp),
            ) {
              Text(
                text = item.project.statusEnum.label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
              )
            }
          }
        }

        if (item.overdueTasks > 0) {
          Surface(
            color = MaterialTheme.colorScheme.errorContainer,
            shape = RoundedCornerShape(6.dp),
          ) {
            Text(
              text = "${item.overdueTasks} overdue",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onErrorContainer,
              modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
            )
          }
        } else if (item.nextDeadlineMs != null) {
          DeadlineChip(
            dueDateMs = item.nextDeadlineMs,
            isCompleted = item.project.statusEnum == ProjectStatus.COMPLETED,
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      Text(
        text = item.project.title,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )

      if (item.project.description.isNotBlank()) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = item.project.description,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
      }

      Spacer(modifier = Modifier.height(14.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(
          text = if (item.totalTasks == 0) "No tasks yet" else "${item.completedTasks}/${item.totalTasks} tasks",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
          text = "${(item.progressFraction * 100).roundToInt()}%",
          style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
          color = accentColor,
        )
      }

      Spacer(modifier = Modifier.height(6.dp))

      LinearProgressIndicator(
        progress = { item.progressFraction },
        color = accentColor,
        trackColor = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
          .fillMaxWidth()
          .height(6.dp)
          .clip(RoundedCornerShape(3.dp)),
      )

      if (item.nextMilestone != null || item.totalMilestones > 0) {
        Spacer(modifier = Modifier.height(12.dp))
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Icon(
            imageVector = Icons.Outlined.Flag,
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier.size(15.dp),
          )
          Text(
            text = if (item.nextMilestone != null) {
              "Next: ${item.nextMilestone.title} • ${DateFormatters.formatShortDate(item.nextMilestone.targetDateMs)}"
            } else {
              "${item.reachedMilestones}/${item.totalMilestones} milestones reached"
            },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
    }
  }
}

@Composable
fun TaskRowCard(
  item: TaskWithContext,
  showProjectTag: Boolean,
  onToggleComplete: () -> Unit,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val isDark = isSystemInDarkTheme()
  val isCompleted = item.task.isCompleted
  val projectColor = item.project?.accentEnum?.color(isDark) ?: MaterialTheme.colorScheme.primary

  val checkBg by animateColorAsState(
    targetValue = if (isCompleted) MaterialTheme.colorScheme.tertiary else Color.Transparent,
    label = "task_check_bg",
  )

  Surface(
    onClick = onClick,
    shape = MaterialTheme.shapes.medium,
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(
      width = 1.dp,
      color = if (item.bucket == DeadlineBucket.OVERDUE) {
        MaterialTheme.colorScheme.error.copy(alpha = 0.45f)
      } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
      },
    ),
    modifier = modifier
      .fillMaxWidth()
      .testTag("task_row_${item.task.id}"),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(
        modifier = Modifier
          .minimumInteractiveComponentSize()
          .clip(CircleShape)
          .clickable(onClick = onToggleComplete)
          .testTag("task_toggle_${item.task.id}"),
        contentAlignment = Alignment.Center,
      ) {
        Surface(
          shape = CircleShape,
          color = checkBg,
          border = BorderStroke(
            width = 2.dp,
            color = if (isCompleted) MaterialTheme.colorScheme.tertiary else projectColor,
          ),
          modifier = Modifier.size(24.dp),
        ) {
          if (isCompleted) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = "Mark task incomplete",
                tint = MaterialTheme.colorScheme.onTertiary,
                modifier = Modifier.size(14.dp),
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.width(6.dp))

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = item.task.title,
          style = MaterialTheme.typography.bodyLarge.copy(
            fontWeight = FontWeight.Medium,
            textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None,
          ),
          color = if (isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )

        Spacer(modifier = Modifier.height(6.dp))

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          if (showProjectTag && item.project != null) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              Box(
                modifier = Modifier
                  .size(7.dp)
                  .clip(CircleShape)
                  .background(projectColor)
              )
              Text(
                text = item.project.title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }

          PriorityBadge(priority = item.task.priorityEnum)

          if (item.task.dueDateMs != null) {
            DeadlineChip(
              dueDateMs = item.task.dueDateMs,
              isCompleted = isCompleted,
            )
          }

          if (item.milestone != null) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
              Icon(
                imageVector = Icons.Outlined.Flag,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(12.dp),
              )
              Text(
                text = item.milestone.title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
fun MilestoneCard(
  item: MilestoneWithContext,
  showProjectTag: Boolean,
  onToggleReached: () -> Unit,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val isDark = isSystemInDarkTheme()
  val projectColor = item.project?.accentEnum?.color(isDark) ?: MaterialTheme.colorScheme.primary
  val reached = item.milestone.isReached

  Surface(
    onClick = onClick,
    shape = MaterialTheme.shapes.medium,
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(
      width = 1.dp,
      color = if (reached) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.55f),
    ),
    modifier = modifier
      .fillMaxWidth()
      .testTag("milestone_card_${item.milestone.id}"),
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(
          modifier = Modifier.weight(1f),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Box(
            modifier = Modifier
              .minimumInteractiveComponentSize()
              .clip(CircleShape)
              .clickable(onClick = onToggleReached)
              .testTag("milestone_toggle_${item.milestone.id}"),
            contentAlignment = Alignment.Center,
          ) {
            Box(
              modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(
                  if (reached) MaterialTheme.colorScheme.tertiaryContainer else projectColor.copy(alpha = 0.14f)
                ),
              contentAlignment = Alignment.Center,
            ) {
              Icon(
                imageVector = if (reached) Icons.Outlined.CheckCircle else Icons.Filled.Flag,
                contentDescription = if (reached) "Milestone reached" else "Mark milestone reached",
                tint = if (reached) MaterialTheme.colorScheme.tertiary else projectColor,
                modifier = Modifier.size(16.dp),
              )
            }
          }

          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = item.milestone.title,
              style = MaterialTheme.typography.titleMedium,
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = 2,
              overflow = TextOverflow.Ellipsis,
            )
            if (showProjectTag && item.project != null) {
              Text(
                text = item.project.title,
                style = MaterialTheme.typography.labelSmall,
                color = projectColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }
        }

        Spacer(modifier = Modifier.width(8.dp))

        DeadlineChip(
          dueDateMs = item.milestone.targetDateMs,
          isCompleted = reached,
        )
      }

      if (item.milestone.description.isNotBlank()) {
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = item.milestone.description,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
      }

      if (item.linkedTaskCount > 0) {
        Spacer(modifier = Modifier.height(10.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text(
            text = "${item.completedLinkedTaskCount}/${item.linkedTaskCount} linked tasks complete",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Text(
            text = "${(item.taskProgressFraction * 100).roundToInt()}%",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = if (reached) MaterialTheme.colorScheme.tertiary else projectColor,
          )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
          progress = { item.taskProgressFraction },
          color = if (reached) MaterialTheme.colorScheme.tertiary else projectColor,
          trackColor = MaterialTheme.colorScheme.surfaceVariant,
          modifier = Modifier
            .fillMaxWidth()
            .height(5.dp)
            .clip(RoundedCornerShape(3.dp)),
        )
      }
    }
  }
}

@Composable
fun EmptyProjectsCard(
  onCreateProject: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Card(
    shape = MaterialTheme.shapes.large,
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
    modifier = modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(20.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Image(
        painter = painterResource(id = R.drawable.img_empty_projects),
        contentDescription = "Illustration for creating your first project",
        modifier = Modifier
          .fillMaxWidth()
          .height(170.dp)
          .clip(MaterialTheme.shapes.medium),
        contentScale = ContentScale.Crop,
      )
      Spacer(modifier = Modifier.height(16.dp))
      Text(
        text = stringResource(R.string.empty_projects_title),
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = stringResource(R.string.empty_projects_body),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(modifier = Modifier.height(16.dp))
      Button(
        onClick = onCreateProject,
        modifier = Modifier
          .fillMaxWidth()
          .height(48.dp)
          .testTag("empty_create_project_button"),
      ) {
        Icon(imageVector = Icons.Outlined.Add, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = stringResource(R.string.action_new_project))
      }
    }
  }
}
