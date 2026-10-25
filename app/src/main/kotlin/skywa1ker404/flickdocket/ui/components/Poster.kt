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

/** Poster + title + subtitle. [status] is optional so the same tile serves the library and TMDB results. */
@Composable
fun PosterTile(
    title: String, subtitle: String, posterPath: String?, status: WatchStatus?,
    onClick: () -> Unit, scopes: SharedScopes?, sharedKey: String?, modifier: Modifier = Modifier,
) {
    val description = if (status != null) "$title, ${statusLabel(status)}" else title
    val openLabel = stringResource(R.string.open_details)
    Column(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .semantics(mergeDescendants = true) { contentDescription = description }
            .clickable(onClickLabel = openLabel, role = Role.Button, onClick = onClick),
    ) {
        Box {
            val art = Modifier.fillMaxWidth().aspectRatio(2f / 3f)
            PosterImage(posterPath, title, if (sharedKey != null) art.posterSharedElement(sharedKey, scopes) else art)
            if (status != null) StatusPill(status, Modifier.align(Alignment.BottomStart).padding(8.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
        if (subtitle.isNotBlank()) Text(subtitle, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun PosterCard(item: LibraryItem, onClick: () -> Unit, scopes: SharedScopes?, modifier: Modifier = Modifier) {
    PosterTile(
        item.title, listOfNotNull(item.year?.toString(), item.rating?.let { "★ %.1f".format(it / 10f) }).joinToString(" · "),
        item.posterPath, item.status, onClick, scopes, posterKey(item.titleId), modifier,
    )
}
