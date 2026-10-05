package com.movies.data.repository

import com.movies.MovieApp
import com.movies.data.local.AppDatabase
import com.movies.data.local.dao.FavoriteMovieDao
import com.movies.data.local.entity.toFavoriteEntity
import com.movies.data.local.entity.toMovie
import com.movies.data.model.Movie
import com.movies.data.model.MovieDetails
import com.movies.data.remote.RetrofitClient
import com.movies.data.remote.TmdbApiService
import com.movies.data.remote.TmdbConstants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface MovieRepository {

    suspend fun getPopularMovies(): Result<List<Movie>>
    suspend fun searchMovies(query: String): Result<List<Movie>>
    suspend fun getMovieDetails(movieId: Int): Result<MovieDetails>

    // Room Favorites
    fun getFavoriteMovies(): Flow<List<Movie>>
    fun isFavorite(movieId: Int): Flow<Boolean>
    suspend fun addFavorite(movie: MovieDetails)
    suspend fun removeFavorite(movieId: Int)
}

class MovieRepositoryImpl(
    private val apiService: TmdbApiService = RetrofitClient.apiService,
    private val favoriteDao: FavoriteMovieDao = AppDatabase.getDatabase(MovieApp.instance).favoriteMovieDao()
) : MovieRepository {

    override suspend fun getPopularMovies(): Result<List<Movie>> {
        return try {
            val response = apiService.getPopularMovies()
            val movies = response.results.map { dto ->
                Movie(
                    id = dto.id,
                    title = dto.title,
                    overview = dto.overview ?: "",
                    posterUrl = dto.posterPath?.let { "${TmdbConstants.IMAGE_BASE_URL}$it" },
                    releaseYear = dto.releaseDate?.takeIf { it.length >= 4 }?.substring(0, 4) ?: "N/A",
                    rating = dto.voteAverage?.let { Math.round(it * 10.0) / 10.0 } ?: 0.0
                )
            }
            Result.success(movies)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun searchMovies(query: String): Result<List<Movie>> {
        return try {
            val response = apiService.searchMovies(query = query)
            val movies = response.results.map { dto ->
                Movie(
                    id = dto.id,
                    title = dto.title,
                    overview = dto.overview ?: "",
                    posterUrl = dto.posterPath?.let { "${TmdbConstants.IMAGE_BASE_URL}$it" },
                    releaseYear = dto.releaseDate?.takeIf { it.length >= 4 }?.substring(0, 4) ?: "N/A",
                    rating = dto.voteAverage?.let { Math.round(it * 10.0) / 10.0 } ?: 0.0
                )
            }
            Result.success(movies)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getMovieDetails(movieId: Int): Result<MovieDetails> {
        return try {
            val dto = apiService.getMovieDetails(movieId = movieId)
            val movieDetails = MovieDetails(
                id = dto.id,
                title = dto.title,
                overview = dto.overview ?: "",
                posterUrl = dto.posterPath?.let { "${TmdbConstants.IMAGE_BASE_URL}$it" },
                backdropUrl = (dto.backdropPath ?: dto.posterPath)?.let { "${TmdbConstants.IMAGE_BASE_URL}$it" },
                releaseDate = dto.releaseDate ?: "N/A",
                releaseYear = dto.releaseDate?.takeIf { it.length >= 4 }?.substring(0, 4) ?: "N/A",
                rating = dto.voteAverage?.let { Math.round(it * 10.0) / 10.0 } ?: 0.0,
                runtime = dto.runtime?.let { formatRuntime(it) } ?: "N/A",
                genres = dto.genres?.map { it.name } ?: emptyList()
            )
            Result.success(movieDetails)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun formatRuntime(minutes: Int): String {
        if (minutes <= 0) return "N/A"
        val hours = minutes / 60
        val remainingMinutes = minutes % 60
        return if (hours > 0) {
            if (remainingMinutes > 0) "${hours}h ${remainingMinutes}m" else "${hours}h"
        } else {
            "${remainingMinutes}m"
        }
    }

    override fun getFavoriteMovies(): Flow<List<Movie>> {
        return favoriteDao.getAllFavorites().map { entities ->
            entities.map { it.toMovie() }
        }
    }

    override fun isFavorite(movieId: Int): Flow<Boolean> {
        return favoriteDao.isFavorite(movieId)
    }

    override suspend fun addFavorite(movie: MovieDetails) {
        favoriteDao.insertFavorite(movie.toFavoriteEntity())
    }

    override suspend fun removeFavorite(movieId: Int) {
        favoriteDao.deleteFavoriteById(movieId)
    }
}
