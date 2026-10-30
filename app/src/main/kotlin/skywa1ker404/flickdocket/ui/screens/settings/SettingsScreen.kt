package skywa1ker404.flickdocket.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import android.content.Intent
import android.os.Build
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import skywa1ker404.flickdocket.CrashReporter
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import skywa1ker404.flickdocket.R
import skywa1ker404.flickdocket.data.prefs.AppSettings
import skywa1ker404.flickdocket.domain.*
import skywa1ker404.flickdocket.ui.components.PillRow
import skywa1ker404.flickdocket.ui.components.SectionHeader
import skywa1ker404.flickdocket.ui.components.SourceAttribution
import skywa1ker404.flickdocket.ui.components.TmdbAttribution

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
        Text(stringResource(R.string.source_heading), Modifier.padding(horizontal = 16.dp), style = MaterialTheme.typography.titleMedium)
        PillRow(MetadataMode.entries, { it == settings.metadataMode }, { stringResource(if (it == MetadataMode.OPEN) R.string.source_open else R.string.source_tmdb_option) }, vm::setMetadataMode)
        Text(
            stringResource(if (settings.metadataMode == MetadataMode.OPEN) R.string.source_open_body else R.string.source_tmdb_body),
            Modifier.padding(horizontal = 16.dp, vertical = 4.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (settings.metadataMode == MetadataMode.TMDB) TmdbCard(vm)
        PlannedProviders.filter { it.id != ProviderId.TMDB }.forEach { p ->
            ListItem(
                headlineContent = { Text(p.name) },
                supportingContent = { Text(p.role) },
                trailingContent = { AssistChip(onClick = {}, enabled = false, label = { Text(stringResource(R.string.provider_coming_phase, p.plannedPhase)) }) },
            )
        }
        Text(stringResource(R.string.providers_body), Modifier.padding(horizontal = 16.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

        SectionHeader(stringResource(R.string.about))
        ListItem(headlineContent = { Text(stringResource(R.string.app_name)) }, supportingContent = { Text(stringResource(R.string.about_body)) })
        CrashCard()
        TmdbAttribution()
        SourceAttribution(RefSource.CINEMETA)
        SourceAttribution(RefSource.TVMAZE)
    }
}

@Composable
private fun TmdbCard(vm: SettingsViewModel) {
    val ui by vm.tmdb.collectAsStateWithLifecycle()
    // Never saved to instance state: the secret must not end up in a Bundle.
    var input by remember { mutableStateOf("") }
    val statusText = stringResource(
        when (ui.state) {
            TmdbConnection.NOT_CONFIGURED -> R.string.tmdb_state_none
            TmdbConnection.CHECKING -> R.string.tmdb_state_checking
            TmdbConnection.CONNECTED -> R.string.tmdb_state_connected
            TmdbConnection.INVALID_KEY -> R.string.tmdb_state_invalid
            TmdbConnection.UNREACHABLE -> R.string.tmdb_state_unreachable
            TmdbConnection.RATE_LIMITED -> R.string.tmdb_state_rate_limited
        }
    )
    ElevatedCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.tmdb_title), style = MaterialTheme.typography.titleMedium)
            Text(statusText, color = if (ui.state == TmdbConnection.CONNECTED) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            Text(stringResource(R.string.tmdb_body), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(
                value = input, onValueChange = { input = it; vm.dismissFormatError() }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                label = { Text(stringResource(R.string.tmdb_key_label)) }, isError = ui.formatError,
                supportingText = if (ui.formatError) { { Text(stringResource(R.string.tmdb_key_invalid_format)) } } else null,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button({ vm.saveTmdbKey(input); input = "" }, enabled = input.isNotBlank()) { Text(stringResource(R.string.tmdb_save)) }
                if (ui.hasKey) OutlinedButton(vm::disconnectTmdb) { Text(stringResource(R.string.tmdb_disconnect)) }
            }
            if (ui.hasKey) TextButton(vm::clearCache) { Text(stringResource(R.string.tmdb_clear_cache)) }
        }
    }
}

private fun themeLabel(t: ThemeMode) = when (t) {
    ThemeMode.SYSTEM -> R.string.theme_system; ThemeMode.LIGHT -> R.string.theme_light
    ThemeMode.DARK -> R.string.theme_dark; ThemeMode.AMOLED -> R.string.theme_amoled
}

private fun motionLabel(m: MotionPreference) = when (m) {
    MotionPreference.FOLLOW_SYSTEM -> R.string.motion_system; MotionPreference.REDUCED -> R.string.motion_reduced; MotionPreference.FULL -> R.string.motion_full
}

/** Shows the last crash saved on this device, if any. Nothing is sent anywhere; sharing is the user's choice. */
@Composable
private fun CrashCard() {
    val context = LocalContext.current
    var report by remember { mutableStateOf(CrashReporter.read(context)) }
    val text = report ?: return
    SectionHeader(stringResource(R.string.last_crash))
    Text(stringResource(R.string.last_crash_body), Modifier.padding(horizontal = 16.dp), style = MaterialTheme.typography.bodyMedium)
    SelectionContainer {
        Text(text.take(1500), Modifier.padding(16.dp), style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
    }
    Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button({
            val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
            context.startActivity(Intent.createChooser(send, null))
        }) { Text(stringResource(R.string.share_report)) }
        TextButton({ CrashReporter.clear(context); report = null }) { Text(stringResource(R.string.clear)) }
    }
}
