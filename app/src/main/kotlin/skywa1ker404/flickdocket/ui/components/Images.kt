package skywa1ker404.flickdocket.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import skywa1ker404.flickdocket.domain.ImageConfig
import skywa1ker404.flickdocket.ui.theme.LocalReducedMotion

val LocalImageConfig = compositionLocalOf { ImageConfig() }

@Composable
private fun rememberRequest(url: String): ImageRequest {
    val context = LocalPlatformContext.current
    val reduced = LocalReducedMotion.current
    return remember(url, reduced, context) { ImageRequest.Builder(context).data(url).crossfade(!reduced).build() }
}

/** Remote artwork over the generated placeholder, so missing/slow/offline images still look intentional. */
@Composable
fun PosterImage(path: String?, title: String, modifier: Modifier = Modifier, minWidthPx: Int = 342, corner: Dp = 20.dp) {
    val url = LocalImageConfig.current.poster(path, minWidthPx)
    Box(modifier.clip(RoundedCornerShape(corner))) {
        PosterArt(title, Modifier.fillMaxSize(), corner)
        if (url != null) AsyncImage(rememberRequest(url), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
    }
}

@Composable
fun BackdropImage(path: String?, modifier: Modifier = Modifier, minWidthPx: Int = 780) {
    val url = LocalImageConfig.current.backdrop(path, minWidthPx)
    val bg = MaterialTheme.colorScheme.background
    Box(modifier) {
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(MaterialTheme.colorScheme.secondaryContainer, bg))))
        if (url != null) AsyncImage(rememberRequest(url), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, bg.copy(alpha = 0.55f), bg))))
    }
}

@Composable
fun ProfileImage(path: String?, name: String, modifier: Modifier = Modifier) {
    val url = LocalImageConfig.current.profile(path, 185)
    Box(modifier.clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer), contentAlignment = Alignment.Center) {
        Text(name.firstOrNull()?.uppercase().orEmpty(), color = MaterialTheme.colorScheme.onSecondaryContainer)
        if (url != null) AsyncImage(rememberRequest(url), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
    }
}
