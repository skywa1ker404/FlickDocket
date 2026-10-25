package skywa1ker404.flickdocket.data.repo

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.decodeFromString
import skywa1ker404.flickdocket.data.remote.tmdb.*
import skywa1ker404.flickdocket.domain.*

interface MetadataRepository {
    suspend fun trending(type: MediaType, page: Int): MetadataResult<ResultPage<TmdbSummary>>
    suspend fun search(type: MediaType, query: String, page: Int): MetadataResult<ResultPage<TmdbSummary>>
    suspend fun detail(ref: TmdbRef): MetadataResult<TmdbDetail>
    suspend fun season(tvId: Long, number: Int): MetadataResult<SeasonDetail>
    /** Calls /configuration (also refreshes image sizes). [force] bypasses the cache to really test the credential. */
    suspend fun verifyKey(force: Boolean): MetadataResult<Unit>
    suspend fun clearCache()
}

class TmdbMetadataRepository(
    private val client: TmdbClient,
    private val cache: ResponseCache,
    private val imageConfig: MutableStateFlow<ImageConfig>,
    private val region: () -> String = { "US" },
) : MetadataRepository {
    private fun seg(t: MediaType) = if (t == MediaType.MOVIE) "movie" else "tv"

    override suspend fun trending(type: MediaType, page: Int) =
        client.get("trending/${seg(type)}/week", mapOf("page" to page.toString()), TTL_TRENDING) {
            client.json.decodeFromString<PageDto<SummaryDto>>(it).toPage(type)
        }

    override suspend fun search(type: MediaType, query: String, page: Int) =
        client.get("search/${seg(type)}", mapOf("query" to query, "page" to page.toString(), "include_adult" to "false"), TTL_SEARCH) {
            client.json.decodeFromString<PageDto<SummaryDto>>(it).toPage(type)
        }

    override suspend fun detail(ref: TmdbRef): MetadataResult<TmdbDetail> {
        val append = if (ref.type == MediaType.MOVIE) "credits,external_ids,release_dates" else "credits,external_ids,content_ratings"
        return client.get("${seg(ref.type)}/${ref.id}", mapOf("append_to_response" to append), TTL_DETAIL) {
            client.json.decodeFromString<DetailDto>(it).toDetail(ref.type, region())
        }
    }

    override suspend fun season(tvId: Long, number: Int) =
        client.get("tv/$tvId/season/$number", emptyMap(), TTL_DETAIL) { client.json.decodeFromString<SeasonDetailDto>(it).toDomain() }

    override suspend fun verifyKey(force: Boolean): MetadataResult<Unit> =
        client.get("configuration", emptyMap(), TTL_CONFIG, forceRefresh = force) {
            client.json.decodeFromString<ConfigurationDto>(it).toImageConfig()
        }.mapOk { cfg -> imageConfig.value = cfg }

    override suspend fun clearCache() = cache.clear()

    private companion object {
        const val HOUR = 3_600_000L
        const val TTL_SEARCH = HOUR
        const val TTL_TRENDING = 6 * HOUR
        const val TTL_DETAIL = 24 * HOUR
        const val TTL_CONFIG = 7 * 24 * HOUR
    }
}
