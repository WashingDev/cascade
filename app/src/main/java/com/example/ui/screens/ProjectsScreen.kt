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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.ProjectStatus
import com.example.ui.components.EmptyProjectsCard
import com.example.ui.components.ProjectCard
import com.example.ui.dashboard.DashboardUiState

@Composable
fun ProjectsScreen(
  state: DashboardUiState.Success,
  isWideLayout: Boolean,
  onCreateProject: () -> Unit,
  onOpenProject: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  var searchQuery by remember { mutableStateOf("") }
  var selectedStatus by remember { mutableStateOf<ProjectStatus?>(null) }

  val filteredProjects = remember(state.projects, searchQuery, selectedStatus) {
    state.projects.filter { item ->
      val matchesStatus = selectedStatus == null || item.project.statusEnum == selectedStatus
      val q = searchQuery.trim()
      val matchesQuery = q.isEmpty() ||
        item.project.title.contains(q, ignoreCase = true) ||
        item.project.category.contains(q, ignoreCase = true) ||
        item.project.description.contains(q, ignoreCase = true)
      matchesStatus && matchesQuery
    }
  }

  Column(modifier = modifier.fillMaxSize()) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Text(
        text = "Projects",
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onBackground,
      )

      OutlinedTextField(
        value = searchQuery,
        onValueChange = { searchQuery = it },
        placeholder = { Text("Search projects or categories") },
        leadingIcon = { Icon(imageVector = Icons.Outlined.Search, contentDescription = null) },
        trailingIcon = {
          if (searchQuery.isNotEmpty()) {
            IconButton(onClick = { searchQuery = "" }) {
              Icon(imageVector = Icons.Outlined.Close, contentDescription = "Clear search")
            }
          }
        },
        singleLine = true,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("projects_search_input"),
      )

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        FilterChip(
          selected = selectedStatus == null,
          onClick = { selectedStatus = null },
          label = { Text("All (${state.projects.size})") },
        )
        ProjectStatus.entries.forEach { st ->
          val count = state.projects.count { it.project.statusEnum == st }
          FilterChip(
            selected = selectedStatus == st,
            onClick = { selectedStatus = if (selectedStatus == st) null else st },
            label = { Text("${st.label} ($count)") },
          )
        }
      }
    }

    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      if (state.projects.isEmpty()) {
        item(key = "empty") {
          EmptyProjectsCard(onCreateProject = onCreateProject)
        }
      } else if (filteredProjects.isEmpty()) {
        item(key = "no_matches") {
          Text(
            text = "No projects match your current filters.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 24.dp),
          )
        }
      } else if (isWideLayout) {
        val rows = filteredProjects.chunked(2)
        items(rows, key = { row -> "row_${row.first().project.id}" }) { row ->
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
        items(filteredProjects, key = { it.project.id }) { proj ->
          ProjectCard(
            item = proj,
            onClick = { onOpenProject(proj.project.id) },
          )
        }
      }
    }
  }
}
