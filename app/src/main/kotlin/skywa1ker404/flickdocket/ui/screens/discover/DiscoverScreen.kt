package skywa1ker404.flickdocket.ui.screens.discover

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import skywa1ker404.flickdocket.R
import skywa1ker404.flickdocket.domain.MediaType
import skywa1ker404.flickdocket.domain.MetadataError
import skywa1ker404.flickdocket.domain.MetadataMode
import skywa1ker404.flickdocket.domain.TmdbRef
import skywa1ker404.flickdocket.domain.RefSource
import skywa1ker404.flickdocket.ui.components.*

@Composable
fun DiscoverScreen(vm: DiscoverViewModel, scopes: SharedScopes, onOpen: (TmdbRef) -> Unit, onOpenSettings: () -> Unit) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    Column(Modifier.statusBarsPadding()) {
        SearchField(ui.query, vm::setQuery, stringResource(R.string.discover_search), Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp))
        PillRow(MediaType.entries, { it == ui.type }, { mediaTypeLabel(it) }, vm::setType)
        Spacer(Modifier.height(8.dp))
        when (ui.phase) {
            DiscoverPhase.NeedsKey -> EmptyState(Icons.Rounded.Explore, stringResource(R.string.discover_needs_key_title), stringResource(R.string.discover_needs_key_body),
                actionLabel = stringResource(R.string.open_settings), onAction = onOpenSettings)
            DiscoverPhase.Loading -> FullScreenLoader()
            DiscoverPhase.Error -> {
                val needsSettings = ui.error == MetadataError.Unauthorized || ui.error == MetadataError.Unsupported
                EmptyState(Icons.Rounded.CloudOff, ui.error?.let { metadataErrorText(it) }.orEmpty(), stringResource(R.string.err_hint),
                actionLabel = if (needsSettings) stringResource(R.string.open_settings) else stringResource(R.string.retry),
                onAction = if (needsSettings) onOpenSettings else vm::retry)
            }
            DiscoverPhase.Content -> if (ui.paged.items.isEmpty()) {
                EmptyState(Icons.Rounded.SearchOff, stringResource(R.string.discover_empty_title), stringResource(R.string.discover_empty_body))
            } else Results(ui, vm, scopes, onOpen)
        }
    }
}

@Composable
private fun Results(ui: DiscoverUi, vm: DiscoverViewModel, scopes: SharedScopes, onOpen: (MediaType, Long) -> Unit) {
    val state = rememberLazyGridState()
    val nearEnd by remember {
        derivedStateOf {
            val info = state.layoutInfo
            info.totalItemsCount > 0 && (info.visibleItemsInfo.lastOrNull()?.index ?: 0) >= info.totalItemsCount - 6
        }
    }
    LaunchedEffect(nearEnd, ui.paged.items.size) { if (nearEnd) vm.loadMore() }

    LazyVerticalGrid(
        state = state, columns = GridCells.Adaptive(120.dp),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column {
                SectionHeader(stringResource(if (ui.query.isNotBlank()) R.string.discover_results else if (ui.mode == MetadataMode.TMDB) R.string.discover_trending else R.string.discover_on_tv))
                if (ui.stale) Text(stringResource(R.string.stale_banner), Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.tertiary, style = MaterialTheme.typography.labelLarge)
            }
        }
        items(ui.paged.items, key = { tmdbPosterKey(it.ref) }) { s ->
            PosterTile(
                s.title, listOfNotNull(s.year?.toString(), s.rating?.let { "★ %.1f".format(it) }).joinToString(" · "), s.posterPath, null,
                { onOpen(s.ref) }, scopes, tmdbPosterKey(s.ref),
            )
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                if (ui.loadingMore) ShapeMorphLoader(Modifier.padding(16.dp), size = 40.dp)
                if (ui.moreFailed) TextButton(vm::loadMore) { Text(stringResource(R.string.load_more_failed)) }
                SourceAttribution(if (ui.mode == MetadataMode.TMDB) RefSource.TMDB else RefSource.TVMAZE)
            }
        }
    }
}

fun tmdbPosterKey(ref: TmdbRef) = "poster-${ref.source.name}-${ref.type.name}-${ref.id}"
