package com.movies.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.movies.data.model.Movie
import com.movies.data.repository.MovieRepository
import com.movies.data.repository.MovieRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FavoritesViewModel(
    private val repository: MovieRepository = MovieRepositoryImpl()
) : ViewModel() {

    private val _favorites = MutableStateFlow<List<Movie>>(emptyList())
    val favorites: StateFlow<List<Movie>> = _favorites.asStateFlow()

    init {
        loadFavorites()
    }

    private fun loadFavorites() {
        viewModelScope.launch {
            repository.getFavoriteMovies().collect { movies ->
                _favorites.value = movies
            }
        }
    }

    fun removeFavorite(movieId: Int) {
        viewModelScope.launch {
            repository.removeFavorite(movieId)
        }
    }
}
