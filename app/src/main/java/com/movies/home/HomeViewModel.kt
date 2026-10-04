package com.movies.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.movies.data.model.Movie
import com.movies.data.repository.MovieRepository
import com.movies.data.repository.MovieRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Success(val movies: List<Movie>) : HomeUiState
    data class Error(val message: String) : HomeUiState
}

class HomeViewModel(
    private val repository: MovieRepository = MovieRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        fetchPopularMovies()
    }

    fun fetchPopularMovies() {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            repository.getPopularMovies()
                .onSuccess { movies ->
                    _uiState.value = HomeUiState.Success(movies)
                }
                .onFailure { throwable ->
                    _uiState.value = HomeUiState.Error(
                        throwable.localizedMessage ?: "An unexpected error occurred"
                    )
                }
        }
    }
}
