package com.emusite.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.emusite.app.data.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepoDetailScreen(
    repoUrl: String,
    onBack: () -> Unit,
    viewModel: MainViewModel = viewModel()
) {
    val state by viewModel.pluginsState.collectAsStateWithLifecycle()
    val repo = state.repositories.find { it.url == repoUrl }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(repo?.name ?: "Repository") },
                modifier = Modifier.height(40.dp),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (repo == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text(
                        repo.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                items(repo.plugins) { rp ->
                    val installed = state.plugins.any { it.plugin.id == rp.internalName }
                    Card(modifier = Modifier.fillMaxWidth()) {
                        ListItem(
                            headlineContent = { Text(rp.name) },
                            supportingContent = {
                                Column {
                                    Text("v${rp.version}")
                                    if (rp.description.isNotBlank()) {
                                        Text(rp.description, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            },
                            trailingContent = {
                                if (installed) {
                                    Text("Installed", color = MaterialTheme.colorScheme.primary)
                                } else {
                                    IconButton(onClick = {
                                        viewModel.downloadAndInstallPlugin(rp.url)
                                    }) {
                                        Icon(Icons.Default.Download, contentDescription = "Install")
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
