package com.emusite.app.plugin

import android.content.Context
import com.emusite.api.Plugin
import com.emusite.api.Source
import com.emusite.api.models.ContentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

data class LoadedPlugin(
    val plugin: Plugin,
    val isEnabled: Boolean = true,
    val filePath: String? = null
)

@Serializable
data class PluginRepository(
    val name: String,
    val description: String = "",
    val url: String,
    val plugins: List<RepositoryPlugin> = emptyList()
)

@Serializable
data class RepositoryPlugin(
    val name: String,
    val version: String,
    val internalName: String,
    val description: String = "",
    val iconUrl: String? = null,
    val url: String
)

class PluginManager(private val context: Context) {

    private val loader = PluginLoader(context)
    private val pluginsDir = File(context.filesDir, "plugins").also { it.mkdirs() }
    private val client = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }
    private val prefs = context.getSharedPreferences("emusite_repos", Context.MODE_PRIVATE)

    private val _plugins = MutableStateFlow<List<LoadedPlugin>>(emptyList())
    val plugins: StateFlow<List<LoadedPlugin>> = _plugins.asStateFlow()

    private val _repositories = MutableStateFlow<List<PluginRepository>>(emptyList())
    val repositories: StateFlow<List<PluginRepository>> = _repositories.asStateFlow()

    suspend fun initialize() = withContext(Dispatchers.IO) {
        val loaded = pluginsDir.listFiles()
            ?.filter { it.extension == "emuplugin" }
            ?.mapNotNull { file ->
                try {
                    file.setReadOnly()
                    val plugin = loader.loadPlugin(file.absolutePath)
                    LoadedPlugin(plugin, filePath = file.absolutePath)
                } catch (e: Exception) {
                    null
                }
            }
            ?: emptyList()
        _plugins.value = loaded

        val savedUrls = prefs.getStringSet("repo_urls", emptySet()) ?: emptySet()
        savedUrls.forEach { url ->
            try {
                fetchRepository(url)
            } catch (_: Exception) {}
        }
    }

    fun getSources(): List<Source> {
        return _plugins.value
            .filter { it.isEnabled }
            .flatMap { it.plugin.getSources() }
    }

    fun getSourcesByType(type: ContentType): List<Source> {
        return getSources().filter { it.type == type }
    }

    suspend fun fetchRepository(repoUrl: String): PluginRepository? = withContext(Dispatchers.IO) {
        try {
            val url = if (repoUrl.startsWith("emusiterepo://")) {
                "https://" + repoUrl.removePrefix("emusiterepo://")
            } else repoUrl

            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext null
            val repo = json.decodeFromString<PluginRepository>(body)

            _repositories.value = _repositories.value.filter { it.url != repo.url } + repo
            saveRepoUrl(repoUrl)
            repo
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun downloadAndInstallPlugin(downloadUrl: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(downloadUrl).build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("HTTP ${response.code}"))
            }
            val bytes = response.body?.bytes()
                ?: return@withContext Result.failure(Exception("Empty response body"))

            val urlFileName = downloadUrl.substringAfterLast("/").substringBefore("?")
            val filename = if (urlFileName.endsWith(".emuplugin")) urlFileName
                else urlFileName.substringBeforeLast(".") + ".emuplugin"

            val targetFile = File(pluginsDir, filename)
            FileOutputStream(targetFile).use { it.write(bytes) }
            targetFile.setReadOnly()

            val plugin = loader.loadPlugin(targetFile.absolutePath)
            _plugins.value = _plugins.value.filter { it.plugin.id != plugin.id } +
                    LoadedPlugin(plugin, filePath = targetFile.absolutePath)
            Result.success("${plugin.name} v${plugin.version} installed")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun installFromFile(file: File): Boolean = withContext(Dispatchers.IO) {
        try {
            val targetFile = File(pluginsDir, file.name)
            file.copyTo(targetFile, overwrite = true)
            targetFile.setReadOnly()
            val plugin = loader.loadPlugin(targetFile.absolutePath)
            _plugins.value = _plugins.value.filter { it.plugin.id != plugin.id } +
                    LoadedPlugin(plugin, filePath = targetFile.absolutePath)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun uninstallPlugin(pluginId: String): Boolean = withContext(Dispatchers.IO) {
        val entry = _plugins.value.find { it.plugin.id == pluginId } ?: return@withContext false
        entry.filePath?.let { path ->
            try { File(path).delete() } catch (_: Exception) {}
        }
        _plugins.value = _plugins.value.filter { it.plugin.id != pluginId }
        true
    }

    fun togglePlugin(pluginId: String, enabled: Boolean) {
        _plugins.value = _plugins.value.map {
            if (it.plugin.id == pluginId) it.copy(isEnabled = enabled) else it
        }
    }

    fun getSourceById(sourceId: String): Source? {
        return getSources().find { it.id == sourceId }
    }

    fun removeRepository(repoUrl: String) {
        _repositories.value = _repositories.value.filter { it.url != repoUrl }
        val urls = (prefs.getStringSet("repo_urls", emptySet()) ?: emptySet()).toMutableSet()
        urls.remove(repoUrl)
        prefs.edit().putStringSet("repo_urls", urls).apply()
    }

    suspend fun refreshAllRepos(): Int = withContext(Dispatchers.IO) {
        val urls = prefs.getStringSet("repo_urls", emptySet()) ?: emptySet()
        var count = 0
        urls.forEach { url ->
            if (fetchRepository(url) != null) count++
        }
        count
    }

    private fun saveRepoUrl(repoUrl: String) {
        val urls = (prefs.getStringSet("repo_urls", emptySet()) ?: emptySet()).toMutableSet()
        urls.add(repoUrl)
        prefs.edit().putStringSet("repo_urls", urls).apply()
    }
}
