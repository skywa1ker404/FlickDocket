package skywa1ker404.flickdocket.domain

/** Pure functions kept free of Android types so they run as plain JVM unit tests. */
object LibraryRules {

    fun apply(items: List<LibraryItem>, q: LibraryQuery): List<LibraryItem> {
        val needle = q.text.trim().lowercase()
        val filtered = items.filter { i ->
            (needle.isEmpty() || i.title.lowercase().contains(needle)) &&
                (q.statuses.isEmpty() || i.status in q.statuses) &&
                (q.mediaType == null || i.mediaType == q.mediaType) &&
                (!q.favoritesOnly || i.favorite)
        }
        return when (q.sort) {
            LibrarySort.RECENTLY_UPDATED -> filtered.sortedByDescending { it.updatedAt }
            LibrarySort.TITLE -> filtered.sortedBy { it.title.lowercase() }
            LibrarySort.RATING -> filtered.sortedWith(compareByDescending<LibraryItem> { it.rating != null }.thenByDescending { it.rating ?: 0 })
            LibrarySort.RELEASE -> filtered.sortedWith(compareByDescending<LibraryItem> { it.year != null }.thenByDescending { it.year ?: 0 })
        }
    }

    fun statusCounts(items: List<LibraryItem>): Map<WatchStatus, Int> =
        items.groupingBy { it.status }.eachCount()

    /**
     * Status after recording a new watch. Only ever moves forward from a not-started or paused state;
     * an explicit user choice such as COMPLETED or DROPPED is never overwritten by a TV episode tick.
     */
    fun statusAfterWatch(type: MediaType, current: WatchStatus, newWatchCount: Int, totalEpisodes: Int?): WatchStatus = when (type) {
        MediaType.MOVIE -> WatchStatus.COMPLETED
        MediaType.TV -> when {
            totalEpisodes != null && newWatchCount >= totalEpisodes -> WatchStatus.COMPLETED
            current == WatchStatus.PLANNING || current == WatchStatus.ON_HOLD -> WatchStatus.WATCHING
            else -> current
        }
    }

    fun snapRating(raw: Int): Int = ((raw.coerceIn(0, RATING_MAX) + RATING_STEP / 2) / RATING_STEP) * RATING_STEP
}

sealed interface DraftIssue {
    data object NameBlank : DraftIssue
    data object NameTooLong : DraftIssue
    data object YearOutOfRange : DraftIssue
    data object NonPositiveNumber : DraftIssue
}

object DraftValidation {
    const val MAX_NAME = 200
    const val FIRST_FILM_YEAR = 1888

    fun validate(d: NewTitleDraft, currentYear: Int): List<DraftIssue> = buildList {
        val n = d.name.trim()
        if (n.isEmpty()) add(DraftIssue.NameBlank)
        if (n.length > MAX_NAME) add(DraftIssue.NameTooLong)
        d.year?.let { if (it < FIRST_FILM_YEAR || it > currentYear + 10) add(DraftIssue.YearOutOfRange) }
        if (listOfNotNull(d.runtimeMinutes, d.totalEpisodes).any { it <= 0 }) add(DraftIssue.NonPositiveNumber)
    }
}

object MotionRules {
    /** Reduced motion applies when the user forces it, or follows the system and animations are disabled. */
    fun isReduced(pref: MotionPreference, systemAnimatorScale: Float): Boolean = when (pref) {
        MotionPreference.REDUCED -> true
        MotionPreference.FULL -> false
        MotionPreference.FOLLOW_SYSTEM -> systemAnimatorScale == 0f
    }
}

object ThemeRules {
    fun isDark(mode: ThemeMode, systemDark: Boolean): Boolean = when (mode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK, ThemeMode.AMOLED -> true
    }
}
