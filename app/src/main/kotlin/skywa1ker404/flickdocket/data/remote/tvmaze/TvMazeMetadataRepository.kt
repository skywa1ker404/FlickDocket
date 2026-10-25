package skywa1ker404.flickdocket.data.remote.tvmaze

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.decodeFromString
import skywa1ker404.flickdocket.data.remote.tmdb.ResponseCache
import skywa1ker404.flickdocket.data.remote.tmdb.TmdbClient
import skywa1ker404.flickdocket.data.repo.MetadataRepository
import skywa1ker404.flickdocket.domain.*

/** Keyless TV metadata from TVmaze (CC BY-SA 4.0, attribution required). Movies are not available from this source. */
class TvMazeMetadataRepository(
    private val client: TmdbClient,
    private val cache: ResponseCache,
    private val region: () -> String = { "US" },
) : MetadataRepository {
    private val unsupported = MetadataResult.Failure(MetadataError.Unsupported)

    private fun page(items: List<TvmShowDto>) =
        ResultPage(1, 1, items.filterNot { it.isAnime() }.map { it.toSummary() })

    override suspend fun trending(type: MediaType, page: Int): MetadataResult<ResultPage<TmdbSummary>> {
        if (type == MediaType.MOVIE) return unsupported
        if (page > 1) return MetadataResult.Ok(ResultPage(page, 1, emptyList()))
        return client.get("schedule", mapOf("country" to region()), TTL_SCHEDULE) { body ->
            val shows = client.json.decodeFromString(ListSerializer(TvmEpisodeDto.serializer()), body)
                .mapNotNull { it.show ?: it.embedded?.show }.distinctBy { it.id }
                .sortedByDescending { it.rating?.average ?: 0.0 }.take(60)
            page(shows)
        }
    }

    override suspend fun search(type: MediaType, query: String, page: Int): MetadataResult<ResultPage<TmdbSummary>> {
        if (type == MediaType.MOVIE) return unsupported
        if (page > 1) return MetadataResult.Ok(ResultPage(page, 1, emptyList()))
        val q = query.trim()
        return if (isImdbId(q)) {
            client.get("lookup/shows", mapOf("imdb" to q.lowercase()), TTL_DETAIL) { page(listOf(client.json.decodeFromString<TvmShowDto>(it))) }
        } else {
            client.get("search/shows", mapOf("q" to q), TTL_SCHEDULE) { body ->
                page(client.json.decodeFromString(ListSerializer(TvmSearchHitDto.serializer()), body).map { it.show })
            }
        }
    }

    private suspend fun episodes(id: Long) =
        client.get("shows/$id/episodes", mapOf("specials" to "1"), TTL_DETAIL) {
            client.json.decodeFromString(ListSerializer(TvmEpisodeDto.serializer()), it)
        }

    override suspend fun detail(ref: TmdbRef): MetadataResult<TmdbDetail> {
        if (ref.type == MediaType.MOVIE) return unsupported
        val show = client.get("shows/${ref.id}", mapOf("embed[]" to "cast"), TTL_DETAIL) { client.json.decodeFromString<TvmShowDto>(it) }
        val eps = episodes(ref.id)
        return when {
            show is MetadataResult.Failure -> show
            eps is MetadataResult.Failure -> eps
            else -> {
                show as MetadataResult.Ok; eps as MetadataResult.Ok
                MetadataResult.Ok(show.value.toDetail(eps.value), show.stale || eps.stale)
            }
        }
    }

    override suspend fun season(ref: TmdbRef, number: Int) = episodes(ref.id).mapOk { seasonDetail(it, number) }
    override suspend fun verifyKey(force: Boolean): MetadataResult<Unit> = MetadataResult.Ok(Unit) // nothing to verify
    override suspend fun clearCache() = cache.clear()

    private companion object { const val HOUR = 3_600_000L; const val TTL_SCHEDULE = HOUR; const val TTL_DETAIL = 6 * HOUR }
}
