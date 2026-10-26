package skywa1ker404.flickdocket.tmdb

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test
import skywa1ker404.flickdocket.data.remote.tmdb.*
import skywa1ker404.flickdocket.domain.*

/** Fixtures are invented test data in TMDB's documented response shape, not real TMDB content. */
class TmdbMappingTest {
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    private val movieJson = """{
      "id": 1, "title": "Fixture Film", "tagline": "", "overview": "An invented plot.", "poster_path": "/p.jpg", "backdrop_path": "/b.jpg",
      "release_date": "2001-05-06", "runtime": 95, "status": "Released", "vote_average": 7.25, "imdb_id": "tt0000001",
      "genres": [{"id": 18, "name": "Drama"}],
      "credits": {"cast": [{"id": 2, "name": "B Actor", "character": "Second", "order": 1}, {"id": 1, "name": "A Actor", "character": "First", "order": 0}],
                  "crew": [{"id": 9, "name": "D Irector", "job": "Director"}, {"id": 8, "name": "Cam Op", "job": "Camera Operator"}, {"id": 9, "name": "D Irector", "job": "Director"}]},
      "external_ids": {"imdb_id": "tt0000001", "wikidata_id": "Q1"},
      "release_dates": {"results": [
        {"iso_3166_1": "US", "release_dates": [{"certification": "PG", "release_date": "2001-05-06T00:00:00.000Z", "type": 3}, {"certification": "", "release_date": "2001-09-01T00:00:00.000Z", "type": 4}]},
        {"iso_3166_1": "IN", "release_dates": [{"certification": "U", "release_date": "2001-05-10T00:00:00.000Z", "type": 3}]}]}
    }"""

    private val tvJson = """{
      "id": 7, "name": "Fixture Show", "overview": "x", "first_air_date": "2010-02-03", "last_air_date": "2012-01-01",
      "episode_run_time": [0, 42], "number_of_episodes": 99, "number_of_seasons": 2, "status": "Ended",
      "seasons": [{"season_number": 2, "name": "Season 2", "episode_count": 8}, {"season_number": 0, "name": "Specials", "episode_count": 3}, {"season_number": 1, "name": "Season 1", "episode_count": 10}],
      "external_ids": {"imdb_id": "tt0000002", "tvdb_id": 555},
      "content_ratings": {"results": [{"iso_3166_1": "US", "rating": "TV-14"}]}
    }"""

    @Test fun movieMapsCoreFields() {
        val d = json.decodeFromString<DetailDto>(movieJson).toDetail(MediaType.MOVIE, "IN")
        assertEquals("Fixture Film", d.title); assertEquals(2001, d.year); assertEquals(95, d.runtimeMinutes)
        assertNull(d.tagline) // blank becomes null
        assertEquals("tt0000001", d.externalIds.imdb); assertEquals("Q1", d.externalIds.wikidata)
        assertEquals(7.25f, d.rating!!, 0.001f)
        assertEquals(listOf("A Actor", "B Actor"), d.cast.map { it.name }) // sorted by billing order
    }

    @Test fun certificationPrefersRegionThenFallsBackToUs() {
        assertEquals("U", json.decodeFromString<DetailDto>(movieJson).toDetail(MediaType.MOVIE, "IN").certification)
        assertEquals("PG", json.decodeFromString<DetailDto>(movieJson).toDetail(MediaType.MOVIE, "FR").certification)
    }

    @Test fun releasesAreTypedAndDated() {
        val r = json.decodeFromString<DetailDto>(movieJson).toDetail(MediaType.MOVIE, "US").releases
        assertEquals(3, r.size)
        assertTrue(r.any { it.country == "US" && it.kind == ReleaseKind.THEATRICAL && it.date == "2001-05-06" && it.certification == "PG" })
        assertTrue(r.any { it.kind == ReleaseKind.DIGITAL && it.certification == null })
    }

    @Test fun crewKeepsKeyJobsOnceEach() {
        val c = json.decodeFromString<DetailDto>(movieJson).toDetail(MediaType.MOVIE, "US").crew
        assertEquals(listOf(CrewMember(9, "D Irector", "Director")), c)
    }

    @Test fun tvSeasonsSortedAndSpecialsExcludedFromTotal() {
        val d = json.decodeFromString<DetailDto>(tvJson).toDetail(MediaType.TV, "US")
        assertEquals(listOf(0, 1, 2), d.seasons.map { it.number })
        assertTrue(d.seasons.first().isSpecials)
        assertEquals(18, d.totalEpisodes) // 10 + 8, specials not counted
        assertEquals(42, d.runtimeMinutes) // first positive episode runtime
        assertEquals("TV-14", d.certification)
        assertEquals(555L, d.externalIds.tvdb); assertEquals("tt0000002", d.externalIds.imdb)
        assertTrue(d.releases.isEmpty())
    }

    @Test fun tvTotalFallsBackToReportedCountWithoutSeasons() {
        val d = json.decodeFromString<DetailDto>("""{"id":1,"name":"S","number_of_episodes":12}""").toDetail(MediaType.TV, "US")
        assertEquals(12, d.totalEpisodes)
    }

    @Test fun animeIsFilteredFromTvOnly() {
        val page = json.decodeFromString<PageDto<SummaryDto>>("""{"page":1,"total_pages":3,"results":[
          {"id":1,"name":"Anime","genre_ids":[16,10759],"original_language":"ja"},
          {"id":2,"name":"Western Cartoon","genre_ids":[16],"original_language":"en"},
          {"id":3,"name":"Live Action","genre_ids":[18],"original_language":"ja"}]}""")
        assertEquals(listOf("Western Cartoon", "Live Action"), page.toPage(MediaType.TV).items.map { it.title })
        assertEquals(3, page.toPage(MediaType.MOVIE).items.size) // movies are never filtered
        assertEquals(3, page.toPage(MediaType.TV).totalPages)
    }

    @Test fun summaryUsesMovieOrTvFields() {
        val s = SummaryDto(1, title = "M", name = "T", releaseDate = "1999-01-01", firstAirDate = "2005-01-01", voteAverage = 0.0)
        assertEquals("M" to 1999, s.toSummary(MediaType.MOVIE).let { it.title to it.year })
        assertEquals("T" to 2005, s.toSummary(MediaType.TV).let { it.title to it.year })
        assertNull(s.toSummary(MediaType.MOVIE).rating) // 0.0 means "unrated" on TMDB
    }

    @Test fun seasonEpisodesAreOrdered() {
        val s = json.decodeFromString<SeasonDetailDto>("""{"season_number":1,"episodes":[{"episode_number":2,"name":"B","runtime":0},{"episode_number":1,"name":"A","runtime":44}]}""").toDomain()
        assertEquals(listOf("A", "B"), s.episodes.map { it.name })
        assertEquals(44, s.episodes[0].runtimeMinutes); assertNull(s.episodes[1].runtimeMinutes)
    }

    @Test fun unknownFieldsAndNullsDoNotBreakParsing() {
        val d = json.decodeFromString<DetailDto>("""{"id":5,"title":null,"name":"N","brand_new_field":{"x":1},"genres":null,"seasons":null}""")
        assertEquals("N", d.toDetail(MediaType.TV, "US").title)
    }

    @Test fun configurationBuildsImageConfigWithFallbacks() {
        val c = json.decodeFromString<ConfigurationDto>("""{"images":{"secure_base_url":"https://img.example/t/p/","poster_sizes":["w200","original"]}}""").toImageConfig()
        assertEquals("https://img.example/t/p/", c.baseUrl)
        assertEquals(listOf("w200", "original"), c.posterSizes)
        assertEquals(ImageConfig().backdropSizes, c.backdropSizes)
    }

    @Test fun nextEpisodeMappedForTvOnly() {
        val body = """{"id":9,"name":"S","next_episode_to_air":{"air_date":"2027-01-05","season_number":2,"episode_number":3,"name":"Third"}}"""
        val tv = json.decodeFromString<DetailDto>(body).toDetail(MediaType.TV, "US")
        assertEquals(NextEpisode(2, 3, "Third", "2027-01-05"), tv.nextEpisode)
        assertNull(json.decodeFromString<DetailDto>(body).toDetail(MediaType.MOVIE, "US").nextEpisode)
        val none = json.decodeFromString<DetailDto>("""{"id":9,"name":"S","next_episode_to_air":{"air_date":""}}""").toDetail(MediaType.TV, "US")
        assertNull(none.nextEpisode)
    }
}
