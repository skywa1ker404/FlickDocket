package skywa1ker404.flickdocket

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import skywa1ker404.flickdocket.data.local.FlickDocketDatabase
import skywa1ker404.flickdocket.data.repo.RoomLibraryRepository
import skywa1ker404.flickdocket.domain.*

/** Instrumented (needs an emulator/device). Uses an in-memory database; no sample data ships in the app. */
@RunWith(AndroidJUnit4::class)
class LibraryRepositoryTest {
    private lateinit var db: FlickDocketDatabase
    private lateinit var repo: RoomLibraryRepository
    private var clock = 1_000L

    @Before fun setUp() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, FlickDocketDatabase::class.java).allowMainThreadQueries().build()
        repo = RoomLibraryRepository(db) { clock++ }
    }

    @After fun tearDown() = db.close()

    @Test fun addManualCreatesLibraryRow() = runTest {
        val id = repo.addManual(NewTitleDraft("Test Film", MediaType.MOVIE, year = 2000))
        val items = repo.observeLibrary().first()
        assertEquals(1, items.size)
        assertEquals(id, items[0].titleId)
        assertEquals(WatchStatus.PLANNING, items[0].status)
    }

    @Test fun movieWatchCompletesAndUndoKeepsStatus() = runTest {
        val id = repo.addManual(NewTitleDraft("Test Film", MediaType.MOVIE))
        repo.addWatch(id)
        assertEquals(WatchStatus.COMPLETED, repo.observeDetail(id).first()!!.status)
        assertEquals(1, repo.observeDetail(id).first()!!.watchCount)
        repo.undoLastWatch(id)
        val d = repo.observeDetail(id).first()!!
        assertEquals(0, d.watchCount)
        assertEquals(WatchStatus.COMPLETED, d.status) // never silently reverted
    }

    @Test fun tvCompletesAtTotalEpisodes() = runTest {
        val id = repo.addManual(NewTitleDraft("Test Show", MediaType.TV, totalEpisodes = 2))
        repo.addWatch(id)
        assertEquals(WatchStatus.WATCHING, repo.observeDetail(id).first()!!.status)
        repo.addWatch(id)
        assertEquals(WatchStatus.COMPLETED, repo.observeDetail(id).first()!!.status)
    }

    @Test fun ratingSnapsAndDeleteCascades() = runTest {
        val id = repo.addManual(NewTitleDraft("Test Film", MediaType.MOVIE))
        repo.setRating(id, 73)
        assertEquals(75, repo.observeDetail(id).first()!!.rating)
        repo.addWatch(id)
        repo.delete(id)
        assertNull(repo.observeDetail(id).first())
        assertEquals(0, repo.observeStats().first().watchEvents)
    }
}
