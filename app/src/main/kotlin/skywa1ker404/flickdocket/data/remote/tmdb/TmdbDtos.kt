package skywa1ker404.flickdocket.data.remote.tmdb

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable data class PageDto<T>(
    val page: Int = 1,
    val results: List<T> = emptyList(),
    @SerialName("total_pages") val totalPages: Int = 1,
)

/** One DTO for movie and TV list items: movies use title/release_date, TV uses name/first_air_date. */
@Serializable data class SummaryDto(
    val id: Long,
    val title: String? = null, val name: String? = null, val overview: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("first_air_date") val firstAirDate: String? = null,
    @SerialName("vote_average") val voteAverage: Double? = null,
    @SerialName("genre_ids") val genreIds: List<Int> = emptyList(),
    @SerialName("original_language") val originalLanguage: String? = null,
)

@Serializable data class GenreDto(val id: Int = 0, val name: String = "")
@Serializable data class CastDto(val id: Long = 0, val name: String = "", val character: String? = null, @SerialName("profile_path") val profilePath: String? = null, val order: Int = Int.MAX_VALUE)
@Serializable data class CrewDto(val id: Long = 0, val name: String = "", val job: String = "")
@Serializable data class CreditsDto(val cast: List<CastDto> = emptyList(), val crew: List<CrewDto> = emptyList())
@Serializable data class ExternalIdsDto(
    @SerialName("imdb_id") val imdbId: String? = null,
    @SerialName("tvdb_id") val tvdbId: Long? = null,
    @SerialName("wikidata_id") val wikidataId: String? = null,
)
@Serializable data class ReleaseEntryDto(val certification: String? = null, @SerialName("release_date") val releaseDate: String? = null, val type: Int = 0)
@Serializable data class ReleaseCountryDto(@SerialName("iso_3166_1") val country: String = "", @SerialName("release_dates") val releaseDates: List<ReleaseEntryDto> = emptyList())
@Serializable data class ReleaseDatesDto(val results: List<ReleaseCountryDto> = emptyList())
@Serializable data class ContentRatingDto(@SerialName("iso_3166_1") val country: String = "", val rating: String? = null)
@Serializable data class ContentRatingsDto(val results: List<ContentRatingDto> = emptyList())
@Serializable data class SeasonSummaryDto(
    @SerialName("season_number") val seasonNumber: Int = 0, val name: String? = null,
    @SerialName("episode_count") val episodeCount: Int = 0, @SerialName("air_date") val airDate: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
)

@Serializable data class NextEpisodeDto(
    @SerialName("air_date") val airDate: String? = null,
    @SerialName("season_number") val seasonNumber: Int = 0,
    @SerialName("episode_number") val episodeNumber: Int? = null,
    val name: String? = null,
)

@Serializable data class DetailDto(
    val id: Long,
    val title: String? = null, val name: String? = null, val tagline: String? = null, val overview: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("first_air_date") val firstAirDate: String? = null,
    @SerialName("last_air_date") val lastAirDate: String? = null,
    val runtime: Int? = null,
    @SerialName("episode_run_time") val episodeRunTime: List<Int> = emptyList(),
    @SerialName("number_of_episodes") val numberOfEpisodes: Int? = null,
    @SerialName("number_of_seasons") val numberOfSeasons: Int? = null,
    val status: String? = null,
    @SerialName("vote_average") val voteAverage: Double? = null,
    val genres: List<GenreDto> = emptyList(),
    @SerialName("next_episode_to_air") val nextEpisodeToAir: NextEpisodeDto? = null,
    val seasons: List<SeasonSummaryDto> = emptyList(),
    val credits: CreditsDto? = null,
    @SerialName("external_ids") val externalIds: ExternalIdsDto? = null,
    @SerialName("imdb_id") val imdbId: String? = null,
    @SerialName("release_dates") val releaseDates: ReleaseDatesDto? = null,
    @SerialName("content_ratings") val contentRatings: ContentRatingsDto? = null,
)

@Serializable data class EpisodeDto(
    @SerialName("episode_number") val episodeNumber: Int = 0, val name: String? = null, val overview: String? = null,
    @SerialName("air_date") val airDate: String? = null, val runtime: Int? = null, @SerialName("still_path") val stillPath: String? = null,
)
@Serializable data class SeasonDetailDto(
    @SerialName("season_number") val seasonNumber: Int = 0, val name: String? = null, val overview: String? = null,
    val episodes: List<EpisodeDto> = emptyList(),
)

@Serializable data class ImagesConfigDto(
    @SerialName("secure_base_url") val secureBaseUrl: String = "",
    @SerialName("poster_sizes") val posterSizes: List<String> = emptyList(),
    @SerialName("backdrop_sizes") val backdropSizes: List<String> = emptyList(),
    @SerialName("profile_sizes") val profileSizes: List<String> = emptyList(),
    @SerialName("still_sizes") val stillSizes: List<String> = emptyList(),
)
@Serializable data class FindDto(
    @SerialName("movie_results") val movieResults: List<SummaryDto> = emptyList(),
    @SerialName("tv_results") val tvResults: List<SummaryDto> = emptyList(),
)
@Serializable data class ConfigurationDto(val images: ImagesConfigDto = ImagesConfigDto())
