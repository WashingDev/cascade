package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.ProjectAccent
import com.example.data.model.ProjectStatus
import com.example.data.model.TaskPriority
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CadenceRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun createAndObserveProjectMilestoneAndTask_authenticatedOwner_succeeds() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val repo = CadenceRepository(firestore, auth)

    val projectResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.createProject(
        title = "Design System Refresh",
        description = "Audit typography and token contrast",
        category = "Work",
        accentColor = ProjectAccent.COBALT,
        targetDateMs = System.currentTimeMillis() + 7 * 86_400_000L,
      )
    }
    assertTrue("Expected project creation success: ${projectResult.exceptionOrNull()}", projectResult.isSuccess)
    val projectId = projectResult.getOrThrow()

    val milestoneResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.createMilestone(
        projectId = projectId,
        title = "Token Spec Frozen",
        description = "Sign off on light/dark schemes",
        targetDateMs = System.currentTimeMillis() + 3 * 86_400_000L,
      )
    }
    assertTrue("Expected milestone creation success: ${milestoneResult.exceptionOrNull()}", milestoneResult.isSuccess)
    val milestoneId = milestoneResult.getOrThrow()

    val taskResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.createTask(
        projectId = projectId,
        milestoneId = milestoneId,
        title = "Export color tokens",
        notes = "Verify WCAG AA contrast",
        priority = TaskPriority.HIGH,
        dueDateMs = System.currentTimeMillis() + 86_400_000L,
      )
    }
    assertTrue("Expected task creation success: ${taskResult.exceptionOrNull()}", taskResult.isSuccess)
    val taskId = taskResult.getOrThrow()

    val projects = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeProjects(uid).first { list -> list.any { it.id == projectId } }
    }
    assertTrue(projects.any { it.id == projectId && it.title == "Design System Refresh" })

    val milestones = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeMilestones(uid).first { list -> list.any { it.id == milestoneId } }
    }
    val milestone = milestones.first { it.id == milestoneId }
    assertTrue(repo.toggleMilestoneReached(milestone).isSuccess)

    val tasks = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeTasks(uid).first { list -> list.any { it.id == taskId } }
    }
    val task = tasks.first { it.id == taskId }
    assertTrue(repo.toggleTaskCompleted(task).isSuccess)

    val updatedProject = repo.updateProject(
      projectId = projectId,
      title = "Design System v2",
      description = "Updated description",
      category = "Work",
      accentColor = ProjectAccent.EMERALD,
      status = ProjectStatus.COMPLETED,
      targetDateMs = null,
    )
    assertTrue("Expected project update success: ${updatedProject.exceptionOrNull()}", updatedProject.isSuccess)

    val deleteResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.deleteProjectCascade(projectId)
    }
    assertTrue("Expected cascade delete success: ${deleteResult.exceptionOrNull()}", deleteResult.isSuccess)
  }

  @Test
  fun crossUserRead_failsWithPermissionDenied() = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    val aliceRepo = CadenceRepository(firestore, auth)
    val projectId = withTimeout(DEFAULT_TIMEOUT_MS) {
      aliceRepo.createProject(
        title = "Alice Private Project",
        description = "Secret notes",
        category = "Personal",
        accentColor = ProjectAccent.PLUM,
        targetDateMs = null,
      ).getOrThrow()
    }

    signInTestUser(BOB_EMAIL)
    var caught: Exception? = null
    try {
      withTimeout(DEFAULT_TIMEOUT_MS) {
        firestore.collection("users").document(aliceUid)
          .collection("projects").document(projectId).get().await()
      }
    } catch (e: Exception) {
      caught = e
    }
    val firestoreException = caught?.unwrapFirestoreException()
    assertNotNull("Expected FirebaseFirestoreException, got $caught", firestoreException)
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException?.code)
  }

  @Test
  fun observeProjects_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
    val targetUid = "some_user_uid"
    auth.signOut()
    val repo = CadenceRepository(firestore, auth)

    var caught: Exception? = null
    try {
      withTimeout(FLOW_TIMEOUT_MS) {
        repo.observeProjects(targetUid).first()
      }
    } catch (e: Exception) {
      caught = e
    }
    val firestoreException = caught?.unwrapFirestoreException()
    assertNotNull("Expected nested FirebaseFirestoreException, got $caught", firestoreException)
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException?.code)
  }

  private companion object {
    const val ALICE_EMAIL = "alice_cadence@test.com"
    const val BOB_EMAIL = "bob_cadence@test.com"
    const val DEFAULT_TIMEOUT_MS = 5000L
    const val FLOW_TIMEOUT_MS = 3000L
  }
}
