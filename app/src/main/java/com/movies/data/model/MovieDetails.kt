package com.movies.data.model

data class MovieDetails(
    val id: Int,
    val title: String,
    val overview: String,
    val posterUrl: String?,
    val backdropUrl: String?,
    val releaseDate: String,
    val releaseYear: String,
    val rating: Double,
    val runtime: String,
    val genres: List<String>
)
