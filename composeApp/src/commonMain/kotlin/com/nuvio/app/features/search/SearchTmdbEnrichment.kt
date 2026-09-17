package com.nuvio.app.features.search

import co.touchlab.kermit.Logger
import com.nuvio.app.features.addons.httpGetText
import com.nuvio.app.features.home.MetaPreview
import com.nuvio.app.features.tmdb.TmdbSettingsRepository
import com.nuvio.app.features.tmdb.buildTmdbUrl
import com.nuvio.app.features.tmdb.tmdbGenreNames
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Cinemeta's search catalog often returns bare-bones previews (no genres, no rating) for most
 * exact-title matches, only including the full metadata blob for a lucky few fuzzy matches. This
 * backfills the missing fields from TMDB by IMDb id so sort/filter by genre and rating work
 * consistently across all search results, not just the ones Cinemeta happened to enrich.
 */
internal object SearchTmdbEnrichment {
    private val log = Logger.withTag("SearchTmdbEnrichment")
    private val json = Json { ignoreUnknownKeys = true }
    private val cache = linkedMapOf<String, TmdbFindMovieResult?>()
    private val concurrencyLimit = Semaphore(6)
    private const val MAX_ITEMS_PER_PASS = 60

    suspend fun enrichMissingFields(items: List<MetaPreview>): List<MetaPreview> {
        val apiKey = TmdbSettingsRepository.effectiveApiKey()
        if (apiKey.isBlank()) return items

        val candidates = items
            .filter { it.needsEnrichment() && it.id.startsWith("tt", ignoreCase = true) }
            .take(MAX_ITEMS_PER_PASS)
        if (candidates.isEmpty()) return items

        val enrichedById = coroutineScope {
            candidates.map { item ->
                async {
                    item.id to runCatching { fetchFindResult(item.id, item.type, apiKey) }.getOrNull()
                }
            }.associate { it.await() }
        }

        return items.map { item ->
            val found = enrichedById[item.id] ?: return@map item
            item.copy(
                genres = item.genres.ifEmpty { found.genreNames(item.type) },
                imdbRating = item.imdbRating ?: found.voteAverage?.takeIf { it > 0.0 }?.let { formatRating(it) },
                rawReleaseDate = item.rawReleaseDate ?: found.releaseDate(),
                popularity = item.popularity ?: found.popularity,
            )
        }
    }

    private fun MetaPreview.needsEnrichment(): Boolean =
        genres.isEmpty() || imdbRating.isNullOrBlank()

    private suspend fun fetchFindResult(imdbId: String, type: String, apiKey: String): TmdbFindMovieResult? {
        cache[imdbId]?.let { return it }
        concurrencyLimit.withPermit {
            cache[imdbId]?.let { return it }
            val url = buildTmdbUrl(
                endpoint = "find/$imdbId",
                apiKey = apiKey,
                query = mapOf("external_source" to "imdb_id"),
            )
            val result = runCatching {
                val response = json.decodeFromString<TmdbFindResponse>(httpGetText(url))
                if (type.equals("series", ignoreCase = true) || type.equals("tv", ignoreCase = true)) {
                    response.tvResults.firstOrNull()
                } else {
                    response.movieResults.firstOrNull()
                }
            }.onFailure { error ->
                log.w { "TMDB find failed for $imdbId: ${error.message}" }
            }.getOrNull()
            cache[imdbId] = result
            return result
        }
    }

    private fun formatRating(value: Double): String =
        (kotlin.math.round(value * 10) / 10.0).toString()
}

@Serializable
private data class TmdbFindResponse(
    @SerialName("movie_results") val movieResults: List<TmdbFindMovieResult> = emptyList(),
    @SerialName("tv_results") val tvResults: List<TmdbFindMovieResult> = emptyList(),
)

@Serializable
private data class TmdbFindMovieResult(
    @SerialName("genre_ids") val genreIds: List<Int> = emptyList(),
    @SerialName("vote_average") val voteAverage: Double? = null,
    @SerialName("popularity") val popularity: Double? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("first_air_date") val firstAirDate: String? = null,
) {
    fun releaseDate(): String? = releaseDate?.takeIf { it.isNotBlank() } ?: firstAirDate?.takeIf { it.isNotBlank() }

    fun genreNames(type: String): List<String> =
        tmdbGenreNames(genreIds, isTv = type.equals("series", ignoreCase = true) || type.equals("tv", ignoreCase = true))
}
