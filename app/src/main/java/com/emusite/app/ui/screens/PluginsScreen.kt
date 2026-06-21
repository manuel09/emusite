package com.emusite.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.emusite.app.data.MainViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PluginsScreen(
    onRepoClick: (String) -> Unit,
    viewModel: MainViewModel = viewModel()
) {
    val state by viewModel.pluginsState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var showUninstallDialog by remember { mutableStateOf<String?>(null) }
    var showRepoDialog by remember { mutableStateOf(false) }
    var showQrDialog by remember { mutableStateOf(false) }
    var showRemoveRepoDialog by remember { mutableStateOf<String?>(null) }
    var repoUrl by remember { mutableStateOf("") }

    LaunchedEffect(state.repoMessage) {
        if (state.repoMessage != null) {
            kotlinx.coroutines.delay(3000)
            viewModel.clearRepoMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Plugins") },
                actions = {
                    IconButton(onClick = { showQrDialog = true }) {
                        Icon(Icons.Default.QrCode, contentDescription = "Add via QR")
                    }
                    IconButton(onClick = { showRepoDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add repository")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.height(40.dp)
            )
            }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (state.repoMessage != null) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Text(
                            state.repoMessage!!,
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            item {
                Text("Installed Plugins", style = MaterialTheme.typography.titleSmall)
            }

            if (state.plugins.isEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("No plugins installed")
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Add a repository to browse and install plugins",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            items(state.plugins, key = { it.plugin.id }) { loaded ->
                val plugin = loaded.plugin
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(plugin.name, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    plugin.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "v${plugin.version} - ${plugin.language}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { showUninstallDialog = plugin.id }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Uninstall",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                if (loaded.isEnabled) "Enabled" else "Disabled",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Switch(
                                checked = loaded.isEnabled,
                                onCheckedChange = { viewModel.togglePlugin(plugin.id, it) }
                            )
                        }
                        Text(
                            "Sources: ${plugin.getSources().joinToString(", ") { it.name }}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            if (state.repositories.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Repositories", style = MaterialTheme.typography.titleSmall)
                }

                items(state.repositories, key = { it.url }) { repo ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onRepoClick(repo.url) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(repo.name, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "${repo.plugins.size} plugins",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { showRemoveRepoDialog = repo.url }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Remove",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showRepoDialog) {
        AlertDialog(
            onDismissRequest = { showRepoDialog = false },
            title = { Text("Add Repository") },
            text = {
                Column {
                    Text("Enter the repository URL")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = repoUrl,
                        onValueChange = { repoUrl = it },
                        label = { Text("Repository URL") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (repoUrl.isNotBlank()) {
                        viewModel.fetchRepository(repoUrl)
                        showRepoDialog = false
                        repoUrl = ""
                    }
                }) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { showRepoDialog = false }) { Text("Cancel") }
            }
        )
    }

    showUninstallDialog?.let { pluginId ->
        AlertDialog(
            onDismissRequest = { showUninstallDialog = null },
            title = { Text("Uninstall Plugin") },
            text = { Text("Are you sure you want to uninstall this plugin?") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch { viewModel.uninstallPlugin(pluginId) }
                        showUninstallDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("Uninstall") }
            },
            dismissButton = {
                TextButton(onClick = { showUninstallDialog = null }) { Text("Cancel") }
            }
        )
    }

    showRemoveRepoDialog?.let { url ->
        AlertDialog(
            onDismissRequest = { showRemoveRepoDialog = null },
            title = { Text("Remove Repository") },
            text = { Text("Are you sure you want to remove this repository?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.removeRepository(url)
                        showRemoveRepoDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("Remove") }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveRepoDialog = null }) { Text("Cancel") }
            }
        )
    }

    if (showQrDialog) {
        FirestickRepoDialog(
            viewModel = viewModel,
            onDismiss = { showQrDialog = false }
        )
    }
}
