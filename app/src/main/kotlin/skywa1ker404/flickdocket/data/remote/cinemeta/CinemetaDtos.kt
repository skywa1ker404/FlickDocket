package skywa1ker404.flickdocket.data.remote.cinemeta

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

// Field names follow the responses observed from v3-cinemeta.strem.io on 2026-10-26. The service publishes no schema or
// terms, so everything is optional, loosely typed where the payload varies, and checked only against fixtures.
@Serializable data class CinemetaCatalogDto(val metas: List<CinemetaMetaDto> = emptyList())
@Serializable data class CinemetaMetaEnvelope(val meta: CinemetaMetaDto? = null)

@Serializable data class CinemetaMetaDto(
    val id: String? = null,
    @SerialName("imdb_id") val imdbId: String? = null,
    val name: String? = null,
    val type: String? = null,
    val description: String? = null,
    val poster: String? = null,
    val background: String? = null,
    val released: String? = null,
    val year: JsonElement? = null,
    val releaseInfo: JsonElement? = null,
    val runtime: JsonElement? = null,
    val status: String? = null,
    val country: JsonElement? = null,
    val genres: JsonElement? = null,
    val genre: JsonElement? = null,
    val cast: JsonElement? = null,
    val director: JsonElement? = null,
    val writer: JsonElement? = null,
    @SerialName("tvdb_id") val tvdbId: JsonElement? = null,
    val videos: List<CinemetaVideoDto> = emptyList(),
)

@Serializable data class CinemetaVideoDto(
    val id: String? = null,
    val name: String? = null,
    val season: Int? = null,
    val number: Int? = null,
    val episode: Int? = null,
    val firstAired: String? = null,
    val released: String? = null,
    val overview: String? = null,
    val description: String? = null,
    val thumbnail: String? = null,
)
