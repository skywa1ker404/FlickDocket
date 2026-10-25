package skywa1ker404.flickdocket.data.remote.tvmaze

import skywa1ker404.flickdocket.data.remote.tmdb.yearOf
import skywa1ker404.flickdocket.domain.*

private val TAGS = Regex("<[^>]*>")
private val IMDB_ID = Regex("^tt\\d{6,10}$")

fun isImdbId(q: String) = IMDB_ID.matches(q.trim().lowercase())

/** TVmaze summaries are small HTML fragments; show them as plain text. */
internal fun stripHtml(html: String?): String? = html?.replace("<br>", "\n")?.replace("</p>", "\n")?.replace(TAGS, "")
    ?.replace("&amp;", "&")?.replace("&quot;", "\"")?.replace("&#39;", "'")?.replace("&lt;", "<")?.replace("&gt;", ">")?.replace("&nbsp;", " ")
    ?.lines()?.joinToString("\n") { it.trim() }?.trim()?.ifBlank { null }

internal fun TvmImageDto?.url(preferOriginal: Boolean): String? =
    (if (preferOriginal) this?.original ?: this?.medium else this?.medium ?: this?.original)
        ?.replaceFirst("http://", "https://")?.takeIf { it.startsWith("https://") }

internal fun TvmShowDto.isAnime() = AnimeFilter.isAnimeByName(genres, language)
private fun ref(id: Long) = TmdbRef(MediaType.TV, id, RefSource.TVMAZE)

internal fun TvmShowDto.toSummary() = TmdbSummary(
    ref(id), name, yearOf(premiered), stripHtml(summary), image.url(false), null,
    rating?.average?.takeIf { it > 0 }?.toFloat(),
)

internal fun isRegular(e: TvmEpisodeDto) = e.type.isNullOrBlank() || e.type == "regular"

internal fun seasonsOf(eps: List<TvmEpisodeDto>): List<SeasonSummary> {
    val regular = eps.filter(::isRegular).groupBy { it.season }.toSortedMap().map { (n, l) ->
        SeasonSummary(n, null, l.size, l.mapNotNull { it.airdate?.ifBlank { null } }.minOrNull(), null)
    }
    val specials = eps.filterNot(::isRegular)
    return (if (specials.isEmpty()) emptyList() else listOf(SeasonSummary(0, null, specials.size, specials.mapNotNull { it.airdate?.ifBlank { null } }.minOrNull(), null))) + regular
}

internal fun seasonDetail(eps: List<TvmEpisodeDto>, number: Int): SeasonDetail {
    val list = if (number == 0) eps.filterNot(::isRegular).sortedWith(compareBy({ it.season }, { it.number ?: Int.MAX_VALUE }, { it.airdate ?: "" }))
    else eps.filter { isRegular(it) && it.season == number }.sortedBy { it.number ?: Int.MAX_VALUE }
    return SeasonDetail(number, null, null, list.mapIndexed { i, e ->
        EpisodeInfo(e.number ?: (i + 1), e.name?.ifBlank { null }, stripHtml(e.summary), e.airdate?.ifBlank { null }, e.runtime?.takeIf { it > 0 }, e.image.url(false))
    })
}

internal fun TvmShowDto.toDetail(eps: List<TvmEpisodeDto>) = TmdbDetail(
    ref = ref(id), title = name, tagline = null, overview = stripHtml(summary),
    posterPath = image.url(true), backdropPath = null, year = yearOf(premiered),
    firstAirDate = premiered?.ifBlank { null }, lastAirDate = ended?.ifBlank { null },
    runtimeMinutes = (runtime ?: averageRuntime)?.takeIf { it > 0 },
    totalEpisodes = eps.count(::isRegular).takeIf { it > 0 }, seasonCount = eps.filter(::isRegular).map { it.season }.distinct().size.takeIf { it > 0 },
    status = status?.ifBlank { null }, genres = genres, rating = rating?.average?.takeIf { it > 0 }?.toFloat(), certification = null,
    cast = embedded?.cast.orEmpty().take(20).map { CastMember(it.person.id, it.person.name, it.character?.name?.ifBlank { null }, it.person.image.url(false)) },
    crew = emptyList(), releases = emptyList(), seasons = seasonsOf(eps),
    externalIds = ExternalIds(imdb = externals?.imdb?.ifBlank { null }, tvdb = externals?.thetvdb),
    sourceUrl = url?.takeIf { it.startsWith("https://www.tvmaze.com/") },
)
