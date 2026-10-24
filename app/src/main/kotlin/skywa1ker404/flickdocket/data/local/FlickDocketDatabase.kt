package skywa1ker404.flickdocket.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/** Tracking data only. Provider credentials never live in this database (see MASTER_SPEC.md, Security). */
@Database(
    entities = [TitleEntity::class, ExternalIdEntity::class, LibraryEntryEntity::class, WatchEventEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class FlickDocketDatabase : RoomDatabase() {
    abstract fun titleDao(): TitleDao
    abstract fun libraryDao(): LibraryDao
    abstract fun watchDao(): WatchDao

    companion object {
        const val NAME = "flickdocket.db"
        fun create(context: Context): FlickDocketDatabase =
            Room.databaseBuilder(context, FlickDocketDatabase::class.java, NAME).build()
    }
}
