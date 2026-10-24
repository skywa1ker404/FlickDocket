@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package skywa1ker404.flickdocket.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import skywa1ker404.flickdocket.R
import skywa1ker404.flickdocket.ui.theme.LocalReducedMotion

/** Every Material 3 shape, morphing one into the next. Cached once; shape objects are immutable. */
private val AllMaterialShapes by lazy {
    listOf(
        MaterialShapes.Circle, MaterialShapes.Square, MaterialShapes.Slanted, MaterialShapes.Arch, MaterialShapes.Fan,
        MaterialShapes.Arrow, MaterialShapes.SemiCircle, MaterialShapes.Oval, MaterialShapes.Pill, MaterialShapes.Triangle,
        MaterialShapes.Diamond, MaterialShapes.ClamShell, MaterialShapes.Pentagon, MaterialShapes.Gem, MaterialShapes.Sunny,
        MaterialShapes.VerySunny, MaterialShapes.Cookie4Sided, MaterialShapes.Cookie6Sided, MaterialShapes.Cookie7Sided,
        MaterialShapes.Cookie9Sided, MaterialShapes.Cookie12Sided, MaterialShapes.Ghostish, MaterialShapes.Clover4Leaf,
        MaterialShapes.Clover8Leaf, MaterialShapes.Burst, MaterialShapes.SoftBurst, MaterialShapes.Boom, MaterialShapes.SoftBoom,
        MaterialShapes.Flower, MaterialShapes.Puffy, MaterialShapes.PuffyDiamond, MaterialShapes.PixelCircle,
        MaterialShapes.PixelTriangle, MaterialShapes.Bun, MaterialShapes.Heart,
    )
}

/**
 * The app's only loading indicator. Under reduced motion it shows one static shape instead of morphing.
 */
@Composable
fun ShapeMorphLoader(modifier: Modifier = Modifier, size: Dp = 56.dp, contained: Boolean = false) {
    val description = stringResource(R.string.loading)
    val base = modifier.size(size).semantics { contentDescription = description }
    if (LocalReducedMotion.current) {
        Box(base.clip(MaterialShapes.Cookie9Sided.toShape()).background(MaterialTheme.colorScheme.primary))
    } else if (contained) {
        ContainedLoadingIndicator(modifier = base, polygons = AllMaterialShapes)
    } else {
        LoadingIndicator(modifier = base, polygons = AllMaterialShapes)
    }
}

@Composable
fun FullScreenLoader(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { ShapeMorphLoader(size = 72.dp, contained = true) }
}
