package skywa1ker404.flickdocket.ui.screens.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import skywa1ker404.flickdocket.data.repo.LibraryRepository
import skywa1ker404.flickdocket.domain.*

sealed interface LibraryUiState {
    data object Loading : LibraryUiState
    data class Content(
        val items: List<LibraryItem>,
        val query: LibraryQuery,
        val totalCount: Int,
        val statusCounts: Map<WatchStatus, Int>,
    ) : LibraryUiState
}

class LibraryViewModel(private val repo: LibraryRepository) : ViewModel() {
    private val query = MutableStateFlow(LibraryQuery())

    val state: StateFlow<LibraryUiState> = combine(repo.observeLibrary(), query) { all, q ->
        LibraryUiState.Content(LibraryRules.apply(all, q), q, all.size, LibraryRules.statusCounts(all)) as LibraryUiState
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryUiState.Loading)

    fun setText(t: String) = query.update { it.copy(text = t) }
    fun setMediaType(t: MediaType?) = query.update { it.copy(mediaType = t) }
    fun toggleStatus(s: WatchStatus) = query.update { it.copy(statuses = if (s in it.statuses) it.statuses - s else it.statuses + s) }
    fun setSort(s: LibrarySort) = query.update { it.copy(sort = s) }
    fun setFavoritesOnly(v: Boolean) = query.update { it.copy(favoritesOnly = v) }
    fun clearFilters() = query.update { LibraryQuery(sort = it.sort) }
    private val statusBeforeAdvance = HashMap<Long, WatchStatus>()
    fun advance(item: LibraryItem) { statusBeforeAdvance[item.titleId] = item.status; viewModelScope.launch { repo.addWatch(item.titleId) } }
    /** Removes the watch just added and puts back the status it had, so the quick action is fully reversible. */
    fun undoAdvance(id: Long) {
        val prior = statusBeforeAdvance.remove(id)
        viewModelScope.launch { repo.undoLastWatch(id); if (prior != null) repo.setStatus(id, prior) }
    }
    fun add(draft: NewTitleDraft, onAdded: (Long) -> Unit = {}) = viewModelScope.launch { onAdded(repo.addManual(draft)) }
}
