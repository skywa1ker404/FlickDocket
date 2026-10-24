package skywa1ker404.flickdocket.ui.screens.discover

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import skywa1ker404.flickdocket.R
import skywa1ker404.flickdocket.ui.components.EmptyState

/** Placeholder until TMDB is connected (Phase 2). Shows no fabricated titles. */
@Composable
fun DiscoverScreen() {
    EmptyState(Icons.Rounded.Explore, stringResource(R.string.discover_title), stringResource(R.string.discover_body))
}
