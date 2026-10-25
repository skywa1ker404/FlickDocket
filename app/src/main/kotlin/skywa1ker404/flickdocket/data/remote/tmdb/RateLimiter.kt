package skywa1ker404.flickdocket.data.remote.tmdb

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Client-side sliding-window limiter. TMDB documents an upper bound "somewhere in the 40 requests per second range"
 * that may change, so we stay well under it by default and still honour any 429.
 */
class RateLimiter(
    private val maxRequests: Int = 20,
    private val windowMs: Long = 1_000,
    private val nowMs: () -> Long = System::currentTimeMillis,
    private val pause: suspend (Long) -> Unit = { delay(it) },
) {
    private val mutex = Mutex()
    private val stamps = ArrayDeque<Long>()

    suspend fun acquire() = mutex.withLock {
        while (true) {
            val now = nowMs()
            while (stamps.isNotEmpty() && now - stamps.first() >= windowMs) stamps.removeFirst()
            if (stamps.size < maxRequests) { stamps.addLast(now); break }
            pause(windowMs - (now - stamps.first()))
        }
    }
}
