package skywa1ker404.flickdocket.tmdb

import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import skywa1ker404.flickdocket.data.remote.tmdb.RateLimiter
import skywa1ker404.flickdocket.domain.*

class TmdbSupportTest {
    @Test fun rateLimiterDelaysOnlyWhenWindowIsFull() = runTest {
        var t = 0L
        val rl = RateLimiter(maxRequests = 2, windowMs = 1000, nowMs = { t }, pause = { t += it })
        rl.acquire(); rl.acquire()
        assertEquals(0L, t)
        rl.acquire()
        assertEquals(1000L, t)
    }

    @Test fun rateLimiterWindowSlides() = runTest {
        var t = 0L
        val rl = RateLimiter(maxRequests = 1, windowMs = 1000, nowMs = { t }, pause = { t += it })
        rl.acquire(); t = 1500; rl.acquire()
        assertEquals(1500L, t)
    }

    @Test fun keyFormatAcceptsV3AndV4Only() {
        assertTrue(TmdbKey.isValidFormat("0123456789abcdef0123456789ABCDEF"))
        assertTrue(TmdbKey.isValidFormat("eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ4In0.c2lnbmF0dXJlc2lnbmF0dXJlc2lnbmF0dXJl"))
        assertFalse(TmdbKey.isValidFormat("")); assertFalse(TmdbKey.isValidFormat("short"))
        assertFalse(TmdbKey.isValidFormat("0123456789abcdef0123456789abcdeg"))
        assertFalse(TmdbKey.isValidFormat("has space.in.token" + "x".repeat(40)))
        assertTrue(TmdbKey.isBearer("a.b.c")); assertFalse(TmdbKey.isBearer("0123456789abcdef0123456789abcdef"))
    }

    @Test fun imageUrlPicksSmallestSizeThatFits() {
        val c = ImageConfig(baseUrl = "https://i.example/p/", posterSizes = listOf("w92", "w342", "w780", "original"))
        assertEquals("https://i.example/p/w342/x.jpg", c.poster("/x.jpg", 200))
        assertEquals("https://i.example/p/w342/x.jpg", c.poster("/x.jpg", 342))
        assertEquals("https://i.example/p/original/x.jpg", c.poster("/x.jpg", 5000))
        assertNull(c.poster(null, 100)); assertNull(c.poster("", 100))
        assertEquals("https://i.example/p/w92/x.jpg", c.poster("x.jpg", 10)) // tolerates a missing leading slash
    }

    @Test fun animeRule() {
        assertTrue(AnimeFilter.isAnime(listOf(16, 35), "ja"))
        assertFalse(AnimeFilter.isAnime(listOf(16), "en"))
        assertFalse(AnimeFilter.isAnime(listOf(35), "ja"))
        assertFalse(AnimeFilter.isAnime(listOf(16), null))
    }

    @Test fun pagedItemsAppendDedupesAndTracksMore() {
        val p1 = PagedItems<Int>().append(ResultPage(1, 2, listOf(1, 2, 3))) { it }
        assertTrue(p1.hasMore)
        val p2 = p1.append(ResultPage(2, 2, listOf(3, 4))) { it }
        assertEquals(listOf(1, 2, 3, 4), p2.items)
        assertFalse(p2.hasMore)
    }

    @Test fun mapOkKeepsStaleFlagAndFailures() {
        assertEquals(MetadataResult.Ok(2, true), MetadataResult.Ok(1, true).mapOk { it + 1 })
        val f = MetadataResult.Failure(MetadataError.Offline)
        assertEquals(f, (f as MetadataResult<Int>).mapOk { it + 1 })
        assertTrue(MetadataError.Offline.isTransient); assertFalse(MetadataError.Unauthorized.isTransient)
    }
}
