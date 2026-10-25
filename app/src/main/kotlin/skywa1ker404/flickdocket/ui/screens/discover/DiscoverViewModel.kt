@file:OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)

package skywa1ker404.flickdocket.ui.screens.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import skywa1ker404.flickdocket.data.prefs.SettingsStore
import skywa1ker404.flickdocket.data.repo.MetadataRepository
import skywa1ker404.flickdocket.data.secure.CredentialStore
import skywa1ker404.flickdocket.domain.*

enum class DiscoverPhase { NeedsKey, Loading, Content, Error }

data class DiscoverUi(
    val query: String = "",
    val mode: MetadataMode = MetadataMode.OPEN,
    val type: MediaType = MediaType.MOVIE,
    val paged: PagedItems<TmdbSummary> = PagedItems(),
    val phase: DiscoverPhase = DiscoverPhase.Loading,
    val error: MetadataError? = null,
    val stale: Boolean = false,
    val loadingMore: Boolean = false,
    val moreFailed: Boolean = false,
)

class DiscoverViewModel(private val repo: MetadataRepository, credentials: CredentialStore, settings: SettingsStore) : ViewModel() {
    private val _ui = MutableStateFlow(DiscoverUi())
    val ui: StateFlow<DiscoverUi> = _ui.asStateFlow()

    private val query = MutableStateFlow("")
    private val type = MutableStateFlow(MediaType.MOVIE)
    private val retry = MutableStateFlow(0)
    private var generation = 0
    private var activeQuery = ""
    private var activeType = MediaType.MOVIE

    init {
        viewModelScope.launch {
            val mode = settings.settings.map { it.metadataMode }.distinctUntilChanged()
            combine(credentials.hasTmdbKey, mode, query.debounce { if (it.isEmpty()) 0L else 400L }, type, retry) { has, m, q, t, _ -> Req(has, m, q.trim(), t) }
                .collectLatest { reload(it) }
        }
    }

    fun setQuery(q: String) { _ui.update { it.copy(query = q) }; query.value = q }
    fun setType(t: MediaType) { _ui.update { it.copy(type = t) }; type.value = t }
    fun retry() { retry.update { it + 1 } }

    private data class Req(val hasKey: Boolean, val mode: MetadataMode, val q: String, val t: MediaType)

    private suspend fun reload(req: Req) {
        val (hasKey, mode, q, t) = req
        val gen = ++generation
        activeQuery = q; activeType = t
        _ui.update { it.copy(mode = mode) }
        if (mode == MetadataMode.TMDB && !hasKey) { _ui.update { it.copy(phase = DiscoverPhase.NeedsKey, paged = PagedItems(), error = null) }; return }
        _ui.update { it.copy(phase = DiscoverPhase.Loading, paged = PagedItems(), error = null, stale = false, loadingMore = false, moreFailed = false) }
        if (mode == MetadataMode.TMDB) {
            val check = repo.verifyKey(false) // also loads image sizes; an invalid key stops here with a clear error
            if (check is MetadataResult.Failure && check.error == MetadataError.Unauthorized) {
                _ui.update { it.copy(phase = DiscoverPhase.Error, error = check.error) }; return
            }
        }
        when (val r = fetch(q, t, 1)) {
            is MetadataResult.Ok -> if (gen == generation) _ui.update {
                it.copy(phase = DiscoverPhase.Content, paged = PagedItems<TmdbSummary>().append(r.value) { s -> s.ref }, stale = r.stale)
            }
            is MetadataResult.Failure -> if (gen == generation) _ui.update { it.copy(phase = DiscoverPhase.Error, error = r.error) }
        }
    }

    private suspend fun fetch(q: String, t: MediaType, page: Int) =
        if (q.isEmpty()) repo.trending(t, page) else repo.search(t, q, page)

    fun loadMore() {
        val s = _ui.value
        if (s.phase != DiscoverPhase.Content || s.loadingMore || !s.paged.hasMore) return
        val gen = generation
        _ui.update { it.copy(loadingMore = true, moreFailed = false) }
        viewModelScope.launch {
            val r = fetch(activeQuery, activeType, s.paged.page + 1)
            if (gen != generation) return@launch
            when (r) {
                is MetadataResult.Ok -> _ui.update { it.copy(paged = it.paged.append(r.value) { x -> x.ref }, loadingMore = false, stale = it.stale || r.stale) }
                is MetadataResult.Failure -> _ui.update { it.copy(loadingMore = false, moreFailed = true) }
            }
        }
    }
}
