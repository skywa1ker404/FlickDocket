package skywa1ker404.flickdocket.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import skywa1ker404.flickdocket.domain.MediaType
import skywa1ker404.flickdocket.domain.MetadataSource
import skywa1ker404.flickdocket.domain.WatchStatus

@Entity(tableName = "titles", indices = [Index(value = ["mediaType", "tmdbId"], unique = true)])
data class TitleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mediaType: MediaType,
    val name: String,
    val tmdbId: Long? = null,
    val overview: String? = null,
    val posterPath: String? = null,
    val backdropPath: String? = null,
    val releaseYear: Int? = null,
    val runtimeMinutes: Int? = null,
    val totalEpisodes: Int? = null,
    val source: MetadataSource = MetadataSource.MANUAL,
    val createdAt: Long,
)

/** Provider ids (IMDb, Trakt, Simkl...) are plain identifiers, never credentials. */
@Entity(
    tableName = "external_ids",
    foreignKeys = [ForeignKey(TitleEntity::class, ["id"], ["titleId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["titleId", "provider"], unique = true)],
)
data class ExternalIdEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val titleId: Long,
    val provider: String,
    val externalId: String,
)

@Entity(
    tableName = "library_entries",
    foreignKeys = [ForeignKey(TitleEntity::class, ["id"], ["titleId"], onDelete = ForeignKey.CASCADE)],
)
data class LibraryEntryEntity(
    @PrimaryKey val titleId: Long,
    val status: WatchStatus,
    val rating: Int? = null,
    val favorite: Boolean = false,
    val notes: String = "",
    val addedAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "watch_events",
    foreignKeys = [ForeignKey(TitleEntity::class, ["id"], ["titleId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("titleId")],
)
data class WatchEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val titleId: Long,
    val watchedAt: Long,
    val seasonNumber: Int? = null,
    val episodeNumber: Int? = null,
)
