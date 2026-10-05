package com.movies.profile

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
import com.movies.R
import com.movies.databinding.FragmentProfileBinding
import com.movies.details.FragmentDetails
import kotlinx.coroutines.launch

class FragmentProfile : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfileViewModel by viewModels()
    private lateinit var favoriteAdapter: FavoriteMovieAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeFavorites()
    }

    private fun setupRecyclerView() {
        favoriteAdapter = FavoriteMovieAdapter(
            onMovieClick = { movie ->
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, FragmentDetails.newInstance(movie.id))
                    .addToBackStack(null)
                    .commit()
            },
            onDeleteClick = { movie ->
                viewModel.removeFavorite(movie.id)
                Toast.makeText(requireContext(), R.string.removed_from_favourites, Toast.LENGTH_SHORT).show()
            }
        )
        binding.rvFavorites.adapter = favoriteAdapter
    }

    private fun observeFavorites() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.favorites.collect { movies ->
                    if (movies.isEmpty()) {
                        binding.rvFavorites.isVisible = false
                        binding.tvEmptyMessage.isVisible = true
                    } else {
                        binding.rvFavorites.isVisible = true
                        binding.tvEmptyMessage.isVisible = false
                        favoriteAdapter.submitList(movies)
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