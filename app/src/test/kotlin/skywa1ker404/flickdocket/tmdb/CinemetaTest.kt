package skywa1ker404.flickdocket.tmdb

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import skywa1ker404.flickdocket.data.remote.cinemeta.*
import skywa1ker404.flickdocket.data.remote.tmdb.CachedBody
import skywa1ker404.flickdocket.data.remote.tmdb.ResponseCache
import skywa1ker404.flickdocket.data.remote.tmdb.TmdbClient
import skywa1ker404.flickdocket.data.repo.MetadataRepository
import skywa1ker404.flickdocket.data.repo.OpenMetadataRepository
import skywa1ker404.flickdocket.domain.*

// All titles and ids below are invented fixtures shaped like the Cinemeta responses observed on 2026-10-26.
class CinemetaTest {
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    private val series = """{"meta":{"id":"tt0000123","imdb_id":"tt0000123","type":"series","name":"Fixture Series","status":"Continuing",
      "released":"2020-03-01T00:00:00.000Z","releaseInfo":"2020–","runtime":"1h 5min","description":"An invented plot.",
      "poster":"https://images.metahub.space/poster/small/tt0000123/img","background":"https://images.metahub.space/background/medium/tt0000123/img",
      "genres":["Drama","Mystery"],"cast":["A Actor","B Actor"],"director":[],"writer":["W Writer"],"tvdb_id":4242,"imdbRating":"8.1",
      "country":"Canada",
      "videos":[
        {"id":"tt0000123:0:1","name":"Behind","season":0,"number":1,"firstAired":"2020-02-01T05:00:00.000Z"},
        {"id":"tt0000123:1:1","name":"One","season":1,"number":1,"firstAired":"2020-03-01T05:00:00.000Z","overview":"First.","thumbnail":"https://episodes.metahub.space/tt0000123/1/1/w780.jpg"},
        {"id":"tt0000123:1:2","name":"Two","season":1,"number":2,"firstAired":"2020-03-08T05:00:00.000Z","thumbnail":"http://evil.example/x.jpg"},
        {"id":"tt0000123:2:1","name":"Three","season":2,"episode":1,"firstAired":"2030-01-01T05:00:00.000Z"}]}}"""
    private val movie = """{"meta":{"id":"tt0000456","imdb_id":"tt0000456","type":"movie","name":"Fixture Movie","year":"1999","runtime":"136 min",
      "released":"1999-03-31T00:00:00.000Z","genres":["Action"],"cast":"X One, Y Two","director":["D Director"],"videos":[]}}"""

    private fun meta(s: String) = json.decodeFromString<CinemetaMetaEnvelope>(s).meta!!

    @Test fun imdbIdsRoundTripThroughLong() {
        assertEquals(133093L, ImdbIds.toLong("tt0133093")); assertEquals("tt0133093", ImdbIds.fromLong(133093))
        assertEquals("tt11540908", ImdbIds.fromLong(11540908)); assertNull(ImdbIds.toLong("tt12")); assertNull(ImdbIds.toLong("nm0000123"))
    }

    @Test fun runtimeFormats() {
        assertEquals(136, parseRuntime("136 min")); assertEquals(65, parseRuntime("1h 5min")); assertEquals(120, parseRuntime("2h"))
        assertNull(parseRuntime("0 min")); assertNull(parseRuntime(null)); assertNull(parseRuntime("n/a"))
    }

    @Test fun summaryUsesOnlyOwnImageHostAndIgnoresImdbRating() {
        val s = meta(series).toSummary(MediaType.TV)!!
        assertEquals(TmdbRef(MediaType.TV, 123, RefSource.CINEMETA), s.ref); assertEquals(2020, s.year); assertNull(s.rating)
        val amazon = meta("""{"meta":{"id":"tt0000789","name":"X","poster":"https://m.media-amazon.com/x.jpg"}}""").toSummary(MediaType.MOVIE)!!
        assertEquals("https://images.metahub.space/poster/small/tt0000789/img", amazon.posterPath)
        assertNull(meta("""{"meta":{"id":"bad","name":"X"}}""").toSummary(MediaType.MOVIE)) // no valid IMDb id
    }

    @Test fun seriesDetailMapsEpisodesSpecialsAndNextEpisode() {
        val d = meta(series).toDetail(MediaType.TV, today = "2026-10-26")!!
        assertEquals(65, d.runtimeMinutes); assertEquals(3, d.totalEpisodes); assertEquals(2, d.seasonCount)
        assertEquals("tt0000123", d.externalIds.imdb); assertEquals(4242L, d.externalIds.tvdb)
        assertEquals(listOf(1, 2, 0), d.seasons.map { it.number }) // specials last
        assertEquals(NextEpisode(2, 1, "Three", "2030-01-01"), d.nextEpisode)
        assertEquals(listOf("Writer"), d.crew.map { it.job })
        assertEquals(listOf("A Actor", "B Actor"), d.cast.map { it.name })
        val eps = seasonDetail(meta(series).videos, 1).episodes
        assertEquals(listOf("One", "Two"), eps.map { it.name }); assertEquals("2020-03-01", eps[0].airDate)
        assertEquals("https://episodes.metahub.space/tt0000123/1/1/w780.jpg", eps[0].stillPath); assertNull(eps[1].stillPath) // non-https host dropped
        assertEquals("Behind", seasonDetail(meta(series).videos, 0).episodes.single().name)
    }

    @Test fun movieDetailAcceptsCommaStringsAndHasNoEpisodes() {
        val d = meta(movie).toDetail(MediaType.MOVIE)!!
        assertEquals(listOf("X One", "Y Two"), d.cast.map { it.name }); assertEquals(1999, d.year); assertEquals(136, d.runtimeMinutes)
        assertNull(d.totalEpisodes); assertNull(d.nextEpisode); assertTrue(d.seasons.isEmpty()); assertEquals("Director", d.crew.single().job)
    }

    @Test fun nextEpisodeNullWhenNothingUpcoming() = assertNull(nextEpisodeOf(meta(series).videos, "2031-01-01"))

    @Test fun animeSeriesFilteredFromCatalogButNotMovies() {
        val cat = json.decodeFromString<CinemetaCatalogDto>("""{"metas":[
          {"id":"tt0000001","name":"Cartoon","genres":["Animation"],"country":"Japan"},
          {"id":"tt0000002","name":"Western Cartoon","genres":["Animation"],"country":"United States"}]}""")
        assertEquals(listOf("Western Cartoon"), cat.toPage(MediaType.TV).items.map { it.title })
        assertEquals(2, cat.toPage(MediaType.MOVIE).items.size)
    }

    // --- repository over a fake network
    private class Net(val respond: (Request) -> Response) { val calls = mutableListOf<Request>() }
    private fun repo(net: Net): CinemetaMetadataRepository {
        val http = OkHttpClient.Builder().addInterceptor(Interceptor { net.calls += it.request(); net.respond(it.request()) }).build()
        val cache = object : ResponseCache {
            val m = mutableMapOf<String, CachedBody>()
            override suspend fun get(key: String) = m[key]
            override suspend fun put(key: String, body: String, at: Long) { m[key] = CachedBody(body, at) }
            override suspend fun clear() = m.clear()
        }
        val c = TmdbClient(http, { error("Cinemeta must never read a credential") }, cache, baseUrl = "https://v3-cinemeta.strem.io/", requiresKey = false, sendLanguage = false, cachePrefix = "cinemeta:")
        return CinemetaMetadataRepository(c, cache)
    }
    private fun ok(r: Request, body: String) = Response.Builder().request(r).protocol(Protocol.HTTP_1_1).code(200).message("OK").body(body.toResponseBody("application/json".toMediaType())).build()

    @Test fun searchPathCarriesTermAndNoCredentials() = runTest {
        val net = Net { r -> ok(r, """{"metas":[{"id":"tt0000456","name":"Fixture Movie","releaseInfo":"1999"}]}""") }
        val res = repo(net).search(MediaType.MOVIE, "fix/ture movie", 1) as MetadataResult.Ok
        assertEquals("Fixture Movie", res.value.items.single().title)
        val url = net.calls.single().url
        assertEquals(listOf("catalog", "movie", "top", "search=fix%20ture%20movie.json"), url.encodedPathSegments)
        assertNull(url.queryParameter("api_key")); assertNull(net.calls.single().header("Authorization"))
    }

    @Test fun imdbIdSearchFetchesMetaAndMissingIsEmpty() = runTest {
        val net = Net { r -> ok(r, movie) }
        val res = repo(net).search(MediaType.MOVIE, "TT0000456", 1) as MetadataResult.Ok
        assertEquals(1, res.value.items.size); assertEquals("/meta/movie/tt0000456.json", net.calls.single().url.encodedPath)
        val missing = repo(Net { r -> Response.Builder().request(r).protocol(Protocol.HTTP_1_1).code(404).message("NF").body("".toResponseBody()).build() })
        assertEquals(0, ((missing.search(MediaType.TV, "tt0000456", 1)) as MetadataResult.Ok).value.items.size)
    }

    @Test fun laterPagesAreEmptyWithoutNetwork() = runTest {
        val net = Net { r -> ok(r, "{}") }
        val r = repo(net).trending(MediaType.MOVIE, 2) as MetadataResult.Ok
        assertTrue(r.value.items.isEmpty()); assertFalse(PagedItems<TmdbSummary>().append(r.value) { it.ref }.hasMore); assertTrue(net.calls.isEmpty())
    }

    @Test fun emptyMetaIsMalformedNotACrash() = runTest {
        val r = repo(Net { r -> ok(r, """{"meta":{}}""") }).detail(TmdbRef(MediaType.MOVIE, 456, RefSource.CINEMETA))
        assertEquals(MetadataResult.Failure(MetadataError.Malformed), r)
    }

    @Test fun detailAndSeasonUseOneMetaRequestPerCall() = runTest {
        val net = Net { r -> ok(r, series) }
        val repo = repo(net); val ref = TmdbRef(MediaType.TV, 123, RefSource.CINEMETA)
        assertEquals(3, ((repo.detail(ref)) as MetadataResult.Ok).value.totalEpisodes)
        assertEquals(2, (repo.season(ref, 1) as MetadataResult.Ok).value.episodes.size)
        assertEquals(1, net.calls.size) // second call served from the cache
        assertEquals("/meta/series/tt0000123.json", net.calls.single().url.encodedPath)
    }

    // --- open source = Cinemeta first, TVmaze fallback for TV
    private class Fake(val list: MetadataResult<ResultPage<TmdbSummary>>) : MetadataRepository {
        var calls = 0
        override suspend fun trending(type: MediaType, page: Int): MetadataResult<ResultPage<TmdbSummary>> { calls++; return list }
        override suspend fun search(type: MediaType, query: String, page: Int): MetadataResult<ResultPage<TmdbSummary>> { calls++; return list }
        override suspend fun detail(ref: TmdbRef): MetadataResult<TmdbDetail> { calls++; return MetadataResult.Failure(MetadataError.Offline) }
        override suspend fun season(ref: TmdbRef, number: Int): MetadataResult<SeasonDetail> { calls++; return MetadataResult.Failure(MetadataError.Offline) }
        override suspend fun verifyKey(force: Boolean): MetadataResult<Unit> = MetadataResult.Ok(Unit)
        override suspend fun clearCache() {}
    }
    private fun item(n: String, s: RefSource) = TmdbSummary(TmdbRef(MediaType.TV, 1, s), n, null, null, null, null, null)
    private fun page(vararg i: TmdbSummary) = MetadataResult.Ok(ResultPage(1, 1, i.toList()))

    @Test fun openFallsBackToTvMazeOnlyForTvWhenCinemetaFailsOrIsEmpty() = runTest {
        val tv = Fake(page(item("From TVmaze", RefSource.TVMAZE)))
        val failing = Fake(MetadataResult.Failure(MetadataError.Offline))
        val open = OpenMetadataRepository(failing, tv)
        assertEquals("From TVmaze", (open.search(MediaType.TV, "x", 1) as MetadataResult.Ok).value.items.single().title)
        assertEquals(MetadataResult.Failure(MetadataError.Offline), open.search(MediaType.MOVIE, "x", 1)) // movies never fall back
        assertEquals(1, tv.calls)
        val primary = Fake(page(item("From Cinemeta", RefSource.CINEMETA))); val tv2 = Fake(page())
        assertEquals("From Cinemeta", (OpenMetadataRepository(primary, tv2).trending(MediaType.TV, 1) as MetadataResult.Ok).value.items.single().title)
        assertEquals(0, tv2.calls)
        val emptyPrimary = Fake(page()); val tv3 = Fake(page(item("Found", RefSource.TVMAZE)))
        assertEquals("Found", (OpenMetadataRepository(emptyPrimary, tv3).search(MediaType.TV, "x", 1) as MetadataResult.Ok).value.items.single().title)
    }

    @Test fun openOpensTitlesBySourceInTheirRef() = runTest {
        val cin = Fake(page()); val tvm = Fake(page()); val open = OpenMetadataRepository(cin, tvm)
        open.detail(TmdbRef(MediaType.TV, 1, RefSource.TVMAZE)); open.detail(TmdbRef(MediaType.MOVIE, 1, RefSource.CINEMETA))
        assertEquals(1, tvm.calls); assertEquals(1, cin.calls)
    }

    @Test fun compactCatalogDropsEpisodesAndCapsItems() {
        val big = """{"metas":[{"id":"tt0000123","name":"S","poster":"p","videos":[{"id":"a"},{"id":"b"}],"trailers":[1]}]}"""
        val out = compactCatalog(big, json)
        assertFalse(out.contains("videos")); assertFalse(out.contains("trailers")); assertTrue(out.contains("tt0000123"))
        assertEquals("""{"metas":[]}""", compactCatalog("""{"nope":1}""", json))
        val many = """{"metas":[""" + (1..100).joinToString(",") { """{"id":"tt${1000000 + it}","name":"N$it"}""" } + "]}"
        assertEquals(60, json.decodeFromString<CinemetaCatalogDto>(compactCatalog(many, json)).metas.size)
    }

    @Test fun clientSurvivesBrokenCacheAndHugeBodies() = runTest {
        val broken = object : ResponseCache {
            override suspend fun get(key: String): CachedBody? = throw IllegalStateException("row too big for cursor window")
            override suspend fun put(key: String, body: String, at: Long) = throw IllegalStateException("disk full")
            override suspend fun clear() {}
        }
        val http = OkHttpClient.Builder().addInterceptor(Interceptor { ok(it.request(), """{"metas":[{"id":"tt0000456","name":"M"}]}""") }).build()
        val c = TmdbClient(http, { null }, broken, baseUrl = "https://v3-cinemeta.strem.io/", requiresKey = false, sendLanguage = false)
        val r = CinemetaMetadataRepository(c, broken).trending(MediaType.MOVIE, 1)
        assertEquals("M", (r as MetadataResult.Ok).value.items.single().title) // cache errors are only a miss
        val bad = c.get("x", ttlMs = 1) { error("parser blew up") }
        assertEquals(MetadataResult.Failure(MetadataError.Malformed), bad) // any parse failure is an error value, not a crash
    }

    @Test fun crashReportsRedactSecrets() {
        val t = skywa1ker404.flickdocket.CrashReporter.redact("GET https://api.example/3/x?api_key=abcdef0123&language=en Authorization: Bearer eyJhbGciOi.abc.def")
        assertFalse(t.contains("abcdef0123")); assertFalse(t.contains("eyJhbGciOi")); assertTrue(t.contains("language=en"))
    }
}
