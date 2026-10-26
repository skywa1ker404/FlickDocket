package skywa1ker404.flickdocket

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import skywa1ker404.flickdocket.domain.*

class DomainRulesTest {
    // Test fixtures only; the app itself never ships sample titles.
    private fun item(id: Long, title: String, type: MediaType = MediaType.MOVIE, status: WatchStatus = WatchStatus.PLANNING,
                     rating: Int? = null, year: Int? = null, fav: Boolean = false, updated: Long = id) =
        LibraryItem(id, title, type, year, null, status, rating, fav, 0, null, updated)

    private val items = listOf(
        item(1, "Alpha", MediaType.MOVIE, WatchStatus.COMPLETED, rating = 80, year = 2001, updated = 10),
        item(2, "bravo", MediaType.TV, WatchStatus.WATCHING, rating = null, year = 2020, fav = true, updated = 30),
        item(3, "Charlie", MediaType.TV, WatchStatus.PLANNING, rating = 95, year = null, updated = 20),
    )

    @Test fun textFilterIsCaseInsensitive() =
        assertEquals(listOf(2L), LibraryRules.apply(items, LibraryQuery(text = " BRA ")).map { it.titleId })

    @Test fun statusAndTypeFiltersCombine() =
        assertEquals(listOf(2L), LibraryRules.apply(items, LibraryQuery(statuses = setOf(WatchStatus.WATCHING), mediaType = MediaType.TV)).map { it.titleId })

    @Test fun favoritesOnly() =
        assertEquals(listOf(2L), LibraryRules.apply(items, LibraryQuery(favoritesOnly = true)).map { it.titleId })

    @Test fun defaultSortIsMostRecentlyUpdated() =
        assertEquals(listOf(2L, 3L, 1L), LibraryRules.apply(items, LibraryQuery()).map { it.titleId })

    @Test fun titleSortIgnoresCase() =
        assertEquals(listOf(1L, 2L, 3L), LibraryRules.apply(items, LibraryQuery(sort = LibrarySort.TITLE)).map { it.titleId })

    @Test fun ratingSortPutsUnratedLast() =
        assertEquals(listOf(3L, 1L, 2L), LibraryRules.apply(items, LibraryQuery(sort = LibrarySort.RATING)).map { it.titleId })

    @Test fun releaseSortPutsUnknownYearLast() =
        assertEquals(listOf(2L, 1L, 3L), LibraryRules.apply(items, LibraryQuery(sort = LibrarySort.RELEASE)).map { it.titleId })

    @Test fun isFilteringReflectsActiveFilters() {
        assertFalse(LibraryQuery().isFiltering)
        assertTrue(LibraryQuery(text = "x").isFiltering)
        assertFalse(LibraryQuery(sort = LibrarySort.TITLE).isFiltering)
    }

    @Test fun statusCountsGroup() =
        assertEquals(mapOf(WatchStatus.COMPLETED to 1, WatchStatus.WATCHING to 1, WatchStatus.PLANNING to 1), LibraryRules.statusCounts(items))

    @Test fun movieWatchCompletes() =
        assertEquals(WatchStatus.COMPLETED, LibraryRules.statusAfterWatch(MediaType.MOVIE, WatchStatus.PLANNING, 1, null))

    @Test fun tvFirstEpisodeStartsWatching() =
        assertEquals(WatchStatus.WATCHING, LibraryRules.statusAfterWatch(MediaType.TV, WatchStatus.PLANNING, 1, 10))

    @Test fun tvFinalEpisodeCompletes() =
        assertEquals(WatchStatus.COMPLETED, LibraryRules.statusAfterWatch(MediaType.TV, WatchStatus.WATCHING, 10, 10))

    @Test fun tvEpisodeDoesNotOverrideDropped() =
        assertEquals(WatchStatus.DROPPED, LibraryRules.statusAfterWatch(MediaType.TV, WatchStatus.DROPPED, 2, 10))

    @Test fun tvUnknownTotalNeverAutoCompletes() =
        assertEquals(WatchStatus.WATCHING, LibraryRules.statusAfterWatch(MediaType.TV, WatchStatus.WATCHING, 500, null))

    @Test fun ratingSnapsToHalfStepsAndClamps() {
        assertEquals(75, LibraryRules.snapRating(73))
        assertEquals(0, LibraryRules.snapRating(-4))
        assertEquals(100, LibraryRules.snapRating(999))
    }

    @Test fun draftValidation() {
        val ok = NewTitleDraft("  Heat ", MediaType.MOVIE, year = 1995, runtimeMinutes = 170)
        assertTrue(DraftValidation.validate(ok, 2026).isEmpty())
        assertEquals(listOf(DraftIssue.NameBlank), DraftValidation.validate(ok.copy(name = "  "), 2026))
        assertEquals(listOf(DraftIssue.YearOutOfRange), DraftValidation.validate(ok.copy(year = 1500), 2026))
        assertEquals(listOf(DraftIssue.YearOutOfRange), DraftValidation.validate(ok.copy(year = 2100), 2026))
        assertEquals(listOf(DraftIssue.NonPositiveNumber), DraftValidation.validate(ok.copy(totalEpisodes = 0), 2026))
        assertEquals(listOf(DraftIssue.NameTooLong), DraftValidation.validate(ok.copy(name = "x".repeat(201)), 2026))
    }

    @Test fun reducedMotionResolution() {
        assertTrue(MotionRules.isReduced(MotionPreference.REDUCED, 1f))
        assertFalse(MotionRules.isReduced(MotionPreference.FULL, 0f))
        assertTrue(MotionRules.isReduced(MotionPreference.FOLLOW_SYSTEM, 0f))
        assertFalse(MotionRules.isReduced(MotionPreference.FOLLOW_SYSTEM, 1f))
    }

    @Test fun darkResolution() {
        assertTrue(ThemeRules.isDark(ThemeMode.AMOLED, false))
        assertFalse(ThemeRules.isDark(ThemeMode.LIGHT, true))
        assertTrue(ThemeRules.isDark(ThemeMode.SYSTEM, true))
    }

    @Test fun progressFractionClampsAndHandlesUnknownTotals() {
        assertEquals(0.5f, ProgressRules.fraction(5, 10)!!, 0.001f)
        assertEquals(1f, ProgressRules.fraction(12, 10)!!, 0.001f)
        assertNull(ProgressRules.fraction(3, null))
        assertNull(ProgressRules.fraction(3, 0))
    }

    @Test fun advanceAvailability() {
        assertTrue(ProgressRules.canAdvance(MediaType.TV, WatchStatus.WATCHING, 3, 10))
        assertFalse(ProgressRules.canAdvance(MediaType.TV, WatchStatus.COMPLETED, 10, 10))
        assertTrue(ProgressRules.canAdvance(MediaType.TV, WatchStatus.WATCHING, 3, null))
        assertTrue(ProgressRules.canAdvance(MediaType.MOVIE, WatchStatus.PLANNING, 0, null))
        assertFalse(ProgressRules.canAdvance(MediaType.MOVIE, WatchStatus.COMPLETED, 1, null))
    }
}
