package com.example.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.automirrored.outlined.EventNote
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.R
import com.example.data.model.Milestone
import com.example.data.model.Project
import com.example.data.model.ProjectTask
import com.example.data.repository.CadenceRepository
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.signOutUser
import com.example.ui.components.AccountDialog
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.MilestoneEditorSheet
import com.example.ui.components.ProjectEditorSheet
import com.example.ui.components.QuickAddMenuSheet
import com.example.ui.components.TaskEditorSheet
import com.example.ui.dashboard.DashboardUiState
import com.example.ui.dashboard.DashboardViewModel
import com.example.ui.screens.DeadlinesScreen
import com.example.ui.screens.MilestonesScreen
import com.example.ui.screens.OverviewScreen
import com.example.ui.screens.ProjectDetailScreen
import com.example.ui.screens.ProjectsScreen
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

private const val ROUTE_OVERVIEW = "overview"
private const val ROUTE_PROJECTS = "projects"
private const val ROUTE_DEADLINES = "deadlines"
private const val ROUTE_MILESTONES = "milestones"
private const val ROUTE_PROJECT_DETAIL = "project/{projectId}"

private data class TopLevelDestination(
  val route: String,
  val labelRes: Int,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
)

private val TopDestinations = listOf(
  TopLevelDestination(ROUTE_OVERVIEW, R.string.nav_dashboard, Icons.Filled.Dashboard, Icons.Outlined.Dashboard),
  TopLevelDestination(ROUTE_PROJECTS, R.string.nav_projects, Icons.Filled.Folder, Icons.Outlined.Folder),
  TopLevelDestination(ROUTE_DEADLINES, R.string.nav_deadlines, Icons.AutoMirrored.Filled.EventNote, Icons.AutoMirrored.Outlined.EventNote),
  TopLevelDestination(ROUTE_MILESTONES, R.string.nav_milestones, Icons.Filled.Flag, Icons.Outlined.Flag),
)

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
  val listener = FirebaseAuth.AuthStateListener { auth ->
    trySend(auth.currentUser)
  }
  addAuthStateListener(listener)
  awaitClose { removeAuthStateListener(listener) }
}

@Composable
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
  val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
  val user = currentUser

  if (user == null) {
    AuthScreen(onAuthSuccess = {})
  } else {
    key(user.uid) {
      CadenceMainShell(
        currentUserId = user.uid,
        userDisplayName = user.displayName,
        userEmail = user.email,
      )
    }
  }
}

private sealed interface ActiveSheet {
  data object QuickAdd : ActiveSheet
  data class EditProject(val project: Project?) : ActiveSheet
  data class EditTask(val task: ProjectTask?, val defaultProjectId: String? = null) : ActiveSheet
  data class EditMilestone(val milestone: Milestone?, val defaultProjectId: String? = null) : ActiveSheet
}

@Composable
private fun CadenceMainShell(
  currentUserId: String,
  userDisplayName: String?,
  userEmail: String?,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }

  val viewModel: DashboardViewModel = viewModel(
    key = currentUserId,
    factory = viewModelFactory {
      initializer {
        val app = checkNotNull(this[APPLICATION_KEY]) {
          "APPLICATION_KEY missing from CreationExtras"
        }
        val databaseId = app.getString(R.string.firestore_database_id)
        val db = FirebaseFirestore.getInstance(databaseId)
        DashboardViewModel(CadenceRepository(db), currentUserId)
      }
    },
  )

  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
  val snackbarHostState = remember { SnackbarHostState() }

  LaunchedEffect(viewModel) {
    viewModel.events.collect { message ->
      snackbarHostState.showSnackbar(message)
    }
  }

  val navController = rememberNavController()
  val navBackStackEntry by navController.currentBackStackEntryAsState()
  val currentRoute = navBackStackEntry?.destination?.route ?: ROUTE_OVERVIEW
  val isDetailRoute = currentRoute == ROUTE_PROJECT_DETAIL

  var activeSheet by remember { mutableStateOf<ActiveSheet?>(null) }
  var showAccountDialog by remember { mutableStateOf(false) }
  var projectToDelete by remember { mutableStateOf<Project?>(null) }

  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val isWideLayout = maxWidth >= 600.dp

    Scaffold(
      containerColor = MaterialTheme.colorScheme.background,
      contentWindowInsets = WindowInsets.safeDrawing,
      snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
      bottomBar = {
        if (!isWideLayout && !isDetailRoute) {
          NavigationBar(
            windowInsets = WindowInsets.navigationBars,
            containerColor = MaterialTheme.colorScheme.surface,
          ) {
            TopDestinations.forEach { dest ->
              val selected = currentRoute == dest.route
              val label = stringResource(dest.labelRes)
              NavigationBarItem(
                selected = selected,
                onClick = {
                  if (!selected) {
                    navController.navigate(dest.route) {
                      popUpTo(ROUTE_OVERVIEW) { saveState = true }
                      launchSingleTop = true
                      restoreState = true
                    }
                  }
                },
                icon = {
                  Icon(
                    imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                    contentDescription = label,
                  )
                },
                label = { Text(label) },
                modifier = Modifier.testTag("nav_${dest.route}"),
              )
            }
          }
        }
      },
      floatingActionButton = {
        if (!isDetailRoute && uiState is DashboardUiState.Success) {
          FloatingActionButton(
            onClick = {
              when (currentRoute) {
                ROUTE_PROJECTS -> activeSheet = ActiveSheet.EditProject(null)
                else -> activeSheet = ActiveSheet.QuickAdd
              }
            },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.testTag("main_fab"),
          ) {
            Icon(imageVector = Icons.Outlined.Add, contentDescription = "Create new item")
          }
        }
      },
    ) { innerPadding ->
      Row(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding),
      ) {
        if (isWideLayout && !isDetailRoute) {
          NavigationRail(
            containerColor = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxHeight(),
          ) {
            Spacer(modifier = Modifier.height(12.dp))
            TopDestinations.forEach { dest ->
              val selected = currentRoute == dest.route
              val label = stringResource(dest.labelRes)
              NavigationRailItem(
                selected = selected,
                onClick = {
                  if (!selected) {
                    navController.navigate(dest.route) {
                      popUpTo(ROUTE_OVERVIEW) { saveState = true }
                      launchSingleTop = true
                      restoreState = true
                    }
                  }
                },
                icon = {
                  Icon(
                    imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                    contentDescription = label,
                  )
                },
                label = { Text(label) },
                modifier = Modifier.testTag("rail_${dest.route}"),
              )
            }
          }
        }

        Box(
          modifier = Modifier
            .weight(1f)
            .fillMaxHeight(),
          contentAlignment = Alignment.TopCenter,
        ) {
          Box(
            modifier = Modifier
              .widthIn(max = 960.dp)
              .fillMaxSize(),
          ) {
            when (val state = uiState) {
              DashboardUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                  CircularProgressIndicator()
                }
              }

              is DashboardUiState.Error -> {
                Column(
                  modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                  verticalArrangement = Arrangement.Center,
                  horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                  Text(
                    text = stringResource(R.string.error_generic_load),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                  )
                  Spacer(modifier = Modifier.height(8.dp))
                  Text(
                    text = state.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                  Spacer(modifier = Modifier.height(16.dp))
                  Button(onClick = { showAccountDialog = true }) {
                    Text("Account Options")
                  }
                }
              }

              is DashboardUiState.Success -> {
                NavHost(
                  navController = navController,
                  startDestination = ROUTE_OVERVIEW,
                  modifier = Modifier.fillMaxSize(),
                ) {
                  composable(ROUTE_OVERVIEW) {
                    OverviewScreen(
                      state = state,
                      userDisplayName = userDisplayName,
                      isWideLayout = isWideLayout,
                      onOpenAccount = { showAccountDialog = true },
                      onCreateProject = { activeSheet = ActiveSheet.EditProject(null) },
                      onCreateTask = { activeSheet = ActiveSheet.EditTask(null) },
                      onOpenProject = { id -> navController.navigate("project/$id") },
                      onViewAllProjects = { navController.navigate(ROUTE_PROJECTS) },
                      onViewAllDeadlines = { navController.navigate(ROUTE_DEADLINES) },
                      onViewAllMilestones = { navController.navigate(ROUTE_MILESTONES) },
                      onToggleTask = viewModel::toggleTaskCompleted,
                      onEditTask = { task -> activeSheet = ActiveSheet.EditTask(task) },
                      onToggleMilestone = viewModel::toggleMilestoneReached,
                      onEditMilestone = { ms -> activeSheet = ActiveSheet.EditMilestone(ms) },
                    )
                  }

                  composable(ROUTE_PROJECTS) {
                    ProjectsScreen(
                      state = state,
                      isWideLayout = isWideLayout,
                      onCreateProject = { activeSheet = ActiveSheet.EditProject(null) },
                      onOpenProject = { id -> navController.navigate("project/$id") },
                    )
                  }

                  composable(ROUTE_DEADLINES) {
                    DeadlinesScreen(
                      state = state,
                      onToggleTask = viewModel::toggleTaskCompleted,
                      onEditTask = { task -> activeSheet = ActiveSheet.EditTask(task) },
                    )
                  }

                  composable(ROUTE_MILESTONES) {
                    MilestonesScreen(
                      state = state,
                      onToggleMilestone = viewModel::toggleMilestoneReached,
                      onEditMilestone = { ms -> activeSheet = ActiveSheet.EditMilestone(ms) },
                    )
                  }

                  composable(
                    route = ROUTE_PROJECT_DETAIL,
                    arguments = listOf(navArgument("projectId") { type = NavType.StringType }),
                  ) { entry ->
                    val projectId = entry.arguments?.getString("projectId").orEmpty()
                    ProjectDetailScreen(
                      projectId = projectId,
                      state = state,
                      onBack = { navController.popBackStack() },
                      onEditProject = { proj -> activeSheet = ActiveSheet.EditProject(proj) },
                      onChangeStatus = { status -> viewModel.updateProjectStatus(projectId, status) },
                      onRequestDeleteProject = { proj -> projectToDelete = proj },
                      onAddMilestone = {
                        activeSheet = ActiveSheet.EditMilestone(null, defaultProjectId = projectId)
                      },
                      onToggleMilestone = viewModel::toggleMilestoneReached,
                      onEditMilestone = { ms -> activeSheet = ActiveSheet.EditMilestone(ms) },
                      onAddTask = {
                        activeSheet = ActiveSheet.EditTask(null, defaultProjectId = projectId)
                      },
                      onToggleTask = viewModel::toggleTaskCompleted,
                      onEditTask = { task -> activeSheet = ActiveSheet.EditTask(task) },
                    )
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  val successState = uiState as? DashboardUiState.Success
  val allProjects = successState?.projects?.map { it.project }.orEmpty()
  val allMilestones = successState?.milestones?.map { it.milestone }.orEmpty()

  when (val sheet = activeSheet) {
    ActiveSheet.QuickAdd -> {
      QuickAddMenuSheet(
        hasProjects = allProjects.isNotEmpty(),
        onDismiss = { activeSheet = null },
        onNewProject = { activeSheet = ActiveSheet.EditProject(null) },
        onNewTask = { activeSheet = ActiveSheet.EditTask(null) },
        onNewMilestone = { activeSheet = ActiveSheet.EditMilestone(null) },
      )
    }

    is ActiveSheet.EditProject -> {
      ProjectEditorSheet(
        existingProject = sheet.project,
        isSaving = isSaving,
        onDismiss = { activeSheet = null },
        onSave = { title, description, category, accent, status, targetDateMs ->
          if (sheet.project == null) {
            viewModel.createProject(title, description, category, accent, targetDateMs)
          } else {
            viewModel.updateProject(sheet.project.id, title, description, category, accent, status, targetDateMs)
          }
          activeSheet = null
        },
      )
    }

    is ActiveSheet.EditTask -> {
      TaskEditorSheet(
        existingTask = sheet.task,
        projects = allProjects,
        milestones = allMilestones,
        initialProjectId = sheet.defaultProjectId,
        isSaving = isSaving,
        onDismiss = { activeSheet = null },
        onDelete = sheet.task?.let { t ->
          {
            viewModel.deleteTask(t.id)
            activeSheet = null
          }
        },
        onSave = { projectId, milestoneId, title, notes, priority, dueDateMs ->
          if (sheet.task == null) {
            viewModel.createTask(projectId, milestoneId, title, notes, priority, dueDateMs)
          } else {
            viewModel.updateTask(
              taskId = sheet.task.id,
              milestoneId = milestoneId,
              title = title,
              notes = notes,
              priority = priority,
              dueDateMs = dueDateMs,
              isCompleted = sheet.task.isCompleted,
              completedAtMs = sheet.task.completedAtMs,
            )
          }
          activeSheet = null
        },
      )
    }

    is ActiveSheet.EditMilestone -> {
      MilestoneEditorSheet(
        existingMilestone = sheet.milestone,
        projects = allProjects,
        initialProjectId = sheet.defaultProjectId,
        isSaving = isSaving,
        onDismiss = { activeSheet = null },
        onDelete = sheet.milestone?.let { m ->
          {
            viewModel.deleteMilestone(m.id)
            activeSheet = null
          }
        },
        onSave = { projectId, title, description, targetDateMs ->
          if (sheet.milestone == null) {
            viewModel.createMilestone(projectId, title, description, targetDateMs)
          } else {
            viewModel.updateMilestone(
              milestoneId = sheet.milestone.id,
              title = title,
              description = description,
              targetDateMs = targetDateMs,
              isReached = sheet.milestone.isReached,
              reachedAtMs = sheet.milestone.reachedAtMs,
            )
          }
          activeSheet = null
        },
      )
    }

    null -> Unit
  }

  if (showAccountDialog) {
    AccountDialog(
      displayName = userDisplayName,
      email = userEmail,
      summary = successState?.summary,
      onDismiss = { showAccountDialog = false },
      onSignOut = {
        showAccountDialog = false
        signOutUser(credentialManager, scope)
      },
    )
  }

  projectToDelete?.let { proj ->
    ConfirmDeleteDialog(
      title = "Delete \"${proj.title}\"?",
      message = "This removes the project along with all of its tasks and milestones.",
      onConfirm = {
        projectToDelete = null
        viewModel.deleteProject(proj.id) {
          navController.popBackStack()
        }
      },
      onDismiss = { projectToDelete = null },
    )
  }
}
