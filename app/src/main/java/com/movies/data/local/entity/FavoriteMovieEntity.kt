package com.movies.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.movies.data.model.Movie
import com.movies.data.model.MovieDetails


@Entity(tableName = "favorite_movies")
data class FavoriteMovieEntity(
    @PrimaryKey
    val id: Int,
    val title: String,
    val overview: String,
    val posterUrl: String?,
    val releaseYear: String,
    val rating: Double
)


fun FavoriteMovieEntity.toMovie(): Movie {
    return Movie(
        id = id,
        title = title,
        overview = overview,
        posterUrl = posterUrl,
        releaseYear = releaseYear,
        rating = rating
    )
}


fun MovieDetails.toFavoriteEntity(): FavoriteMovieEntity {
    return FavoriteMovieEntity(
        id = id,
        title = title,
        overview = overview,
        posterUrl = posterUrl,
        releaseYear = releaseYear,
        rating = rating
    )
}
