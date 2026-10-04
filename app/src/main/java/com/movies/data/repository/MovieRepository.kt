package com.movies.data.repository

import com.movies.data.model.Movie
import com.movies.data.remote.RetrofitClient
import com.movies.data.remote.TmdbApiService
import com.movies.data.remote.TmdbConstants

interface MovieRepository {
    suspend fun getPopularMovies(): Result<List<Movie>>
    suspend fun searchMovies(query: String): Result<List<Movie>>
}

class MovieRepositoryImpl(
    private val apiService: TmdbApiService = RetrofitClient.apiService
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
}
