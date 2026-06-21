package com.emusite.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
fun BrowseSectionScreen(
    sectionIndex: Int,
    onBack: () -> Unit,
    onItemClick: (sourceId: String, url: String) -> Unit,
    viewModel: MainViewModel = viewModel()
) {
    val state by viewModel.homeState.collectAsStateWithLifecycle()
    val section = state.sections.getOrNull(sectionIndex)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(section?.name ?: "Browse", style = MaterialTheme.typography.titleSmall) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                modifier = Modifier.height(40.dp)
            )
        }
    ) { padding ->
        if (section == null || section.items.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                Text("No items")
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(130.dp),
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize().padding(padding)
            ) {
                items(section.items, key = { "${it.sourceId}-${it.id}" }) { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onItemClick(item.sourceId, item.id) }
                    ) {
                        Column {
                            AsyncImage(
                                model = item.posterUrl,
                                contentDescription = item.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(195.dp)
                            )
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                item.year?.let {
                                    Text(
                                        text = it.toString(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
