package com.movies

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.movies.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        if (savedInstanceState == null){
            replaceFragment(FragmentHome())
        }

        binding.homeButton.setOnClickListener {
            replaceFragment(FragmentHome())
        }
        binding.searchButton.setOnClickListener {
            replaceFragment(FragmentSearch())
        }
        binding.profileButton.setOnClickListener {
            replaceFragment(FragmentProfile())
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction().replace(binding.fragmentContainer.id, fragment)
            .commit()

    }
}