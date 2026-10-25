package skywa1ker404.flickdocket.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import skywa1ker404.flickdocket.di.AppContainer
import skywa1ker404.flickdocket.ui.screens.detail.DetailViewModel
import skywa1ker404.flickdocket.ui.screens.library.LibraryViewModel
import skywa1ker404.flickdocket.ui.screens.settings.SettingsViewModel
import skywa1ker404.flickdocket.ui.screens.stats.StatsViewModel

class AppViewModelFactory(private val c: AppContainer) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when (modelClass) {
        LibraryViewModel::class.java -> LibraryViewModel(c.libraryRepository)
        StatsViewModel::class.java -> StatsViewModel(c.libraryRepository)
        SettingsViewModel::class.java -> SettingsViewModel(c.settingsStore)
        DetailViewModel::class.java -> DetailViewModel(c.libraryRepository)
        else -> error("Unknown ViewModel ${modelClass.name}")
    } as T
}
