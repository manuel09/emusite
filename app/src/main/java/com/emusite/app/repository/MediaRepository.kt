package com.emusite.app.repository

import com.emusite.api.models.ContentType
import com.emusite.api.models.Episode
import com.emusite.api.models.HomePageSection
import com.emusite.api.models.MediaDetails
import com.emusite.api.models.SearchResult
import com.emusite.api.models.StreamLink
import com.emusite.app.plugin.PluginManager

class MediaRepository(private val pluginManager: PluginManager) {

    suspend fun getHomeFeed(): List<HomePageSection> {
        val sources = pluginManager.getSources()
        if (sources.isEmpty()) return emptyList()

        return sources.flatMap { source ->
            try {
                source.getHomePageSections()
            } catch (e: AbstractMethodError) {
                try {
                    val items = source.getHomePage()
                    listOf(HomePageSection(name = "Home", items = items))
                } catch (e2: Exception) { emptyList() }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    suspend fun search(query: String, page: Int = 1): List<SearchResult> {
        val sources = pluginManager.getSources()
        if (sources.isEmpty() || query.isBlank()) return emptyList()

        return sources.flatMap { source ->
            try {
                source.search(query, page).map { it.copy(sourceId = source.id) }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    suspend fun searchByType(query: String, type: ContentType, page: Int = 1): List<SearchResult> {
        val sources = pluginManager.getSourcesByType(type)
        if (sources.isEmpty() || query.isBlank()) return emptyList()

        return sources.flatMap { source ->
            try {
                source.search(query, page).map { it.copy(sourceId = source.id) }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    suspend fun getDetails(sourceId: String, url: String): MediaDetails? {
        val source = pluginManager.getSourceById(sourceId) ?: return null
        return try {
            source.getDetails(url)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getEpisodes(sourceId: String, url: String): List<Episode> {
        val source = pluginManager.getSourceById(sourceId) ?: return emptyList()
        return try {
            source.getEpisodes(url)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getStreamLinks(sourceId: String, url: String): List<StreamLink> {
        val source = pluginManager.getSourceById(sourceId) ?: return emptyList()
        return try {
            source.getStreamLinks(url)
        } catch (e: Exception) {
            emptyList()
        }
    }
}
