@file:OptIn(ExperimentalCoroutinesApi::class)

package skywa1ker404.flickdocket.ui.screens.remote

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import skywa1ker404.flickdocket.data.repo.LibraryRepository
import skywa1ker404.flickdocket.data.repo.MetadataRepository
import skywa1ker404.flickdocket.domain.*

sealed interface SeasonState {
    data object Loading : SeasonState
    data class Loaded(val season: SeasonDetail, val stale: Boolean) : SeasonState
    data class Failed(val error: MetadataError) : SeasonState
}

data class RemoteDetailUi(
    /** null while loading */
    val detail: MetadataResult<TmdbDetail>? = null,
    val seasons: Map<Int, SeasonState> = emptyMap(),
    val adding: Boolean = false,
)

class RemoteDetailViewModel(private val metadata: MetadataRepository, private val library: LibraryRepository) : ViewModel() {
    private val ref = MutableStateFlow<TmdbRef?>(null)
    private val _ui = MutableStateFlow(RemoteDetailUi())
    val ui: StateFlow<RemoteDetailUi> = _ui.asStateFlow()
    /** Library id of this title if it is already tracked. */
    val libraryId: StateFlow<Long?> = ref.flatMapLatest { r -> if (r == null) flowOf(null) else library.observeLibraryId(r) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Local record for this title when tracked: status and watched-episode progress. Works offline. */
    val entry: StateFlow<TitleDetail?> = libraryId.flatMapLatest { id -> if (id == null) flowOf(null) else library.observeDetail(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    private var statusBeforeAdvance: WatchStatus? = null

    fun advance() {
        val e = entry.value ?: return
        statusBeforeAdvance = e.status
        viewModelScope.launch { library.addWatch(e.titleId) }
    }
    fun undoAdvance() {
        val e = entry.value ?: return
        val prior = statusBeforeAdvance.also { statusBeforeAdvance = null }
        viewModelScope.launch { library.undoLastWatch(e.titleId); if (prior != null) library.setStatus(e.titleId, prior) }
    }
    fun removeLastWatch() { val e = entry.value ?: return; statusBeforeAdvance = null; viewModelScope.launch { library.undoLastWatch(e.titleId) } }
    fun setStatus(status: WatchStatus) { val e = entry.value ?: return; viewModelScope.launch { library.setStatus(e.titleId, status) } }

    fun load(r: TmdbRef, force: Boolean = false) {
        if (!force && ref.value == r) return
        ref.value = r
        _ui.value = RemoteDetailUi()
        viewModelScope.launch { val d = metadata.detail(r); if (ref.value == r) _ui.update { it.copy(detail = d) } }
    }

    fun retry() { ref.value?.let { load(it, force = true) } }

    fun loadSeason(number: Int) {
        val r = ref.value ?: return
        if (_ui.value.seasons[number] is SeasonState.Loaded) return
        _ui.update { it.copy(seasons = it.seasons + (number to SeasonState.Loading)) }
        viewModelScope.launch {
            val s = when (val res = metadata.season(r, number)) {
                is MetadataResult.Ok -> SeasonState.Loaded(res.value, res.stale)
                is MetadataResult.Failure -> SeasonState.Failed(res.error)
            }
            if (ref.value == r) _ui.update { it.copy(seasons = it.seasons + (number to s)) }
        }
    }

    fun addToLibrary(status: WatchStatus) {
        val d = (_ui.value.detail as? MetadataResult.Ok)?.value ?: return
        if (_ui.value.adding) return
        _ui.update { it.copy(adding = true) }
        viewModelScope.launch { library.addFromTmdb(d, status); _ui.update { it.copy(adding = false) } }
    }
}
