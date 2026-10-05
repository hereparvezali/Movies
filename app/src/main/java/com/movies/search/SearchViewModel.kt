package com.movies.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.movies.data.model.Movie
import com.movies.data.repository.MovieRepository
import com.movies.data.repository.MovieRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface SearchUiState {
    data object Idle : SearchUiState
    data object Loading : SearchUiState
    data class Success(val movies: List<Movie>) : SearchUiState
    data class Error(val message: String) : SearchUiState
}

class SearchViewModel(
    private val repository: MovieRepository = MovieRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var lastQuery: String = ""

    fun searchMovies(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return

        lastQuery = trimmed
        viewModelScope.launch {
            _uiState.value = SearchUiState.Loading
            repository.searchMovies(trimmed)
                .onSuccess { movies ->
                    _uiState.value = SearchUiState.Success(movies)
                }
                .onFailure { throwable ->
                    _uiState.value = SearchUiState.Error(
                        throwable.localizedMessage ?: "Failed to search movies"
                    )
                }
        }
    }

    fun retry() {
        if (lastQuery.isNotEmpty()) {
            searchMovies(lastQuery)
        }
    }
}
