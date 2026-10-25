package skywa1ker404.flickdocket.data.repo

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import skywa1ker404.flickdocket.data.local.*
import skywa1ker404.flickdocket.domain.*

interface LibraryRepository {
    fun observeLibrary(): Flow<List<LibraryItem>>
    fun observeDetail(titleId: Long): Flow<TitleDetail?>
    fun observeHistory(titleId: Long): Flow<List<WatchRecord>>
    fun observeStats(): Flow<LibraryStats>
    suspend fun addManual(draft: NewTitleDraft): Long
    /** Adds a TMDB title; if already tracked the existing entry is returned untouched (never overwritten). */
    suspend fun addFromTmdb(detail: TmdbDetail, status: WatchStatus): Long
    fun observeLibraryId(ref: TmdbRef): Flow<Long?>
    suspend fun setStatus(titleId: Long, status: WatchStatus)
    suspend fun setRating(titleId: Long, rating: Int?)
    suspend fun setFavorite(titleId: Long, favorite: Boolean)
    suspend fun setNotes(titleId: Long, notes: String)
    suspend fun addWatch(titleId: Long)
    suspend fun undoLastWatch(titleId: Long)
    suspend fun delete(titleId: Long)
}

class RoomLibraryRepository(
    private val db: FlickDocketDatabase,
    private val now: () -> Long = System::currentTimeMillis,
) : LibraryRepository {
    private val titles = db.titleDao()
    private val library = db.libraryDao()
    private val watches = db.watchDao()
    private val externalIds = db.externalIdDao()

    override fun observeLibrary() = library.observeLibrary().map { rows ->
        rows.map { LibraryItem(it.titleId, it.title, it.mediaType, it.year, it.posterPath, it.status, it.rating, it.favorite, it.watchCount, it.totalEpisodes, it.updatedAt) }
    }

    override fun observeDetail(titleId: Long): Flow<TitleDetail?> =
        combine(titles.observe(titleId), library.observeEntry(titleId), watches.observeFor(titleId), externalIds.observeFor(titleId)) { t, e, w, ids ->
            if (t == null || e == null) null
            else TitleDetail(t.id, t.name, t.mediaType, t.releaseYear, t.runtimeMinutes, t.totalEpisodes, t.overview, t.posterPath,
                t.backdropPath, ids.firstOrNull { it.provider == "imdb" }?.externalId,
                t.source, e.status, e.rating, e.favorite, e.notes, w.size, e.addedAt, e.updatedAt)
        }

    override fun observeHistory(titleId: Long) =
        watches.observeFor(titleId).map { l -> l.map { WatchRecord(it.id, it.watchedAt) } }

    override fun observeStats(): Flow<LibraryStats> =
        combine(library.observeStatusCounts(), watches.observeTotal(), watches.observeMinutes()) { counts, events, minutes ->
            val map = counts.associate { it.status to it.count }
            LibraryStats(map.values.sum(), map, events, minutes)
        }

    override suspend fun addManual(draft: NewTitleDraft): Long = db.withTransaction {
        val t = now()
        val id = titles.insert(
            TitleEntity(mediaType = draft.mediaType, name = draft.name.trim(), releaseYear = draft.year,
                runtimeMinutes = draft.runtimeMinutes, totalEpisodes = if (draft.mediaType == MediaType.TV) draft.totalEpisodes else null, createdAt = t)
        )
        library.upsert(LibraryEntryEntity(id, draft.status, addedAt = t, updatedAt = t))
        id
    }

    override suspend fun addFromTmdb(detail: TmdbDetail, status: WatchStatus): Long = db.withTransaction {
        titles.findByTmdb(detail.ref.type, detail.ref.id)?.let { return@withTransaction it.id }
        val t = now()
        val id = titles.insert(
            TitleEntity(
                mediaType = detail.ref.type, name = detail.title, tmdbId = detail.ref.id, overview = detail.overview,
                posterPath = detail.posterPath, backdropPath = detail.backdropPath, releaseYear = detail.year,
                runtimeMinutes = detail.runtimeMinutes, totalEpisodes = detail.totalEpisodes,
                source = MetadataSource.TMDB, createdAt = t,
            )
        )
        library.upsert(LibraryEntryEntity(id, status, addedAt = t, updatedAt = t))
        listOfNotNull(
            detail.externalIds.imdb?.let { "imdb" to it },
            detail.externalIds.tvdb?.let { "tvdb" to it.toString() },
            detail.externalIds.wikidata?.let { "wikidata" to it },
        ).forEach { (p, v) -> externalIds.upsert(ExternalIdEntity(titleId = id, provider = p, externalId = v)) }
        id
    }

    override fun observeLibraryId(ref: TmdbRef) = titles.observeIdByTmdb(ref.type, ref.id)

    private suspend fun mutate(titleId: Long, change: (LibraryEntryEntity) -> LibraryEntryEntity) {
        val e = library.getEntry(titleId) ?: return
        library.upsert(change(e).copy(updatedAt = now()))
    }

    override suspend fun setStatus(titleId: Long, status: WatchStatus) = mutate(titleId) { it.copy(status = status) }
    override suspend fun setRating(titleId: Long, rating: Int?) = mutate(titleId) { it.copy(rating = rating?.let(LibraryRules::snapRating)) }
    override suspend fun setFavorite(titleId: Long, favorite: Boolean) = mutate(titleId) { it.copy(favorite = favorite) }
    override suspend fun setNotes(titleId: Long, notes: String) = mutate(titleId) { it.copy(notes = notes) }

    override suspend fun addWatch(titleId: Long) = db.withTransaction {
        val entry = library.getEntry(titleId) ?: return@withTransaction
        val title = titles.observe(titleId).first() ?: return@withTransaction
        watches.insert(WatchEventEntity(titleId = titleId, watchedAt = now()))
        val next = LibraryRules.statusAfterWatch(title.mediaType, entry.status, watches.count(titleId), title.totalEpisodes)
        library.upsert(entry.copy(status = next, updatedAt = now()))
    }

    /** Removes only the newest watch record; never changes status automatically. */
    override suspend fun undoLastWatch(titleId: Long) = db.withTransaction {
        watches.deleteLatest(titleId)
        mutate(titleId) { it }
    }

    override suspend fun delete(titleId: Long) = titles.delete(titleId)
}
