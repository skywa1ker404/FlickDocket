package skywa1ker404.flickdocket.tmdb

import kotlinx.coroutines.test.runTest
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
import skywa1ker404.flickdocket.domain.MetadataError
import skywa1ker404.flickdocket.domain.MetadataResult
import java.io.IOException

private class FakeCache : ResponseCache {
    val map = mutableMapOf<String, CachedBody>()
    override suspend fun get(key: String) = map[key]
    override suspend fun put(key: String, body: String, at: Long) { map[key] = CachedBody(body, at) }
    override suspend fun clear() = map.clear()
}

private class FakeNet(var respond: (Request, Int) -> Response) {
    val requests = mutableListOf<Request>()
    fun client() = OkHttpClient.Builder().addInterceptor(Interceptor { chain ->
        requests += chain.request(); respond(chain.request(), requests.size)
    }).build()
}

private fun reply(req: Request, code: Int, body: String = "{}", headers: Map<String, String> = emptyMap()) =
    Response.Builder().request(req).protocol(Protocol.HTTP_1_1).code(code).message("m")
        .body(body.toResponseBody("application/json".toMediaType()))
        .apply { headers.forEach { (k, v) -> header(k, v) } }.build()

class TmdbClientTest {
    private val v3Key = "0123456789abcdef0123456789abcdef"
    private val v4Token = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ4In0.c2lnbmF0dXJlc2lnbmF0dXJlc2lnbmF0dXJl"
    private var clock = 1_000_000L

    private fun client(net: FakeNet, cache: FakeCache, key: String? = v3Key) =
        TmdbClient(net.client(), { key }, cache, now = { clock }, pause = { clock += it })

    private suspend fun TmdbClient.raw(ttl: Long = 1000, force: Boolean = false) =
        get("search/movie", mapOf("query" to "x"), ttl, force) { it }

    @Test fun missingCredentialsMakesNoRequest() = runTest {
        val net = FakeNet { r, _ -> reply(r, 200) }
        val res = client(net, FakeCache(), key = null).raw()
        assertEquals(MetadataResult.Failure(MetadataError.MissingCredentials), res)
        assertTrue(net.requests.isEmpty())
    }

    @Test fun blankKeyCountsAsMissing() = runTest {
        val res = client(FakeNet { r, _ -> reply(r, 200) }, FakeCache(), key = "   ").raw()
        assertEquals(MetadataResult.Failure(MetadataError.MissingCredentials), res)
    }

    @Test fun v4TokenUsesBearerHeaderAndNoQueryKey() = runTest {
        val net = FakeNet { r, _ -> reply(r, 200) }
        client(net, FakeCache(), key = v4Token).raw()
        val req = net.requests.single()
        assertEquals("Bearer $v4Token", req.header("Authorization"))
        assertNull(req.url.queryParameter("api_key"))
    }

    @Test fun v3KeyUsesQueryParamAndIsNeverInCacheKey() = runTest {
        val net = FakeNet { r, _ -> reply(r, 200) }
        val cache = FakeCache()
        client(net, cache).raw()
        assertEquals(v3Key, net.requests.single().url.queryParameter("api_key"))
        assertNull(net.requests.single().header("Authorization"))
        assertTrue(cache.map.keys.none { it.contains(v3Key) || it.contains("api_key") })
    }

    @Test fun freshCacheAvoidsSecondRequest() = runTest {
        val net = FakeNet { r, _ -> reply(r, 200, "{\"a\":1}") }
        val c = client(net, FakeCache())
        c.raw(); c.raw()
        assertEquals(1, net.requests.size)
    }

    @Test fun expiredCacheRefetches() = runTest {
        val net = FakeNet { r, _ -> reply(r, 200) }
        val c = client(net, FakeCache())
        c.raw(ttl = 1000); clock += 2000; c.raw(ttl = 1000)
        assertEquals(2, net.requests.size)
    }

    @Test fun forceRefreshBypassesFreshCache() = runTest {
        val net = FakeNet { r, _ -> reply(r, 200) }
        val c = client(net, FakeCache())
        c.raw(); c.raw(force = true)
        assertEquals(2, net.requests.size)
    }

    @Test fun unauthorizedIsNotServedFromStaleCache() = runTest {
        val cache = FakeCache()
        client(FakeNet { r, _ -> reply(r, 200, "old") }, cache).raw()
        clock += 5000
        val res = client(FakeNet { r, _ -> reply(r, 401) }, cache).raw(ttl = 1000)
        assertEquals(MetadataResult.Failure(MetadataError.Unauthorized), res)
    }

    @Test fun notFoundMaps() = runTest {
        assertEquals(MetadataResult.Failure(MetadataError.NotFound), client(FakeNet { r, _ -> reply(r, 404) }, FakeCache()).raw())
    }

    @Test fun retriesOn429ThenSucceeds() = runTest {
        val net = FakeNet { r, n -> if (n == 1) reply(r, 429, headers = mapOf("Retry-After" to "2")) else reply(r, 200, "ok") }
        val start = clock
        val res = client(net, FakeCache()).raw()
        assertEquals(MetadataResult.Ok("ok", false), res)
        assertEquals(2, net.requests.size)
        assertTrue("waited Retry-After", clock - start >= 2000)
    }

    @Test fun persistent429ReportsRateLimitedAfterRetries() = runTest {
        val net = FakeNet { r, _ -> reply(r, 429, headers = mapOf("Retry-After" to "1")) }
        val res = client(net, FakeCache()).raw()
        assertEquals(MetadataResult.Failure(MetadataError.RateLimited(1)), res)
        assertEquals(3, net.requests.size) // first try + 2 retries
    }

    @Test fun rateLimitFallsBackToStaleCache() = runTest {
        val cache = FakeCache()
        client(FakeNet { r, _ -> reply(r, 200, "cached") }, cache).raw()
        clock += 5000
        val res = client(FakeNet { r, _ -> reply(r, 429, headers = mapOf("Retry-After" to "0")) }, cache).raw(ttl = 1000)
        assertEquals(MetadataResult.Ok("cached", true), res)
    }

    @Test fun offlineFallsBackToStaleOrReportsOffline() = runTest {
        val offline = FakeNet { _, _ -> throw IOException("no network") }
        assertEquals(MetadataResult.Failure(MetadataError.Offline), client(offline, FakeCache()).raw())
        val cache = FakeCache()
        client(FakeNet { r, _ -> reply(r, 200, "cached") }, cache).raw()
        clock += 5000
        assertEquals(MetadataResult.Ok("cached", true), client(offline, cache).raw(ttl = 1000))
    }

    @Test fun serverErrorMapsWithCode() = runTest {
        assertEquals(MetadataResult.Failure(MetadataError.Server(503)), client(FakeNet { r, _ -> reply(r, 503) }, FakeCache()).raw())
    }

    @Test fun malformedBodyIsMalformedAndNotCached() = runTest {
        val cache = FakeCache()
        val res = client(FakeNet { r, _ -> reply(r, 200, "not json") }, cache)
            .get("x", ttlMs = 1000) { kotlinx.serialization.json.Json.parseToJsonElement(it) }
        assertEquals(MetadataResult.Failure(MetadataError.Malformed), res)
        assertTrue(cache.map.isEmpty())
    }

    @Test fun missingKeyStillServesCachedData() = runTest {
        val cache = FakeCache()
        client(FakeNet { r, _ -> reply(r, 200, "cached") }, cache).raw()
        clock += 5000
        assertEquals(MetadataResult.Ok("cached", true), client(FakeNet { r, _ -> reply(r, 200) }, cache, key = null).raw(ttl = 1000))
    }
}
