package skywa1ker404.flickdocket.ui.theme

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

/** True when the user (or system) wants minimal motion. Every animated component must honour it. */
val LocalReducedMotion = staticCompositionLocalOf { false }

/** Springy "expressive" spatial motion, or an instant snap under reduced motion. */
@Composable
fun <T> spatialSpec(): FiniteAnimationSpec<T> =
    if (LocalReducedMotion.current) snap() else spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow)
