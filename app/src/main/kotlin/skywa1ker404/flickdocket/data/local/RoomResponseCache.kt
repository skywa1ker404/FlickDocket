package skywa1ker404.flickdocket.data.local

import skywa1ker404.flickdocket.data.remote.tmdb.CachedBody
import skywa1ker404.flickdocket.data.remote.tmdb.ResponseCache

class RoomResponseCache(private val dao: MetadataCacheDao) : ResponseCache {
    override suspend fun get(key: String) = dao.get(key)?.let { CachedBody(it.body, it.fetchedAt) }
    override suspend fun put(key: String, body: String, at: Long) {
        dao.put(MetadataCacheEntity(key, body, at))
        dao.prune(at - MAX_AGE_MS)
    }
    override suspend fun clear() = dao.clear()
    private companion object { const val MAX_AGE_MS = 7L * 24 * 3_600_000 }
}
