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

/**
 * One shape from each Material 3 family, morphing one into the next. The indicator builds a Morph per neighbouring pair on the
 * main thread whenever it enters composition, so the list is kept short (all 35 shapes made every loader start stutter).
 */
private val AllMaterialShapes by lazy {
    listOf(
        MaterialShapes.Circle, MaterialShapes.Square, MaterialShapes.Slanted, MaterialShapes.Arch, MaterialShapes.Pill,
        MaterialShapes.Triangle, MaterialShapes.Diamond, MaterialShapes.Pentagon, MaterialShapes.Sunny, MaterialShapes.Cookie6Sided,
        MaterialShapes.Cookie9Sided, MaterialShapes.Clover4Leaf, MaterialShapes.SoftBurst, MaterialShapes.Flower, MaterialShapes.Heart,
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
