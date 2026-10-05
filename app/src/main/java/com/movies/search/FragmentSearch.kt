package com.movies.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.movies.R
import com.movies.databinding.FragmentSearchBinding
import com.movies.details.FragmentDetails
import com.movies.home.MovieAdapter
import kotlinx.coroutines.launch

class FragmentSearch : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SearchViewModel by viewModels()
    private lateinit var movieAdapter: MovieAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
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
        binding.rvSearchResults.adapter = movieAdapter
    }

    private fun setupListeners() {
        binding.btnSearch.setOnClickListener {
            val query = binding.etSearch.text.toString()
            viewModel.searchMovies(query)
        }

        binding.etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = binding.etSearch.text.toString()
                viewModel.searchMovies(query)
                true
            } else {
                false
            }
        }

        binding.btnRetry.setOnClickListener {
            viewModel.retry()
        }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is SearchUiState.Idle -> {
                            binding.progressBar.isVisible = false
                            binding.rvSearchResults.isVisible = false
                            binding.tvEmpty.isVisible = false
                            binding.layoutError.isVisible = false
                        }
                        is SearchUiState.Loading -> {
                            binding.progressBar.isVisible = true
                            binding.rvSearchResults.isVisible = false
                            binding.tvEmpty.isVisible = false
                            binding.layoutError.isVisible = false
                        }
                        is SearchUiState.Success -> {
                            binding.progressBar.isVisible = false
                            binding.layoutError.isVisible = false
                            if (state.movies.isEmpty()) {
                                binding.rvSearchResults.isVisible = false
                                binding.tvEmpty.isVisible = true
                            } else {
                                binding.rvSearchResults.isVisible = true
                                binding.tvEmpty.isVisible = false
                                movieAdapter.submitList(state.movies)
                            }
                        }
                        is SearchUiState.Error -> {
                            binding.progressBar.isVisible = false
                            binding.rvSearchResults.isVisible = false
                            binding.tvEmpty.isVisible = false
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