@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package skywa1ker404.flickdocket.ui.screens.detail

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import skywa1ker404.flickdocket.R
import skywa1ker404.flickdocket.domain.*
import skywa1ker404.flickdocket.ui.components.*
import skywa1ker404.flickdocket.ui.theme.LocalReducedMotion
import java.text.DateFormat
import java.util.Date

private enum class DetailTab(val label: Int) { Overview(R.string.tab_overview), History(R.string.tab_history) }

@Composable
fun DetailScreen(titleId: Long, vm: DetailViewModel, scopes: SharedScopes, onBack: () -> Unit) {
    LaunchedEffect(titleId) { vm.load(titleId) }
    val detail by vm.detail.collectAsStateWithLifecycle()
    val history by vm.history.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(DetailTab.Overview) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = { IconButton(onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back)) } },
                actions = {
                    detail?.let { d ->
                        IconToggleButton(d.favorite, { vm.setFavorite(it) }) {
                            Icon(if (d.favorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, stringResource(R.string.favorite))
                        }
                        IconButton({ confirmDelete = true }) { Icon(Icons.Rounded.Delete, stringResource(R.string.delete)) }
                    }
                },
            )
        },
    ) { inner ->
        val d = detail
        if (d == null) FullScreenLoader(Modifier.padding(inner)) else Column(Modifier.padding(inner).verticalScroll(rememberScrollState()).padding(bottom = 32.dp)) {
            Header(d, scopes)
            Spacer(Modifier.height(16.dp))
            PillRow(DetailTab.entries, { it == tab }, { stringResource(it.label) }, { tab = it })
            Spacer(Modifier.height(12.dp))
            val reduced = LocalReducedMotion.current
            AnimatedContent(tab, transitionSpec = { (if (reduced) EnterTransition.None else fadeIn()) togetherWith (if (reduced) ExitTransition.None else fadeOut()) }, label = "detail-tab") { t ->
                when (t) {
                    DetailTab.Overview -> Overview(d, vm)
                    DetailTab.History -> HistoryList(history)
                }
            }
        }
    }

    if (confirmDelete) AlertDialog(
        onDismissRequest = { confirmDelete = false },
        title = { Text(stringResource(R.string.delete_confirm_title)) },
        text = { Text(stringResource(R.string.delete_confirm_body)) },
        confirmButton = { TextButton({ confirmDelete = false; vm.delete(onBack) }) { Text(stringResource(R.string.delete)) } },
        dismissButton = { TextButton({ confirmDelete = false }) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun Header(d: TitleDetail, scopes: SharedScopes) {
    Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.Bottom) {
        PosterImage(d.posterPath, d.title, Modifier.width(120.dp).aspectRatio(2f / 3f).posterSharedElement(posterKey(d.titleId), scopes))
        Spacer(Modifier.width(16.dp))
        Column {
            Text(d.title, style = MaterialTheme.typography.headlineSmall)
            Text(
                listOfNotNull(mediaTypeLabel(d.mediaType), d.year?.toString(), d.runtimeMinutes?.let { stringResource(R.string.minutes_short, it) }).joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Overview(d: TitleDetail, vm: DetailViewModel) {
    val pad = Modifier.padding(horizontal = 16.dp)
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(stringResource(R.string.status), pad, style = MaterialTheme.typography.titleMedium)
        PillRow(WatchStatus.entries, { it == d.status }, { statusLabel(it) }, vm::setStatus)
        d.overview?.let { Text(it, pad, style = MaterialTheme.typography.bodyMedium) }
        d.imdbId?.let { Text("IMDb: $it", pad, style = MaterialTheme.typography.labelLarge) }
        Box(pad) { WatchControls(d, vm) }
        Box(pad) { RatingControl(d.rating, vm::setRating) }
        Column(pad) { NotesEditor(d.notes, vm::saveNotes) }
        Text(stringResource(when (d.source) {
            MetadataSource.MANUAL -> R.string.source_manual
            MetadataSource.TMDB -> R.string.source_tmdb
            MetadataSource.TVMAZE -> R.string.source_tvmaze
            MetadataSource.CINEMETA -> R.string.source_cinemeta
        }),
            pad, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun WatchControls(d: TitleDetail, vm: DetailViewModel) {
    val countText = if (d.mediaType == MediaType.MOVIE) pluralStringResource(R.plurals.times_watched, d.watchCount, d.watchCount)
    else d.totalEpisodes?.let { stringResource(R.string.episodes_progress, d.watchCount, it) } ?: pluralStringResource(R.plurals.episodes_watched, d.watchCount, d.watchCount)
    ElevatedCard(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(countText, Modifier.weight(1f).semantics { stateDescription = countText }, style = MaterialTheme.typography.titleMedium)
            FilledTonalIconButton(vm::undoWatch, enabled = d.watchCount > 0) { Icon(Icons.Rounded.Remove, stringResource(R.string.undo_watch)) }
            Spacer(Modifier.width(8.dp))
            FilledIconButton(vm::addWatch) { Icon(Icons.Rounded.Add, stringResource(if (d.mediaType == MediaType.MOVIE) R.string.mark_watched else R.string.mark_episode)) }
        }
    }
}

@Composable
private fun RatingControl(rating: Int?, onChange: (Int?) -> Unit) {
    var local by remember(rating) { mutableFloatStateOf((rating ?: 0).toFloat()) }
    val label = if (rating == null) stringResource(R.string.unrated) else "%.1f / 10".format(rating / 10f)
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.rating), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Text(label, style = MaterialTheme.typography.labelLarge)
            if (rating != null) IconButton({ onChange(null) }) { Icon(Icons.Rounded.Close, stringResource(R.string.clear_rating)) }
        }
        Slider(
            value = local, onValueChange = { local = it }, onValueChangeFinished = { onChange(LibraryRules.snapRating(local.toInt()).takeIf { it > 0 }) },
            valueRange = 0f..RATING_MAX.toFloat(), steps = RATING_MAX / RATING_STEP - 1,
            modifier = Modifier.semantics { contentDescription = label },
        )
    }
}

@Composable
private fun NotesEditor(saved: String, onSave: (String) -> Unit) {
    var text by rememberSaveable(saved) { mutableStateOf(saved) }
    OutlinedTextField(text, { text = it }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.notes)) }, minLines = 3)
    AnimatedVisibilityHint(visible = text != saved) { TextButton({ onSave(text) }) { Text(stringResource(R.string.save_notes)) } }
}

@Composable
private fun AnimatedVisibilityHint(visible: Boolean, content: @Composable () -> Unit) { if (visible) content() }

@Composable
private fun HistoryList(history: List<WatchRecord>) {
    if (history.isEmpty()) {
        Box(Modifier.fillMaxWidth().height(320.dp)) {
            EmptyState(Icons.Rounded.History, stringResource(R.string.empty_history_title), stringResource(R.string.empty_history_body), shape = MaterialShapes.Clover4Leaf)
        }
        return
    }
    val fmt = remember { DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT) }
    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        history.forEach { r -> ListItem(headlineContent = { Text(fmt.format(Date(r.watchedAt))) }, leadingContent = { Icon(Icons.Rounded.Visibility, null) }) }
    }
}
