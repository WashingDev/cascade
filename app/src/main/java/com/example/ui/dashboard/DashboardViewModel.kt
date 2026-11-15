package com.example.ui.dashboard

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Milestone
import com.example.data.model.Project
import com.example.data.model.ProjectAccent
import com.example.data.model.ProjectStatus
import com.example.data.model.ProjectTask
import com.example.data.model.TaskPriority
import com.example.data.repository.CadenceRepository
import com.example.data.repository.extractFriendlyMessage
import com.example.util.DateFormatters
import com.example.util.DeadlineBucket
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProjectWithStats(
  val project: Project,
  val totalTasks: Int,
  val completedTasks: Int,
  val overdueTasks: Int,
  val totalMilestones: Int,
  val reachedMilestones: Int,
  val nextDeadlineMs: Long?,
  val nextMilestone: Milestone?,
) {
  val progressFraction: Float
    get() = when {
      totalTasks > 0 -> completedTasks.toFloat() / totalTasks.toFloat()
      totalMilestones > 0 -> reachedMilestones.toFloat() / totalMilestones.toFloat()
      project.statusEnum == ProjectStatus.COMPLETED -> 1f
      else -> 0f
    }
}

data class MilestoneWithContext(
  val milestone: Milestone,
  val project: Project?,
  val linkedTaskCount: Int,
  val completedLinkedTaskCount: Int,
) {
  val taskProgressFraction: Float
    get() = if (linkedTaskCount > 0) completedLinkedTaskCount.toFloat() / linkedTaskCount.toFloat() else 0f
}

data class TaskWithContext(
  val task: ProjectTask,
  val project: Project?,
  val milestone: Milestone?,
  val bucket: DeadlineBucket,
)

data class WorkspaceSummary(
  val activeProjectsCount: Int,
  val completedProjectsCount: Int,
  val openTasksCount: Int,
  val completedTasksCount: Int,
  val overdueCount: Int,
  val dueTodayCount: Int,
  val upcomingMilestonesCount: Int,
  val reachedMilestonesCount: Int,
) {
  val overallProgress: Float
    get() {
      val total = openTasksCount + completedTasksCount
      return if (total > 0) completedTasksCount.toFloat() / total.toFloat() else 0f
    }
}

sealed interface DashboardUiState {
  data object Loading : DashboardUiState
  data class Success(
    val summary: WorkspaceSummary,
    val projects: List<ProjectWithStats>,
    val milestones: List<MilestoneWithContext>,
    val tasks: List<TaskWithContext>,
  ) : DashboardUiState

  data class Error(val message: String) : DashboardUiState
}

class DashboardViewModel(
  private val repository: CadenceRepository,
  private val currentUserId: String,
) : ViewModel() {

  private val _events = MutableSharedFlow<String>(extraBufferCapacity = 8)
  val events: SharedFlow<String> = _events.asSharedFlow()

  private val _isSaving = MutableStateFlow(false)
  val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

  val uiState: StateFlow<DashboardUiState> = combine(
    repository.observeProjects(currentUserId),
    repository.observeMilestones(currentUserId),
    repository.observeTasks(currentUserId),
  ) { projects, milestones, tasks ->
    buildDashboardSuccess(projects, milestones, tasks) as DashboardUiState
  }
    .catch { error ->
      val firestoreError = generateSequence(error) { it.cause }
        .filterIsInstance<FirebaseFirestoreException>()
        .firstOrNull()
      if (firestoreError == null && error is CancellationException) throw error
      val rootError = firestoreError ?: error
      Log.w(TAG, "Error observing workspace data", rootError)
      emit(DashboardUiState.Error(rootError.extractFriendlyMessage("Failed to load workspace")))
    }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
      initialValue = DashboardUiState.Loading,
    )

  private fun buildDashboardSuccess(
    projects: List<Project>,
    milestones: List<Milestone>,
    tasks: List<ProjectTask>,
  ): DashboardUiState.Success {
    val nowMs = System.currentTimeMillis()
    val projectById = projects.associateBy { it.id }
    val milestoneById = milestones.associateBy { it.id }

    val tasksByProject = tasks.groupBy { it.projectId }
    val milestonesByProject = milestones.groupBy { it.projectId }
    val tasksByMilestone = tasks.filter { !it.milestoneId.isNullOrBlank() }.groupBy { it.milestoneId!! }

    val enrichedProjects = projects
      .sortedWith(
        compareBy<Project> {
          when (it.statusEnum) {
            ProjectStatus.ACTIVE -> 0
            ProjectStatus.PAUSED -> 1
            ProjectStatus.COMPLETED -> 2
            ProjectStatus.ARCHIVED -> 3
          }
        }.thenBy { it.targetDateMs ?: Long.MAX_VALUE }
          .thenByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
      )
      .map { project ->
        val pTasks = tasksByProject[project.id].orEmpty()
        val pMilestones = milestonesByProject[project.id].orEmpty()
        val openTasks = pTasks.filter { !it.isCompleted }
        val overdueTasks = openTasks.count { t ->
          t.dueDateMs != null && DateFormatters.daysDeltaFromToday(t.dueDateMs, nowMs) < 0
        }
        val nextTaskDue = openTasks.mapNotNull { it.dueDateMs }.minOrNull()
        val nextProjectDeadline = listOfNotNull(project.targetDateMs, nextTaskDue).minOrNull()
        val nextMs = pMilestones
          .filter { !it.isReached }
          .minByOrNull { it.targetDateMs }

        ProjectWithStats(
          project = project,
          totalTasks = pTasks.size,
          completedTasks = pTasks.count { it.isCompleted },
          overdueTasks = overdueTasks,
          totalMilestones = pMilestones.size,
          reachedMilestones = pMilestones.count { it.isReached },
          nextDeadlineMs = nextProjectDeadline,
          nextMilestone = nextMs,
        )
      }

    val enrichedMilestones = milestones
      .sortedWith(compareBy<Milestone> { it.isReached }.thenBy { it.targetDateMs })
      .map { ms ->
        val linked = tasksByMilestone[ms.id].orEmpty()
        MilestoneWithContext(
          milestone = ms,
          project = projectById[ms.projectId],
          linkedTaskCount = linked.size,
          completedLinkedTaskCount = linked.count { it.isCompleted },
        )
      }

    val enrichedTasks = tasks
      .map { task ->
        TaskWithContext(
          task = task,
          project = projectById[task.projectId],
          milestone = task.milestoneId?.let { milestoneById[it] },
          bucket = DateFormatters.bucketForTask(task.isCompleted, task.dueDateMs, nowMs),
        )
      }
      .sortedWith(
        compareBy<TaskWithContext> { it.task.isCompleted }
          .thenBy { it.task.dueDateMs ?: Long.MAX_VALUE }
          .thenBy { it.task.priorityEnum.sortOrder }
          .thenByDescending { it.task.updatedAt ?: it.task.createdAt ?: Timestamp(0, 0) }
      )

    val openTasks = enrichedTasks.filter { !it.task.isCompleted }
    val summary = WorkspaceSummary(
      activeProjectsCount = projects.count { it.statusEnum == ProjectStatus.ACTIVE },
      completedProjectsCount = projects.count { it.statusEnum == ProjectStatus.COMPLETED },
      openTasksCount = openTasks.size,
      completedTasksCount = tasks.count { it.isCompleted },
      overdueCount = openTasks.count { it.bucket == DeadlineBucket.OVERDUE },
      dueTodayCount = openTasks.count { it.bucket == DeadlineBucket.TODAY },
      upcomingMilestonesCount = milestones.count { !it.isReached },
      reachedMilestonesCount = milestones.count { it.isReached },
    )

    return DashboardUiState.Success(
      summary = summary,
      projects = enrichedProjects,
      milestones = enrichedMilestones,
      tasks = enrichedTasks,
    )
  }

  fun createProject(
    title: String,
    description: String,
    category: String,
    accent: ProjectAccent,
    targetDateMs: Long?,
    onCreated: (String) -> Unit = {},
  ) {
    if (title.isBlank()) {
      emitEvent("Please enter a project title.")
      return
    }
    viewModelScope.launch {
      _isSaving.value = true
      val res = repository.createProject(title, description, category, accent, targetDateMs)
      _isSaving.value = false
      res.onSuccess { id ->
        emitEvent("Project created")
        onCreated(id)
      }.onFailure { err ->
        emitEvent(err.extractFriendlyMessage("Could not create project"))
      }
    }
  }

  fun updateProject(
    projectId: String,
    title: String,
    description: String,
    category: String,
    accent: ProjectAccent,
    status: ProjectStatus,
    targetDateMs: Long?,
  ) {
    if (title.isBlank()) {
      emitEvent("Project title cannot be empty.")
      return
    }
    viewModelScope.launch {
      _isSaving.value = true
      val res = repository.updateProject(projectId, title, description, category, accent, status, targetDateMs)
      _isSaving.value = false
      res.onSuccess { emitEvent("Project updated") }
        .onFailure { emitEvent(it.extractFriendlyMessage("Could not update project")) }
    }
  }

  fun updateProjectStatus(projectId: String, status: ProjectStatus) {
    viewModelScope.launch {
      val res = repository.updateProjectStatus(projectId, status)
      res.onSuccess { emitEvent("Marked as ${status.label}") }
        .onFailure { emitEvent(it.extractFriendlyMessage("Could not update status")) }
    }
  }

  fun deleteProject(projectId: String, onDeleted: () -> Unit = {}) {
    viewModelScope.launch {
      _isSaving.value = true
      val res = repository.deleteProjectCascade(projectId)
      _isSaving.value = false
      res.onSuccess {
        emitEvent("Project deleted")
        onDeleted()
      }.onFailure {
        emitEvent(it.extractFriendlyMessage("Could not delete project"))
      }
    }
  }

  fun createMilestone(
    projectId: String,
    title: String,
    description: String,
    targetDateMs: Long,
  ) {
    if (title.isBlank()) {
      emitEvent("Please enter a milestone title.")
      return
    }
    viewModelScope.launch {
      _isSaving.value = true
      val res = repository.createMilestone(projectId, title, description, targetDateMs)
      _isSaving.value = false
      res.onSuccess { emitEvent("Milestone added") }
        .onFailure { emitEvent(it.extractFriendlyMessage("Could not add milestone")) }
    }
  }

  fun updateMilestone(
    milestoneId: String,
    title: String,
    description: String,
    targetDateMs: Long,
    isReached: Boolean,
    reachedAtMs: Long?,
  ) {
    if (title.isBlank()) {
      emitEvent("Milestone title cannot be empty.")
      return
    }
    viewModelScope.launch {
      _isSaving.value = true
      val res = repository.updateMilestone(milestoneId, title, description, targetDateMs, isReached, reachedAtMs)
      _isSaving.value = false
      res.onSuccess { emitEvent("Milestone updated") }
        .onFailure { emitEvent(it.extractFriendlyMessage("Could not update milestone")) }
    }
  }

  fun toggleMilestoneReached(milestone: Milestone) {
    viewModelScope.launch {
      val res = repository.toggleMilestoneReached(milestone)
      res.onSuccess {
        emitEvent(if (!milestone.isReached) "Milestone reached!" else "Milestone reopened")
      }.onFailure {
        emitEvent(it.extractFriendlyMessage("Could not update milestone"))
      }
    }
  }

  fun deleteMilestone(milestoneId: String) {
    viewModelScope.launch {
      val res = repository.deleteMilestone(milestoneId)
      res.onSuccess { emitEvent("Milestone removed") }
        .onFailure { emitEvent(it.extractFriendlyMessage("Could not remove milestone")) }
    }
  }

  fun createTask(
    projectId: String,
    milestoneId: String?,
    title: String,
    notes: String,
    priority: TaskPriority,
    dueDateMs: Long?,
  ) {
    if (title.isBlank()) {
      emitEvent("Please enter a task title.")
      return
    }
    viewModelScope.launch {
      _isSaving.value = true
      val res = repository.createTask(projectId, milestoneId, title, notes, priority, dueDateMs)
      _isSaving.value = false
      res.onSuccess { emitEvent("Task added") }
        .onFailure { emitEvent(it.extractFriendlyMessage("Could not add task")) }
    }
  }

  fun updateTask(
    taskId: String,
    milestoneId: String?,
    title: String,
    notes: String,
    priority: TaskPriority,
    dueDateMs: Long?,
    isCompleted: Boolean,
    completedAtMs: Long?,
  ) {
    if (title.isBlank()) {
      emitEvent("Task title cannot be empty.")
      return
    }
    viewModelScope.launch {
      _isSaving.value = true
      val res = repository.updateTask(taskId, milestoneId, title, notes, priority, dueDateMs, isCompleted, completedAtMs)
      _isSaving.value = false
      res.onSuccess { emitEvent("Task updated") }
        .onFailure { emitEvent(it.extractFriendlyMessage("Could not update task")) }
    }
  }

  fun toggleTaskCompleted(task: ProjectTask) {
    viewModelScope.launch {
      val res = repository.toggleTaskCompleted(task)
      res.onFailure {
        emitEvent(it.extractFriendlyMessage("Could not update task"))
      }
    }
  }

  fun deleteTask(taskId: String) {
    viewModelScope.launch {
      val res = repository.deleteTask(taskId)
      res.onSuccess { emitEvent("Task deleted") }
        .onFailure { emitEvent(it.extractFriendlyMessage("Could not delete task")) }
    }
  }

  private fun emitEvent(message: String) {
    _events.tryEmit(message)
  }

  private companion object {
    const val TAG = "DashboardVM"
    const val STOP_TIMEOUT_MILLIS = 5000L
  }
}
