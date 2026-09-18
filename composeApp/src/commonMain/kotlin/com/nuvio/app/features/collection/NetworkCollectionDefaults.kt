package com.nuvio.app.features.collection

/**
 * Default "browse by streaming service" collections, seeded once per profile the first time
 * Collections ever loads with nothing configured. Each collection browses TMDB's Discover
 * endpoint filtered by `with_watch_providers`, split into a Movies tab and a TV Shows tab.
 *
 * Provider ids come from TMDB's watch-provider catalog (https://www.themoviedb.org/talk/605707962ac499002acc3e6a).
 * A couple of services have more than one id for regional/rebrand reasons — those are combined
 * with `|` (TMDB OR syntax) so results aren't split across an old and a new id.
 */
internal fun buildDefaultNetworkCollections(): List<Collection> =
    listOf(
        networkCollection(id = "network_netflix", title = "Netflix", watchProviders = "8"),
        networkCollection(id = "network_disney_plus", title = "Disney+", watchProviders = "337"),
        networkCollection(id = "network_prime_video", title = "Prime Video", watchProviders = "9|119"),
        networkCollection(id = "network_hbo_max", title = "HBO Max", watchProviders = "384|1899"),
        networkCollection(id = "network_hulu", title = "Hulu", watchProviders = "15"),
        networkCollection(id = "network_peacock", title = "Peacock", watchProviders = "386|387"),
        networkCollection(id = "network_apple_tv_plus", title = "Apple TV+", watchProviders = "350"),
    )

/**
 * Retired default network collection ids mapped to the id that replaces them, so installs
 * that already seeded the retired one get swapped over to the replacement instead of just
 * losing it. Only the key ids need to be excluded from [buildDefaultNetworkCollections].
 */
internal val RetiredDefaultNetworkReplacements: Map<String, String> = mapOf(
    "network_paramount_plus" to "network_apple_tv_plus",
)

private fun networkCollection(
    id: String,
    title: String,
    watchProviders: String,
): Collection {
    val filters = TmdbCollectionFilters(
        withWatchProviders = watchProviders,
        watchRegion = "US",
    )
    return Collection(
        id = id,
        title = title,
        viewMode = FolderViewMode.TABBED_GRID.name,
        showAllTab = false,
        folders = listOf(
            CollectionFolder(
                id = "${id}_folder",
                title = title,
                sources = listOf(
                    CollectionSource(
                        provider = "tmdb",
                        tmdbSourceType = TmdbCollectionSourceType.DISCOVER.name,
                        title = "Movies",
                        mediaType = TmdbCollectionMediaType.MOVIE.value,
                        sortBy = TmdbCollectionSort.POPULAR_DESC.value,
                        filters = filters,
                    ),
                    CollectionSource(
                        provider = "tmdb",
                        tmdbSourceType = TmdbCollectionSourceType.DISCOVER.name,
                        title = "TV Shows",
                        mediaType = TmdbCollectionMediaType.TV.value,
                        sortBy = TmdbCollectionSort.POPULAR_DESC.value,
                        filters = filters,
                    ),
                ),
            ),
        ),
    )
}
