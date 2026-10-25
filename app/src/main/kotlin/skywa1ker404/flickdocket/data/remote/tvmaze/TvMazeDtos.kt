package skywa1ker404.flickdocket.data.remote.tvmaze

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Field names follow TVmaze's public schema; the official page only documents endpoints, so parsing is
// deliberately tolerant (everything optional, unknown keys ignored) and verified only against fixtures.
@Serializable data class TvmImageDto(val medium: String? = null, val original: String? = null)
@Serializable data class TvmRatingDto(val average: Double? = null)
@Serializable data class TvmExternalsDto(val imdb: String? = null, val thetvdb: Long? = null)
@Serializable data class TvmPersonDto(val id: Long = 0, val name: String = "", val image: TvmImageDto? = null)
@Serializable data class TvmCharacterDto(val name: String? = null)
@Serializable data class TvmCastDto(val person: TvmPersonDto = TvmPersonDto(), val character: TvmCharacterDto? = null)
@Serializable data class TvmEmbeddedDto(val cast: List<TvmCastDto> = emptyList(), val show: TvmShowDto? = null)

@Serializable data class TvmShowDto(
    val id: Long, val name: String = "", val type: String? = null, val language: String? = null,
    val genres: List<String> = emptyList(), val status: String? = null,
    val runtime: Int? = null, val averageRuntime: Int? = null,
    val premiered: String? = null, val ended: String? = null,
    val rating: TvmRatingDto? = null, val externals: TvmExternalsDto? = null,
    val image: TvmImageDto? = null, val summary: String? = null, val url: String? = null,
    @SerialName("_embedded") val embedded: TvmEmbeddedDto? = null,
)

@Serializable data class TvmSearchHitDto(val show: TvmShowDto)

@Serializable data class TvmEpisodeDto(
    val id: Long = 0, val name: String? = null, val season: Int = 0, val number: Int? = null, val type: String? = null,
    val airdate: String? = null, val runtime: Int? = null, val image: TvmImageDto? = null, val summary: String? = null,
    /** Schedule responses carry the show directly or under _embedded depending on the endpoint. */
    val show: TvmShowDto? = null,
    @SerialName("_embedded") val embedded: TvmEmbeddedDto? = null,
)
