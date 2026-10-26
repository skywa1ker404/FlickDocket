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
 * Poster + title + one info line. Status sits top-start and the quick "+" bottom-end, so nothing shares a row and
 * nothing is clipped on narrow tiles. When [progressText] is set a bar replaces the subtitle line.
 */
@Composable
fun PosterTile(
    title: String, subtitle: String, posterPath: String?, status: WatchStatus?,
    onClick: () -> Unit, scopes: SharedScopes?, sharedKey: String?, modifier: Modifier = Modifier,
    progressText: String? = null, progressFraction: Float? = null, onAdvance: (() -> Unit)? = null,
) {
    val description = listOfNotNull(title, status?.let { statusLabel(it) }, progressText).joinToString(", ")
    val openLabel = stringResource(R.string.open_details)
    Column(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClickLabel = openLabel, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
    ) {
        Box {
            val art = Modifier.fillMaxWidth().aspectRatio(2f / 3f)
            PosterImage(posterPath, title, if (sharedKey != null) art.posterSharedElement(sharedKey, scopes) else art)
            if (status != null) StatusPill(status, Modifier.align(Alignment.TopStart).padding(8.dp))
            if (onAdvance != null) {
                FilledIconButton(onAdvance, Modifier.align(Alignment.BottomEnd).padding(8.dp).size(44.dp)) {
                    Icon(Icons.Rounded.Add, stringResource(R.string.advance_progress, title))
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            title, Modifier.padding(horizontal = 2.dp), style = MaterialTheme.typography.titleSmall,
            minLines = 2, maxLines = 2, overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(2.dp))
        if (progressText != null) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                if (progressFraction != null) {
                    LinearProgressIndicator({ progressFraction }, Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(50)))
                    Spacer(Modifier.width(8.dp))
                } else Spacer(Modifier.weight(1f))
                Text(progressText, style = MaterialTheme.typography.labelMedium, maxLines = 1, softWrap = false)
            }
        } else if (subtitle.isNotBlank()) {
            Text(subtitle, Modifier.padding(horizontal = 2.dp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun PosterCard(item: LibraryItem, onClick: () -> Unit, scopes: SharedScopes?, onAdvance: (() -> Unit)?, modifier: Modifier = Modifier) {
    val total = item.totalEpisodes
    val tvStarted = item.mediaType == MediaType.TV && item.watchCount > 0
    PosterTile(
        item.title,
        listOfNotNull(mediaTypeLabel(item.mediaType), item.year?.toString(), item.rating?.let { "★ %.1f".format(it / 10f) }).joinToString(" · "),
        item.posterPath, item.status, onClick, scopes,
        item.remoteRef?.let { skywa1ker404.flickdocket.ui.screens.discover.tmdbPosterKey(it) } ?: posterKey(item.titleId), modifier,
        progressText = if (tvStarted) (if (total != null && total > 0) "${item.watchCount.coerceAtMost(total)} / $total" else "${item.watchCount} eps") else null,
        progressFraction = if (tvStarted) ProgressRules.fraction(item.watchCount, total) else null,
        onAdvance = onAdvance.takeIf { ProgressRules.canAdvance(item.mediaType, item.status, item.watchCount, total) },
    )
}
