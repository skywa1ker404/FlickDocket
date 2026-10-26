package skywa1ker404.flickdocket.data.remote.tmdb

import skywa1ker404.flickdocket.domain.*

internal fun yearOf(date: String?): Int? = date?.takeIf { it.length >= 4 }?.substring(0, 4)?.toIntOrNull()

private val KEY_CREW_JOBS = setOf("Director", "Writer", "Screenplay", "Story", "Creator", "Executive Producer")

internal fun SummaryDto.toSummary(type: MediaType) = TmdbSummary(
    ref = TmdbRef(type, id),
    title = (if (type == MediaType.MOVIE) title ?: name else name ?: title).orEmpty(),
    year = yearOf(if (type == MediaType.MOVIE) releaseDate else firstAirDate),
    overview = overview?.ifBlank { null },
    posterPath = posterPath, backdropPath = backdropPath,
    rating = voteAverage?.takeIf { it > 0 }?.toFloat(),
)

internal fun SummaryDto.isAnime() = AnimeFilter.isAnime(genreIds, originalLanguage)

/** Anime heuristic applies to TV only; movies are never filtered. */
internal fun PageDto<SummaryDto>.toPage(type: MediaType) = ResultPage(
    page, totalPages,
    results.filter { type == MediaType.MOVIE || !it.isAnime() }.map { it.toSummary(type) },
)

private fun kindOf(type: Int): ReleaseKind? = when (type) {
    1 -> ReleaseKind.PREMIERE; 2 -> ReleaseKind.THEATRICAL_LIMITED; 3 -> ReleaseKind.THEATRICAL
    4 -> ReleaseKind.DIGITAL; 5 -> ReleaseKind.PHYSICAL; 6 -> ReleaseKind.TV; else -> null
}

internal fun DetailDto.toDetail(type: MediaType, region: String): TmdbDetail {
    val isMovie = type == MediaType.MOVIE
    val seasonList = seasons.map { SeasonSummary(it.seasonNumber, it.name?.ifBlank { null }, it.episodeCount, it.airDate?.ifBlank { null }, it.posterPath) }
        .sortedBy { it.number }
    val releases = releaseDates?.results.orEmpty().flatMap { c ->
        c.releaseDates.mapNotNull { e -> kindOf(e.type)?.let { ReleaseInfo(c.country, e.releaseDate?.take(10)?.ifBlank { null }, e.certification?.ifBlank { null }, it) } }
    }.sortedBy { it.date ?: "9999" }

    fun movieCert(country: String) = releaseDates?.results?.firstOrNull { it.country == country }
        ?.releaseDates?.firstNotNullOfOrNull { it.certification?.ifBlank { null } }
    fun tvCert(country: String) = contentRatings?.results?.firstOrNull { it.country == country }?.rating?.ifBlank { null }
    val cert = if (isMovie) movieCert(region) ?: movieCert("US") else tvCert(region) ?: tvCert("US")

    val crew = credits?.crew.orEmpty().filter { it.job in KEY_CREW_JOBS }
        .distinctBy { it.name to it.job }.take(12).map { CrewMember(it.id, it.name, it.job) }
    val cast = credits?.cast.orEmpty().sortedBy { it.order }.take(20).map { CastMember(it.id, it.name, it.character?.ifBlank { null }, it.profilePath) }

    return TmdbDetail(
        ref = TmdbRef(type, id),
        title = (if (isMovie) title ?: name else name ?: title).orEmpty(),
        tagline = tagline?.ifBlank { null }, overview = overview?.ifBlank { null },
        posterPath = posterPath, backdropPath = backdropPath,
        year = yearOf(if (isMovie) releaseDate else firstAirDate),
        firstAirDate = firstAirDate?.ifBlank { null }, lastAirDate = lastAirDate?.ifBlank { null },
        runtimeMinutes = if (isMovie) runtime?.takeIf { it > 0 } else episodeRunTime.firstOrNull { it > 0 },
        // Specials (season 0) are not counted towards the regular episode total.
        totalEpisodes = if (isMovie) null else seasonList.filter { it.number > 0 }.sumOf { it.episodeCount }.takeIf { it > 0 } ?: numberOfEpisodes,
        seasonCount = if (isMovie) null else numberOfSeasons,
        status = status?.ifBlank { null },
        genres = genres.map { it.name }.filter { it.isNotBlank() },
        rating = voteAverage?.takeIf { it > 0 }?.toFloat(),
        certification = cert, cast = cast, crew = crew, releases = if (isMovie) releases else emptyList(),
        seasons = if (isMovie) emptyList() else seasonList,
        nextEpisode = if (isMovie) null else nextEpisodeToAir?.let { n -> n.airDate?.ifBlank { null }?.let { date -> NextEpisode(n.seasonNumber, n.episodeNumber, n.name?.ifBlank { null }, date) } },
        externalIds = ExternalIds(
            imdb = (externalIds?.imdbId ?: imdbId)?.ifBlank { null },
            tvdb = externalIds?.tvdbId, wikidata = externalIds?.wikidataId?.ifBlank { null },
        ),
    )
}

internal fun SeasonDetailDto.toDomain() = SeasonDetail(
    seasonNumber, name?.ifBlank { null }, overview?.ifBlank { null },
    episodes.sortedBy { it.episodeNumber }.map { EpisodeInfo(it.episodeNumber, it.name?.ifBlank { null }, it.overview?.ifBlank { null }, it.airDate?.ifBlank { null }, it.runtime?.takeIf { r -> r > 0 }, it.stillPath) },
)

internal fun ConfigurationDto.toImageConfig(): ImageConfig {
    val d = ImageConfig()
    val i = images
    return ImageConfig(
        baseUrl = i.secureBaseUrl.ifBlank { d.baseUrl },
        posterSizes = i.posterSizes.ifEmpty { d.posterSizes }, backdropSizes = i.backdropSizes.ifEmpty { d.backdropSizes },
        profileSizes = i.profileSizes.ifEmpty { d.profileSizes }, stillSizes = i.stillSizes.ifEmpty { d.stillSizes },
    )
}
