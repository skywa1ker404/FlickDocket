@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package skywa1ker404.flickdocket.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import skywa1ker404.flickdocket.R
import skywa1ker404.flickdocket.domain.MediaType
import skywa1ker404.flickdocket.domain.WatchStatus

@Composable
fun statusLabel(s: WatchStatus): String = stringResource(
    when (s) {
        WatchStatus.PLANNING -> R.string.status_planning
        WatchStatus.WATCHING -> R.string.status_watching
        WatchStatus.COMPLETED -> R.string.status_completed
        WatchStatus.ON_HOLD -> R.string.status_on_hold
        WatchStatus.DROPPED -> R.string.status_dropped
    }
)

@Composable
fun mediaTypeLabel(t: MediaType): String = stringResource(if (t == MediaType.MOVIE) R.string.type_movie else R.string.type_tv)

@Composable
private fun statusColor(s: WatchStatus): Color = when (s) {
    WatchStatus.PLANNING -> MaterialTheme.colorScheme.outline
    WatchStatus.WATCHING -> MaterialTheme.colorScheme.secondary
    WatchStatus.COMPLETED -> MaterialTheme.colorScheme.primary
    WatchStatus.ON_HOLD -> MaterialTheme.colorScheme.tertiary
    WatchStatus.DROPPED -> MaterialTheme.colorScheme.error
}

/** Colour is never the only signal: the status name is always rendered as text beside the dot. */
@Composable
fun StatusPill(status: WatchStatus, modifier: Modifier = Modifier) {
    Row(
        modifier.clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)).padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(statusColor(status)))
        Spacer(Modifier.width(6.dp))
        Text(statusLabel(status), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface)
    }
}

/** Selectable pill used for filters, tabs and option groups. */
@Composable
fun SelectPill(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    FilterChip(
        selected = selected, onClick = onClick, modifier = modifier, label = { Text(label) },
        leadingIcon = if (selected) { { Icon(Icons.Rounded.Check, contentDescription = null, Modifier.size(FilterChipDefaults.IconSize)) } } else null,
        shape = RoundedCornerShape(50),
    )
}

@Composable
fun <T> PillRow(options: List<T>, selected: (T) -> Boolean, label: @Composable (T) -> String, onSelect: (T) -> Unit, modifier: Modifier = Modifier) {
    LazyRow(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(horizontal = 16.dp)) {
        items(options) { o -> SelectPill(label(o), selected(o), { onSelect(o) }) }
    }
}

/** Section title with a small morphing-family shape marker instead of a plain bar. */
@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Row(modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(12.dp).clip(MaterialShapes.Cookie4Sided.toShape()).background(MaterialTheme.colorScheme.primary))
        Spacer(Modifier.width(10.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
    }
}
