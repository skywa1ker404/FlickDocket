package skywa1ker404.flickdocket.domain

enum class MediaType { MOVIE, TV }
enum class WatchStatus { PLANNING, WATCHING, COMPLETED, ON_HOLD, DROPPED }
enum class MetadataSource { MANUAL, TMDB }
enum class ThemeMode { SYSTEM, LIGHT, DARK, AMOLED }
enum class MotionPreference { FOLLOW_SYSTEM, REDUCED, FULL }
enum class LibrarySort { RECENTLY_UPDATED, TITLE, RATING, RELEASE }

/** Rating uses a 0..100 internal scale (display = value / 10, half-star steps of 5). */
const val RATING_MAX = 100
const val RATING_STEP = 5

data class LibraryItem(
    val titleId: Long,
    val title: String,
    val mediaType: MediaType,
    val year: Int?,
    val posterPath: String?,
    val status: WatchStatus,
    val rating: Int?,
    val favorite: Boolean,
    val watchCount: Int,
    val totalEpisodes: Int?,
    val updatedAt: Long,
)

data class TitleDetail(
    val titleId: Long,
    val title: String,
    val mediaType: MediaType,
    val year: Int?,
    val runtimeMinutes: Int?,
    val totalEpisodes: Int?,
    val overview: String?,
    val posterPath: String?,
    val source: MetadataSource,
    val status: WatchStatus,
    val rating: Int?,
    val favorite: Boolean,
    val notes: String,
    val watchCount: Int,
    val addedAt: Long,
    val updatedAt: Long,
)

data class WatchRecord(val id: Long, val watchedAt: Long)

data class LibraryStats(
    val totalTitles: Int = 0,
    val byStatus: Map<WatchStatus, Int> = emptyMap(),
    val watchEvents: Int = 0,
    val minutesWatched: Long = 0,
)

data class NewTitleDraft(
    val name: String,
    val mediaType: MediaType,
    val year: Int? = null,
    val runtimeMinutes: Int? = null,
    val totalEpisodes: Int? = null,
    val status: WatchStatus = WatchStatus.PLANNING,
)

data class LibraryQuery(
    val text: String = "",
    val statuses: Set<WatchStatus> = emptySet(),
    val mediaType: MediaType? = null,
    val favoritesOnly: Boolean = false,
    val sort: LibrarySort = LibrarySort.RECENTLY_UPDATED,
) {
    val isFiltering: Boolean get() = text.isNotBlank() || statuses.isNotEmpty() || mediaType != null || favoritesOnly
}
