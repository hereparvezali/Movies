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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import com.movies.R
import com.movies.data.model.MovieCategory
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

        setupTabs()
        setupRecyclerView()
        setupListeners()
        observeUiState()
    }

    private fun setupTabs() {
        val categories = MovieCategory.values()
        categories.forEach { category ->
            val tab = binding.tabLayoutCategories.newTab().setText(category.displayName)
            binding.tabLayoutCategories.addTab(tab)
        }

        binding.tabLayoutCategories.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val selectedIndex = tab?.position ?: return
                if (selectedIndex in categories.indices) {
                    viewModel.selectCategory(categories[selectedIndex])
                    binding.rvMovies.scrollToPosition(0)
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupRecyclerView() {
        movieAdapter = MovieAdapter { movie ->
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, FragmentDetails.newInstance(movie.id))
                .addToBackStack(null)
                .commit()
        }
        binding.rvMovies.adapter = movieAdapter

        // Infinite scrolling: load next page when scrolled near the end
        binding.rvMovies.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy > 0) { // Only when scrolling downward
                    val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return
                    val visibleItemCount = layoutManager.childCount
                    val totalItemCount = layoutManager.itemCount
                    val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                    // If remaining items are 4 or fewer, load next page
                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 4) {
                        viewModel.loadNextPage()
                    }
                }
            }
        })
    }

    private fun setupListeners() {
        binding.btnRetry.setOnClickListener {
            viewModel.retry()
        }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is HomeUiState.Loading -> {
                            binding.progressBar.isVisible = true
                            binding.progressBarBottom.isVisible = false
                            binding.layoutError.isVisible = false
                        }
                        is HomeUiState.Success -> {
                            binding.progressBar.isVisible = false
                            binding.layoutError.isVisible = false
                            binding.rvMovies.isVisible = true
                            binding.progressBarBottom.isVisible = state.isLoadingMore
                            movieAdapter.submitList(state.movies)
                        }
                        is HomeUiState.Error -> {
                            binding.progressBar.isVisible = false
                            binding.progressBarBottom.isVisible = false
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