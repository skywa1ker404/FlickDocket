package skywa1ker404.flickdocket.ui.screens.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import skywa1ker404.flickdocket.data.repo.LibraryRepository
import skywa1ker404.flickdocket.domain.*

@OptIn(ExperimentalCoroutinesApi::class)
class DetailViewModel(private val repo: LibraryRepository) : ViewModel() {
    private val id = MutableStateFlow<Long?>(null)
    /** null = still loading; Missing = title no longer exists (e.g. just deleted). */
    val detail: StateFlow<TitleDetail?> = id.flatMapLatest { i -> if (i == null) flowOf(null) else repo.observeDetail(i) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val history: StateFlow<List<WatchRecord>> = id.flatMapLatest { i -> if (i == null) flowOf(emptyList()) else repo.observeHistory(i) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun load(titleId: Long) { id.value = titleId }
    private fun run(block: suspend (Long) -> Unit) { id.value?.let { viewModelScope.launch { block(it) } } }

    fun setStatus(s: WatchStatus) = run { repo.setStatus(it, s) }
    fun setRating(r: Int?) = run { repo.setRating(it, r) }
    fun setFavorite(f: Boolean) = run { repo.setFavorite(it, f) }
    fun saveNotes(n: String) = run { repo.setNotes(it, n) }
    fun addWatch() = run { repo.addWatch(it) }
    fun undoWatch() = run { repo.undoLastWatch(it) }
    fun delete(onDone: () -> Unit) = run { repo.delete(it); onDone() }
}
