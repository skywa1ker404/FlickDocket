package skywa1ker404.flickdocket.di

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import okhttp3.OkHttpClient
import skywa1ker404.flickdocket.data.local.FlickDocketDatabase
import skywa1ker404.flickdocket.data.local.RoomResponseCache
import skywa1ker404.flickdocket.data.prefs.SettingsStore
import skywa1ker404.flickdocket.data.remote.tmdb.TmdbClient
import skywa1ker404.flickdocket.data.repo.LibraryRepository
import skywa1ker404.flickdocket.data.repo.MetadataRepository
import skywa1ker404.flickdocket.data.repo.RoomLibraryRepository
import skywa1ker404.flickdocket.data.repo.TmdbMetadataRepository
import skywa1ker404.flickdocket.data.secure.CredentialStore
import skywa1ker404.flickdocket.data.secure.EncryptedCredentialStore
import skywa1ker404.flickdocket.domain.ImageConfig
import java.util.Locale
import java.util.concurrent.TimeUnit

/** Manual dependency container: no DI framework or annotation processor beyond Room's KSP. */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val database by lazy { FlickDocketDatabase.create(appContext) }
    val settingsStore by lazy { SettingsStore(appContext) }
    val libraryRepository: LibraryRepository by lazy { RoomLibraryRepository(database) }

    /** The only TMDB credential is the one the user enters; nothing is embedded in the app. */
    val credentialStore: CredentialStore by lazy { EncryptedCredentialStore(appContext) }
    val imageConfig = MutableStateFlow(ImageConfig())

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS).readTimeout(20, TimeUnit.SECONDS).callTimeout(30, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                chain.proceed(chain.request().newBuilder().header("User-Agent", "FlickDocket/0.2 (+https://github.com/skywa1ker404/FlickDocket)").build())
            }.build()
    }
    private val responseCache by lazy { RoomResponseCache(database.cacheDao()) }
    private val tmdbClient by lazy {
        TmdbClient(
            http = httpClient, keyProvider = credentialStore::tmdbKey, cache = responseCache,
            language = { Locale.getDefault().toLanguageTag().takeIf { it != "und" } ?: "en-US" },
        )
    }
    val metadataRepository: MetadataRepository by lazy {
        TmdbMetadataRepository(tmdbClient, responseCache, imageConfig) { Locale.getDefault().country.ifBlank { "US" } }
    }
}
