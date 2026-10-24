@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package skywa1ker404.flickdocket.ui.screens.library

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import skywa1ker404.flickdocket.R
import skywa1ker404.flickdocket.domain.*
import skywa1ker404.flickdocket.ui.components.*
import skywa1ker404.flickdocket.ui.screens.add.AddTitleSheet

@Composable
fun LibraryScreen(vm: LibraryViewModel, scopes: SharedScopes, onOpen: (Long) -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    var showAdd by rememberSaveable { mutableStateOf(false) }
    var showFilters by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = { showAdd = true }, icon = { Icon(Icons.Rounded.Add, null) }, text = { Text(stringResource(R.string.add_title)) })
        },
        contentWindowInsets = WindowInsets.statusBars,
    ) { inner ->
        when (val s = state) {
            LibraryUiState.Loading -> FullScreenLoader(Modifier.padding(inner))
            is LibraryUiState.Content -> Column(Modifier.padding(inner)) {
                SearchRow(s.query.text, vm::setText, onFilters = { showFilters = true })
                PillRow(
                    options = listOf<MediaType?>(null, MediaType.MOVIE, MediaType.TV),
                    selected = { it == s.query.mediaType },
                    label = { if (it == null) stringResource(R.string.filter_all, s.totalCount) else mediaTypeLabel(it) },
                    onSelect = vm::setMediaType,
                )
                Spacer(Modifier.height(4.dp))
                PillRow(WatchStatus.entries, { it in s.query.statuses }, { statusLabel(it) + (s.statusCounts[it]?.let { c -> " $c" } ?: "") }, vm::toggleStatus)
                Spacer(Modifier.height(8.dp))
                when {
                    s.totalCount == 0 -> EmptyState(Icons.Rounded.Movie, stringResource(R.string.empty_library_title), stringResource(R.string.empty_library_body),
                        actionLabel = stringResource(R.string.add_title), onAction = { showAdd = true })
                    s.items.isEmpty() -> EmptyState(Icons.Rounded.SearchOff, stringResource(R.string.empty_search_title), stringResource(R.string.empty_search_body),
                        shape = MaterialShapes.Sunny, actionLabel = stringResource(R.string.clear_filters), onAction = vm::clearFilters)
                    else -> LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 120.dp),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        item(span = { GridItemSpan(maxLineSpan) }) { SectionHeader(stringResource(R.string.nav_library)) }
                        items(s.items, key = { it.titleId }) { PosterCard(it, { onOpen(it.titleId) }, scopes) }
                    }
                }
            }
        }
    }

    if (showAdd) AddTitleSheet(onDismiss = { showAdd = false }, onSave = { draft -> vm.add(draft); showAdd = false })
    if (showFilters && state is LibraryUiState.Content) {
        FilterSheet((state as LibraryUiState.Content).query, vm, onDismiss = { showFilters = false })
    }
}

@Composable
private fun SearchRow(text: String, onText: (String) -> Unit, onFilters: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = text, onValueChange = onText, modifier = Modifier.weight(1f), singleLine = true, shape = RoundedCornerShape(50),
            placeholder = { Text(stringResource(R.string.search_library)) },
            leadingIcon = { Icon(Icons.Rounded.Search, null) },
            trailingIcon = if (text.isNotEmpty()) { { IconButton({ onText("") }) { Icon(Icons.Rounded.Close, stringResource(R.string.clear)) } } } else null,
        )
        Spacer(Modifier.width(8.dp))
        FilledTonalIconButton(onFilters, Modifier.size(48.dp)) { Icon(Icons.Rounded.Tune, stringResource(R.string.filters)) }
    }
}

@Composable
private fun FilterSheet(q: LibraryQuery, vm: LibraryViewModel, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionHeader(stringResource(R.string.sort_by))
            PillRow(LibrarySort.entries, { it == q.sort }, { sortLabel(it) }, vm::setSort)
            SectionHeader(stringResource(R.string.filters))
            Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.favorites_only), Modifier.weight(1f))
                Switch(q.favoritesOnly, vm::setFavoritesOnly)
            }
            TextButton(vm::clearFilters, Modifier.padding(horizontal = 8.dp)) { Text(stringResource(R.string.clear_filters)) }
        }
    }
}

@Composable
private fun sortLabel(s: LibrarySort) = stringResource(
    when (s) {
        LibrarySort.RECENTLY_UPDATED -> R.string.sort_recent
        LibrarySort.TITLE -> R.string.sort_title
        LibrarySort.RATING -> R.string.sort_rating
        LibrarySort.RELEASE -> R.string.sort_release
    }
)
