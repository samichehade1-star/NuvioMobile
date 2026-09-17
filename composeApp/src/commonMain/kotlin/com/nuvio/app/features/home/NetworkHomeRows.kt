package com.nuvio.app.features.home

import co.touchlab.kermit.Logger
import com.nuvio.app.features.catalog.CatalogTarget
import com.nuvio.app.features.collection.CollectionRepository
import com.nuvio.app.features.collection.TmdbCollectionSourceResolver
import com.nuvio.app.features.collection.catalogRouteKey
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

private val log = Logger.withTag("NetworkHomeRows")

/**
 * Builds real Home rows (actual title posters, not a click-through folder tile) for the default
 * "browse by streaming service" collections seeded by [com.nuvio.app.features.collection.buildDefaultNetworkCollections].
 * Reuses [TmdbCollectionSourceResolver] and [CatalogTarget.CollectionSource] so "See All"/pagination
 * on these rows goes through the same path collection folders already use.
 */
internal suspend fun fetchNetworkHomeSections(): List<HomeCatalogSection> = coroutineScope {
    val networkCollections = CollectionRepository.collections.value.filter { it.id.startsWith("network_") }
    if (networkCollections.isEmpty()) return@coroutineScope emptyList()

    networkCollections.flatMap { collection ->
        collection.folders.flatMap { folder ->
            folder.sources.map { source ->
                async {
                    runCatching {
                        val page = TmdbCollectionSourceResolver.resolve(source, page = 1)
                        if (page.items.isEmpty()) return@runCatching null
                        HomeCatalogSection(
                            key = "network:${collection.id}:${source.catalogRouteKey()}",
                            title = listOfNotNull(collection.title, source.title).joinToString(" - "),
                            subtitle = collection.title,
                            addonName = collection.title,
                            target = CatalogTarget.CollectionSource(
                                collectionId = collection.id,
                                folderId = folder.id,
                                sourceKey = source.catalogRouteKey(),
                                contentType = source.mediaType ?: "movie",
                                supportsPagination = true,
                            ),
                            items = page.items,
                            availableItemCount = page.rawItemCount,
                            hasMore = page.nextSkip != null,
                        )
                    }.onFailure { error ->
                        log.w(error) { "Failed to fetch network row for ${collection.id}/${source.title}" }
                    }.getOrNull()
                }
            }
        }
    }.awaitAll().filterNotNull()
}
