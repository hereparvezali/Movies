package com.movies.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.movies.data.model.MovieDetails
import com.movies.data.repository.MovieRepository
import com.movies.data.repository.MovieRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface DetailsUiState {
    data object Loading : DetailsUiState
    data class Success(val movie: MovieDetails) : DetailsUiState
    data class Error(val message: String) : DetailsUiState
}

class DetailsViewModel(
    private val repository: MovieRepository = MovieRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow<DetailsUiState>(DetailsUiState.Loading)
    val uiState: StateFlow<DetailsUiState> = _uiState.asStateFlow()

    private var currentMovieId: Int = -1

    fun loadMovieDetails(movieId: Int) {
        currentMovieId = movieId
        viewModelScope.launch {
            _uiState.value = DetailsUiState.Loading
            repository.getMovieDetails(movieId)
                .onSuccess { details ->
                    _uiState.value = DetailsUiState.Success(details)
                }
                .onFailure { throwable ->
                    _uiState.value = DetailsUiState.Error(
                        throwable.localizedMessage ?: "An unexpected error occurred"
                    )
                }
        }
    }

    fun retry() {
        if (currentMovieId != -1) {
            loadMovieDetails(currentMovieId)
        }
    }

    fun isFavorite(movieId: Int): kotlinx.coroutines.flow.Flow<Boolean> {
        return repository.isFavorite(movieId)
    }

    fun toggleFavorite(movie: MovieDetails, isCurrentlyFavorite: Boolean) {
        viewModelScope.launch {
            if (isCurrentlyFavorite) {
                repository.removeFavorite(movie.id)
            } else {
                repository.addFavorite(movie)
            }
        }
    }
}
