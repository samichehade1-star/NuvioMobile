package com.nuvio.app.features.search

import com.nuvio.app.features.home.HomeCatalogSection
import com.nuvio.app.features.home.MetaPreview
import com.nuvio.app.features.home.stableKey

enum class SearchSortOption {
    Relevance,
    Newest,
    Oldest,
    TopRated,
    AlphabeticalAZ,
}

data class SearchFilterState(
    val type: String? = null,
    val genre: String? = null,
    val minRating: Double? = null,
    val minYear: Int? = null,
    val maxYear: Int? = null,
) {
    val activeCount: Int
        get() = listOfNotNull(type, genre, minRating, minYear ?: maxYear).size
}

/** Flattens per-addon/catalog search sections into one deduplicated list, keeping first-seen order. */
internal fun mergeSearchSections(sections: List<HomeCatalogSection>): List<MetaPreview> {
    val merged = LinkedHashMap<String, MetaPreview>()
    sections.forEach { section ->
        section.items.forEach { item ->
            val key = item.stableKey()
            if (key !in merged) merged[key] = item
        }
    }
    return merged.values.toList()
}

internal fun MetaPreview.releaseYear(): Int? =
    (rawReleaseDate?.take(4) ?: releaseInfo?.take(4))?.toIntOrNull()

internal fun MetaPreview.ratingValue(): Double? = imdbRating?.toDoubleOrNull()

internal fun availableSearchTypes(items: List<MetaPreview>): List<String> =
    items.map { it.type }.distinct()

internal fun availableSearchGenres(items: List<MetaPreview>): List<String> =
    items.flatMap { it.genres }.distinct().sorted()

internal fun applySearchFilters(items: List<MetaPreview>, filter: SearchFilterState): List<MetaPreview> =
    items.filter { item ->
        (filter.type == null || item.type.equals(filter.type, ignoreCase = true)) &&
            (filter.genre == null || item.genres.any { it.equals(filter.genre, ignoreCase = true) }) &&
            (filter.minRating == null || (item.ratingValue() ?: -1.0) >= filter.minRating) &&
            (filter.minYear == null || (item.releaseYear() ?: Int.MIN_VALUE) >= filter.minYear) &&
            (filter.maxYear == null || (item.releaseYear() ?: Int.MAX_VALUE) <= filter.maxYear)
    }

internal fun applySearchSort(items: List<MetaPreview>, sort: SearchSortOption): List<MetaPreview> =
    when (sort) {
        SearchSortOption.Relevance -> items
        SearchSortOption.Newest -> items.sortedByDescending { it.releaseYear() ?: Int.MIN_VALUE }
        SearchSortOption.Oldest -> items.sortedBy { it.releaseYear() ?: Int.MAX_VALUE }
        SearchSortOption.TopRated -> items.sortedByDescending { it.ratingValue() ?: -1.0 }
        SearchSortOption.AlphabeticalAZ -> items.sortedBy { it.name.lowercase() }
    }
