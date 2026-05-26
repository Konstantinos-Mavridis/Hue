package com.hue.feature.history.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.hue.core.color.model.Season
import com.hue.core.design.components.SeasonBadge
import com.hue.core.design.components.SeasonUi
import com.hue.domain.model.FabricAnalysis
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onScanSelected: (Long) -> Unit,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            Column {
                TopAppBar(title = { Text("History") })
                // Search bar
                SearchBar(
                    query = state.searchQuery,
                    onQueryChange = { viewModel.setSearch(it) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
                // Season filter chips
                SeasonFilterRow(
                    selected = state.filterSeason,
                    onSelect = { viewModel.setFilter(it) }
                )
            }
        }
    ) { padding ->
        when {
            state.isLoading -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            state.filteredScans.isEmpty() -> EmptyState(
                hasFilter = state.filterSeason != null || state.searchQuery.isNotBlank(),
                modifier = Modifier.fillMaxSize().padding(padding)
            )

            else -> LazyColumn(
                contentPadding = PaddingValues(
                    top = padding.calculateTopPadding() + 8.dp,
                    bottom = padding.calculateBottomPadding() + 8.dp
                ),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                items(state.filteredScans, key = { it.id }) { scan ->
                    HistoryItem(
                        scan = scan,
                        onClick = { onScanSelected(scan.id) },
                        onDelete = { viewModel.delete(scan.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchBar(query: String, onQueryChange: (String) -> Unit, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text("Search by colour or name…") },
        leadingIcon = { Icon(Icons.Default.Search, null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Clear, "Clear search")
                }
            }
        },
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(50)
    )
}

@Composable
private fun SeasonFilterRow(selected: Season?, onSelect: (Season?) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FilterChip(
                selected = selected == null,
                onClick = { onSelect(null) },
                label = { Text("All") }
            )
        }
        items(Season.values().toList()) { season ->
            FilterChip(
                selected = selected == season,
                onClick = { onSelect(if (selected == season) null else season) },
                label = { Text(season.displayName) },
                leadingIcon = {
                    if (selected == season) Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                }
            )
        }
    }
}

@Composable
private fun HistoryItem(
    scan: FabricAnalysis,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val dominantColor = Color(
        android.graphics.Color.rgb(scan.dominantRgb.r, scan.dominantRgb.g, scan.dominantRgb.b)
    )
    val locale = LocalLocale.current.platformLocale
    val dateStr = SimpleDateFormat("MMM d, yyyy · HH:mm", locale)
        .format(Date(scan.timestamp))
    val bestMatch = scan.topMatches.firstOrNull()

    ListItem(
        headlineContent = {
            Text(
                bestMatch?.color?.name ?: "Unknown colour",
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                bestMatch?.let { Text(it.color.code, style = MaterialTheme.typography.bodySmall,
                     color = MaterialTheme.colorScheme.onSurfaceVariant) }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    SeasonBadge(season = scan.season.primarySeason.toUi(), showIcon = false)
                    Text(dateStr, style = MaterialTheme.typography.bodySmall,
                         color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(dominantColor)
            ) {
                AsyncImage(
                    model = scan.thumbnailPath,
                    contentDescription = "Fabric thumbnail",
                    modifier = Modifier.fillMaxSize()
                )
            }
        },
        trailingContent = {
            IconButton(onClick = { showDeleteConfirm = true }) {
                Icon(Icons.Default.DeleteOutline, "Delete",
                     tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        modifier = Modifier
            .clickable { onClick() }
            .background(MaterialTheme.colorScheme.surface)
    )

    HorizontalDivider(modifier = Modifier.padding(start = 72.dp))

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete scan?") },
            text = { Text("This will permanently remove this colour analysis from your history.") },
            confirmButton = {
                TextButton(onClick = { showDeleteConfirm = false; onDelete() }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun EmptyState(hasFilter: Boolean, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.History, null, modifier = Modifier.size(64.dp),
             tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        Text(
            if (hasFilter) "No scans match your filter" else "No saved scans yet",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.height(8.dp))
        Text(
            if (hasFilter) "Try clearing your filter or search." else "Scan a fabric to see your history here.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

private fun Season.toUi() = when (this) {
    Season.SPRING -> SeasonUi.SPRING
    Season.SUMMER -> SeasonUi.SUMMER
    Season.AUTUMN -> SeasonUi.AUTUMN
    Season.WINTER -> SeasonUi.WINTER
}
