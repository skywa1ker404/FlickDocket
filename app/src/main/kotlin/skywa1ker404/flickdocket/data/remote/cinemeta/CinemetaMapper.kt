package skywa1ker404.flickdocket.data.remote.cinemeta

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.longOrNull
import skywa1ker404.flickdocket.data.remote.tmdb.yearOf
import skywa1ker404.flickdocket.domain.*

private const val POSTER_HOST = "https://images.metahub.space/"
private const val BACKGROUND_HOST = "https://images.metahub.space/"
private const val THUMB_HOST = "https://episodes.metahub.space/"
private val DIGITS = Regex("\\d+")
private val HOURS = Regex("(\\d+)\\s*h")
private val MINUTES = Regex("(\\d+)\\s*m")

/** Accepts a JSON array of strings or a comma separated string; anything else is empty. */
internal fun JsonElement?.strings(): List<String> = when (this) {
    is JsonArray -> mapNotNull { (it as? JsonPrimitive)?.contentOrNull?.trim()?.takeIf(String::isNotEmpty) }
    is JsonPrimitive -> contentOrNull?.split(',')?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
    else -> emptyList()
}

private fun JsonElement?.text(): String? = (this as? JsonPrimitive)?.contentOrNull?.trim()?.ifBlank { null }

internal fun parseRuntime(raw: String?): Int? {
    if (raw.isNullOrBlank()) return null
    val h = HOURS.find(raw)?.groupValues?.get(1)?.toIntOrNull()
    val m = MINUTES.find(raw)?.groupValues?.get(1)?.toIntOrNull()
    val total = if (h != null) h * 60 + (m ?: 0) else m ?: DIGITS.find(raw)?.value?.toIntOrNull()
    return total?.takeIf { it > 0 }
}

internal fun CinemetaMetaDto.imdb(): String? = listOf(imdbId, id).firstOrNull { ImdbIds.isValid(it?.lowercase()) }?.lowercase()

/** Only the service's own image hosts are accepted; anything else (including third-party CDNs) is dropped. */
private fun imageOnly(url: String?, host: String) = url?.takeIf { it.startsWith(host) }

internal fun posterUrl(imdb: String, provided: String?) = imageOnly(provided, POSTER_HOST) ?: "${POSTER_HOST}poster/small/$imdb/img"

private fun CinemetaMetaDto.yearValue(): Int? =
    yearOf(released) ?: (releaseInfo.text() ?: year.text())?.let { DIGITS.find(it)?.value?.takeIf { d -> d.length == 4 }?.toIntOrNull() }

private fun CinemetaMetaDto.genreList() = (genres.strings().ifEmpty { genre.strings() }).distinct()

/** Catalog and meta items both go through here; items without a valid IMDb id or name are dropped by the caller. */
internal fun CinemetaMetaDto.toSummary(type: MediaType): TmdbSummary? {
    val imdb = imdb() ?: return null
    val n = name?.trim()?.ifBlank { null } ?: return null
    return TmdbSummary(
        ref = TmdbRef(type, ImdbIds.toLong(imdb) ?: return null, RefSource.CINEMETA), title = n, year = yearValue(),
        overview = description?.trim()?.ifBlank { null }, posterPath = posterUrl(imdb, poster),
        backdropPath = imageOnly(background, BACKGROUND_HOST), rating = null, // IMDb ratings are not shown: no licensed access
    )
}

/** Anime is excluded from TV like the other sources; catalog entries name genres and country, not language. */
internal fun CinemetaMetaDto.isAnime(): Boolean =
    AnimeFilter.isAnimeByName(genreList(), if (country.strings().any { it.contains("Japan", ignoreCase = true) }) "Japanese" else null)

internal fun CinemetaCatalogDto.toPage(type: MediaType): ResultPage<TmdbSummary> =
    ResultPage(1, 1, metas.filter { type == MediaType.MOVIE || !it.isAnime() }.mapNotNull { it.toSummary(type) }.distinctBy { it.ref })

private fun String?.day() = this?.takeIf { it.length >= 10 }?.substring(0, 10)
private fun CinemetaVideoDto.airDay() = (firstAired ?: released).day()
private fun CinemetaVideoDto.episodeNumber() = number ?: episode
private fun isRegularEpisode(v: CinemetaVideoDto) = (v.season ?: -1) > 0 && v.episodeNumber() != null

internal fun nextEpisodeOf(videos: List<CinemetaVideoDto>, today: String): NextEpisode? =
    videos.filter(::isRegularEpisode).mapNotNull { v -> v.airDay()?.takeIf { it >= today }?.let { v to it } }
        .minWithOrNull(compareBy({ it.second }, { it.first.season }, { it.first.episodeNumber() }))
        ?.let { (v, day) -> NextEpisode(v.season!!, v.episodeNumber(), v.name?.trim()?.ifBlank { null }, day) }

internal fun seasonsOf(videos: List<CinemetaVideoDto>): List<SeasonSummary> {
    fun summary(n: Int, list: List<CinemetaVideoDto>) = SeasonSummary(n, null, list.size, list.mapNotNull { it.airDay() }.minOrNull(), null)
    val regular = videos.filter(::isRegularEpisode).groupBy { it.season!! }.toSortedMap().map { (n, l) -> summary(n, l) }
    val specials = videos.filter { it.season == 0 && it.episodeNumber() != null }
    return regular + if (specials.isEmpty()) emptyList() else listOf(summary(0, specials))
}

internal fun seasonDetail(videos: List<CinemetaVideoDto>, number: Int): SeasonDetail =
    SeasonDetail(number, null, null, videos.filter { it.season == number && it.episodeNumber() != null }
        .sortedWith(compareBy({ it.episodeNumber() }, { it.airDay() ?: "" }))
        .map { v ->
            EpisodeInfo(
                v.episodeNumber()!!, v.name?.trim()?.ifBlank { null }, (v.overview ?: v.description)?.trim()?.ifBlank { null },
                v.airDay(), null, imageOnly(v.thumbnail, THUMB_HOST),
            )
        })

internal fun CinemetaMetaDto.toDetail(type: MediaType, today: String = java.time.LocalDate.now().toString()): TmdbDetail? {
    val summary = toSummary(type) ?: return null
    val imdb = imdb()!!
    val isTv = type == MediaType.TV
    val regular = videos.filter(::isRegularEpisode)
    val crew = director.strings().map { "Director" to it } + writer.strings().map { "Writer" to it }
    return TmdbDetail(
        ref = summary.ref, title = summary.title, tagline = null, overview = summary.overview,
        posterPath = summary.posterPath, backdropPath = summary.backdropPath, year = summary.year,
        firstAirDate = released.day(), lastAirDate = null,
        runtimeMinutes = parseRuntime(runtime.text()),
        totalEpisodes = if (isTv) regular.size.takeIf { it > 0 } else null,
        seasonCount = if (isTv) regular.map { it.season }.distinct().size.takeIf { it > 0 } else null,
        status = if (isTv) status?.ifBlank { null } else null,
        genres = genreList(), rating = null, certification = null,
        cast = cast.strings().take(20).mapIndexed { i, n -> CastMember(i + 1L, n, null, null) },
        crew = crew.take(12).mapIndexed { i, (job, n) -> CrewMember(i + 1L, n, job) },
        releases = emptyList(),
        seasons = if (isTv) seasonsOf(videos) else emptyList(),
        externalIds = ExternalIds(imdb = imdb, tvdb = tvdbId.text()?.toLongOrNull() ?: (tvdbId as? JsonPrimitive)?.longOrNull),
        nextEpisode = if (isTv) nextEpisodeOf(videos, today) else null,
    )
}
