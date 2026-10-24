@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package skywa1ker404.flickdocket.ui.screens.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import skywa1ker404.flickdocket.R
import skywa1ker404.flickdocket.domain.WatchStatus
import skywa1ker404.flickdocket.ui.components.*

@Composable
fun StatsScreen(vm: StatsViewModel) {
    val stats by vm.stats.collectAsStateWithLifecycle()
    val s = stats
    when {
        s == null -> FullScreenLoader()
        s.totalTitles == 0 -> EmptyState(Icons.Rounded.BarChart, stringResource(R.string.empty_stats_title), stringResource(R.string.empty_stats_body), shape = MaterialShapes.Flower)
        else -> Column(Modifier.verticalScroll(rememberScrollState()).statusBarsPadding().padding(bottom = 24.dp)) {
            SectionHeader(stringResource(R.string.nav_stats))
            Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Tile(s.totalTitles.toString(), stringResource(R.string.stat_titles), Modifier.weight(1f))
                Tile(s.watchEvents.toString(), stringResource(R.string.stat_watches), Modifier.weight(1f))
                Tile("%.1f".format(s.minutesWatched / 60f), stringResource(R.string.stat_hours), Modifier.weight(1f))
            }
            Spacer(Modifier.height(16.dp))
            SectionHeader(stringResource(R.string.status))
            StatusBar(s.byStatus, s.totalTitles)
        }
    }
}

@Composable
private fun Tile(value: String, label: String, modifier: Modifier) {
    Card(modifier, shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
        Column(Modifier.padding(16.dp).semantics(mergeDescendants = true) { contentDescription = "$label: $value" }) {
            Text(value, style = MaterialTheme.typography.headlineMedium)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun StatusBar(byStatus: Map<WatchStatus, Int>, total: Int) {
    val colors = listOf(MaterialTheme.colorScheme.outline, MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary, MaterialTheme.colorScheme.error)
    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth().height(16.dp).clip(RoundedCornerShape(50)), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            WatchStatus.entries.forEachIndexed { i, st ->
                val n = byStatus[st] ?: 0
                if (n > 0) Box(Modifier.weight(n.toFloat() / total).fillMaxHeight().background(colors[i]))
            }
        }
        WatchStatus.entries.forEachIndexed { i, st ->
            Row {
                Box(Modifier.size(12.dp).align(androidx.compose.ui.Alignment.CenterVertically).clip(RoundedCornerShape(50)).background(colors[i]))
                Spacer(Modifier.width(8.dp))
                Text(statusLabel(st), Modifier.weight(1f))
                Text((byStatus[st] ?: 0).toString())
            }
        }
    }
}
