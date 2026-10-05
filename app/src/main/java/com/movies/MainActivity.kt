package com.movies

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.google.firebase.messaging.FirebaseMessaging
import com.movies.databinding.ActivityMainBinding
import com.movies.details.FragmentDetails
import com.movies.home.FragmentHome
import com.movies.notification.NotificationHelper
import com.movies.profile.FragmentProfile
import com.movies.search.FragmentSearch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                Log.d("FCM_TOKEN", "Notification permission granted.")
            } else {
                Log.w("FCM_TOKEN", "Notification permission denied.")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup notification channel
        NotificationHelper.createNotificationChannel(this)

        // Request runtime permission for Android 13+
        askNotificationPermission()

        // Fetch and log the device's FCM token
        fetchFcmToken()

        // Initialize bottom navigation
        setupBottomNavigation()

        if (savedInstanceState == null) {
            replaceFragment(FragmentHome())
            handleMovieIntent(intent)
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    replaceFragment(FragmentHome())
                    true
                }
                R.id.nav_search -> {
                    replaceFragment(FragmentSearch())
                    true
                }
                R.id.nav_profile -> {
                    replaceFragment(FragmentProfile())
                    true
                }
                else -> false
            }
        }

        binding.bottomNavigation.setOnItemReselectedListener {
            // Keep current screen steady on re-select
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleMovieIntent(intent)
    }

    private fun handleMovieIntent(intent: Intent?) {
        if (intent == null) return

        val movieId = when {
            intent.hasExtra("EXTRA_MOVIE_ID") -> intent.getIntExtra("EXTRA_MOVIE_ID", -1)
            intent.hasExtra("movie_id") -> intent.getStringExtra("movie_id")?.toIntOrNull() ?: -1
            intent.hasExtra("movieId") -> intent.getStringExtra("movieId")?.toIntOrNull() ?: -1
            intent.hasExtra("id") -> intent.getStringExtra("id")?.toIntOrNull() ?: -1
            else -> -1
        }

        if (movieId != -1) {
            Log.d("FCM_DEMO", "Opening movie details from notification: movieId=$movieId")
            supportFragmentManager.beginTransaction()
                .replace(binding.fragmentContainer.id, FragmentDetails.newInstance(movieId))
                .addToBackStack(null)
                .commit()
        }
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun fetchFcmToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("FCM_TOKEN", "Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }
            val token = task.result
            Log.d("FCM_TOKEN", "====================================================")
            Log.d("FCM_TOKEN", "YOUR FCM DEVICE TOKEN:\n$token")
            Log.d("FCM_TOKEN", "====================================================")
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        if (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        }
        supportFragmentManager.beginTransaction()
            .replace(binding.fragmentContainer.id, fragment)
            .commit()
    }
}