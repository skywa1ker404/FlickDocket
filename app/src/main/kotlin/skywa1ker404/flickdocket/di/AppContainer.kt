package skywa1ker404.flickdocket.di

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import okhttp3.OkHttpClient
import skywa1ker404.flickdocket.data.local.FlickDocketDatabase
import skywa1ker404.flickdocket.data.local.RoomResponseCache
import skywa1ker404.flickdocket.data.prefs.SettingsStore
import kotlinx.coroutines.flow.first
import skywa1ker404.flickdocket.data.remote.tmdb.RateLimiter
import skywa1ker404.flickdocket.data.remote.tmdb.TmdbClient
import skywa1ker404.flickdocket.data.remote.tvmaze.TvMazeMetadataRepository
import skywa1ker404.flickdocket.data.repo.SwitchingMetadataRepository
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
    private fun region() = Locale.getDefault().country.ifBlank { "US" }
    private val tvMazeClient by lazy {
        TmdbClient(
            http = httpClient, keyProvider = { null }, cache = responseCache,
            // TVmaze documents at least 20 calls per 10 seconds per IP; stay under it.
            limiter = RateLimiter(maxRequests = 15, windowMs = 10_000),
            baseUrl = "https://api.tvmaze.com/", requiresKey = false, sendLanguage = false, cachePrefix = "tvmaze:",
        )
    }
    val metadataRepository: MetadataRepository by lazy {
        SwitchingMetadataRepository(
            mode = { settingsStore.settings.first().metadataMode },
            tmdb = TmdbMetadataRepository(tmdbClient, responseCache, imageConfig, ::region),
            open = TvMazeMetadataRepository(tvMazeClient, responseCache, ::region),
        )
    }
}
