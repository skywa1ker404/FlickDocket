package skywa1ker404.flickdocket.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import skywa1ker404.flickdocket.domain.MediaType
import skywa1ker404.flickdocket.domain.WatchStatus

data class LibraryRow(
    val titleId: Long,
    val title: String,
    val mediaType: MediaType,
    val year: Int?,
    val posterPath: String?,
    val totalEpisodes: Int?,
    val status: WatchStatus,
    val rating: Int?,
    val favorite: Boolean,
    val updatedAt: Long,
    val watchCount: Int,
    val tmdbId: Long?,
    val tvmazeId: String?,
    val imdbId: String?,
)

data class StatusCount(val status: WatchStatus, val count: Int)

@Dao
interface TitleDao {
    @Insert suspend fun insert(e: TitleEntity): Long
    @Update suspend fun update(e: TitleEntity)
    @Query("SELECT * FROM titles WHERE id = :id") fun observe(id: Long): Flow<TitleEntity?>
    @Query("DELETE FROM titles WHERE id = :id") suspend fun delete(id: Long)
    @Query("SELECT * FROM titles WHERE mediaType = :type AND tmdbId = :tmdbId LIMIT 1")
    suspend fun findByTmdb(type: MediaType, tmdbId: Long): TitleEntity?
    @Query("SELECT id FROM titles WHERE mediaType = :type AND tmdbId = :tmdbId LIMIT 1")
    fun observeIdByTmdb(type: MediaType, tmdbId: Long): Flow<Long?>
}

@Dao
interface ExternalIdDao {
    @Upsert suspend fun upsert(e: ExternalIdEntity)
    @Query("SELECT * FROM external_ids WHERE titleId = :id") fun observeFor(id: Long): Flow<List<ExternalIdEntity>>
    @Query("SELECT titleId FROM external_ids WHERE provider = :provider AND externalId = :value LIMIT 1")
    suspend fun findTitleId(provider: String, value: String): Long?
    @Query("SELECT titleId FROM external_ids WHERE provider = :provider AND externalId = :value LIMIT 1")
    fun observeTitleId(provider: String, value: String): Flow<Long?>
}

@Dao
interface MetadataCacheDao {
    @Query("SELECT * FROM metadata_cache WHERE cacheKey = :key") suspend fun get(key: String): MetadataCacheEntity?
    @Upsert suspend fun put(e: MetadataCacheEntity)
    @Query("DELETE FROM metadata_cache WHERE fetchedAt < :cutoff") suspend fun prune(cutoff: Long)
    @Query("DELETE FROM metadata_cache") suspend fun clear()
}

@Dao
interface LibraryDao {
    @Upsert suspend fun upsert(e: LibraryEntryEntity)
    @Query("SELECT * FROM library_entries WHERE titleId = :id") fun observeEntry(id: Long): Flow<LibraryEntryEntity?>
    @Query("SELECT * FROM library_entries WHERE titleId = :id") suspend fun getEntry(id: Long): LibraryEntryEntity?

    @Query(
        """SELECT t.id AS titleId, t.name AS title, t.mediaType AS mediaType, t.releaseYear AS year,
           t.posterPath AS posterPath, t.totalEpisodes AS totalEpisodes, e.status AS status, e.rating AS rating,
           e.favorite AS favorite, e.updatedAt AS updatedAt,
           (SELECT COUNT(*) FROM watch_events w WHERE w.titleId = t.id) AS watchCount,
           t.tmdbId AS tmdbId,
           (SELECT x.externalId FROM external_ids x WHERE x.titleId = t.id AND x.provider = 'tvmaze' LIMIT 1) AS tvmazeId,
           (SELECT x.externalId FROM external_ids x WHERE x.titleId = t.id AND x.provider = 'imdb' LIMIT 1) AS imdbId
           FROM titles t INNER JOIN library_entries e ON e.titleId = t.id"""
    )
    fun observeLibrary(): Flow<List<LibraryRow>>

    @Query("SELECT status AS status, COUNT(*) AS count FROM library_entries GROUP BY status")
    fun observeStatusCounts(): Flow<List<StatusCount>>
}

@Dao
interface WatchDao {
    @Insert suspend fun insert(e: WatchEventEntity): Long
    @Query("SELECT * FROM watch_events WHERE titleId = :id ORDER BY watchedAt DESC, id DESC")
    fun observeFor(id: Long): Flow<List<WatchEventEntity>>
    @Query("SELECT COUNT(*) FROM watch_events WHERE titleId = :id") suspend fun count(id: Long): Int
    @Query("SELECT COUNT(*) FROM watch_events") fun observeTotal(): Flow<Int>
    @Query("SELECT COALESCE(SUM(t.runtimeMinutes), 0) FROM watch_events w JOIN titles t ON t.id = w.titleId")
    fun observeMinutes(): Flow<Long>
    @Query("DELETE FROM watch_events WHERE id = (SELECT id FROM watch_events WHERE titleId = :id ORDER BY watchedAt DESC, id DESC LIMIT 1)")
    suspend fun deleteLatest(id: Long)
}
