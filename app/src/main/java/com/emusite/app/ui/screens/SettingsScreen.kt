package com.emusite.app.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.emusite.app.data.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel = viewModel()
) {
    val state by viewModel.pluginsState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("emusite_settings", Context.MODE_PRIVATE) }

    var defaultPage by remember {
        mutableStateOf(prefs.getString("default_page", "home") ?: "home")
    }
    var defaultQuality by remember {
        mutableStateOf(prefs.getString("default_quality", "highest") ?: "highest")
    }
    var subtitlesEnabled by remember {
        mutableStateOf(prefs.getBoolean("subtitles", false))
    }
    var contentLanguage by remember {
        mutableStateOf(prefs.getString("content_language", "it-IT") ?: "it-IT")
    }
    var showClearDialog by remember { mutableStateOf(false) }
    var isUpdating by remember { mutableStateOf(false) }
    var updateMessage by remember { mutableStateOf<String?>(null) }

    val pageOptions = listOf("home" to "Home", "search" to "Search", "plugins" to "Plugins")
    val qualityOptions = listOf("highest" to "Highest", "1080p" to "1080p", "720p" to "720p", "480p" to "480p")
    val languageOptions = listOf(
        "it-IT" to "Italiano",
        "en-US" to "English",
        "es-ES" to "Espa\u00f1ol",
        "fr-FR" to "Fran\u00e7ais",
        "de-DE" to "Deutsch"
    )

    fun saveSetting(key: String, value: Any) {
        prefs.edit().apply {
            when (value) {
                is String -> putString(key, value)
                is Boolean -> putBoolean(key, value)
            }
        }.apply()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.height(40.dp)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (state.repoMessage != null) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Text(state.repoMessage!!, modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
                }
            }

            Text("General", style = MaterialTheme.typography.titleMedium)

            Card(modifier = Modifier.fillMaxWidth()) {
                Column {
                    ListItem(
                        headlineContent = { Text("Default page") },
                        supportingContent = { Text(pageOptions.find { it.first == defaultPage }?.second ?: "Home") },
                        leadingContent = { Icon(Icons.Default.Home, null) }
                    )
                    pageOptions.forEach { (value, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { defaultPage = value; saveSetting("default_page", value) }
                                .padding(horizontal = 72.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = defaultPage == value,
                                onClick = { defaultPage = value; saveSetting("default_page", value) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(label, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    HorizontalDivider()
                    ListItem(
                        headlineContent = { Text("Content language") },
                        supportingContent = { Text(languageOptions.find { it.first == contentLanguage }?.second ?: "Italiano") },
                        leadingContent = { Icon(Icons.Default.Language, null) }
                    )
                    languageOptions.forEach { (value, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { contentLanguage = value; saveSetting("content_language", value) }
                                .padding(horizontal = 72.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = contentLanguage == value,
                                onClick = { contentLanguage = value; saveSetting("content_language", value) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(label, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            Text("Player", style = MaterialTheme.typography.titleMedium)

            Card(modifier = Modifier.fillMaxWidth()) {
                Column {
                    ListItem(
                        headlineContent = { Text("Default quality") },
                        supportingContent = { Text(qualityOptions.find { it.first == defaultQuality }?.second ?: "Highest") },
                        leadingContent = { Icon(Icons.Default.HighQuality, null) }
                    )
                    qualityOptions.forEach { (value, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { defaultQuality = value; saveSetting("default_quality", value) }
                                .padding(horizontal = 72.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = defaultQuality == value,
                                onClick = { defaultQuality = value; saveSetting("default_quality", value) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(label, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    HorizontalDivider()
                    ListItem(
                        headlineContent = { Text("Subtitles") },
                        supportingContent = { Text(if (subtitlesEnabled) "On" else "Off") },
                        leadingContent = { Icon(Icons.Default.ClosedCaption, null) },
                        trailingContent = {
                            Switch(
                                checked = subtitlesEnabled,
                                onCheckedChange = { subtitlesEnabled = it; saveSetting("subtitles", it) }
                            )
                        }
                    )
                    HorizontalDivider()
                    ListItem(
                        headlineContent = { Text("Auto-rotate") },
                        supportingContent = { Text("Landscape") },
                        leadingContent = { Icon(Icons.Default.ScreenRotation, null) }
                    )
                }
            }

            Text("Repositories & Plugins", style = MaterialTheme.typography.titleMedium)

            Card(modifier = Modifier.fillMaxWidth()) {
                Column {
                    ListItem(
                        headlineContent = { Text("Installed") },
                        supportingContent = { Text("${state.plugins.size} plugins") },
                        leadingContent = { Icon(Icons.Default.Extension, null) }
                    )
                    HorizontalDivider()
                    ListItem(
                        headlineContent = { Text("Repositories") },
                        supportingContent = { Text("${state.repositories.size} repos") },
                        leadingContent = { Icon(Icons.Default.Cloud, null) }
                    )
                    HorizontalDivider()
                    ListItem(
                        headlineContent = { Text("Check for updates") },
                        supportingContent = { Text("Refresh all repositories") },
                        leadingContent = { Icon(Icons.Default.Refresh, null) },
                        trailingContent = {
                            if (state.isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            }
                        },
                        modifier = Modifier.clickable(enabled = !state.isLoading) {
                            viewModel.refreshAllRepos()
                        }
                    )
                }
            }

            state.repositories.forEach { repo ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(repo.name, style = MaterialTheme.typography.titleSmall)
                        Text(repo.url, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                        Spacer(modifier = Modifier.height(8.dp))
                        repo.plugins.forEach { rp ->
                            val installed = state.plugins.find { it.plugin.id == rp.internalName }
                            val needsUpdate = installed != null && installed.plugin.version != rp.version
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(rp.name, style = MaterialTheme.typography.bodySmall)
                                    Text("v${rp.version}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                when {
                                    needsUpdate -> TextButton(onClick = { viewModel.downloadAndInstallPlugin(rp.url) }) { Text("Update") }
                                    installed != null -> Text("Installed", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                    else -> TextButton(onClick = { viewModel.downloadAndInstallPlugin(rp.url) }) { Text("Install") }
                                }
                            }
                        }
                    }
                }
            }

            Text("Data", style = MaterialTheme.typography.titleMedium)

            Card(modifier = Modifier.fillMaxWidth()) {
                ListItem(
                    headlineContent = { Text("Clear all plugins") },
                    supportingContent = { Text("Remove ${state.plugins.size} installed plugins") },
                    leadingContent = { Icon(Icons.Default.Delete, null) },
                    modifier = Modifier.clickable { showClearDialog = true }
                )
            }

            Text("App Updates", style = MaterialTheme.typography.titleMedium)

            Card(modifier = Modifier.fillMaxWidth()) {
                Column {
                    ListItem(
                        headlineContent = { Text("Check for app updates") },
                        supportingContent = { Text(updateMessage ?: "v1.0.0 - Check for new version") },
                        leadingContent = { Icon(Icons.Default.SystemUpdate, null) },
                        trailingContent = {
                            if (isUpdating) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            }
                        },
                        modifier = Modifier.clickable(enabled = !isUpdating) {
                            scope.launch {
                                isUpdating = true
                                updateMessage = "Checking..."
                                try {
                                    withContext(Dispatchers.IO) {
                                        CheckForUpdate(context)
                                    }
                                    updateMessage = null
                                } catch (e: Exception) {
                                    updateMessage = "Update failed: ${e.message}"
                                }
                                isUpdating = false
                            }
                        }
                    )
                }
            }

            Text("About", style = MaterialTheme.typography.titleMedium)

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Emusite", style = MaterialTheme.typography.titleLarge)
                    Text("v1.0.0", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Plugin-based streaming app. Uses TMDB for metadata.", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear all plugins") },
            text = { Text("This will remove all installed plugins. Continue?") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            state.plugins.toList().forEach { viewModel.uninstallPlugin(it.plugin.id) }
                        }
                        showClearDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Clear") }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("Cancel") }
            }
        )
    }
}

private fun CheckForUpdate(context: Context) {
    val client = OkHttpClient()
    val versionUrl = "https://raw.githubusercontent.com/manuel09/emusite/refs/heads/main/version.json"

    val versionRequest = Request.Builder().url(versionUrl).build()
    val versionResponse = client.newCall(versionRequest).execute()
    val versionBody = versionResponse.body?.string() ?: throw Exception("No response")
    val latestVersion = versionBody.trim().removeSurrounding("\"")

    val currentVersion = context.packageManager.getPackageInfo(context.packageName, 0).versionName
    if (latestVersion == currentVersion) throw Exception("App is up to date")

    val apkUrl = "https://raw.githubusercontent.com/manuel09/emusite/refs/heads/main/app-debug.apk"

    val apkRequest = Request.Builder().url(apkUrl).build()
    val apkResponse = client.newCall(apkRequest).execute()
    val apkBytes = apkResponse.body?.bytes() ?: throw Exception("Download failed")

    val updateDir = File(context.cacheDir, "updates")
    updateDir.mkdirs()
    val apkFile = File(updateDir, "emusite-update.apk")
    FileOutputStream(apkFile).use { it.write(apkBytes) }

    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apkFile)
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "application/vnd.android.package-archive")
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
    }
    context.startActivity(intent)
}
