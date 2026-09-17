package com.nuvio.app.features.search

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nuvio.app.core.ui.NuvioDropdownChip
import com.nuvio.app.core.ui.NuvioDropdownOption
import com.nuvio.app.features.home.MetaPreview
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.discover_all_genres
import nuvio.composeapp.generated.resources.discover_select_genre
import nuvio.composeapp.generated.resources.discover_select_type
import nuvio.composeapp.generated.resources.media_anime
import nuvio.composeapp.generated.resources.media_channels
import nuvio.composeapp.generated.resources.media_movies
import nuvio.composeapp.generated.resources.media_series
import nuvio.composeapp.generated.resources.media_tv
import nuvio.composeapp.generated.resources.search_all_ratings
import nuvio.composeapp.generated.resources.search_all_types
import nuvio.composeapp.generated.resources.search_all_years
import nuvio.composeapp.generated.resources.search_rating_min_format
import nuvio.composeapp.generated.resources.search_select_rating
import nuvio.composeapp.generated.resources.search_select_sort
import nuvio.composeapp.generated.resources.search_select_year
import nuvio.composeapp.generated.resources.search_sort_az
import nuvio.composeapp.generated.resources.search_sort_newest
import nuvio.composeapp.generated.resources.search_sort_oldest
import nuvio.composeapp.generated.resources.search_sort_relevance
import nuvio.composeapp.generated.resources.search_sort_top_rated
import nuvio.composeapp.generated.resources.search_year_earlier
import org.jetbrains.compose.resources.stringResource

private val ratingThresholds = listOf(9.0, 8.0, 7.0, 6.0, 5.0)

/** (label decade start, minYear, maxYear) — null maxYear means "and newer". */
private val yearBuckets = listOf(
    Triple("2020s", 2020, null),
    Triple("2010s", 2010, 2019),
    Triple("2000s", 2000, 2009),
    Triple("1990s", 1990, 1999),
)

@Composable
internal fun SearchResultsFilterBar(
    allItems: List<MetaPreview>,
    sortOption: SearchSortOption,
    filterState: SearchFilterState,
    onSortSelected: (SearchSortOption) -> Unit,
    onTypeSelected: (String?) -> Unit,
    onGenreSelected: (String?) -> Unit,
    onRatingSelected: (Double?) -> Unit,
    modifier: Modifier = Modifier,
    onYearSelected: (Int?, Int?) -> Unit = { _, _ -> },
) {
    val typeOptions = availableSearchTypes(allItems)
    val genreOptions = availableSearchGenres(allItems)

    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        NuvioDropdownChip(
            title = stringResource(Res.string.search_select_sort),
            label = sortOption.displayLabel(),
            selectedKey = sortOption.name,
            options = SearchSortOption.entries.map { option ->
                NuvioDropdownOption(key = option.name, label = option.displayLabel())
            },
            onSelected = { onSortSelected(SearchSortOption.valueOf(it.key)) },
        )

        // Only shown when the row actually mixes types — a single-type row (e.g. a "Movies"
        // tab) has nothing to filter, so a disabled control here would just look broken.
        if (typeOptions.size > 1) {
            val typeDropdownOptions = buildList {
                add(NuvioDropdownOption(key = "", label = stringResource(Res.string.search_all_types)))
                addAll(typeOptions.map { type -> NuvioDropdownOption(key = type, label = type.displayTypeLabel()) })
            }
            NuvioDropdownChip(
                title = stringResource(Res.string.discover_select_type),
                label = filterState.type?.displayTypeLabel() ?: stringResource(Res.string.search_all_types),
                selectedKey = filterState.type ?: "",
                options = typeDropdownOptions,
                onSelected = { onTypeSelected(it.key.ifBlank { null }) },
            )
        }

        // Only shown when at least one item actually carries genre data — many addon/search
        // results don't, and a permanently-empty dropdown is worse than no dropdown.
        if (genreOptions.isNotEmpty()) {
            val genreDropdownOptions = buildList {
                add(NuvioDropdownOption(key = "", label = stringResource(Res.string.discover_all_genres)))
                addAll(genreOptions.map { genre -> NuvioDropdownOption(key = genre, label = genre) })
            }
            NuvioDropdownChip(
                title = stringResource(Res.string.discover_select_genre),
                label = filterState.genre ?: stringResource(Res.string.discover_all_genres),
                selectedKey = filterState.genre ?: "",
                options = genreDropdownOptions,
                onSelected = { onGenreSelected(it.key.ifBlank { null }) },
            )
        }

        val yearDropdownOptions = buildList {
            add(NuvioDropdownOption(key = "", label = stringResource(Res.string.search_all_years)))
            yearBuckets.forEach { (label, _, _) ->
                add(NuvioDropdownOption(key = label, label = label))
            }
            add(NuvioDropdownOption(key = "earlier", label = stringResource(Res.string.search_year_earlier)))
        }
        val selectedYearKey = yearBuckets.firstOrNull { (_, min, max) ->
            filterState.minYear == min && filterState.maxYear == max
        }?.first ?: if (filterState.minYear == null && filterState.maxYear == 1989) "earlier" else ""
        NuvioDropdownChip(
            title = stringResource(Res.string.search_select_year),
            label = yearDropdownOptions.firstOrNull { it.key == selectedYearKey }?.label
                ?: stringResource(Res.string.search_all_years),
            selectedKey = selectedYearKey,
            options = yearDropdownOptions,
            onSelected = { option ->
                when (option.key) {
                    "" -> onYearSelected(null, null)
                    "earlier" -> onYearSelected(null, 1989)
                    else -> {
                        val bucket = yearBuckets.first { it.first == option.key }
                        onYearSelected(bucket.second, bucket.third)
                    }
                }
            },
        )

        val ratingDropdownOptions = buildList {
            add(NuvioDropdownOption(key = "", label = stringResource(Res.string.search_all_ratings)))
            ratingThresholds.forEach { threshold ->
                add(
                    NuvioDropdownOption(
                        key = threshold.toString(),
                        label = stringResource(Res.string.search_rating_min_format, threshold.toInt().toString()),
                    ),
                )
            }
        }
        NuvioDropdownChip(
            title = stringResource(Res.string.search_select_rating),
            label = filterState.minRating?.let {
                stringResource(Res.string.search_rating_min_format, it.toInt().toString())
            } ?: stringResource(Res.string.search_all_ratings),
            selectedKey = filterState.minRating?.toString() ?: "",
            options = ratingDropdownOptions,
            onSelected = { onRatingSelected(it.key.toDoubleOrNull()) },
        )
    }
}

@Composable
private fun SearchSortOption.displayLabel(): String = when (this) {
    SearchSortOption.Relevance -> stringResource(Res.string.search_sort_relevance)
    SearchSortOption.Newest -> stringResource(Res.string.search_sort_newest)
    SearchSortOption.Oldest -> stringResource(Res.string.search_sort_oldest)
    SearchSortOption.TopRated -> stringResource(Res.string.search_sort_top_rated)
    SearchSortOption.AlphabeticalAZ -> stringResource(Res.string.search_sort_az)
}

@Composable
private fun String.displayTypeLabel(): String =
    when (lowercase()) {
        "movie" -> stringResource(Res.string.media_movies)
        "series" -> stringResource(Res.string.media_series)
        "anime" -> stringResource(Res.string.media_anime)
        "channel" -> stringResource(Res.string.media_channels)
        "tv" -> stringResource(Res.string.media_tv)
        else -> replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
