package skywa1ker404.flickdocket.domain

/** Static description of planned provider integrations. Connection state lives elsewhere (Phase 2+). */
enum class ProviderId { TMDB, TRAKT, SIMKL, MDBLIST, SERIALIZD_IMPORT }

enum class ConnectionState { COMING_SOON, NOT_CONNECTED, CONNECTED }

data class ProviderInfo(val id: ProviderId, val name: String, val role: String, val plannedPhase: Int)

val PlannedProviders = listOf(
    ProviderInfo(ProviderId.TMDB, "TMDB", "Primary metadata and posters", 2),
    ProviderInfo(ProviderId.SERIALIZD_IMPORT, "Serializd", "File import with preview", 3),
    ProviderInfo(ProviderId.TRAKT, "Trakt", "Optional two-way sync", 4),
    ProviderInfo(ProviderId.SIMKL, "Simkl", "Optional two-way sync", 4),
    ProviderInfo(ProviderId.MDBLIST, "MDBList", "Optional ratings via your API key", 5),
)
