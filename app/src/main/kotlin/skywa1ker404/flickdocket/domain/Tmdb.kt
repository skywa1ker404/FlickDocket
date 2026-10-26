package skywa1ker404.flickdocket.domain

/** Which catalogue an id belongs to. Names stay "Tmdb*" for the shared remote-title types. */
enum class RefSource { TMDB, TVMAZE, CINEMETA }
/** User-selectable metadata source: OPEN needs no setup (Cinemeta + TVmaze, keyed by IMDb ids); TMDB needs the user's own key. */
enum class MetadataMode { OPEN, TMDB }

/** IMDb ids ("tt0133093") are carried as their numeric part in [TmdbRef.id] for the Cinemeta source. */
object ImdbIds {
    private val VALID = Regex("^tt\\d{6,10}$")
    fun isValid(id: String?) = id != null && VALID.matches(id)
    fun toLong(id: String): Long? = if (isValid(id)) id.substring(2).toLongOrNull() else null
    fun fromLong(n: Long): String = "tt" + n.toString().padStart(7, '0')
}

data class TmdbRef(val type: MediaType, val id: Long, val source: RefSource = RefSource.TMDB)

data class TmdbSummary(
    val ref: TmdbRef, val title: String, val year: Int?, val overview: String?,
    val posterPath: String?, val backdropPath: String?, val rating: Float?,
)

data class ResultPage<T>(val page: Int, val totalPages: Int, val items: List<T>)

/** Accumulates pages without duplicates; TMDB can repeat an item across page boundaries. */
data class PagedItems<T>(val items: List<T> = emptyList(), val page: Int = 0, val totalPages: Int = 0) {
    val hasMore: Boolean get() = page < totalPages
    fun append(next: ResultPage<T>, key: (T) -> Any): PagedItems<T> {
        val seen = items.mapTo(HashSet(), key)
        return PagedItems(items + next.items.filter { seen.add(key(it)) }, next.page, next.totalPages)
    }
}

data class CastMember(val id: Long, val name: String, val character: String?, val profilePath: String?)
data class CrewMember(val id: Long, val name: String, val job: String)

enum class ReleaseKind { PREMIERE, THEATRICAL_LIMITED, THEATRICAL, DIGITAL, PHYSICAL, TV }
data class ReleaseInfo(val country: String, val date: String?, val certification: String?, val kind: ReleaseKind)

data class SeasonSummary(val number: Int, val name: String?, val episodeCount: Int, val airDate: String?, val posterPath: String?) {
    val isSpecials: Boolean get() = number == 0
}
data class EpisodeInfo(val number: Int, val name: String?, val overview: String?, val airDate: String?, val runtimeMinutes: Int?, val stillPath: String?)
data class SeasonDetail(val number: Int, val name: String?, val overview: String?, val episodes: List<EpisodeInfo>)

data class NextEpisode(val season: Int, val number: Int?, val name: String?, val airDate: String)

data class ExternalIds(val imdb: String? = null, val tvdb: Long? = null, val wikidata: String? = null)

data class TmdbDetail(
    val ref: TmdbRef, val title: String, val tagline: String?, val overview: String?,
    val posterPath: String?, val backdropPath: String?, val year: Int?, val firstAirDate: String?, val lastAirDate: String?,
    val runtimeMinutes: Int?, val totalEpisodes: Int?, val seasonCount: Int?, val status: String?,
    val genres: List<String>, val rating: Float?, val certification: String?,
    val cast: List<CastMember>, val crew: List<CrewMember>, val releases: List<ReleaseInfo>,
    val seasons: List<SeasonSummary>, val externalIds: ExternalIds,
    /** Link back to the source page, required for attribution by some sources. */
    val sourceUrl: String? = null,
    /** Next unaired episode, when the source knows one. */
    val nextEpisode: NextEpisode? = null,
)

/** Minimal detail built from the local record, so a tracked title still opens when the source is unreachable. */
fun TitleDetail.toOfflineDetail(ref: TmdbRef) = TmdbDetail(
    ref = ref, title = title, tagline = null, overview = overview, posterPath = posterPath, backdropPath = backdropPath,
    year = year, firstAirDate = null, lastAirDate = null, runtimeMinutes = runtimeMinutes, totalEpisodes = totalEpisodes,
    seasonCount = null, status = null, genres = emptyList(), rating = null, certification = null,
    cast = emptyList(), crew = emptyList(), releases = emptyList(), seasons = emptyList(),
    externalIds = ExternalIds(imdb = imdbId),
)

sealed interface MetadataError {
    data object MissingCredentials : MetadataError
    data object Unauthorized : MetadataError
    data object NotFound : MetadataError
    data class RateLimited(val retryAfterSeconds: Int?) : MetadataError
    data object Offline : MetadataError
    data class Server(val code: Int) : MetadataError
    data object Malformed : MetadataError
    /** The active source cannot provide this (e.g. movies without TMDB). */
    data object Unsupported : MetadataError

    /** Worth falling back to cached data for; credentials and missing-title errors are not. */
    val isTransient: Boolean get() = this is Offline || this is RateLimited || this is Server
}

sealed interface MetadataResult<out T> {
    /** [stale] = served from cache that is past its TTL because a refresh was not possible. */
    data class Ok<T>(val value: T, val stale: Boolean = false) : MetadataResult<T>
    data class Failure(val error: MetadataError) : MetadataResult<Nothing>
}

inline fun <T, R> MetadataResult<T>.mapOk(f: (T) -> R): MetadataResult<R> = when (this) {
    is MetadataResult.Ok -> MetadataResult.Ok(f(value), stale)
    is MetadataResult.Failure -> this
}

/** Heuristic only: TMDB has no anime flag. Japanese-language animation is treated as anime and hidden from TV results. */
object AnimeFilter {
    const val ANIMATION_GENRE_ID = 16
    fun isAnime(genreIds: List<Int>, originalLanguage: String?): Boolean =
        ANIMATION_GENRE_ID in genreIds && originalLanguage.equals("ja", ignoreCase = true)

    /** TVmaze variant: it names genres and languages instead of using TMDB ids. */
    fun isAnimeByName(genres: List<String>, language: String?): Boolean =
        genres.any { it.equals("Anime", ignoreCase = true) } ||
            (genres.any { it.equals("Animation", ignoreCase = true) } && language.equals("Japanese", ignoreCase = true))
}

object TmdbKey {
    private val V3 = Regex("^[0-9a-fA-F]{32}$")
    fun isValidFormat(k: String): Boolean =
        V3.matches(k) || (k.count { it == '.' } == 2 && k.length in 40..2000 && k.all { it.isLetterOrDigit() || it == '-' || it == '_' || it == '.' })
    /** A v4 read access token is a JWT (dots); a v3 key is 32 hex chars sent as a query parameter. */
    fun isBearer(k: String): Boolean = k.contains('.')
}

/** Image URL builder. Defaults are fallbacks only; /configuration replaces them once fetched. */
data class ImageConfig(
    val baseUrl: String = "https://image.tmdb.org/t/p/",
    val posterSizes: List<String> = listOf("w92", "w154", "w185", "w342", "w500", "w780", "original"),
    val backdropSizes: List<String> = listOf("w300", "w780", "w1280", "original"),
    val profileSizes: List<String> = listOf("w45", "w185", "h632", "original"),
    val stillSizes: List<String> = listOf("w92", "w185", "w300", "original"),
) {
    fun poster(path: String?, minWidthPx: Int) = url(path, posterSizes, minWidthPx)
    fun backdrop(path: String?, minWidthPx: Int) = url(path, backdropSizes, minWidthPx)
    fun profile(path: String?, minWidthPx: Int) = url(path, profileSizes, minWidthPx)
    fun still(path: String?, minWidthPx: Int) = url(path, stillSizes, minWidthPx)

    private fun url(path: String?, sizes: List<String>, minWidthPx: Int): String? {
        if (path.isNullOrBlank()) return null
        // Some sources (TVmaze) return complete URLs; accept only https, never http or other schemes.
        if (path.contains("://")) return path.takeIf { it.startsWith("https://") }
        val size = sizes.mapNotNull { s -> if (s.startsWith("w")) s.drop(1).toIntOrNull()?.let { it to s } else null }
            .sortedBy { it.first }.firstOrNull { it.first >= minWidthPx }?.second ?: "original"
        return baseUrl.trimEnd('/') + "/" + size + (if (path.startsWith("/")) path else "/$path")
    }
}
