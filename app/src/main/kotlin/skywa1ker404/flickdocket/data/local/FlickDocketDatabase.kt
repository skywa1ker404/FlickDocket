package skywa1ker404.flickdocket.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Tracking data only. Provider credentials never live in this database (see MASTER_SPEC.md, Security). */
@Database(
    entities = [TitleEntity::class, ExternalIdEntity::class, LibraryEntryEntity::class, WatchEventEntity::class, MetadataCacheEntity::class],
    version = 2,
    exportSchema = true,
)
abstract class FlickDocketDatabase : RoomDatabase() {
    abstract fun titleDao(): TitleDao
    abstract fun libraryDao(): LibraryDao
    abstract fun watchDao(): WatchDao
    abstract fun externalIdDao(): ExternalIdDao
    abstract fun cacheDao(): MetadataCacheDao

    companion object {
        const val NAME = "flickdocket.db"
        fun create(context: Context): FlickDocketDatabase =
            Room.databaseBuilder(context, FlickDocketDatabase::class.java, NAME).addMigrations(MIGRATION_1_2).build()
    }
}

/** v1 -> v2: adds the metadata cache. Existing tracking data is untouched. */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `metadata_cache` (`cacheKey` TEXT NOT NULL, `body` TEXT NOT NULL, `fetchedAt` INTEGER NOT NULL, PRIMARY KEY(`cacheKey`))")
    }
}
