package com.nuvio.app.features.tmdb

/**
 * TMDB's discover/list responses only include numeric `genre_ids`, not names — the name list
 * lives behind a separate `/genre/movie/list` call. These ids are stable and publicly documented
 * (https://developer.themoviedb.org/reference/genre-movie-list), so it's simpler to hardcode them
 * than spend an extra API round trip on every catalog fetch.
 */
private val tmdbMovieGenreNamesById = mapOf(
    28 to "Action", 12 to "Adventure", 16 to "Animation", 35 to "Comedy", 80 to "Crime",
    99 to "Documentary", 18 to "Drama", 10751 to "Family", 14 to "Fantasy", 36 to "History",
    27 to "Horror", 10402 to "Music", 9648 to "Mystery", 10749 to "Romance",
    878 to "Science Fiction", 10770 to "TV Movie", 53 to "Thriller", 10752 to "War", 37 to "Western",
)

private val tmdbTvGenreNamesById = mapOf(
    10759 to "Action & Adventure", 16 to "Animation", 35 to "Comedy", 80 to "Crime",
    99 to "Documentary", 18 to "Drama", 10751 to "Family", 10762 to "Kids", 9648 to "Mystery",
    10763 to "News", 10764 to "Reality", 10765 to "Sci-Fi & Fantasy", 10766 to "Soap",
    10767 to "Talk", 10768 to "War & Politics", 37 to "Western",
)

internal fun tmdbGenreNames(genreIds: List<Int>, isTv: Boolean): List<String> {
    val map = if (isTv) tmdbTvGenreNamesById else tmdbMovieGenreNamesById
    return genreIds.mapNotNull { map[it] }
}
