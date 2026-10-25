@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package skywa1ker404.flickdocket.ui.screens.remote

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import skywa1ker404.flickdocket.R
import skywa1ker404.flickdocket.domain.*
import skywa1ker404.flickdocket.ui.components.*
import skywa1ker404.flickdocket.ui.screens.discover.tmdbPosterKey
import androidx.compose.ui.platform.LocalUriHandler
import java.util.Locale

@Composable
fun RemoteDetailScreen(
    ref: TmdbRef, vm: RemoteDetailViewModel, scopes: SharedScopes,
    onBack: () -> Unit, onOpenLibrary: (Long) -> Unit, onOpenSettings: () -> Unit,
) {
    LaunchedEffect(ref) { vm.load(ref) }
    val ui by vm.ui.collectAsStateWithLifecycle()
    val libraryId by vm.libraryId.collectAsStateWithLifecycle()

    Scaffold(topBar = {
        TopAppBar(title = {}, navigationIcon = { IconButton(onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back)) } })
    }) { inner ->
        when (val r = ui.detail) {
            null -> FullScreenLoader(Modifier.padding(inner))
            is MetadataResult.Failure -> Box(Modifier.padding(inner)) {
                val needsSettings = r.error == MetadataError.MissingCredentials || r.error == MetadataError.Unauthorized
                EmptyState(Icons.Rounded.CloudOff, metadataErrorText(r.error), stringResource(R.string.err_hint),
                    actionLabel = stringResource(if (needsSettings) R.string.open_settings else R.string.retry),
                    onAction = if (needsSettings) onOpenSettings else vm::retry)
            }
            is MetadataResult.Ok -> Content(r.value, r.stale, ui, libraryId, vm, scopes, onOpenLibrary, Modifier.padding(inner))
        }
    }
}

@Composable
private fun Content(
    d: TmdbDetail, stale: Boolean, ui: RemoteDetailUi, libraryId: Long?, vm: RemoteDetailViewModel,
    scopes: SharedScopes, onOpenLibrary: (Long) -> Unit, modifier: Modifier,
) {
    val pad = Modifier.padding(horizontal = 16.dp)
    Column(modifier.verticalScroll(rememberScrollState()).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Box {
            BackdropImage(d.backdropPath, Modifier.fillMaxWidth().aspectRatio(16f / 9f))
            Row(pad.align(Alignment.BottomStart), verticalAlignment = Alignment.Bottom) {
                PosterImage(d.posterPath, d.title, Modifier.width(110.dp).aspectRatio(2f / 3f).posterSharedElement(tmdbPosterKey(d.ref), scopes))
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(d.title, style = MaterialTheme.typography.headlineSmall)
                    Text(
                        listOfNotNull(mediaTypeLabel(d.ref.type), d.year?.toString(), d.runtimeMinutes?.let { stringResource(R.string.minutes_short, it) }, d.certification).joinToString(" · "),
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        if (stale) Text(stringResource(R.string.stale_banner), pad, color = MaterialTheme.colorScheme.tertiary, style = MaterialTheme.typography.labelLarge)

        LibraryCard(d, libraryId, ui.adding, vm, onOpenLibrary, pad)

        d.tagline?.let { Text(it, pad, fontStyle = FontStyle.Italic, style = MaterialTheme.typography.bodyLarge) }
        d.overview?.let { Text(it, pad, style = MaterialTheme.typography.bodyMedium) }
        if (d.genres.isNotEmpty()) FlowRow(pad, horizontalArrangement = Arrangement.spacedBy(8.dp)) { d.genres.forEach { SuggestionChip({}, { Text(it) }, enabled = false) } }

        Facts(d, pad)
        if (d.releases.isNotEmpty()) Releases(d.releases)
        if (d.cast.isNotEmpty()) {
            SectionHeader(stringResource(R.string.cast))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(horizontal = 16.dp)) {
                items(d.cast, key = { it.id.toString() + it.character }) { c ->
                    Column(Modifier.width(88.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        ProfileImage(c.profilePath, c.name, Modifier.size(72.dp))
                        Text(c.name, style = MaterialTheme.typography.labelMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        c.character?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis) }
                    }
                }
            }
        }
        if (d.crew.isNotEmpty()) {
            SectionHeader(stringResource(R.string.crew))
            Column(pad) { d.crew.forEach { Text("${it.job}: ${it.name}", style = MaterialTheme.typography.bodyMedium) } }
        }
        ExternalIdsBlock(d.externalIds, pad)
        if (d.seasons.isNotEmpty()) Seasons(d, ui, vm)
        val uri = LocalUriHandler.current
        d.sourceUrl?.let { url -> TextButton({ uri.openUri(url) }, pad) { Text(stringResource(R.string.view_on_tvmaze)) } }
        SourceAttribution(d.ref.source)
    }
}

@Composable
private fun LibraryCard(d: TmdbDetail, libraryId: Long?, adding: Boolean, vm: RemoteDetailViewModel, onOpen: (Long) -> Unit, modifier: Modifier) {
    var status by rememberSaveable { mutableStateOf(WatchStatus.PLANNING) }
    ElevatedCard(modifier.fillMaxWidth()) {
        Column(Modifier.padding(vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (libraryId != null) {
                Text(stringResource(R.string.in_library), Modifier.padding(horizontal = 16.dp), style = MaterialTheme.typography.titleMedium)
                Button({ onOpen(libraryId) }, Modifier.padding(horizontal = 16.dp)) { Text(stringResource(R.string.open_in_library)) }
            } else {
                PillRow(WatchStatus.entries, { it == status }, { statusLabel(it) }, { status = it })
                Button({ vm.addToLibrary(status) }, Modifier.padding(horizontal = 16.dp), enabled = !adding) { Text(stringResource(R.string.add_to_library)) }
            }
        }
    }
}

@Composable
private fun Facts(d: TmdbDetail, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        d.rating?.let { Fact(stringResource(R.string.label_rating), "★ %.1f / 10".format(it)) }
        d.status?.let { Fact(stringResource(R.string.label_status), it) }
        d.firstAirDate?.let { Fact(stringResource(R.string.first_aired), it) }
        d.lastAirDate?.let { Fact(stringResource(R.string.last_aired), it) }
        d.totalEpisodes?.let { Fact(stringResource(R.string.field_episodes), it.toString()) }
    }
}

@Composable private fun Fact(label: String, value: String) {
    Row { Text(label, Modifier.width(120.dp), color = MaterialTheme.colorScheme.onSurfaceVariant); Text(value) }
}

@Composable
private fun kindLabel(k: ReleaseKind) = stringResource(
    when (k) {
        ReleaseKind.PREMIERE -> R.string.kind_premiere; ReleaseKind.THEATRICAL_LIMITED -> R.string.kind_limited
        ReleaseKind.THEATRICAL -> R.string.kind_theatrical; ReleaseKind.DIGITAL -> R.string.kind_digital
        ReleaseKind.PHYSICAL -> R.string.kind_physical; ReleaseKind.TV -> R.string.kind_tv
    }
)

@Composable
private fun Releases(all: List<ReleaseInfo>) {
    val region = remember { Locale.getDefault().country.ifBlank { "US" } }
    val shown = all.filter { it.country == region }.ifEmpty { all.filter { it.country == "US" } }.take(6)
    if (shown.isEmpty()) return
    SectionHeader(stringResource(R.string.release_dates))
    Column(Modifier.padding(horizontal = 16.dp)) {
        shown.forEach { Text("${it.date ?: "?"} · ${kindLabel(it.kind)} · ${it.country}" + (it.certification?.let { c -> " · $c" } ?: "")) }
    }
}

@Composable
private fun ExternalIdsBlock(ids: ExternalIds, modifier: Modifier) {
    val rows = listOfNotNull(ids.imdb?.let { "IMDb" to it }, ids.tvdb?.let { "TVDB" to it.toString() }, ids.wikidata?.let { "Wikidata" to it })
    if (rows.isEmpty()) return
    SectionHeader(stringResource(R.string.external_ids))
    SelectionContainer { Column(modifier) { rows.forEach { (k, v) -> Fact(k, v) } } }
}

@Composable
private fun Seasons(d: TmdbDetail, ui: RemoteDetailUi, vm: RemoteDetailViewModel) {
    SectionHeader(stringResource(R.string.seasons))
    Column(Modifier.padding(horizontal = 8.dp)) {
        d.seasons.forEach { s ->
            var open by rememberSaveable(s.number) { mutableStateOf(false) }
            val label = s.name ?: stringResource(if (s.isSpecials) R.string.specials else R.string.season_n, s.number)
            ListItem(
                headlineContent = { Text(label) },
                supportingContent = { Text(pluralStringResource(R.plurals.episode_count, s.episodeCount, s.episodeCount) + (s.airDate?.let { " · $it" } ?: "")) },
                trailingContent = { Icon(if (open) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, null) },
            )
            TextButton({ open = !open; if (open) vm.loadSeason(s.number) }) { Text(stringResource(if (open) R.string.hide_episodes else R.string.show_episodes)) }
            if (open) when (val st = ui.seasons[s.number]) {
                null, SeasonState.Loading -> ShapeMorphLoader(Modifier.padding(16.dp), size = 40.dp)
                is SeasonState.Failed -> Text(metadataErrorText(st.error), Modifier.padding(16.dp), color = MaterialTheme.colorScheme.error)
                is SeasonState.Loaded -> st.season.episodes.forEach { e ->
                    ListItem(
                        headlineContent = { Text("E${e.number} · ${e.name ?: ""}") },
                        supportingContent = { Text(listOfNotNull(e.airDate, e.runtimeMinutes?.let { stringResource(R.string.minutes_short, it) }).joinToString(" · ")) },
                    )
                }
            }
        }
    }
}
