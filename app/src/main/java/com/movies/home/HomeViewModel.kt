package com.movies.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.movies.data.model.Movie
import com.movies.data.model.MovieCategory
import com.movies.data.repository.MovieRepository
import com.movies.data.repository.MovieRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Success(
        val movies: List<Movie>,
        val isLoadingMore: Boolean = false
    ) : HomeUiState
    data class Error(val message: String) : HomeUiState
}

class HomeViewModel(
    private val repository: MovieRepository = MovieRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var currentCategory = MovieCategory.POPULAR
    private var currentPage = 1
    private var isFetching = false
    private val allMovies = mutableListOf<Movie>()

    init {
        fetchMovies()
    }

    fun selectCategory(category: MovieCategory) {
        if (currentCategory == category) return
        currentCategory = category
        currentPage = 1
        allMovies.clear()
        _uiState.value = HomeUiState.Loading
        fetchMovies()
    }

    fun loadNextPage() {
        val currentState = _uiState.value
        if (isFetching || currentState !is HomeUiState.Success || currentState.isLoadingMore) {
            return
        }

        _uiState.value = currentState.copy(isLoadingMore = true)
        currentPage++
        fetchMovies(isLoadMore = true)
    }

    fun retry() {
        fetchMovies(isLoadMore = currentPage > 1)
    }

    private fun fetchMovies(isLoadMore: Boolean = false) {
        if (isFetching) return
        isFetching = true

        viewModelScope.launch {
            repository.getMoviesByCategory(currentCategory.endpoint, currentPage)
                .onSuccess { newMovies ->
                    isFetching = false
                    allMovies.addAll(newMovies)
                    _uiState.value = HomeUiState.Success(
                        movies = allMovies.toList(),
                        isLoadingMore = false
                    )
                }
                .onFailure { throwable ->
                    isFetching = false
                    if (isLoadMore) {
                        currentPage--
                        val currentState = _uiState.value
                        if (currentState is HomeUiState.Success) {
                            _uiState.value = currentState.copy(isLoadingMore = false)
                        }
                    } else {
                        _uiState.value = HomeUiState.Error(
                            throwable.localizedMessage ?: "Failed to load movies"
                        )
                    }
                }
        }
    }
}
