package skywa1ker404.flickdocket.ui.screens.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import skywa1ker404.flickdocket.data.repo.LibraryRepository
import skywa1ker404.flickdocket.domain.LibraryStats

class StatsViewModel(repo: LibraryRepository) : ViewModel() {
    /** null while the first value loads. */
    val stats: StateFlow<LibraryStats?> = repo.observeStats().map<LibraryStats, LibraryStats?> { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
