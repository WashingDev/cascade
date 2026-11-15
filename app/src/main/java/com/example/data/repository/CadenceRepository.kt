package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.Milestone
import com.example.data.model.Project
import com.example.data.model.ProjectAccent
import com.example.data.model.ProjectStatus
import com.example.data.model.ProjectTask
import com.example.data.model.TaskPriority
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class CadenceRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = Firebase.auth,
) {
  constructor(context: Context) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    )
  )

  private fun requireUserId(): String {
    return auth.currentUser?.uid
      ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
  }

  private fun projectsCollection(uid: String) =
    db.collection("users").document(uid).collection("projects")

  private fun milestonesCollection(uid: String) =
    db.collection("users").document(uid).collection("milestones")

  private fun tasksCollection(uid: String) =
    db.collection("users").document(uid).collection("tasks")

  fun observeProjects(userId: String? = null): Flow<List<Project>> = flow {
    val uid = userId ?: requireUserId()
    val path = "users/$uid/projects"
    emitAll(
      projectsCollection(uid)
        .snapshots()
        .map { snapshot ->
          snapshot.toObjects(Project::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        }
        .catch { error ->
          handleFlowError(error, OperationType.LIST, path, uid)
          throw error
        }
    )
  }

  fun observeMilestones(userId: String? = null): Flow<List<Milestone>> = flow {
    val uid = userId ?: requireUserId()
    val path = "users/$uid/milestones"
    emitAll(
      milestonesCollection(uid)
        .snapshots()
        .map { snapshot ->
          snapshot.toObjects(Milestone::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        }
        .catch { error ->
          handleFlowError(error, OperationType.LIST, path, uid)
          throw error
        }
    )
  }

  fun observeTasks(userId: String? = null): Flow<List<ProjectTask>> = flow {
    val uid = userId ?: requireUserId()
    val path = "users/$uid/tasks"
    emitAll(
      tasksCollection(uid)
        .snapshots()
        .map { snapshot ->
          snapshot.toObjects(ProjectTask::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        }
        .catch { error ->
          handleFlowError(error, OperationType.LIST, path, uid)
          throw error
        }
    )
  }

  private fun handleFlowError(
    error: Throwable,
    operationType: OperationType,
    path: String,
    uid: String,
  ) {
    val firestoreError = generateSequence(error) { it.cause }
      .filterIsInstance<FirebaseFirestoreException>()
      .firstOrNull()
    if (firestoreError == null && error is CancellationException) throw error
    val exceptionToLog = firestoreError ?: (error as? Exception)
    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
      handleFirestoreError(exceptionToLog, operationType, path)
    }
  }

  suspend fun createProject(
    title: String,
    description: String,
    category: String,
    accentColor: ProjectAccent,
    targetDateMs: Long?,
  ): Result<String> {
    return try {
      val uid = requireUserId()
      val docRef = projectsCollection(uid).document()
      val payload = buildMap<String, Any> {
        put("userId", uid)
        put("title", title.trim().take(100))
        put("description", description.trim().take(500))
        put("category", category.trim().ifBlank { "Personal" }.take(30))
        put("accentColor", accentColor.key)
        put("status", ProjectStatus.ACTIVE.key)
        if (targetDateMs != null && targetDateMs >= 0L) {
          put("targetDateMs", targetDateMs)
        }
        put("createdAt", FieldValue.serverTimestamp())
        put("updatedAt", FieldValue.serverTimestamp())
      }
      docRef.set(payload).await()
      Result.success(docRef.id)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      val uid = auth.currentUser?.uid ?: "unauthenticated"
      handleFirestoreError(e, OperationType.CREATE, "users/$uid/projects")
      Result.failure(e)
    }
  }

  suspend fun updateProject(
    projectId: String,
    title: String,
    description: String,
    category: String,
    accentColor: ProjectAccent,
    status: ProjectStatus,
    targetDateMs: Long?,
  ): Result<Unit> {
    return try {
      val uid = requireUserId()
      require(projectId.isNotBlank()) { "Project ID must not be blank" }
      val docRef = projectsCollection(uid).document(projectId)
      val updates = mapOf<String, Any>(
        "title" to title.trim().take(100),
        "description" to description.trim().take(500),
        "category" to category.trim().ifBlank { "Personal" }.take(30),
        "accentColor" to accentColor.key,
        "status" to status.key,
        "targetDateMs" to (targetDateMs?.takeIf { it >= 0L } ?: FieldValue.delete()),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
      docRef.update(updates).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      val uid = auth.currentUser?.uid ?: "unauthenticated"
      handleFirestoreError(e, OperationType.UPDATE, "users/$uid/projects/$projectId")
      Result.failure(e)
    }
  }

  suspend fun updateProjectStatus(projectId: String, status: ProjectStatus): Result<Unit> {
    return try {
      val uid = requireUserId()
      val docRef = projectsCollection(uid).document(projectId)
      docRef.update(
        mapOf(
          "status" to status.key,
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      val uid = auth.currentUser?.uid ?: "unauthenticated"
      handleFirestoreError(e, OperationType.UPDATE, "users/$uid/projects/$projectId")
      Result.failure(e)
    }
  }

  suspend fun deleteProjectCascade(projectId: String): Result<Unit> {
    return try {
      val uid = requireUserId()
      val projectRef = projectsCollection(uid).document(projectId)
      val relatedTasks = tasksCollection(uid).whereEqualTo("projectId", projectId).get().await()
      val relatedMilestones = milestonesCollection(uid).whereEqualTo("projectId", projectId).get().await()
      val batch = db.batch()
      relatedTasks.documents.forEach { batch.delete(it.reference) }
      relatedMilestones.documents.forEach { batch.delete(it.reference) }
      batch.delete(projectRef)
      batch.commit().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      val uid = auth.currentUser?.uid ?: "unauthenticated"
      handleFirestoreError(e, OperationType.DELETE, "users/$uid/projects/$projectId")
      Result.failure(e)
    }
  }

  suspend fun createMilestone(
    projectId: String,
    title: String,
    description: String,
    targetDateMs: Long,
  ): Result<String> {
    return try {
      val uid = requireUserId()
      require(projectId.isNotBlank()) { "Project ID is required" }
      require(targetDateMs > 0L) { "Milestone target date must be positive" }
      val docRef = milestonesCollection(uid).document()
      val payload = mapOf<String, Any>(
        "userId" to uid,
        "projectId" to projectId,
        "title" to title.trim().take(120),
        "description" to description.trim().take(400),
        "targetDateMs" to targetDateMs,
        "isReached" to false,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
      docRef.set(payload).await()
      Result.success(docRef.id)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      val uid = auth.currentUser?.uid ?: "unauthenticated"
      handleFirestoreError(e, OperationType.CREATE, "users/$uid/milestones")
      Result.failure(e)
    }
  }

  suspend fun updateMilestone(
    milestoneId: String,
    title: String,
    description: String,
    targetDateMs: Long,
    isReached: Boolean,
    reachedAtMs: Long?,
  ): Result<Unit> {
    return try {
      val uid = requireUserId()
      val docRef = milestonesCollection(uid).document(milestoneId)
      val resolvedReachedAt: Any = if (isReached) {
        reachedAtMs ?: System.currentTimeMillis()
      } else {
        FieldValue.delete()
      }
      val updates = mapOf<String, Any>(
        "title" to title.trim().take(120),
        "description" to description.trim().take(400),
        "targetDateMs" to targetDateMs,
        "isReached" to isReached,
        "reachedAtMs" to resolvedReachedAt,
        "updatedAt" to FieldValue.serverTimestamp(),
      )
      docRef.update(updates).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      val uid = auth.currentUser?.uid ?: "unauthenticated"
      handleFirestoreError(e, OperationType.UPDATE, "users/$uid/milestones/$milestoneId")
      Result.failure(e)
    }
  }

  suspend fun toggleMilestoneReached(milestone: Milestone): Result<Unit> {
    return try {
      val uid = requireUserId()
      val docRef = milestonesCollection(uid).document(milestone.id)
      val nextReached = !milestone.isReached
      val updates = mapOf<String, Any>(
        "isReached" to nextReached,
        "reachedAtMs" to if (nextReached) System.currentTimeMillis() else FieldValue.delete(),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
      docRef.update(updates).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      val uid = auth.currentUser?.uid ?: "unauthenticated"
      handleFirestoreError(e, OperationType.UPDATE, "users/$uid/milestones/${milestone.id}")
      Result.failure(e)
    }
  }

  suspend fun deleteMilestone(milestoneId: String): Result<Unit> {
    return try {
      val uid = requireUserId()
      val docRef = milestonesCollection(uid).document(milestoneId)
      val linkedTasks = tasksCollection(uid).whereEqualTo("milestoneId", milestoneId).get().await()
      val batch = db.batch()
      linkedTasks.documents.forEach { taskDoc ->
        batch.update(
          taskDoc.reference,
          mapOf(
            "milestoneId" to FieldValue.delete(),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
      }
      batch.delete(docRef)
      batch.commit().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      val uid = auth.currentUser?.uid ?: "unauthenticated"
      handleFirestoreError(e, OperationType.DELETE, "users/$uid/milestones/$milestoneId")
      Result.failure(e)
    }
  }

  suspend fun createTask(
    projectId: String,
    milestoneId: String?,
    title: String,
    notes: String,
    priority: TaskPriority,
    dueDateMs: Long?,
  ): Result<String> {
    return try {
      val uid = requireUserId()
      require(projectId.isNotBlank()) { "Project ID is required" }
      val docRef = tasksCollection(uid).document()
      val payload = buildMap<String, Any> {
        put("userId", uid)
        put("projectId", projectId)
        if (!milestoneId.isNullOrBlank()) {
          put("milestoneId", milestoneId)
        }
        put("title", title.trim().take(140))
        put("notes", notes.trim().take(500))
        put("isCompleted", false)
        put("priority", priority.key)
        if (dueDateMs != null && dueDateMs >= 0L) {
          put("dueDateMs", dueDateMs)
        }
        put("createdAt", FieldValue.serverTimestamp())
        put("updatedAt", FieldValue.serverTimestamp())
      }
      docRef.set(payload).await()
      Result.success(docRef.id)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      val uid = auth.currentUser?.uid ?: "unauthenticated"
      handleFirestoreError(e, OperationType.CREATE, "users/$uid/tasks")
      Result.failure(e)
    }
  }

  suspend fun updateTask(
    taskId: String,
    milestoneId: String?,
    title: String,
    notes: String,
    priority: TaskPriority,
    dueDateMs: Long?,
    isCompleted: Boolean,
    completedAtMs: Long?,
  ): Result<Unit> {
    return try {
      val uid = requireUserId()
      val docRef = tasksCollection(uid).document(taskId)
      val updates = mapOf<String, Any>(
        "milestoneId" to (milestoneId?.takeIf { it.isNotBlank() } ?: FieldValue.delete()),
        "title" to title.trim().take(140),
        "notes" to notes.trim().take(500),
        "priority" to priority.key,
        "dueDateMs" to (dueDateMs?.takeIf { it >= 0L } ?: FieldValue.delete()),
        "isCompleted" to isCompleted,
        "completedAtMs" to if (isCompleted) (completedAtMs ?: System.currentTimeMillis()) else FieldValue.delete(),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
      docRef.update(updates).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      val uid = auth.currentUser?.uid ?: "unauthenticated"
      handleFirestoreError(e, OperationType.UPDATE, "users/$uid/tasks/$taskId")
      Result.failure(e)
    }
  }

  suspend fun toggleTaskCompleted(task: ProjectTask): Result<Unit> {
    return try {
      val uid = requireUserId()
      val docRef = tasksCollection(uid).document(task.id)
      val nextCompleted = !task.isCompleted
      val updates = mapOf<String, Any>(
        "isCompleted" to nextCompleted,
        "completedAtMs" to if (nextCompleted) System.currentTimeMillis() else FieldValue.delete(),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
      docRef.update(updates).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      val uid = auth.currentUser?.uid ?: "unauthenticated"
      handleFirestoreError(e, OperationType.UPDATE, "users/$uid/tasks/${task.id}")
      Result.failure(e)
    }
  }

  suspend fun deleteTask(taskId: String): Result<Unit> {
    return try {
      val uid = requireUserId()
      tasksCollection(uid).document(taskId).delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      val uid = auth.currentUser?.uid ?: "unauthenticated"
      handleFirestoreError(e, OperationType.DELETE, "users/$uid/tasks/$taskId")
      Result.failure(e)
    }
  }
}
