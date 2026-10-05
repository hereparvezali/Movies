package com.movies.data.remote

import com.movies.data.remote.model.TmdbMovieDetailsDto
import com.movies.data.remote.model.TmdbResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface TmdbApiService {

    @GET("movie/popular")
    suspend fun getPopularMovies(
        @Query("api_key") apiKey: String = TmdbConstants.API_KEY,
        @Query("page") page: Int = 1
    ): TmdbResponse

    @GET("search/movie")
    suspend fun searchMovies(
        @Query("query") query: String,
        @Query("api_key") apiKey: String = TmdbConstants.API_KEY,
        @Query("page") page: Int = 1
    ): TmdbResponse

    @GET("movie/{movie_id}")
    suspend fun getMovieDetails(
        @Path("movie_id") movieId: Int,
        @Query("api_key") apiKey: String = TmdbConstants.API_KEY
    ): TmdbMovieDetailsDto

    @GET("movie/{category}")
    suspend fun getMoviesByCategory(
        @Path("category") category: String,
        @Query("api_key") apiKey: String = TmdbConstants.API_KEY,
        @Query("page") page: Int = 1
    ): TmdbResponse
}
