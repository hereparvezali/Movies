package com.movies.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.movies.R
import com.movies.data.model.Movie
import com.movies.databinding.ItemMovieBinding

class MovieAdapter(
    private val onMovieClick: (Movie) -> Unit = {}
) : ListAdapter<Movie, MovieAdapter.MovieViewHolder>(MovieDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MovieViewHolder {
        val binding = ItemMovieBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MovieViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MovieViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class MovieViewHolder(
        private val binding: ItemMovieBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(movie: Movie) {
            binding.tvTitle.text = movie.title
            binding.tvGenreAndYear.text = movie.releaseYear
            binding.tvRating.text = "★ ${movie.rating}"

            binding.ivPoster.load(movie.posterUrl) {
                crossfade(true)
                placeholder(R.drawable.bg_poster_placeholder)
                error(R.drawable.bg_poster_placeholder)
            }

            binding.root.setOnClickListener {
                onMovieClick(movie)
            }
        }
    }

    companion object MovieDiffCallback : DiffUtil.ItemCallback<Movie>() {
        override fun areItemsTheSame(oldItem: Movie, newItem: Movie): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Movie, newItem: Movie): Boolean {
            return oldItem == newItem
        }
    }
}
