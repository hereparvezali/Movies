package com.movies.details

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import coil.load
import com.movies.R
import com.movies.data.model.MovieDetails
import com.movies.databinding.FragmentDetailsBinding
import kotlinx.coroutines.launch

class FragmentDetails : Fragment() {

    private var _binding: FragmentDetailsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DetailsViewModel by viewModels()
    private var isFavourite = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val movieId = arguments?.getInt(ARG_MOVIE_ID, -1) ?: -1

        setupListeners()
        observeUiState(movieId)

        if (viewModel.uiState.value is DetailsUiState.Loading && movieId != -1) {
            viewModel.loadMovieDetails(movieId)
        }
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.btnRetry.setOnClickListener {
            viewModel.retry()
        }

        binding.btnFavourite.setOnClickListener {
            val currentState = viewModel.uiState.value
            if (currentState is DetailsUiState.Success) {
                viewModel.toggleFavorite(currentState.movie, isFavourite)
                val messageRes = if (!isFavourite) {
                    R.string.added_to_favourites
                } else {
                    R.string.removed_from_favourites
                }
                Toast.makeText(requireContext(), messageRes, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun observeUiState(movieId: Int) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isFavorite(movieId).collect { fav ->
                    isFavourite = fav
                    updateFavouriteButton(fav)
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is DetailsUiState.Loading -> {
                            binding.progressBar.isVisible = true
                            binding.layoutError.isVisible = false
                            binding.layoutContent.isVisible = false
                        }
                        is DetailsUiState.Success -> {
                            binding.progressBar.isVisible = false
                            binding.layoutError.isVisible = false
                            binding.layoutContent.isVisible = true
                            bindMovieDetails(state.movie)
                        }
                        is DetailsUiState.Error -> {
                            binding.progressBar.isVisible = false
                            binding.layoutError.isVisible = true
                            binding.layoutContent.isVisible = false
                            binding.tvErrorMessage.text = state.message
                        }
                    }
                }
            }
        }
    }

    private fun bindMovieDetails(movie: MovieDetails) {
        binding.tvTitle.text = movie.title
        binding.tvMeta.text = "${movie.releaseYear} • ${movie.runtime} • ★ ${movie.rating}"

        if (movie.genres.isNotEmpty()) {
            binding.tvGenres.isVisible = true
            binding.tvGenres.text = "Genres: " + movie.genres.joinToString(", ")
        } else {
            binding.tvGenres.isVisible = false
        }

        binding.tvOverview.text = movie.overview.ifBlank {
            getString(R.string.no_overview_available)
        }

        binding.ivPoster.load(movie.backdropUrl ?: movie.posterUrl) {
            crossfade(true)
            placeholder(R.drawable.bg_poster_placeholder)
            error(R.drawable.bg_poster_placeholder)
        }
    }

    private fun updateFavouriteButton(isFav: Boolean) {
        if (isFav) {
            binding.btnFavourite.setText(R.string.remove_from_favourites)
        } else {
            binding.btnFavourite.setText(R.string.add_to_favourites)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_MOVIE_ID = "arg_movie_id"

        fun newInstance(movieId: Int): FragmentDetails {
            return FragmentDetails().apply {
                arguments = Bundle().apply {
                    putInt(ARG_MOVIE_ID, movieId)
                }
            }
        }
    }
}
