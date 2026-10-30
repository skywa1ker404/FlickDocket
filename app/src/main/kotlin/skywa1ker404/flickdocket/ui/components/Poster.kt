@file:OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3ExpressiveApi::class)

package skywa1ker404.flickdocket.ui.components

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import skywa1ker404.flickdocket.domain.LibraryItem
import skywa1ker404.flickdocket.domain.MediaType
import skywa1ker404.flickdocket.domain.ProgressRules
import skywa1ker404.flickdocket.domain.WatchStatus
import skywa1ker404.flickdocket.ui.theme.LocalReducedMotion
import skywa1ker404.flickdocket.ui.theme.spatialSpec
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.res.stringResource
import skywa1ker404.flickdocket.R

/** Bundles the scopes needed for a poster to fly between list and detail. Null disables the transition. */
class SharedScopes(val shared: SharedTransitionScope, val visibility: AnimatedVisibilityScope)

fun posterKey(titleId: Long) = "poster-$titleId"

@Composable
fun Modifier.posterSharedElement(key: String, scopes: SharedScopes?): Modifier {
    if (scopes == null || LocalReducedMotion.current) return this
    val spec: FiniteAnimationSpec<Rect> = spatialSpec()
    return with(scopes.shared) {
        this@posterSharedElement.sharedElement(
            sharedContentState = rememberSharedContentState(key),
            animatedVisibilityScope = scopes.visibility,
            boundsTransform = BoundsTransform { _, _ -> spec },
        )
    }
}

/**
 * Original generated poster art (deterministic gradient + initials). Real artwork arrives with TMDB in Phase 2;
 * nothing here is copied from any provider or other app.
 */
@Composable
fun PosterArt(title: String, modifier: Modifier = Modifier, corner: androidx.compose.ui.unit.Dp = 20.dp) {
    val hue = ((title.hashCode().toLong() and 0x7fffffff) % 360).toFloat()
    val top = Color.hsv(hue, 0.55f, 0.55f)
    val bottom = Color.hsv((hue + 48f) % 360f, 0.65f, 0.28f)
    val initials = title.split(' ', '-', ':').filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }
    Box(
        modifier.clip(RoundedCornerShape(corner)).background(Brush.verticalGradient(listOf(top, bottom))),
        contentAlignment = Alignment.Center,
    ) {
        Text(initials, style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Black, color = Color.White.copy(alpha = 0.85f))
    }
}

/**
 * Card with poster, a one-line title and one info line (subtitle, or a progress bar). The status pill sits top-start and the
 * quick "+" bottom-end of the poster, so nothing shares a row and nothing is clipped. Pass [sharedKey] only for the tile that
 * was just opened: giving every tile a shared-element state makes long grids slow.
 */
@Composable
fun PosterTile(
    title: String, subtitle: String, posterPath: String?, status: WatchStatus?,
    onClick: () -> Unit, scopes: SharedScopes?, sharedKey: String?, modifier: Modifier = Modifier,
    progressText: String? = null, progressFraction: Float? = null, onAdvance: (() -> Unit)? = null,
) {
    val description = remember(title, status, progressText) { listOfNotNull(title, progressText).joinToString(", ") }
    val statusText = status?.let { statusLabel(it) }
    val openLabel = stringResource(R.string.open_details)
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .clickable(onClickLabel = openLabel, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = listOfNotNull(description, statusText).joinToString(", ") }
            .padding(8.dp),
    ) {
        Box {
            val art = Modifier.fillMaxWidth().aspectRatio(2f / 3f)
            PosterImage(posterPath, title, if (sharedKey != null) art.posterSharedElement(sharedKey, scopes) else art, corner = 18.dp)
            if (status != null) StatusPill(status, Modifier.align(Alignment.TopStart).padding(6.dp))
            if (onAdvance != null) {
                FilledIconButton(onAdvance, Modifier.align(Alignment.BottomEnd).padding(6.dp).size(44.dp), shape = RoundedCornerShape(16.dp)) {
                    Icon(Icons.Rounded.Add, stringResource(R.string.advance_progress, title))
                }
            }
        }
        Text(
            title, Modifier.padding(start = 4.dp, end = 4.dp, top = 10.dp), style = MaterialTheme.typography.titleSmall,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
        if (progressText != null) {
            Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 8.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                if (progressFraction != null) {
                    LinearProgressIndicator({ progressFraction }, Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(50)))
                    Spacer(Modifier.width(8.dp))
                } else Spacer(Modifier.weight(1f))
                Text(progressText, style = MaterialTheme.typography.labelMedium, maxLines = 1, softWrap = false)
            }
        } else {
            Text(
                subtitle.ifBlank { " " }, Modifier.padding(start = 4.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun PosterCard(item: LibraryItem, onClick: () -> Unit, scopes: SharedScopes?, sharedKey: String?, onAdvance: (() -> Unit)?, modifier: Modifier = Modifier) {
    val total = item.totalEpisodes
    val tvStarted = item.mediaType == MediaType.TV && item.watchCount > 0
    val type = mediaTypeLabel(item.mediaType)
    val subtitle = remember(item.mediaType, item.year, item.rating, type) {
        listOfNotNull(type, item.year?.toString(), item.rating?.let { "★ %.1f".format(it / 10f) }).joinToString(" · ")
    }
    PosterTile(
        item.title, subtitle, item.posterPath, item.status, onClick, scopes, sharedKey, modifier,
        progressText = if (tvStarted) (if (total != null && total > 0) "${item.watchCount.coerceAtMost(total)} / $total" else "${item.watchCount} eps") else null,
        progressFraction = if (tvStarted) ProgressRules.fraction(item.watchCount, total) else null,
        onAdvance = onAdvance.takeIf { ProgressRules.canAdvance(item.mediaType, item.status, item.watchCount, total) },
    )
}

/** Shared-element key used by a library card; matches the details page for linked titles. */
fun libraryPosterKey(item: LibraryItem): String =
    item.remoteRef?.let { skywa1ker404.flickdocket.ui.screens.discover.tmdbPosterKey(it) } ?: posterKey(item.titleId)
