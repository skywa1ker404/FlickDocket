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
import skywa1ker404.flickdocket.domain.TmdbSummary
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
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
private fun Results(ui: DiscoverUi, vm: DiscoverViewModel, scopes: SharedScopes, onOpen: (TmdbRef) -> Unit) {
    val state = rememberLazyGridState()
    val nearEnd by remember {
        derivedStateOf {
            val info = state.layoutInfo
            info.totalItemsCount > 0 && (info.visibleItemsInfo.lastOrNull()?.index ?: 0) >= info.totalItemsCount - 6
        }
    }
    LaunchedEffect(nearEnd, ui.paged.items.size) { if (nearEnd) vm.loadMore() }
    // Only the tile that was just opened takes part in the poster transition; every tile doing so makes the grid slow.
    var openedKey by rememberSaveable { mutableStateOf<String?>(null) }
    val browsing = ui.query.isBlank()
    val hero = if (browsing) ui.paged.items.firstOrNull() else null
    val rest = if (hero != null) ui.paged.items.drop(1) else ui.paged.items

    LazyVerticalGrid(
        state = state, columns = GridCells.Adaptive(156.dp),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (hero != null) item(key = "hero", span = { GridItemSpan(maxLineSpan) }, contentType = "hero") {
            val key = tmdbPosterKey(hero.ref)
            HeroCard(hero, { openedKey = key; onOpen(hero.ref) }, scopes, key.takeIf { it == openedKey })
        }
        item(key = "header", span = { GridItemSpan(maxLineSpan) }, contentType = "header") {
            Column {
                SectionHeader(stringResource(if (!browsing) R.string.discover_results else if (ui.mode == MetadataMode.TMDB) R.string.discover_trending else R.string.discover_popular))
                if (ui.stale) Text(stringResource(R.string.stale_banner), color = MaterialTheme.colorScheme.tertiary, style = MaterialTheme.typography.labelLarge)
            }
        }
        items(rest, key = { tmdbPosterKey(it.ref) }, contentType = { "poster" }) { s ->
            val key = tmdbPosterKey(s.ref)
            val subtitle = remember(s.year, s.rating) { listOfNotNull(s.year?.toString(), s.rating?.let { "★ %.1f".format(it) }).joinToString(" · ") }
            PosterTile(s.title, subtitle, s.posterPath, null, { openedKey = key; onOpen(s.ref) }, scopes, key.takeIf { it == openedKey })
        }
        item(key = "footer", span = { GridItemSpan(maxLineSpan) }, contentType = "footer") {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                if (ui.loadingMore) ShapeMorphLoader(Modifier.padding(16.dp), size = 40.dp)
                if (ui.moreFailed) TextButton(vm::loadMore) { Text(stringResource(R.string.load_more_failed)) }
                SourceAttribution(if (ui.mode == MetadataMode.TMDB) RefSource.TMDB else RefSource.CINEMETA)
                if (ui.mode == MetadataMode.OPEN) SourceAttribution(RefSource.TVMAZE)
            }
        }
    }
}

/** Large lead card for the top result: backdrop (or poster), scrim, pill, title and one info line. */
@Composable
private fun HeroCard(s: TmdbSummary, onClick: () -> Unit, scopes: SharedScopes, sharedKey: String?) {
    val shape = RoundedCornerShape(28.dp)
    val openLabel = stringResource(R.string.open_details)
    Box(
        Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(shape).background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClickLabel = openLabel, role = Role.Button, onClick = onClick),
    ) {
        if (s.backdropPath != null) BackdropImage(s.backdropPath, Modifier.fillMaxSize())
        else PosterImage(s.posterPath, s.title, Modifier.fillMaxSize().then(if (sharedKey != null) Modifier.posterSharedElement(sharedKey, scopes) else Modifier), corner = 28.dp)
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.78f)))))
        Text(
            stringResource(R.string.top_pick), Modifier.align(Alignment.TopStart).padding(14.dp).clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.primaryContainer).padding(horizontal = 12.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        Column(Modifier.align(Alignment.BottomStart).padding(16.dp)) {
            Text(s.title, style = MaterialTheme.typography.headlineSmall, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
            val info = remember(s.year, s.rating) { listOfNotNull(s.year?.toString(), s.rating?.let { "★ %.1f".format(it) }).joinToString(" · ") }
            if (info.isNotBlank()) Text(info, style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = 0.85f))
        }
    }
}

fun tmdbPosterKey(ref: TmdbRef) = "poster-${ref.source.name}-${ref.type.name}-${ref.id}"
