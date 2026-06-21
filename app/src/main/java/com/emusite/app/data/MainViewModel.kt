package com.emusite.app.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emusite.api.models.ContentType
import com.emusite.api.models.Episode
import com.emusite.api.models.HomePageSection
import com.emusite.api.models.MediaDetails
import com.emusite.api.models.SearchResult
import com.emusite.api.models.StreamLink
import com.emusite.app.plugin.LoadedPlugin
import com.emusite.app.plugin.PluginRepository
import com.emusite.app.repository.MediaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeState(
    val sections: List<HomePageSection> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

data class SearchState(
    val query: String = "",
    val results: List<SearchResult> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

data class DetailsState(
    val details: MediaDetails? = null,
    val episodes: List<Episode> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

data class PlayerState(
    val streams: List<StreamLink> = emptyList(),
    val selectedStream: StreamLink? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

data class PluginsState(
    val plugins: List<LoadedPlugin> = emptyList(),
    val repositories: List<PluginRepository> = emptyList(),
    val isLoading: Boolean = false,
    val repoMessage: String? = null
)

class MainViewModel : ViewModel() {

    private val app = EmusiteAppHolder.app
    private val pluginManager get() = app.pluginManager
    private val repository get() = app.mediaRepository

    private val _homeState = MutableStateFlow(HomeState())
    val homeState: StateFlow<HomeState> = _homeState.asStateFlow()

    private val _searchState = MutableStateFlow(SearchState())
    val searchState: StateFlow<SearchState> = _searchState.asStateFlow()

    private val _detailsState = MutableStateFlow(DetailsState())
    val detailsState: StateFlow<DetailsState> = _detailsState.asStateFlow()

    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    private val _pluginsState = MutableStateFlow(PluginsState())
    val pluginsState: StateFlow<PluginsState> = _pluginsState.asStateFlow()

    init {
        viewModelScope.launch {
            pluginManager.plugins.collect { plugins ->
                _pluginsState.value = _pluginsState.value.copy(plugins = plugins)
                if (plugins.isNotEmpty()) {
                    loadHomeFeed()
                }
            }
        }
        viewModelScope.launch {
            pluginManager.repositories.collect { repos ->
                _pluginsState.value = _pluginsState.value.copy(repositories = repos)
            }
        }
    }

    fun loadHomeFeed() {
        if (_homeState.value.sections.isNotEmpty() && !_homeState.value.isLoading) return
        viewModelScope.launch {
            _homeState.value = _homeState.value.copy(isLoading = true, error = null)
            try {
                val feed = repository.getHomeFeed()
                _homeState.value = HomeState(sections = feed)
            } catch (e: Exception) {
                _homeState.value = HomeState(error = e.message)
            }
        }
    }

    fun search(query: String, type: ContentType? = null) {
        viewModelScope.launch {
            _searchState.value = SearchState(query = query, isLoading = true)
            try {
                val results = if (type != null) {
                    repository.searchByType(query, type)
                } else {
                    repository.search(query)
                }
                _searchState.value = SearchState(query = query, results = results)
            } catch (e: Exception) {
                _searchState.value = SearchState(query = query, error = e.message)
            }
        }
    }

    fun clearSearch() {
        _searchState.value = SearchState()
    }

    fun loadDetails(sourceId: String, url: String) {
        viewModelScope.launch {
            _detailsState.value = DetailsState(isLoading = true)
            try {
                val details = repository.getDetails(sourceId, url)
                val episodes = details?.let { repository.getEpisodes(sourceId, url) } ?: emptyList()
                _detailsState.value = DetailsState(details = details, episodes = episodes)
            } catch (e: Exception) {
                _detailsState.value = DetailsState(error = e.message)
            }
        }
    }

    fun loadStreamLinks(sourceId: String, url: String) {
        viewModelScope.launch {
            _playerState.value = PlayerState(isLoading = true)
            try {
                val streams = repository.getStreamLinks(sourceId, url)
                _playerState.value = PlayerState(
                    streams = streams,
                    selectedStream = streams.firstOrNull()
                )
            } catch (e: Exception) {
                _playerState.value = PlayerState(error = e.message)
            }
        }
    }

    fun selectStream(stream: StreamLink) {
        _playerState.value = _playerState.value.copy(selectedStream = stream)
    }

    fun togglePlugin(pluginId: String, enabled: Boolean) {
        app.pluginManager.togglePlugin(pluginId, enabled)
    }

    suspend fun uninstallPlugin(pluginId: String) {
        app.pluginManager.uninstallPlugin(pluginId)
    }

    fun fetchRepository(url: String) {
        viewModelScope.launch {
            _pluginsState.value = _pluginsState.value.copy(isLoading = true, repoMessage = null)
            try {
                val repo = app.pluginManager.fetchRepository(url)
                if (repo != null) {
                    _pluginsState.value = _pluginsState.value.copy(
                        repoMessage = "Repository loaded: ${repo.name}"
                    )
                } else {
                    _pluginsState.value = _pluginsState.value.copy(
                        repoMessage = "Failed to load repository"
                    )
                }
            } catch (e: Exception) {
                _pluginsState.value = _pluginsState.value.copy(
                    repoMessage = "Error: ${e.message}"
                )
            } finally {
                _pluginsState.value = _pluginsState.value.copy(isLoading = false)
            }
        }
    }

    fun downloadAndInstallPlugin(url: String) {
        viewModelScope.launch {
            _pluginsState.value = _pluginsState.value.copy(isLoading = true, repoMessage = "Downloading...")
            try {
                val result = app.pluginManager.downloadAndInstallPlugin(url)
                result.fold(
                    onSuccess = { msg ->
                        _pluginsState.value = _pluginsState.value.copy(repoMessage = msg)
                    },
                    onFailure = { e ->
                        _pluginsState.value = _pluginsState.value.copy(
                            repoMessage = "Install failed: ${e.message}"
                        )
                    }
                )
            } catch (e: Exception) {
                _pluginsState.value = _pluginsState.value.copy(
                    repoMessage = "Error: ${e.message}"
                )
            } finally {
                _pluginsState.value = _pluginsState.value.copy(isLoading = false)
            }
        }
    }

    fun clearRepoMessage() {
        _pluginsState.value = _pluginsState.value.copy(repoMessage = null)
    }

    fun removeRepository(url: String) {
        app.pluginManager.removeRepository(url)
    }

    fun refreshAllRepos() {
        viewModelScope.launch {
            _pluginsState.value = _pluginsState.value.copy(isLoading = true, repoMessage = "Refreshing...")
            val count = app.pluginManager.refreshAllRepos()
            _pluginsState.value = _pluginsState.value.copy(
                isLoading = false,
                repoMessage = "$count repositories refreshed"
            )
        }
    }
}
