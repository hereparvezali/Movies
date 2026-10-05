package com.movies.data.model

/**
 * Movie categories available on TMDB.
 */
enum class MovieCategory(val endpoint: String, val displayName: String) {
    POPULAR("popular", "Popular"),
    NOW_PLAYING("now_playing", "Now Playing"),
    TOP_RATED("top_rated", "Top Rated"),
    UPCOMING("upcoming", "Upcoming")
}
