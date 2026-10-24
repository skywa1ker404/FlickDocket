package skywa1ker404.flickdocket.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import android.os.Build
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import skywa1ker404.flickdocket.R
import skywa1ker404.flickdocket.data.prefs.AppSettings
import skywa1ker404.flickdocket.domain.*
import skywa1ker404.flickdocket.ui.components.PillRow
import skywa1ker404.flickdocket.ui.components.SectionHeader

@Composable
fun SettingsScreen(vm: SettingsViewModel, settings: AppSettings) {
    Column(Modifier.verticalScroll(rememberScrollState()).statusBarsPadding().padding(bottom = 24.dp)) {
        SectionHeader(stringResource(R.string.appearance))
        PillRow(ThemeMode.entries, { it == settings.theme }, { stringResource(themeLabel(it)) }, vm::setTheme)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ListItem(
                headlineContent = { Text(stringResource(R.string.dynamic_color)) },
                supportingContent = { Text(stringResource(R.string.dynamic_color_body)) },
                trailingContent = { Switch(settings.dynamicColor, vm::setDynamic) },
            )
        }
        SectionHeader(stringResource(R.string.motion))
        PillRow(MotionPreference.entries, { it == settings.motion }, { stringResource(motionLabel(it)) }, vm::setMotion)
        Text(stringResource(R.string.motion_body), Modifier.padding(horizontal = 16.dp, vertical = 4.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

        SectionHeader(stringResource(R.string.providers))
        PlannedProviders.forEach { p ->
            ListItem(
                headlineContent = { Text(p.name) },
                supportingContent = { Text(p.role) },
                trailingContent = { AssistChip(onClick = {}, enabled = false, label = { Text(stringResource(R.string.provider_coming_phase, p.plannedPhase)) }) },
            )
        }
        Text(stringResource(R.string.providers_body), Modifier.padding(horizontal = 16.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

        SectionHeader(stringResource(R.string.about))
        ListItem(headlineContent = { Text(stringResource(R.string.app_name)) }, supportingContent = { Text(stringResource(R.string.about_body)) })
    }
}

private fun themeLabel(t: ThemeMode) = when (t) {
    ThemeMode.SYSTEM -> R.string.theme_system; ThemeMode.LIGHT -> R.string.theme_light
    ThemeMode.DARK -> R.string.theme_dark; ThemeMode.AMOLED -> R.string.theme_amoled
}

private fun motionLabel(m: MotionPreference) = when (m) {
    MotionPreference.FOLLOW_SYSTEM -> R.string.motion_system; MotionPreference.REDUCED -> R.string.motion_reduced; MotionPreference.FULL -> R.string.motion_full
}
