package skywa1ker404.flickdocket.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import skywa1ker404.flickdocket.data.prefs.SettingsStore
import skywa1ker404.flickdocket.data.repo.MetadataRepository
import skywa1ker404.flickdocket.data.secure.CredentialStore
import skywa1ker404.flickdocket.domain.*

enum class TmdbConnection { NOT_CONFIGURED, CHECKING, CONNECTED, INVALID_KEY, UNREACHABLE, RATE_LIMITED }

data class TmdbUi(val hasKey: Boolean = false, val state: TmdbConnection = TmdbConnection.NOT_CONFIGURED, val formatError: Boolean = false)

class SettingsViewModel(
    private val store: SettingsStore,
    private val credentials: CredentialStore,
    private val metadata: MetadataRepository,
) : ViewModel() {
    private val _tmdb = MutableStateFlow(TmdbUi())
    val tmdb: StateFlow<TmdbUi> = _tmdb.asStateFlow()

    init {
        viewModelScope.launch {
            if (credentials.tmdbKey() != null) { _tmdb.value = TmdbUi(hasKey = true, state = TmdbConnection.CHECKING); verify(force = false) }
        }
    }

    fun setTheme(v: ThemeMode) { viewModelScope.launch { store.setTheme(v) } }
    fun setDynamic(v: Boolean) { viewModelScope.launch { store.setDynamicColor(v) } }
    fun setMotion(v: MotionPreference) { viewModelScope.launch { store.setMotion(v) } }

    fun saveTmdbKey(raw: String) {
        val key = raw.trim()
        if (!TmdbKey.isValidFormat(key)) { _tmdb.update { it.copy(formatError = true) }; return }
        viewModelScope.launch {
            metadata.clearCache() // a new credential must not be answered from another account's cached responses
            credentials.saveTmdbKey(key)
            _tmdb.value = TmdbUi(hasKey = true, state = TmdbConnection.CHECKING)
            verify(force = true)
        }
    }

    /** Removes the credential and cached TMDB responses. Local library records are never touched. */
    fun disconnectTmdb() {
        viewModelScope.launch { credentials.clearTmdbKey(); metadata.clearCache(); _tmdb.value = TmdbUi() }
    }

    fun clearCache() { viewModelScope.launch { metadata.clearCache() } }
    fun dismissFormatError() { _tmdb.update { it.copy(formatError = false) } }

    private suspend fun verify(force: Boolean) {
        val state = when (val r = metadata.verifyKey(force)) {
            is MetadataResult.Ok -> if (r.stale) TmdbConnection.UNREACHABLE else TmdbConnection.CONNECTED
            is MetadataResult.Failure -> when (r.error) {
                MetadataError.Unauthorized -> TmdbConnection.INVALID_KEY
                MetadataError.MissingCredentials -> TmdbConnection.NOT_CONFIGURED
                is MetadataError.RateLimited -> TmdbConnection.RATE_LIMITED
                else -> TmdbConnection.UNREACHABLE
            }
        }
        _tmdb.update { it.copy(state = state, hasKey = state != TmdbConnection.NOT_CONFIGURED) }
    }
}
