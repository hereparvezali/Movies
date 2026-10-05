package com.movies.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.movies.R
import com.movies.databinding.FragmentHomeBinding
import com.movies.details.FragmentDetails
import kotlinx.coroutines.launch

class FragmentHome : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()
    private lateinit var movieAdapter: MovieAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupListeners()
        observeUiState()
    }

    private fun setupRecyclerView() {
        movieAdapter = MovieAdapter { movie ->
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, FragmentDetails.newInstance(movie.id))
                .addToBackStack(null)
                .commit()
        }
        binding.rvMovies.adapter = movieAdapter
    }

    private fun setupListeners() {
        binding.btnRetry.setOnClickListener {
            viewModel.fetchPopularMovies()
        }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is HomeUiState.Loading -> {
                            binding.progressBar.isVisible = true
                            binding.layoutError.isVisible = false
                        }
                        is HomeUiState.Success -> {
                            binding.progressBar.isVisible = false
                            binding.layoutError.isVisible = false
                            binding.rvMovies.isVisible = true
                            movieAdapter.submitList(state.movies)
                        }
                        is HomeUiState.Error -> {
                            binding.progressBar.isVisible = false
                            binding.layoutError.isVisible = true
                            binding.tvErrorMessage.text = state.message
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}