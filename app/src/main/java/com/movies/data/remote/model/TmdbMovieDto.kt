package com.movies.data.remote.model

import com.google.gson.annotations.SerializedName

data class TmdbResponse(
    @SerializedName("page")
    val page: Int,
    @SerializedName("results")
    val results: List<TmdbMovieDto>,
    @SerializedName("total_pages")
    val totalPages: Int,
    @SerializedName("total_results")
    val totalResults: Int
)

data class TmdbMovieDto(
    @SerializedName("id")
    val id: Int,
    @SerializedName("title")
    val title: String,
    @SerializedName("overview")
    val overview: String?,
    @SerializedName("poster_path")
    val posterPath: String?,
    @SerializedName("release_date")
    val releaseDate: String?,
    @SerializedName("vote_average")
    val voteAverage: Double?
)
