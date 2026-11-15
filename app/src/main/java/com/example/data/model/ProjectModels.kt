package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class ProjectAccent(val key: String, val label: String) {
  COBALT("cobalt", "Cobalt"),
  TERRACOTTA("terracotta", "Terracotta"),
  EMERALD("emerald", "Emerald"),
  AMBER("amber", "Amber"),
  PLUM("plum", "Plum"),
  TEAL("teal", "Teal");

  companion object {
    fun fromKey(key: String?): ProjectAccent = entries.find { it.key == key } ?: COBALT
  }
}

enum class ProjectStatus(val key: String, val label: String) {
  ACTIVE("active", "Active"),
  PAUSED("paused", "Paused"),
  COMPLETED("completed", "Completed"),
  ARCHIVED("archived", "Archived");

  companion object {
    fun fromKey(key: String?): ProjectStatus = entries.find { it.key == key } ?: ACTIVE
  }
}

enum class TaskPriority(val key: String, val label: String, val sortOrder: Int) {
  URGENT("urgent", "Urgent", 0),
  HIGH("high", "High", 1),
  MEDIUM("medium", "Medium", 2),
  LOW("low", "Low", 3);

  companion object {
    fun fromKey(key: String?): TaskPriority = entries.find { it.key == key } ?: MEDIUM
  }
}

@IgnoreExtraProperties
data class Project(
  @DocumentId val id: String = "",
  val userId: String = "",
  val title: String = "",
  val description: String = "",
  val category: String = "Personal",
  val accentColor: String = ProjectAccent.COBALT.key,
  val status: String = ProjectStatus.ACTIVE.key,
  val targetDateMs: Long? = null,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  val accentEnum: ProjectAccent
    get() = ProjectAccent.fromKey(accentColor)

  val statusEnum: ProjectStatus
    get() = ProjectStatus.fromKey(status)
}

@IgnoreExtraProperties
data class Milestone(
  @DocumentId val id: String = "",
  val userId: String = "",
  val projectId: String = "",
  val title: String = "",
  val description: String = "",
  val targetDateMs: Long = 0L,
  val isReached: Boolean = false,
  val reachedAtMs: Long? = null,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class ProjectTask(
  @DocumentId val id: String = "",
  val userId: String = "",
  val projectId: String = "",
  val milestoneId: String? = null,
  val title: String = "",
  val notes: String = "",
  val isCompleted: Boolean = false,
  val priority: String = TaskPriority.MEDIUM.key,
  val dueDateMs: Long? = null,
  val completedAtMs: Long? = null,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  val priorityEnum: TaskPriority
    get() = TaskPriority.fromKey(priority)
}
