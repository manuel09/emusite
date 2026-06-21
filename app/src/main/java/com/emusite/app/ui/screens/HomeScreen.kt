package com.emusite.app.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.emusite.app.data.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onItemClick: (sourceId: String, url: String) -> Unit,
    onSectionClick: (sectionIndex: Int) -> Unit,
    viewModel: MainViewModel = viewModel()
) {
    val state by viewModel.homeState.collectAsStateWithLifecycle()
    val pluginsState by viewModel.pluginsState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.loadHomeFeed()
    }

    LaunchedEffect(pluginsState.plugins) {
        if (pluginsState.plugins.isNotEmpty() && state.sections.isEmpty()) {
            viewModel.loadHomeFeed()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Emusite",
                        style = MaterialTheme.typography.titleSmall
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.height(40.dp)
            )
        }
    ) { padding ->
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }
            }
            state.error != null -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(state.error ?: "Error", color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadHomeFeed() }) { Text("Retry") }
                    }
                }
            }
            state.sections.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No content available", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Install plugins to get started",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .focusGroup(),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    state.sections.forEachIndexed { index, section ->
                        if (section.items.isNotEmpty()) {
                            item {
                                var headerFocused by remember { mutableStateOf(false) }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onSectionClick(index) }
                                        .onFocusChanged { headerFocused = it.isFocused }
                                        .focusable()
                                        .then(
                                            if (headerFocused) Modifier.border(
                                                1.dp, MaterialTheme.colorScheme.primary,
                                                RoundedCornerShape(4.dp)
                                            ) else Modifier
                                        )
                                        .padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        section.name,
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    IconButton(onClick = { onSectionClick(index) }) {
                                        Icon(
                                            Icons.Default.ChevronRight,
                                            contentDescription = "See all",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                            item {
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(section.items, key = { "${it.sourceId}-${it.id}" }) { item ->
                                        var isFocused by remember { mutableStateOf(false) }
                                        Card(
                                            modifier = Modifier
                                                .width(130.dp)
                                                .clickable { onItemClick(item.sourceId, item.id) }
                                                .onFocusChanged { isFocused = it.isFocused }
                                                .focusable()
                                                .then(
                                                    if (isFocused) Modifier.border(
                                                        2.dp,
                                                        MaterialTheme.colorScheme.primary,
                                                        RoundedCornerShape(8.dp)
                                                    ) else Modifier
                                                )
                                        ) {
                                            Column {
                                                AsyncImage(
                                                    model = item.posterUrl,
                                                    contentDescription = item.title,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier
                                                        .width(130.dp)
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
                }
            }
        }
    }
}
