package skywa1ker404.flickdocket.data.remote.cinemeta

import kotlinx.serialization.decodeFromString
import skywa1ker404.flickdocket.data.remote.tmdb.ResponseCache
import skywa1ker404.flickdocket.data.remote.tmdb.TmdbClient
import skywa1ker404.flickdocket.data.repo.MetadataRepository
import skywa1ker404.flickdocket.domain.*

/**
 * Keyless movie and TV metadata from Stremio's public Cinemeta catalogue, keyed by IMDb id. The service is unofficial for
 * third-party apps (no published terms), so it is rate limited, cached, labelled in the UI and replaceable by TMDB.
 * Only page one of a catalogue is used: the page size is not documented. IMDb ratings in the payload are ignored.
 */
class CinemetaMetadataRepository(
    private val client: TmdbClient,
    private val cache: ResponseCache,
) : MetadataRepository {
    private fun seg(t: MediaType) = if (t == MediaType.MOVIE) "movie" else "series"
    private fun empty(page: Int) = MetadataResult.Ok(ResultPage<TmdbSummary>(page, page, emptyList()))

    override suspend fun trending(type: MediaType, page: Int): MetadataResult<ResultPage<TmdbSummary>> {
        if (page > 1) return empty(page)
        return client.get("catalog/${seg(type)}/top/skip=0.json", emptyMap(), TTL_TRENDING, prepare = { compactCatalog(it, client.json) }) {
            client.json.decodeFromString<CinemetaCatalogDto>(it).toPage(type)
        }
    }

    override suspend fun search(type: MediaType, query: String, page: Int): MetadataResult<ResultPage<TmdbSummary>> {
        if (page > 1) return empty(page)
        val q = query.trim().replace('/', ' ').take(100)
        if (q.isEmpty()) return empty(page)
        if (ImdbIds.isValid(q.lowercase())) {
            return when (val r = fetchMeta(type, q.lowercase())) {
                is MetadataResult.Ok -> MetadataResult.Ok(ResultPage(1, 1, listOfNotNull(r.value.toSummary(type))), r.stale)
                is MetadataResult.Failure -> if (r.error == MetadataError.NotFound) empty(1) else r
            }
        }
        return client.get("catalog/${seg(type)}/top/search=$q.json", emptyMap(), TTL_SEARCH, prepare = { compactCatalog(it, client.json) }) {
            client.json.decodeFromString<CinemetaCatalogDto>(it).toPage(type)
        }
    }

    private suspend fun fetchMeta(type: MediaType, imdb: String) =
        client.get("meta/${seg(type)}/$imdb.json", emptyMap(), TTL_DETAIL) {
            client.json.decodeFromString<CinemetaMetaEnvelope>(it).meta?.takeIf { m -> m.name != null && m.imdb() != null }
                ?: throw IllegalArgumentException("empty meta")
        }

    override suspend fun detail(ref: TmdbRef): MetadataResult<TmdbDetail> =
        fetchMeta(ref.type, ImdbIds.fromLong(ref.id)).let { r ->
            when (r) {
                is MetadataResult.Failure -> r
                is MetadataResult.Ok -> r.value.toDetail(ref.type)?.let { MetadataResult.Ok(it, r.stale) } ?: MetadataResult.Failure(MetadataError.Malformed)
            }
        }

    override suspend fun season(ref: TmdbRef, number: Int): MetadataResult<SeasonDetail> =
        fetchMeta(ref.type, ImdbIds.fromLong(ref.id)).mapOk { seasonDetail(it.videos, number) }

    override suspend fun verifyKey(force: Boolean): MetadataResult<Unit> = MetadataResult.Ok(Unit)
    override suspend fun clearCache() = cache.clear()

    private companion object { const val HOUR = 3_600_000L; const val TTL_SEARCH = HOUR; const val TTL_TRENDING = 6 * HOUR; const val TTL_DETAIL = 6 * HOUR }
}
