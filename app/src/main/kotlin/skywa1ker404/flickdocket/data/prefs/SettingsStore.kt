package skywa1ker404.flickdocket.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import skywa1ker404.flickdocket.domain.MotionPreference
import skywa1ker404.flickdocket.domain.ThemeMode

data class AppSettings(
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val motion: MotionPreference = MotionPreference.FOLLOW_SYSTEM,
)

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** UI preferences only. Provider tokens/keys will use a separate store (Phase 2) excluded from backups. */
class SettingsStore(private val context: Context) {
    private val themeKey = stringPreferencesKey("theme")
    private val dynamicKey = booleanPreferencesKey("dynamic_color")
    private val motionKey = stringPreferencesKey("motion")

    val settings: Flow<AppSettings> = context.settingsDataStore.data.map { p ->
        AppSettings(
            theme = p[themeKey].toEnum(ThemeMode.SYSTEM),
            dynamicColor = p[dynamicKey] ?: true,
            motion = p[motionKey].toEnum(MotionPreference.FOLLOW_SYSTEM),
        )
    }

    suspend fun setTheme(v: ThemeMode) { context.settingsDataStore.edit { it[themeKey] = v.name } }
    suspend fun setDynamicColor(v: Boolean) { context.settingsDataStore.edit { it[dynamicKey] = v } }
    suspend fun setMotion(v: MotionPreference) { context.settingsDataStore.edit { it[motionKey] = v.name } }
}

private inline fun <reified E : Enum<E>> String?.toEnum(default: E): E =
    enumValues<E>().firstOrNull { it.name == this } ?: default
