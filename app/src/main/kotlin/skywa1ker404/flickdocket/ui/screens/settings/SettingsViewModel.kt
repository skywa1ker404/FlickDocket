package skywa1ker404.flickdocket.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import skywa1ker404.flickdocket.data.prefs.SettingsStore
import skywa1ker404.flickdocket.domain.MotionPreference
import skywa1ker404.flickdocket.domain.ThemeMode

class SettingsViewModel(private val store: SettingsStore) : ViewModel() {
    fun setTheme(v: ThemeMode) { viewModelScope.launch { store.setTheme(v) } }
    fun setDynamic(v: Boolean) { viewModelScope.launch { store.setDynamicColor(v) } }
    fun setMotion(v: MotionPreference) { viewModelScope.launch { store.setMotion(v) } }
}
