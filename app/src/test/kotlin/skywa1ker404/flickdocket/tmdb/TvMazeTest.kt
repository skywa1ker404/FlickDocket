package skywa1ker404.flickdocket.tmdb

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.builtins.ListSerializer
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
import skywa1ker404.flickdocket.data.remote.tmdb.*
import skywa1ker404.flickdocket.data.remote.tvmaze.*
import skywa1ker404.flickdocket.data.repo.SwitchingMetadataRepository
import skywa1ker404.flickdocket.domain.*

/** Fixtures are invented and follow TVmaze's public schema; they are not copied TVmaze content. */
class TvMazeTest {
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }
    private val show = """{"id":5,"name":"Fixture Show","type":"Scripted","language":"English","genres":["Drama"],"status":"Ended","runtime":null,"averageRuntime":45,
      "premiered":"2012-04-01","ended":"2015-06-01","rating":{"average":8.1},"externals":{"imdb":"tt0000005","thetvdb":77},
      "image":{"medium":"http://static.example/m.jpg","original":"https://static.example/o.jpg"},"summary":"<p>An <b>invented</b> plot &amp; more.</p>",
      "url":"https://www.tvmaze.com/shows/5/fixture-show",
      "_embedded":{"cast":[{"person":{"id":1,"name":"A Actor","image":{"medium":"https://static.example/p.jpg"}},"character":{"name":"Hero"}}]}}"""
    private val episodes = """[
      {"id":1,"name":"Pilot","season":1,"number":1,"type":"regular","airdate":"2012-04-01","runtime":45},
      {"id":2,"name":"Two","season":1,"number":2,"type":"regular","airdate":"2012-04-08"},
      {"id":3,"name":"Xmas Special","season":1,"number":null,"type":"significant_special","airdate":"2012-12-25"},
      {"id":4,"name":"S2E1","season":2,"number":1,"type":"regular","airdate":"2013-04-01"}]"""

    private fun eps() = json.decodeFromString(ListSerializer(TvmEpisodeDto.serializer()), episodes)

    @Test fun htmlSummaryBecomesPlainText() = assertEquals("An invented plot & more.", stripHtml("<p>An <b>invented</b> plot &amp; more.</p>"))

    @Test fun detailMapsShowCastIdsAndAttributionLink() {
        val d = json.decodeFromString<TvmShowDto>(show).toDetail(eps())
        assertEquals(RefSource.TVMAZE, d.ref.source); assertEquals(2012, d.year); assertEquals(45, d.runtimeMinutes)
        assertEquals("tt0000005", d.externalIds.imdb); assertEquals(77L, d.externalIds.tvdb)
        assertEquals("https://static.example/o.jpg", d.posterPath)
        assertEquals("Hero", d.cast.single().character); assertEquals("https://www.tvmaze.com/shows/5/fixture-show", d.sourceUrl)
        assertEquals(3, d.totalEpisodes) // regular episodes only
    }

    @Test fun specialsBecomeSeasonZeroAndAreNotCounted() {
        val s = seasonsOf(eps())
        assertEquals(listOf(0, 1, 2), s.map { it.number })
        assertEquals(1, s.first().episodeCount); assertTrue(s.first().isSpecials)
        val sp = seasonDetail(eps(), 0).episodes.single()
        assertEquals("Xmas Special", sp.name); assertEquals(1, sp.number) // missing number falls back to position
        assertEquals(listOf("Pilot", "Two"), seasonDetail(eps(), 1).episodes.map { it.name })
    }

    @Test fun nonHttpsImagesAreUpgradedOrDropped() {
        assertEquals("https://static.example/m.jpg", TvmImageDto(medium = "http://static.example/m.jpg").url(false))
        assertNull(TvmImageDto(medium = "javascript:alert(1)").url(false))
        assertNull(ImageConfig().poster("http://evil.example/x.jpg", 100))
        assertEquals("https://ok.example/x.jpg", ImageConfig().poster("https://ok.example/x.jpg", 100))
    }

    @Test fun animeDetectedByGenreOrJapaneseAnimation() {
        assertTrue(AnimeFilter.isAnimeByName(listOf("Anime"), "English"))
        assertTrue(AnimeFilter.isAnimeByName(listOf("Animation"), "Japanese"))
        assertFalse(AnimeFilter.isAnimeByName(listOf("Animation"), "English"))
    }

    @Test fun imdbIdDetection() {
        assertTrue(isImdbId(" tt0903747 ")); assertTrue(isImdbId("TT0903747"))
        assertFalse(isImdbId("tt12")); assertFalse(isImdbId("breaking bad"))
    }

    // --- repository over a fake network
    private class Net(val respond: (Request) -> Response) { val calls = mutableListOf<Request>() }
    private fun repo(net: Net): TvMazeMetadataRepository {
        val http = OkHttpClient.Builder().addInterceptor(Interceptor { net.calls += it.request(); net.respond(it.request()) }).build()
        val cache = object : ResponseCache {
            val m = mutableMapOf<String, CachedBody>()
            override suspend fun get(key: String) = m[key]
            override suspend fun put(key: String, body: String, at: Long) { m[key] = CachedBody(body, at) }
            override suspend fun clear() = m.clear()
        }
        val c = TmdbClient(http, { error("TVmaze must never read a credential") }, cache, baseUrl = "https://api.tvmaze.com/", requiresKey = false, sendLanguage = false, cachePrefix = "tvmaze:")
        return TvMazeMetadataRepository(c, cache) { "US" }
    }
    private fun ok(r: Request, body: String) = Response.Builder().request(r).protocol(Protocol.HTTP_1_1).code(200).message("OK").body(body.toResponseBody("application/json".toMediaType())).build()

    @Test fun searchSendsNoCredentialAndHidesAnime() = runTest {
        val net = Net { r -> ok(r, """[{"score":1,"show":$show},{"score":0.5,"show":{"id":9,"name":"Anime","genres":["Anime"],"language":"Japanese"}}]""") }
        val res = repo(net).search(MediaType.TV, "fixture", 1) as MetadataResult.Ok
        assertEquals(listOf("Fixture Show"), res.value.items.map { it.title })
        val url = net.calls.single().url
        assertEquals("fixture", url.queryParameter("q")); assertNull(url.queryParameter("api_key")); assertNull(url.queryParameter("language"))
        assertNull(net.calls.single().header("Authorization"))
    }

    @Test fun imdbIdSearchUsesLookup() = runTest {
        val net = Net { r -> ok(r, show) }
        val res = repo(net).search(MediaType.TV, "tt0000005", 1) as MetadataResult.Ok
        assertEquals(1, res.value.items.size)
        assertEquals("/lookup/shows", net.calls.single().url.encodedPath); assertEquals("tt0000005", net.calls.single().url.queryParameter("imdb"))
    }

    @Test fun moviesAreUnsupportedWithoutNetworkCalls() = runTest {
        val net = Net { r -> ok(r, "[]") }
        assertEquals(MetadataResult.Failure(MetadataError.Unsupported), repo(net).search(MediaType.MOVIE, "x", 1))
        assertEquals(MetadataResult.Failure(MetadataError.Unsupported), repo(net).trending(MediaType.MOVIE, 1))
        assertTrue(net.calls.isEmpty())
    }

    @Test fun scheduleToleratesShowDirectOrEmbedded() = runTest {
        val net = Net { r -> ok(r, """[{"id":1,"show":$show},{"id":2,"_embedded":{"show":{"id":6,"name":"Second","rating":{"average":9.0}}}},{"id":3,"show":$show}]""") }
        val items = (repo(net).trending(MediaType.TV, 1) as MetadataResult.Ok).value.items
        assertEquals(listOf("Second", "Fixture Show"), items.map { it.title }) // de-duplicated, best rated first
    }

    @Test fun detailCombinesShowAndEpisodes() = runTest {
        val net = Net { r -> ok(r, if (r.url.encodedPath.endsWith("/episodes")) episodes else show) }
        val d = (repo(net).detail(TmdbRef(MediaType.TV, 5, RefSource.TVMAZE)) as MetadataResult.Ok).value
        assertEquals(listOf(0, 1, 2), d.seasons.map { it.number })
        assertEquals("1", net.calls.first { it.url.encodedPath.endsWith("/episodes") }.url.queryParameter("specials"))
    }

    // --- switching
    private class Stub(val tag: String) : skywa1ker404.flickdocket.data.repo.MetadataRepository {
        val seen = mutableListOf<String>()
        override suspend fun trending(type: MediaType, page: Int): MetadataResult<ResultPage<TmdbSummary>> { seen += "trending"; return MetadataResult.Failure(MetadataError.Offline) }
        override suspend fun search(type: MediaType, query: String, page: Int): MetadataResult<ResultPage<TmdbSummary>> { seen += "search"; return MetadataResult.Failure(MetadataError.Offline) }
        override suspend fun detail(ref: TmdbRef): MetadataResult<TmdbDetail> { seen += "detail"; return MetadataResult.Failure(MetadataError.Offline) }
        override suspend fun season(ref: TmdbRef, number: Int): MetadataResult<SeasonDetail> { seen += "season"; return MetadataResult.Failure(MetadataError.Offline) }
        override suspend fun verifyKey(force: Boolean): MetadataResult<Unit> { seen += "verify"; return MetadataResult.Ok(Unit) }
        override suspend fun clearCache() { seen += "clear" }
    }

    @Test fun switchingRoutesByModeAndByRefSource() = runTest {
        var mode = MetadataMode.OPEN
        val tmdb = Stub("tmdb"); val open = Stub("open")
        val s = SwitchingMetadataRepository({ mode }, tmdb, open)
        s.search(MediaType.TV, "x", 1); assertEquals(listOf("search"), open.seen)
        mode = MetadataMode.TMDB
        s.search(MediaType.TV, "x", 1); assertEquals(listOf("search"), tmdb.seen)
        // a TVmaze-sourced title still opens from TVmaze while TMDB mode is on, and vice versa
        s.detail(TmdbRef(MediaType.TV, 1, RefSource.TVMAZE)); assertEquals(listOf("search", "detail"), open.seen)
        mode = MetadataMode.OPEN
        s.detail(TmdbRef(MediaType.TV, 1, RefSource.TMDB)); assertEquals(listOf("search", "detail"), tmdb.seen)
        s.clearCache(); assertTrue("clear" in open.seen && "clear" in tmdb.seen)
    }

    @Test fun nextEpisodeIsEarliestOnOrAfterToday() {
        val eps = json.decodeFromString(ListSerializer(TvmEpisodeDto.serializer()), episodes)
        val next = nextEpisodeOf(eps, "2012-04-02")!!
        assertEquals("2012-04-08", next.airDate)
        assertEquals(1, next.season); assertEquals(2, next.number)
        assertNull(nextEpisodeOf(eps, "2014-01-01"))
        assertEquals("2013-04-01", nextEpisodeOf(eps, "2013-04-01")!!.airDate)
    }
}
