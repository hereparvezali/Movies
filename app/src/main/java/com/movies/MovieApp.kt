package com.movies

import android.app.Application

class MovieApp : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: MovieApp
            private set
    }
}
