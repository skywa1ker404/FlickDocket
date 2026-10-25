package skywa1ker404.flickdocket.data.remote.tmdb

import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.Json
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import skywa1ker404.flickdocket.domain.MetadataError
import skywa1ker404.flickdocket.domain.MetadataResult
import skywa1ker404.flickdocket.domain.TmdbKey
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class CachedBody(val body: String, val fetchedAt: Long)

interface ResponseCache {
    suspend fun get(key: String): CachedBody?
    suspend fun put(key: String, body: String, at: Long)
    suspend fun clear()
}

/**
 * Thin TMDB v3 client: rate limiting, 429 retry, TTL cache with stale fallback, typed errors.
 * The credential is read per request from [keyProvider], is never logged and never part of a cache key.
 */
class TmdbClient(
    private val http: OkHttpClient,
    private val keyProvider: suspend () -> String?,
    private val cache: ResponseCache,
    private val limiter: RateLimiter = RateLimiter(),
    private val language: () -> String = { "en-US" },
    private val now: () -> Long = System::currentTimeMillis,
    private val pause: suspend (Long) -> Unit = { delay(it) },
    private val baseUrl: String = "https://api.themoviedb.org/3/",
) {
    val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    suspend fun <T> get(
        path: String,
        params: Map<String, String> = emptyMap(),
        ttlMs: Long,
        forceRefresh: Boolean = false,
        parse: (String) -> T,
    ): MetadataResult<T> {
        val all = (params + ("language" to language())).toSortedMap()
        val cacheKey = path + "?" + all.entries.joinToString("&") { "${it.key}=${it.value}" }
        val cached = cache.get(cacheKey)
        fun parsed(body: String, stale: Boolean): MetadataResult<T> =
            try { MetadataResult.Ok(parse(body), stale) } catch (e: IllegalArgumentException) { MetadataResult.Failure(MetadataError.Malformed) }

        if (!forceRefresh && cached != null && now() - cached.fetchedAt < ttlMs) return parsed(cached.body, false)

        val key = keyProvider()?.trim()?.takeIf { it.isNotEmpty() }
            ?: return if (cached != null) parsed(cached.body, true) else MetadataResult.Failure(MetadataError.MissingCredentials)

        return when (val r = fetch(path, all, key)) {
            is Fetch.Body -> parsed(r.text, false).also { if (it is MetadataResult.Ok) cache.put(cacheKey, r.text, now()) }
            is Fetch.Error ->
                if (cached != null && r.error.isTransient) parsed(cached.body, true) else MetadataResult.Failure(r.error)
        }
    }

    private sealed interface Fetch {
        data class Body(val text: String) : Fetch
        data class Error(val error: MetadataError) : Fetch
    }

    private suspend fun fetch(path: String, params: Map<String, String>, key: String): Fetch {
        val bearer = TmdbKey.isBearer(key)
        val url = baseUrl.toHttpUrl().newBuilder().addPathSegments(path).apply {
            params.forEach { (k, v) -> addQueryParameter(k, v) }
            if (!bearer) addQueryParameter("api_key", key)
        }.build()
        val request = Request.Builder().url(url).header("Accept", "application/json")
            .apply { if (bearer) header("Authorization", "Bearer $key") }.build()

        var attempt = 0
        while (true) {
            limiter.acquire()
            val response = try { http.newCall(request).await() } catch (e: IOException) { return Fetch.Error(MetadataError.Offline) }
            var wait = 0L
            response.use { r ->
                when {
                    r.isSuccessful -> return try { Fetch.Body(r.body.string()) } catch (e: IOException) { Fetch.Error(MetadataError.Offline) }
                    r.code == 401 || r.code == 403 -> return Fetch.Error(MetadataError.Unauthorized)
                    r.code == 404 -> return Fetch.Error(MetadataError.NotFound)
                    r.code == 429 -> {
                        val retryAfter = r.header("Retry-After")?.trim()?.toIntOrNull()
                        if (attempt >= MAX_RETRIES) return Fetch.Error(MetadataError.RateLimited(retryAfter))
                        wait = (retryAfter ?: (1 shl attempt)).coerceIn(0, 10) * 1000L
                        attempt++
                    }
                    else -> return Fetch.Error(MetadataError.Server(r.code))
                }
            }
            pause(wait)
        }
    }

    private suspend fun Call.await(): Response = suspendCancellableCoroutine { cont ->
        cont.invokeOnCancellation { cancel() }
        enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                if (cont.isCancelled) response.close() else cont.resume(response)
            }
            override fun onFailure(call: Call, e: IOException) { cont.resumeWithException(e) }
        })
    }

    private companion object { const val MAX_RETRIES = 2 }
}
