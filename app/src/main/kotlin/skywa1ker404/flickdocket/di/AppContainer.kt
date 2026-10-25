package skywa1ker404.flickdocket.di

import android.content.Context
import skywa1ker404.flickdocket.data.local.FlickDocketDatabase
import skywa1ker404.flickdocket.data.prefs.SettingsStore
import skywa1ker404.flickdocket.data.repo.LibraryRepository
import skywa1ker404.flickdocket.data.repo.RoomLibraryRepository

/** Manual dependency container: no DI framework or annotation processor beyond Room's KSP. */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val database by lazy { FlickDocketDatabase.create(appContext) }
    val settingsStore by lazy { SettingsStore(appContext) }
    val libraryRepository: LibraryRepository by lazy { RoomLibraryRepository(database) }
}
