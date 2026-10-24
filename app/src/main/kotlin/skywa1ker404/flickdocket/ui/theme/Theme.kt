@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package skywa1ker404.flickdocket.ui.theme

import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import skywa1ker404.flickdocket.data.prefs.AppSettings
import skywa1ker404.flickdocket.domain.MotionRules
import skywa1ker404.flickdocket.domain.ThemeMode
import skywa1ker404.flickdocket.domain.ThemeRules

// Original "ember & lagoon" palette: warm ember primary, cool lagoon secondary, ink-violet neutrals.
private val LightScheme = lightColorScheme(
    primary = Color(0xFFB4411C), onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDBCF), onPrimaryContainer = Color(0xFF3A0B00),
    secondary = Color(0xFF00696E), onSecondary = Color.White,
    secondaryContainer = Color(0xFF9EF0F5), onSecondaryContainer = Color(0xFF002022),
    tertiary = Color(0xFF6B4EA0), onTertiary = Color.White,
    tertiaryContainer = Color(0xFFEBDDFF), onTertiaryContainer = Color(0xFF240054),
    background = Color(0xFFFFF8F6), onBackground = Color(0xFF231917),
    surface = Color(0xFFFFF8F6), onSurface = Color(0xFF231917),
    surfaceVariant = Color(0xFFF5DED6), onSurfaceVariant = Color(0xFF53433E),
    outline = Color(0xFF85736D),
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFFFFB59C), onPrimary = Color(0xFF5E1700),
    primaryContainer = Color(0xFF862B07), onPrimaryContainer = Color(0xFFFFDBCF),
    secondary = Color(0xFF82D3D9), onSecondary = Color(0xFF00373A),
    secondaryContainer = Color(0xFF004F53), onSecondaryContainer = Color(0xFF9EF0F5),
    tertiary = Color(0xFFD3BBFF), onTertiary = Color(0xFF3C1F70),
    tertiaryContainer = Color(0xFF533787), onTertiaryContainer = Color(0xFFEBDDFF),
    background = Color(0xFF1A1110), onBackground = Color(0xFFF1DFDA),
    surface = Color(0xFF1A1110), onSurface = Color(0xFFF1DFDA),
    surfaceVariant = Color(0xFF53433E), onSurfaceVariant = Color(0xFFD8C2BB),
    outline = Color(0xFFA08C85),
)

internal fun ColorScheme.toAmoled(): ColorScheme = copy(
    background = Color.Black, surface = Color.Black, surfaceDim = Color.Black,
    surfaceContainerLowest = Color.Black, surfaceContainerLow = Color(0xFF0B0B0B),
    surfaceContainer = Color(0xFF121212), surfaceContainerHigh = Color(0xFF1A1A1A),
    surfaceContainerHighest = Color(0xFF222222),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp), small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp), large = RoundedCornerShape(28.dp), extraLarge = RoundedCornerShape(40.dp),
)

private val AppTypography = Typography().let {
    it.copy(
        headlineLarge = it.headlineLarge.copy(fontWeight = FontWeight.Bold),
        headlineMedium = it.headlineMedium.copy(fontWeight = FontWeight.Bold),
        titleLarge = it.titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = it.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = it.labelLarge.copy(fontWeight = FontWeight.SemiBold),
    )
}

@Composable
fun systemAnimatorScale(): Float {
    val resolver = LocalContext.current.contentResolver
    return remember(resolver) { Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) }
}

@Composable
fun FlickDocketTheme(settings: AppSettings, content: @Composable () -> Unit) {
    val dark = ThemeRules.isDark(settings.theme, isSystemInDarkTheme())
    val context = LocalContext.current
    var scheme = when {
        settings.dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> DarkScheme
        else -> LightScheme
    }
    if (settings.theme == ThemeMode.AMOLED) scheme = scheme.toAmoled()
    val reduced = MotionRules.isReduced(settings.motion, systemAnimatorScale())

    CompositionLocalProvider(LocalReducedMotion provides reduced) {
        MaterialExpressiveTheme(
            colorScheme = scheme,
            motionScheme = if (reduced) MotionScheme.standard() else MotionScheme.expressive(),
            shapes = AppShapes,
            typography = AppTypography,
            content = content,
        )
    }
}
