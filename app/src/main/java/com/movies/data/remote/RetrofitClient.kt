package com.movies.data.remote

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    val apiService: TmdbApiService by lazy {
        Retrofit.Builder()
            .baseUrl(TmdbConstants.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TmdbApiService::class.java)
    }
}
