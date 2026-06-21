package com.emusite.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.emusite.app.data.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsScreen(
    sourceId: String,
    url: String,
    onBack: () -> Unit,
    onEpisodeClick: (sourceId: String, episodeUrl: String, title: String) -> Unit,
    viewModel: MainViewModel = viewModel()
) {
    val state by viewModel.detailsState.collectAsStateWithLifecycle()

    LaunchedEffect(sourceId, url) {
        viewModel.loadDetails(sourceId, url)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        state.details?.title ?: "Details",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                modifier = Modifier.height(40.dp)
            )
        }
    ) { padding ->
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            state.error != null -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(state.error ?: "Error")
                }
            }
            state.details != null -> {
                val details = state.details!!
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    item {
                        AsyncImage(
                            model = details.backdropUrl ?: details.posterUrl,
                            contentDescription = details.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                        )
                    }

                    item {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        details.title,
                                        style = MaterialTheme.typography.headlineMedium
                                    )
                                    if (details.year != null) {
                                        Text(
                                            "${details.year} - ${details.type.name}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (details.rating != null) {
                                        Text(
                                            "Rating: ${details.rating}",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }

                            if (details.genres.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(details.genres) { genre ->
                                        AssistChip(onClick = {}, label = { Text(genre) })
                                    }
                                }
                            }

                            if (details.description != null) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    details.description ?: "",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }

                            if (state.episodes.isEmpty()) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { onEpisodeClick(sourceId, url, details.title) },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        modifier = Modifier.padding(end = 8.dp)
                                    )
                                    Text("Play")
                                }
                            }
                        }
                    }

                    if (state.episodes.isNotEmpty()) {
                        item {
                            Text(
                                "Episodes",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }

                        items(state.episodes) { episode ->
                            val title = if (episode.season != null && episode.episode != null) {
                                "S${episode.season}E${episode.episode} - ${episode.title}"
                            } else {
                                episode.title
                            }

                            ListItem(
                                headlineContent = { Text(title, maxLines = 2, overflow = TextOverflow.Ellipsis) },
                                leadingContent = {
                                    if (episode.thumbnailUrl != null) {
                                        AsyncImage(
                                            model = episode.thumbnailUrl,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .width(100.dp)
                                                .height(56.dp)
                                        )
                                    }
                                },
                                trailingContent = {
                                    Icon(Icons.Default.PlayArrow, contentDescription = "Play")
                                },
                                modifier = Modifier.clickable {
                                    onEpisodeClick(sourceId, episode.url, title)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
